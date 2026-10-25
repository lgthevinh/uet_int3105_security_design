#!/usr/bin/env python3
"""
Compares response time of calling services directly vs through the gateway.

Each scenario sends the same request N times and prints latency stats as a Markdown table.
Requests are paced below the gateway rate limit (10 req/s), otherwise the gateway runs
would mostly measure 429 responses. Direct calls use the same pacing so both sides are equal.

Usage: python3 scripts/bench.py [-n 100] [--interval 0.12] [--warmup 10]
Needs the stack running (make compose). Python 3 standard library only.
"""

import argparse
import json
import statistics
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor

GATEWAY = "http://localhost:8080"
USER_SERVICE = "http://localhost:8081"
ORDER_SERVICE = "http://localhost:8082"

EMAIL = "alice@example.com"
PASSWORD = "password123"


def call(url, headers=None, method="GET", body=None):
    """Sends one request and returns (status, seconds, parsed body or None)."""
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers=headers or {})
    if data is not None:
        req.add_header("Content-Type", "application/json")
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            raw = resp.read()
            status = resp.status
    except urllib.error.HTTPError as e:
        raw = e.read()
        status = e.code
    elapsed = time.perf_counter() - start
    try:
        parsed = json.loads(raw) if raw else None
    except ValueError:
        parsed = None
    return status, elapsed, parsed


def login():
    """Logs in as the seeded user, needed because every gateway route except auth requires a JWT."""
    status, _, body = call(f"{GATEWAY}/api/v1/auth/login", method="POST",
                           body={"email": EMAIL, "password": PASSWORD})
    if status != 200:
        raise SystemExit(f"login failed with {status}, is the stack running? (make compose)")
    return body["token"]


def client_aggregation(user_id, parallel):
    """
    Builds the dashboard on the client by calling both services directly.
    This is the alternative the gateway aggregation replaces, so it is the fair baseline for /dashboard.
    """
    urls = [f"{USER_SERVICE}/users/{user_id}", f"{ORDER_SERVICE}/orders?userId={user_id}"]
    start = time.perf_counter()
    if parallel:
        with ThreadPoolExecutor(max_workers=2) as pool:
            results = list(pool.map(call, urls))
    else:
        results = [call(u) for u in urls]
    elapsed = time.perf_counter() - start
    # Worst status wins so a failed part counts as an error
    status = max(r[0] for r in results)
    return status, elapsed


def run(name, fn, n, warmup, interval):
    """Runs one scenario: warm-up calls first (JVM JIT, connection setup), then n timed calls."""
    for _ in range(warmup):
        fn()
        time.sleep(interval)

    times, errors = [], 0
    for _ in range(n):
        status, elapsed = fn()
        if status == 200:
            times.append(elapsed * 1000)
        else:
            errors += 1
        time.sleep(interval)
    return name, times, errors


def percentile(values, p):
    ordered = sorted(values)
    index = min(len(ordered) - 1, round(p / 100 * (len(ordered) - 1)))
    return ordered[index]


def print_table(rows):
    print("| Scenario | OK | Errors | Min | Mean | P50 | P95 | P99 | Max |")
    print("|---|---|---|---|---|---|---|---|---|")
    for name, times, errors in rows:
        if not times:
            print(f"| {name} | 0 | {errors} | - | - | - | - | - | - |")
            continue
        stats = [min(times), statistics.mean(times), percentile(times, 50),
                 percentile(times, 95), percentile(times, 99), max(times)]
        cells = " | ".join(f"{v:.2f}" for v in stats)
        print(f"| {name} | {len(times)} | {errors} | {cells} |")
    print("\nTimes in ms. Errors are non-200 responses (429 means the rate limit was hit).")


def main():
    parser = argparse.ArgumentParser(description="Direct vs gateway response time")
    parser.add_argument("-n", type=int, default=100, help="timed requests per scenario")
    parser.add_argument("--warmup", type=int, default=10, help="untimed requests per scenario")
    parser.add_argument("--interval", type=float, default=0.12,
                        help="seconds between requests, keep >= 0.1 to stay under 10 req/s")
    args = parser.parse_args()

    token = login()
    auth = {"Authorization": f"Bearer {token}"}
    user_id = call(f"{GATEWAY}/api/v1/dashboard", headers=auth)[2]["user"]["id"]
    time.sleep(args.interval)

    def simple(url, headers=None):
        return lambda: call(url, headers)[:2]

    scenarios = [
        ("user: direct", simple(f"{USER_SERVICE}/users/{user_id}")),
        ("user: gateway", simple(f"{GATEWAY}/api/v1/users/{user_id}", auth)),
        ("orders: direct", simple(f"{ORDER_SERVICE}/orders?userId={user_id}")),
        ("orders: gateway", simple(f"{GATEWAY}/api/v1/orders?userId={user_id}", auth)),
        ("dashboard: direct x2 sequential", lambda: client_aggregation(user_id, parallel=False)),
        ("dashboard: direct x2 parallel", lambda: client_aggregation(user_id, parallel=True)),
        ("dashboard: gateway aggregation", simple(f"{GATEWAY}/api/v1/dashboard", auth)),
    ]

    est = len(scenarios) * (args.n + args.warmup) * args.interval
    print(f"Running {len(scenarios)} scenarios x {args.n} requests (~{est:.0f}s)...\n")

    rows = []
    for name, fn in scenarios:
        rows.append(run(name, fn, args.n, args.warmup, args.interval))
    print_table(rows)


if __name__ == "__main__":
    main()
