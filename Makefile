.PHONY: compose down bench

compose:
	docker compose up -d --build

down:
	docker compose down

# $$ escapes make, so the shell fills in the unix time when the recipe runs
bench:
	python ./scripts/bench.py --csv result_$$(date +%s).csv
