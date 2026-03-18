#!/bin/bash
# Smoke-test the Example REST + GraphQL API.
# Usage: ./scripts/api-test.sh [base_url]
# Default: http://localhost:8081

set -euo pipefail

BASE="${1:-http://localhost:8081}"
MGMT="${BASE%:*}:8082"
PASS=0; FAIL=0; TOTAL=0

# ── Helpers ──────────────────────────────────────────────────────────────────

red()    { printf '\033[1;31m%s\033[0m' "$*"; }
green()  { printf '\033[1;32m%s\033[0m' "$*"; }
cyan()   { printf '\033[1;36m%s\033[0m' "$*"; }
bold()   { printf '\033[1m%s\033[0m' "$*"; }
dim()    { printf '\033[2m%s\033[0m' "$*"; }

section() { printf '\n%s\n' "$(cyan "── $1 ──")"; }

assert_status() {
  local label="$1" expected="$2" actual="$3" body="$4"
  TOTAL=$((TOTAL + 1))
  if [ "$actual" = "$expected" ]; then
    PASS=$((PASS + 1))
    printf '  %s %s %s\n' "$(green '✓')" "$label" "$(dim "($actual)")"
  else
    FAIL=$((FAIL + 1))
    printf '  %s %s — expected %s got %s\n' "$(red '✗')" "$label" "$expected" "$actual"
    printf '    %s\n' "$(dim "$body")"
  fi
}

http() {
  local method="$1" path="$2"; shift 2
  curl -s -w '\n%{http_code}' -X "$method" "$BASE$path" \
    -H 'Content-Type: application/json' "$@"
}

graphql() {
  curl -s -w '\n%{http_code}' -X POST "$BASE/graphql" \
    -H 'Content-Type: application/json' \
    -d "$1"
}

split_response() {
  BODY="$(echo "$1" | sed '$d')"
  STATUS="$(echo "$1" | tail -1)"
}

json_field() { echo "$1" | grep -o "\"$2\":[^,}]*" | head -1 | cut -d: -f2- | tr -d '" '; }

# ── Health / Actuator ────────────────────────────────────────────────────────

section "Health & Actuator"

resp=$(curl -s -w '\n%{http_code}' "$MGMT/actuator/health")
split_response "$resp"
assert_status "GET /actuator/health" 200 "$STATUS" "$BODY"

resp=$(curl -s -w '\n%{http_code}' "$MGMT/actuator/prometheus")
split_response "$resp"
assert_status "GET /actuator/prometheus" 200 "$STATUS" "$BODY"

# ── REST: Create ─────────────────────────────────────────────────────────────

section "REST — Create"

resp=$(http POST /examples -d '{"name":"Alpha","description":"First example"}')
split_response "$resp"
assert_status "POST /examples (Alpha)" 201 "$STATUS" "$BODY"
ID_A=$(json_field "$BODY" id)

resp=$(http POST /examples -d '{"name":"Beta"}')
split_response "$resp"
assert_status "POST /examples (Beta, no description)" 201 "$STATUS" "$BODY"
ID_B=$(json_field "$BODY" id)

resp=$(http POST /examples -d '{"name":"Gamma","description":"Third for pagination"}')
split_response "$resp"
assert_status "POST /examples (Gamma)" 201 "$STATUS" "$BODY"
ID_C=$(json_field "$BODY" id)

# ── REST: Create — validation errors ─────────────────────────────────────────

section "REST — Validation errors"

resp=$(http POST /examples -d '{}')
split_response "$resp"
assert_status "POST /examples (missing name)" 400 "$STATUS" "$BODY"

resp=$(http POST /examples -d '{"name":""}')
split_response "$resp"
assert_status "POST /examples (blank name)" 400 "$STATUS" "$BODY"

LONG_NAME=$(printf 'x%.0s' $(seq 1 256))
resp=$(http POST /examples -d "{\"name\":\"$LONG_NAME\"}")
split_response "$resp"
assert_status "POST /examples (name > 255 chars)" 400 "$STATUS" "$BODY"

# ── REST: Get by ID ──────────────────────────────────────────────────────────

section "REST — Get by ID"

resp=$(http GET "/examples/$ID_A")
split_response "$resp"
assert_status "GET /examples/{id} (Alpha)" 200 "$STATUS" "$BODY"

resp=$(http GET "/examples/00000000-0000-0000-0000-000000000000")
split_response "$resp"
assert_status "GET /examples/{id} (not found)" 404 "$STATUS" "$BODY"

resp=$(http GET "/examples/not-a-uuid")
split_response "$resp"
assert_status "GET /examples/{id} (invalid UUID)" 400 "$STATUS" "$BODY"

# ── REST: List & pagination ──────────────────────────────────────────────────

section "REST — List & pagination"

resp=$(http GET "/examples")
split_response "$resp"
assert_status "GET /examples (default)" 200 "$STATUS" "$BODY"

resp=$(http GET "/examples?page=0&size=2")
split_response "$resp"
assert_status "GET /examples?page=0&size=2" 200 "$STATUS" "$BODY"

