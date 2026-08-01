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
