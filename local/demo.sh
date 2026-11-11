#!/usr/bin/env bash
# Runs the end-to-end scenario against a running service: register and publish an API, register a consumer,
# subscribe, issue a key, validate it, revoke it, validate again.
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}/api/v1"
SUFFIX="$(date +%s)"
RESPONSE=""

# call EXPECTED_STATUS METHOD PATH [JSON_BODY] - prints the exchange and stores the body in RESPONSE
call() {
  local expected=$1 method=$2 path=$3 body=${4:-} out status
  if [ -n "$body" ]; then
    out=$(curl -sS -w '\n%{http_code}' -X "$method" "$BASE$path" -H 'Content-Type: application/json' -d "$body")
  else
    out=$(curl -sS -w '\n%{http_code}' -X "$method" "$BASE$path")
  fi
  status=${out##*$'\n'}
  RESPONSE=${out%$'\n'*}
  echo
  echo "== $method $path -> $status"
  if [ -n "$RESPONSE" ]; then echo "$RESPONSE" | python3 -m json.tool; fi
  if [ "$status" != "$expected" ]; then
    echo "FAILED: expected HTTP $expected" >&2
    exit 1
  fi
}

# jget FIELD - reads a top-level field from RESPONSE
jget() { python3 -c 'import json,sys; print(json.loads(sys.argv[1])[sys.argv[2]])' "$RESPONSE" "$1"; }

echo "Step 1: register an API (it starts as DRAFT)"
call 201 POST /apis "{\"name\":\"orders-$SUFFIX\",\"version\":\"v1\",\"basePath\":\"/orders\",\"ownerTeam\":\"team-a\"}"
API_ID=$(jget id)

echo; echo "Step 2: publish it (the rowVersion we read proves we saw the latest state)"
call 200 POST "/apis/$API_ID/status" "{\"status\":\"PUBLISHED\",\"rowVersion\":$(jget rowVersion)}"

echo; echo "Step 3: register a consumer"
call 201 POST /consumers "{\"name\":\"acme-$SUFFIX\",\"contactEmail\":\"ops@example.com\"}"
CONSUMER_ID=$(jget id)

echo; echo "Step 4: subscribe the consumer to the API on the PREMIUM plan"
call 201 POST /subscriptions "{\"apiId\":\"$API_ID\",\"consumerId\":\"$CONSUMER_ID\",\"plan\":\"PREMIUM\"}"
SUBSCRIPTION_ID=$(jget id)

echo; echo "Step 5: issue an API key (the only time the plaintext is shown)"
call 201 POST "/subscriptions/$SUBSCRIPTION_ID/keys"
API_KEY=$(jget apiKey)
KEY_ID=$(jget id)

echo; echo "Step 6: validate the key, as a gateway would"
call 200 POST /keys/validate "{\"apiKey\":\"$API_KEY\",\"apiId\":\"$API_ID\"}"
[ "$(jget valid)" = "True" ] || { echo "FAILED: the key should be valid" >&2; exit 1; }

echo; echo "Step 7: revoke the key"
call 204 POST "/keys/$KEY_ID/revoke"

echo; echo "Step 8: validate again (still HTTP 200, but valid is false)"
call 200 POST /keys/validate "{\"apiKey\":\"$API_KEY\",\"apiId\":\"$API_ID\"}"
[ "$(jget valid)" = "False" ] && [ "$(jget reason)" = "REVOKED" ] || { echo "FAILED: the key should be REVOKED" >&2; exit 1; }

echo; echo "Demo complete: the key validated before revocation and failed after it."
