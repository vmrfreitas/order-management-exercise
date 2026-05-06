#!/bin/bash
set -e

# Configuration
APP_URL="http://localhost:8080"
WAIT_TIME=60

# Function to run docker-compose (v2 or v1)
run_dc() {
  # Try to find absolute paths because sudo might have a limited PATH
  DOCKER_BIN=$(which docker 2>/dev/null || echo "/usr/bin/docker")
  
  # Fallback to the local path found in the user session
  DOCKER_COMPOSE_BIN=$(which docker-compose 2>/dev/null || echo "/home/vmrfreitas/.local/bin/docker-compose")

  if $DOCKER_BIN compose version >/dev/null 2>&1; then
    $DOCKER_BIN compose "$@"
  elif $DOCKER_COMPOSE_BIN version >/dev/null 2>&1; then
    $DOCKER_COMPOSE_BIN "$@"
  else
    echo "❌ Error: Neither 'docker compose' nor 'docker-compose' was found."
    echo "Current PATH: $PATH"
    exit 1
  fi
}

echo "🚀 Starting containers..."
run_dc up --build -d

echo "⏳ Waiting for app to be ready (up to $WAIT_TIME seconds)..."
for i in $(seq 1 $WAIT_TIME); do
  if curl -s "$APP_URL/orders" > /dev/null; then
    echo "✅ App is up!"
    READY=true
    break
  fi
  sleep 1
done

if [ "$READY" != "true" ]; then
  echo "❌ App failed to start within $WAIT_TIME seconds."
  run_dc logs app
  exit 1
fi

echo "📦 Creating a test order..."
RESPONSE=$(curl -s -X POST "$APP_URL/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "customer": "Verification Bot",
    "shippingAddress": "123 Storage Ave, New York, NY 10001",
    "creditCardNumber": "4111111111111111",
    "items": [
      { "productId": "550e8400-e29b-41d4-a716-446655440001", "quantity": 1 },
      { "productId": "550e8400-e29b-41d4-a716-446655440002", "quantity": 2 }
    ]
  }')

echo "Response: $RESPONSE"

# Extract Order ID (assuming format "Order created with ID: <UUID>. ...")
ORDER_ID=$(echo "$RESPONSE" | grep -oE '[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}' | head -n 1)

if [ -z "$ORDER_ID" ]; then
  echo "❌ Failed to extract Order ID from response."
  exit 1
fi

echo "🔍 Order ID: $ORDER_ID"

echo "⏳ Waiting for fulfillment (processing is async)..."
sleep 5

echo "📜 Checking app logs for payment and fulfillment messages..."
run_dc logs app | grep -E "Processing payment|fulfilled"

echo "📊 Verifying final order status..."
curl -s "$APP_URL/orders/$ORDER_ID" | json_pp || curl -s "$APP_URL/orders/$ORDER_ID"

echo "🏁 End-to-end verification complete."
