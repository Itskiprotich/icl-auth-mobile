#!/usr/bin/env bash
set -euo pipefail

bundle_path=${1:-}
deployment_name=${2:-$(basename "${bundle_path:-central-bundle.zip}")}
base_url=${MAVEN_CENTRAL_BASE_URL:-https://central.sonatype.com}
poll_interval=${MAVEN_CENTRAL_POLL_INTERVAL_SECONDS:-10}
timeout_seconds=${MAVEN_CENTRAL_TIMEOUT_SECONDS:-1800}

if [[ -z "$bundle_path" ]]; then
  echo "Usage: $0 <bundle-path> [deployment-name]" >&2
  exit 1
fi

if [[ ! -f "$bundle_path" ]]; then
  echo "Bundle not found: $bundle_path" >&2
  exit 1
fi

: "${MAVEN_CENTRAL_USERNAME:?Set MAVEN_CENTRAL_USERNAME}"
: "${MAVEN_CENTRAL_PASSWORD:?Set MAVEN_CENTRAL_PASSWORD}"

auth_token=$(
  printf '%s:%s' "$MAVEN_CENTRAL_USERNAME" "$MAVEN_CENTRAL_PASSWORD" | base64 | tr -d '\r\n'
)
encoded_name=$(
  python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1]))' "$deployment_name"
)

deployment_id=$(
  curl --fail-with-body --silent --show-error \
    --request POST \
    --header "Authorization: Bearer $auth_token" \
    --form "bundle=@${bundle_path};type=application/octet-stream" \
    "${base_url}/api/v1/publisher/upload?name=${encoded_name}&publishingType=AUTOMATIC"
)
deployment_id=$(printf '%s' "$deployment_id" | tr -d '\r\n')

echo "Deployment ID: $deployment_id"

deadline=$(( $(date +%s) + timeout_seconds ))
last_response=''

while true; do
  last_response=$(
    curl --fail-with-body --silent --show-error \
      --request POST \
      --header "Authorization: Bearer $auth_token" \
      --header "Accept: application/json" \
      "${base_url}/api/v1/publisher/status?id=${deployment_id}"
  )

  deployment_state=$(
    python3 - "$last_response" <<'PY'
import json
import sys

payload = json.loads(sys.argv[1])
print(payload.get("deploymentState", ""))
PY
  )

  echo "Current deployment state: $deployment_state"

  case "$deployment_state" in
    PUBLISHED)
      python3 - "$last_response" <<'PY'
import json
import sys

payload = json.loads(sys.argv[1])

for purl in payload.get("purls", []):
  print(f"PURL: {purl}")

for central_path in payload.get("centralPaths", []):
  print(f"Central path: {central_path}")
PY
      break
      ;;
    FAILED)
      python3 - "$last_response" <<'PY'
import json
import sys

payload = json.loads(sys.argv[1])
errors = payload.get("errors") or ["Deployment failed without an error payload."]

for error in errors:
  if isinstance(error, dict):
    print(json.dumps(error, indent=2, sort_keys=True))
  else:
    print(error)
PY
      exit 1
      ;;
  esac

  if (( $(date +%s) >= deadline )); then
    echo "Timed out waiting for Maven Central to publish deployment $deployment_id" >&2
    exit 1
  fi

  sleep "$poll_interval"
done
