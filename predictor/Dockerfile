FROM python:3.10-slim AS builder

WORKDIR /build

COPY requirements.txt ./

RUN python -m pip install \
    --disable-pip-version-check \
    --no-cache-dir \
    --prefix=/install \
    -r requirements.txt

FROM python:3.10-slim AS runtime

RUN groupadd --system app \
    && useradd --system --gid app --no-create-home app

WORKDIR /app

COPY --from=builder /install /usr/local
COPY --chown=app:app app ./app

USER app

EXPOSE 8000

HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=3 \
    CMD ["python", "-c", "import urllib.request; urllib.request.urlopen('http://localhost:8000/health', timeout=2)"]

ENTRYPOINT ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
