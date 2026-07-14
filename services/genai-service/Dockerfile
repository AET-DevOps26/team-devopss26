# Stage 1: Generate the FastAPI client/models from api/genai-service.yaml, the same way
# the Spring services generate their API interfaces during `mvn package`.
FROM node:22-bookworm-slim AS codegen
RUN apt-get update && apt-get install -y --no-install-recommends default-jre-headless \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY api/genai-service.yaml api/genai-service.yaml
RUN npx --yes @openapitools/openapi-generator-cli generate \
    -i api/genai-service.yaml -g python-fastapi \
    -o services/genai-service/generated --skip-validate-spec

# Stage 2: Runtime
FROM python:3.12-slim

WORKDIR /app

COPY services/genai-service/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY services/genai-service/main.py .
COPY --from=codegen /app/services/genai-service/generated/src ./generated/src

CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port ${GENAI_SERVICE_APP_PORT:-8006}"]