resp=$(http GET "/examples?page=1&size=2")
split_response "$resp"
assert_status "GET /examples?page=1&size=2 (second page)" 200 "$STATUS" "$BODY"

resp=$(http GET "/examples?sort=name,asc")
split_response "$resp"
assert_status "GET /examples?sort=name,asc" 200 "$STATUS" "$BODY"

resp=$(http GET "/examples?sort=createdAt,desc")
split_response "$resp"
assert_status "GET /examples?sort=createdAt,desc" 200 "$STATUS" "$BODY"

# ── REST: Update ─────────────────────────────────────────────────────────────

section "REST — Update"

resp=$(http PUT "/examples/$ID_A" -d '{"name":"Alpha Updated","description":"Updated desc"}')
split_response "$resp"
assert_status "PUT /examples/{id} (full update)" 200 "$STATUS" "$BODY"

resp=$(http PUT "/examples/$ID_B" -d '{"description":"Now has a description"}')
split_response "$resp"
assert_status "PUT /examples/{id} (partial — add description)" 200 "$STATUS" "$BODY"

resp=$(http PUT "/examples/00000000-0000-0000-0000-000000000000" -d '{"name":"Ghost"}')
split_response "$resp"
assert_status "PUT /examples/{id} (not found)" 404 "$STATUS" "$BODY"

# ── GraphQL: Queries ─────────────────────────────────────────────────────────

section "GraphQL — Queries"

resp=$(graphql '{"query":"{ examples { id name description } }"}')
split_response "$resp"
assert_status "query { examples }" 200 "$STATUS" "$BODY"

resp=$(graphql '{"query":"{ examples(page: 0, size: 1) { id name } }"}')
split_response "$resp"
assert_status "query { examples(page:0, size:1) }" 200 "$STATUS" "$BODY"

resp=$(graphql "{\"query\":\"{ example(id: \\\"$ID_A\\\") { id name description version createdAt updatedAt } }\"}")
split_response "$resp"
assert_status "query { example(id) }" 200 "$STATUS" "$BODY"

resp=$(graphql '{"query":"{ example(id: \"00000000-0000-0000-0000-000000000000\") { id } }"}')
split_response "$resp"
assert_status "query { example } (not found → null)" 200 "$STATUS" "$BODY"

# ── GraphQL: Mutations ───────────────────────────────────────────────────────

section "GraphQL — Mutations"

resp=$(graphql '{"query":"mutation { createExample(input: { name: \"Delta\", description: \"Via GraphQL\" }) { id name description } }"}')
split_response "$resp"
assert_status "mutation { createExample }" 200 "$STATUS" "$BODY"
ID_D=$(echo "$BODY" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

resp=$(graphql "{\"query\":\"mutation { updateExample(id: \\\"$ID_D\\\", input: { name: \\\"Delta Updated\\\" }) { id name } }\"}")
split_response "$resp"
assert_status "mutation { updateExample }" 200 "$STATUS" "$BODY"

resp=$(graphql "{\"query\":\"mutation { deleteExample(id: \\\"$ID_D\\\") }\"}")
split_response "$resp"
assert_status "mutation { deleteExample }" 200 "$STATUS" "$BODY"

resp=$(graphql '{"query":"mutation { createExample(input: { name: \"\" }) { id } }"}')
split_response "$resp"
assert_status "mutation { createExample } (blank name → error)" 200 "$STATUS" "$BODY"

# ── REST: Delete ─────────────────────────────────────────────────────────────

section "REST — Delete"

resp=$(http DELETE "/examples/$ID_A")
split_response "$resp"
assert_status "DELETE /examples/{id} (Alpha)" 204 "$STATUS" "$BODY"

resp=$(http DELETE "/examples/$ID_A")
split_response "$resp"
assert_status "DELETE /examples/{id} (already deleted)" 404 "$STATUS" "$BODY"

resp=$(http DELETE "/examples/$ID_B")
split_response "$resp"
assert_status "DELETE /examples/{id} (Beta)" 204 "$STATUS" "$BODY"

resp=$(http DELETE "/examples/$ID_C")
split_response "$resp"
assert_status "DELETE /examples/{id} (Gamma)" 204 "$STATUS" "$BODY"

# ── REST: 404 / unknown routes ───────────────────────────────────────────────

section "REST — Unknown routes"

resp=$(http GET "/nonexistent")
split_response "$resp"
assert_status "GET /nonexistent" 404 "$STATUS" "$BODY"

resp=$(http POST "/examples/unknown/path" -d '{}')
split_response "$resp"
assert_status "POST /examples/unknown/path" 404 "$STATUS" "$BODY"

# ── Summary ──────────────────────────────────────────────────────────────────

printf '\n%s\n' "$(bold "Results: $TOTAL tests — $(green "$PASS passed"), $([ "$FAIL" -gt 0 ] && red "$FAIL failed" || echo "$FAIL failed")")"
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
