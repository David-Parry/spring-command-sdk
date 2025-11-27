#!/bin/bash

# Script: cloud_post.sh
# Purpose: Test AWS CloudWatch webhook endpoint
# Usage: ./cloud_post.sh [log-message]
# Example: ./cloud_post.sh "Test error message"

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Construct URL
URL="http://localhost:8081/api/webhooks/aws/cloud"

# Create JSON payload for CloudWatch Logs
# This simulates a CloudWatch Logs subscription filter message
PAYLOAD='{"jira_project_key" :"SCRUM" , "git_repo_uri":"git@github.com:David-Parry/lambda-farm.git","alert_type":"cloudwatch_logs","severity":"high","log_group":"/aws/lambda/test-function","log_stream":"2024/01/01/[$LATEST]test-stream","event_count":2,"source":"AWS CloudWatch Logs","timestamp":"2025-11-26T20:22:27.493922Z","log_events":[{"id":"event-1","timestamp":1764188544567,"message":"ERROR: For input string: 123abcert  java.lang.NumberFormatException: For input string: 123abcert at java.base/java.lang.NumberFormatException.forInputString(Unknown Source) at java.base/java.lang.Integer.parseInt(Unknown Source) at java.base/java.lang.Integer.parseInt(Unknown Source) at com.davidparry.lambda.MagicNumberHandler.handleRequest(MagicNumberHandler.java:38) at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Unknown Source) at java.base/java.lang.reflect.Method.invoke(Unknown Source) at com.amazonaws.services.lambda.runtime.api.client.EventHandlerLoader$PojoMethodRequestHandler.handleRequest(EventHandlerLoader.java:741) at com.amazonaws.services.lambda.runtime.api.client.EventHandlerLoader$PojoHandlerAsStreamHandler.handleRequest(EventHandlerLoader.java:658) at com.amazonaws.services.lambda.runtime.api.client.EventHandlerLoader$2.call(EventHandlerLoader.java:604) at com.amazonaws.services.lambda.runtime.api.client.AWSLambda.startRuntimeLoop(AWSLambda.java:317) at com.amazonaws.services.lambda.runtime.api.client.AWSLambda.startRuntimeLoops(AWSLambda.java:267) at com.amazonaws.services.lambda.runtime.api.client.AWSLambda.main(AWSLambda.java:224)","timestamp_formatted":"2025-11-26T20:22:24.567Z"},{"id":"event-2","timestamp":1764188544567,"message":"Exception occurred: NullPointerException","timestamp_formatted":"2025-11-26T20:22:24.567Z"}]}'


# Display request information
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Testing AWS CloudWatch Webhook${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Log Message:${NC} $LOG_MESSAGE"
echo -e "${YELLOW}URL:${NC} $URL"
echo -e "${YELLOW}Method:${NC} POST"
echo -e "${YELLOW}Content-Type:${NC} application/json"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Execute curl and capture response
echo -e "${YELLOW}Sending request...${NC}"
HTTP_CODE=$(curl -s -o /tmp/webhook_response.txt -w "%{http_code}" \
     -X POST \
     -H "Content-Type: application/json" \
     -d "$PAYLOAD" \
     "$URL")

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Response${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Check response and display results
if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
    echo -e "${GREEN}✓ Success (HTTP $HTTP_CODE)${NC}"
    echo ""
    echo -e "${YELLOW}Response Body:${NC}"
    cat /tmp/webhook_response.txt
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    rm -f /tmp/webhook_response.txt
    exit 0
else
    echo -e "${RED}✗ Failed (HTTP $HTTP_CODE)${NC}"
    echo ""
    echo -e "${YELLOW}Response Body:${NC}"
    cat /tmp/webhook_response.txt
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    rm -f /tmp/webhook_response.txt
    exit 1
fi
