/**
 * Calls one backend through an internal location and parses its JSON body.
 * Returns the status alongside the body so the caller can decide how to degrade.
 */
async function fetchJson(r, uri, args) {
    const reply = await r.subrequest(uri, { method: 'GET', args: args || '' });
    if (reply.status !== 200) {
        return { status: reply.status };
    }
    try {
        return { status: 200, body: JSON.parse(reply.responseText) };
    } catch (e) {
        r.warn('dashboard: invalid JSON from ' + uri + ': ' + e.message);
        return { status: 502 };
    }
}

/**
 * API aggregation: returns the caller's profile and orders in one response.
 * Saves the client two round trips; the user id comes from the verified JWT, never from the request.
 */
async function getDashboard(r) {
    const userId = r.variables.user_id;
    r.headersOut['Content-Type'] = 'application/json';

    // Both backends are called in parallel (njs has no array destructuring, hence the indexes)
    const results = await Promise.all([
        fetchJson(r, '/_internal/users/' + userId),
        fetchJson(r, '/_internal/orders', 'userId=' + userId),
    ]);
    const user = results[0];
    const orders = results[1];

    // Without the user there is no dashboard: 404 if the user is gone, 502 if user-service failed
    if (user.status !== 200) {
        const status = user.status === 404 ? 404 : 502;
        r.return(status, JSON.stringify({ error: status === 404 ? 'user not found' : 'user service unavailable' }));
        return;
    }

    // Orders are optional: still show the profile if order-service fails
    const body = { user: user.body, orders: orders.status === 200 ? orders.body : null };
    if (orders.status !== 200) {
        r.warn('dashboard: orders subrequest failed with ' + orders.status);
        body.errors = ['orders unavailable'];
    }
    r.return(200, JSON.stringify(body));
}

export default { getDashboard };
