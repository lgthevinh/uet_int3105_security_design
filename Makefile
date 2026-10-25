.PHONY: compose

compose:
	docker compose up -d --build

down:
	docker compose down