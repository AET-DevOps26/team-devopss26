#!/usr/bin/env bash
set -eu

# Loop through all yaml and yml files in the api directory
for spec in api/*.yaml api/*.yml; do
  # Avoid error if no files match
  if [[ ! -f "$spec" ]]; then
    continue
  fi

  filename=$(basename "$spec")
  service_name="${filename%.*}"

  echo "=== Processing spec: $filename ==="

  # 1. Backend Code Generation
  backend_dir="services/$service_name"
  if [[ "$service_name" == "web-client" ]]; then
    echo "Skipping backend generation for web-client (Frontend only)"
  elif [[ -d "$backend_dir" ]]; then
    if [[ "$service_name" == "genai-service" ]]; then
      echo "Generating Python (FastAPI) code for $service_name..."
      npx @openapitools/openapi-generator-cli generate -i "$spec" -g python-fastapi \
        -o "$backend_dir/generated" --skip-validate-spec
    else
      echo "Generating Spring Boot code for $service_name..."
      npx @openapitools/openapi-generator-cli generate -i "$spec" -g spring \
        -o "$backend_dir/generated" --skip-validate-spec \
        --global-property=apis,models,supportingFiles=ApiUtil.java \
        --additional-properties=useTags=true,openApiNullable=false,interfaceOnly=true
    fi
  else
    echo "Skipping backend generation (directory $backend_dir does not exist)"
  fi

  # 2. TypeScript Frontend Code
  echo "Generating TypeScript types for $service_name..."
  npx openapi-typescript "$spec" -o "web-client/src/$service_name.ts"
  
  echo ""
done