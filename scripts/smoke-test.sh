#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPOSITORY_ROOT=$(CDPATH= cd -- "${SCRIPT_DIR}/.." && pwd)
COMPOSE_FILE="${REPOSITORY_ROOT}/compose.yaml"
PROJECT_NAME="flashscale-day-06-smoke-$$"

compose() {
	docker compose \
		--project-name "${PROJECT_NAME}" \
		--file "${COMPOSE_FILE}" \
		"$@"
}

cleanup() {
	echo "==> Removing smoke test resources"
	compose down --volumes --remove-orphans
}

trap cleanup EXIT HUP INT TERM

echo "==> Validating Compose configuration"
compose config --quiet

echo "==> Building and starting all services"
compose up --build --detach --wait --wait-timeout 180

echo "==> Checking container health"
for service in ticketing-api predictor postgres; do
	container_id=$(compose ps --quiet "${service}")
	health_status=$(docker inspect --format '{{.State.Health.Status}}' "${container_id}")

	if [ "${health_status}" != "healthy" ]; then
		echo "${service} is not healthy: ${health_status}" >&2
		exit 1
	fi

	echo "${service}: ${health_status}"
done

echo "==> Checking application health endpoints"
ticketing_response=$(curl --fail --silent --show-error \
	http://localhost:18080/actuator/health)
case "${ticketing_response}" in
	*'"db":{"status":"UP"'*) ;;
	*)
		echo "ticketing-api did not report a healthy database: ${ticketing_response}" >&2
		exit 1
		;;
esac

predictor_response=$(curl --fail --silent --show-error \
	http://localhost:18000/health)
if [ "${predictor_response}" != '{"status":"UP"}' ]; then
	echo "Unexpected predictor health response: ${predictor_response}" >&2
	exit 1
fi

echo "ticketing-api: ${ticketing_response}"
echo "predictor: ${predictor_response}"

echo "==> Checking PostgreSQL query execution"
query_result=$(compose exec --no-TTY postgres \
	psql --username flashscale --dbname flashscale \
	--tuples-only --no-align --command 'SELECT 1;')
if [ "${query_result}" != "1" ]; then
	echo "Unexpected PostgreSQL query result: ${query_result}" >&2
	exit 1
fi
echo "postgres: SELECT 1 returned ${query_result}"

echo "==> Checking Flyway migration history"
migration_count=$(compose exec --no-TTY postgres \
	psql --username flashscale --dbname flashscale \
	--tuples-only --no-align \
	--command "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success;")
if [ "${migration_count}" != "1" ]; then
	echo "Unexpected successful V1 migration count: ${migration_count}" >&2
	exit 1
fi
echo "Flyway V1 successful migration count: ${migration_count}"

echo "==> Checking ticketing API flow"
event_response=$(curl --fail --silent --show-error \
	--header 'Content-Type: application/json' \
	--data '{"name":"Compose Smoke Event","seatCount":2}' \
	http://localhost:18080/api/events)
case "${event_response}" in
	*'"name":"Compose Smoke Event"'*'"seatCount":2'*) ;;
	*)
		echo "Unexpected event response: ${event_response}" >&2
		exit 1
		;;
esac

event_id=$(compose exec --no-TTY postgres \
	psql --username flashscale --dbname flashscale \
	--tuples-only --no-align \
	--command "SELECT id FROM events WHERE name = 'Compose Smoke Event';")
seat_id=$(compose exec --no-TTY postgres \
	psql --username flashscale --dbname flashscale \
	--tuples-only --no-align \
	--command "SELECT id FROM seats WHERE event_id = ${event_id} ORDER BY seat_number LIMIT 1;")

reservation_response=$(curl --fail --silent --show-error \
	--header 'Content-Type: application/json' \
	--data '{"customerId":"compose-smoke-user"}' \
	"http://localhost:18080/api/events/${event_id}/seats/${seat_id}/reservations")
case "${reservation_response}" in
	*"\"seatId\":${seat_id}"*'"status":"RESERVED"'*) ;;
	*)
		echo "Unexpected reservation response: ${reservation_response}" >&2
		exit 1
		;;
esac
echo "event: ${event_response}"
echo "reservation: ${reservation_response}"

echo "==> Restarting ticketing-api and checking migration idempotency"
compose restart ticketing-api
attempt=0
restarted_health=
while [ "${attempt}" -lt 60 ]; do
	restarted_health=$(curl --silent http://localhost:18080/actuator/health || true)
	case "${restarted_health}" in
		*'"status":"UP"'*) break ;;
	esac

	attempt=$((attempt + 1))
	sleep 1
done
case "${restarted_health}" in
	*'"status":"UP"'*) ;;
	*)
		echo "ticketing-api did not recover after restart: ${restarted_health}" >&2
		exit 1
		;;
esac

restarted_migration_count=$(compose exec --no-TTY postgres \
	psql --username flashscale --dbname flashscale \
	--tuples-only --no-align \
	--command "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success;")
if [ "${restarted_migration_count}" != "1" ]; then
	echo "V1 migration was unexpectedly reapplied: ${restarted_migration_count}" >&2
	exit 1
fi
echo "Flyway V1 count after restart: ${restarted_migration_count}"

echo "==> Checking Compose service name resolution"
compose exec --no-TTY predictor python -c \
	"import socket; [socket.getaddrinfo(name, None) for name in ('ticketing-api', 'predictor', 'postgres')]"

echo "==> Checking Spring health when PostgreSQL is unavailable"
compose stop postgres

attempt=0
database_failure_response=
while [ "${attempt}" -lt 30 ]; do
	database_failure_response=$(curl --silent \
		http://localhost:18080/actuator/health || true)
	case "${database_failure_response}" in
		*'"db":{"status":"DOWN"'*) break ;;
	esac

	attempt=$((attempt + 1))
	sleep 1
done

case "${database_failure_response}" in
	*'"db":{"status":"DOWN"'*'"status":"DOWN"'*) ;;
	*)
		echo "ticketing-api stayed healthy without PostgreSQL: ${database_failure_response}" >&2
		exit 1
		;;
esac
echo "ticketing-api without postgres: ${database_failure_response}"

echo "==> Smoke test passed"
