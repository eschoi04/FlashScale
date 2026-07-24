#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPOSITORY_ROOT=$(CDPATH= cd -- "${SCRIPT_DIR}/.." && pwd)
PYTHON="${REPOSITORY_ROOT}/predictor/.venv/bin/python"

if [ ! -x "${PYTHON}" ]; then
	echo "Python virtual environment not found: ${PYTHON}" >&2
	echo "Create it and install predictor/requirements-test.txt first." >&2
	exit 1
fi

echo "==> Verifying Spring formatting, static analysis, and tests"
(
	cd "${REPOSITORY_ROOT}/ticketing-api"
	./gradlew spotlessCheck checkstyleMain checkstyleTest test
)

echo "==> Verifying Python formatting"
(
	cd "${REPOSITORY_ROOT}/predictor"
	"${PYTHON}" -m ruff format --check .
)

echo "==> Verifying Python lint"
(
	cd "${REPOSITORY_ROOT}/predictor"
	"${PYTHON}" -m ruff check .
)

echo "==> Running Python tests"
(
	cd "${REPOSITORY_ROOT}/predictor"
	"${PYTHON}" -m pytest
)

echo "==> All verification checks passed"
