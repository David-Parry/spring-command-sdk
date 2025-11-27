#!/bin/bash

# Script: test-jira-webhook.sh
# Purpose: Test Jira webhook endpoint with dynamic issue number
# Usage: ./test-jira-webhook.sh <issue-number>
# Example: ./test-jira-webhook.sh 348

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to display usage
usage() {
    echo -e "${YELLOW}Usage:${NC} $0 <issue-number>"
    echo ""
    echo "Examples:"
    echo "  $0 348        # Test with SCRUM-348"
    echo "  $0 1234       # Test with SCRUM-1234"
    exit 1
}

# Validate arguments
if [ $# -eq 0 ]; then
    echo -e "${RED}Error: Issue number required${NC}"
    echo ""
    usage
fi

ISSUE_NUMBER=$1

# Validate issue number is numeric
if ! [[ "$ISSUE_NUMBER" =~ ^[0-9]+$ ]]; then
    echo -e "${RED}Error: Issue number must be numeric${NC}"
    echo "Got: $ISSUE_NUMBER"
    echo ""
    usage
fi

# Construct issue key and URL
ISSUE_KEY="SCRUM-${ISSUE_NUMBER}"
URL="http://localhost:8081/api/webhooks/jira/${ISSUE_KEY}"

# Create JSON payload with dynamic issue key
# Note: The issue key appears in multiple places in the payload
PAYLOAD='{"issue":{"id":"99291","self":"https://your-domain.atlassian.net/rest/api/2/issue/99291","key":"'${ISSUE_KEY}'","fields":{"summary":"I feel the need for speed","created":"2009-12-16T23:46:10.612-0600","description":"Make the issue nav load 10x faster","labels":["UI","dialogue","move"],"priority":{"self":"https://your-domain.atlassian.net/rest/api/2/priority/3","iconUrl":"https://your-domain.atlassian.net/images/icons/priorities/minor.svg","name":"Minor","id":"3"}}},"user":{"self":"https://your-domain.atlassian.net/rest/api/2/user?accountId=99:27935d01-92a7-4687-8272-a9b8d3b2ae2e","accountId":"notmen","accountType":"atlassian","avatarUrls":{"16x16":"https://your-domain.atlassian.net/secure/useravatar?size=small&avatarId=10605","48x48":"https://your-domain.atlassian.net/secure/useravatar?avatarId=10605"},"displayName":"Bryan Rollins [Atlassian]","active":"true","timeZone":"Europe/Warsaw"},"changelog":{"items":[{"toString":"A new summary.","to":null,"fromString":"What is going on here?????","from":null,"fieldtype":"jira","field":"summary"},{"toString":"New Feature","to":"2","fromString":"Improvement","from":"4","fieldtype":"jira","field":"issuetype"}],"id":10124},"timestamp":1606480436302,"webhookEvent":"jira:issue_updated","issue_event_type_name":"issue_generic"}'

# Display request information
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Testing Jira Webhook${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Issue Key:${NC} $ISSUE_KEY"
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