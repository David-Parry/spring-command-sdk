#!/bin/bash

# Atlassian/Jira IP Range Fetcher for WAF Configuration (Bash Version)
# Requires: curl, jq

set -e

# Configuration
ATLASSIAN_URL="https://ip-ranges.atlassian.com/"
TEMP_FILE="/tmp/atlassian_ips_$$.json"

# Default values
PRODUCTS=""
DIRECTION="egress"
OUTPUT_FORMAT="list"
OUTPUT_FILE=""
IPV4_ONLY=false
IPV6_ONLY=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to display help
show_help() {
    cat << EOF
Usage: $0 [OPTIONS]

Fetch and filter Atlassian IP ranges for WAF configuration

OPTIONS:
    -p, --product PRODUCT      Filter by product (e.g., jira, confluence, bitbucket)
                              Can be used multiple times for multiple products
    -d, --direction DIRECTION  Filter by direction (egress or ingress, default: egress)
    -r, --region REGION       Filter by region (e.g., us-east-1, global)
    -4, --ipv4-only           Only include IPv4 addresses
    -6, --ipv6-only           Only include IPv6 addresses
    -f, --format FORMAT       Output format: list, csv, json, nginx, apache (default: list)
    -o, --output FILE         Output file (default: stdout)
    -h, --help               Show this help message

PRESET OPTIONS:
    --jira-webhooks          Get IPs for Jira webhooks (product=jira, direction=egress)
    --all-webhooks           Get all Atlassian webhook IPs (direction=egress)

EXAMPLES:
    # Get Jira webhook IPs
    $0 --jira-webhooks

    # Get all Atlassian egress IPs in CSV format
    $0 -d egress -f csv

    # Get Jira IPs for specific region
    $0 -p jira -r us-east-1

    # Save to file
    $0 --jira-webhooks -o jira_ips.txt

EOF
}

# Function to check dependencies
check_dependencies() {
    local deps_missing=false
    
    for cmd in curl jq; do
        if ! command -v $cmd &> /dev/null; then
            echo -e "${RED}Error: $cmd is not installed${NC}" >&2
            deps_missing=true
        fi
    done
    
    if [ "$deps_missing" = true ]; then
        echo "Please install the missing dependencies and try again." >&2
        exit 1
    fi
}

# Function to fetch IP ranges
fetch_ip_ranges() {
    echo -e "${BLUE}Fetching Atlassian IP ranges...${NC}" >&2
    if ! curl -s -f "$ATLASSIAN_URL" -o "$TEMP_FILE"; then
        echo -e "${RED}Error: Failed to fetch IP ranges from $ATLASSIAN_URL${NC}" >&2
        exit 1
    fi
    echo -e "${GREEN}Successfully fetched IP ranges${NC}" >&2
}

# Function to build jq filter
build_jq_filter() {
    local filter=".items[]"
    local conditions=()
    
    # Product filter
    if [ -n "$PRODUCTS" ]; then
        local product_filter="("
        local first=true
        IFS=',' read -ra PROD_ARRAY <<< "$PRODUCTS"
        for prod in "${PROD_ARRAY[@]}"; do
            if [ "$first" = true ]; then
                first=false
            else
                product_filter="$product_filter or "
            fi
            product_filter="${product_filter}.product[]? == \"$prod\""
        done
        product_filter="$product_filter)"
        conditions+=("$product_filter")
    fi
    
    # Direction filter
    if [ -n "$DIRECTION" ]; then
        conditions+=("(.direction[]? == \"$DIRECTION\")")
    fi
    
    # Region filter
    if [ -n "$REGION" ]; then
        conditions+=("(.region[]? == \"$REGION\")")
    fi
    
    # IP version filter
    if [ "$IPV4_ONLY" = true ]; then
        conditions+=("(.cidr | contains(\":\") | not)")
    elif [ "$IPV6_ONLY" = true ]; then
        conditions+=("(.cidr | contains(\":\"))")
    fi
    
    # Combine conditions
    if [ ${#conditions[@]} -gt 0 ]; then
        filter="$filter | select("
        local first=true
        for cond in "${conditions[@]}"; do
            if [ "$first" = true ]; then
                first=false
            else
                filter="$filter and "
            fi
            filter="$filter$cond"
        done
        filter="$filter)"
    fi
    
    echo "$filter"
}

# Function to format output
format_output() {
    local format=$1
    local input_file=$2
    
    case $format in
        list)
            jq -r '.cidr' "$input_file" | sort -u
            ;;
        csv)
            jq -r '.cidr' "$input_file" | sort -u | paste -sd "," -
            ;;
        json)
            jq -r '.cidr' "$input_file" | sort -u | jq -R . | jq -s .
            ;;
        nginx)
            echo "# Nginx geo module format"
            echo "geo \$atlassian_ip {"
            echo "    default 0;"
            jq -r '.cidr' "$input_file" | sort -u | while read -r cidr; do
                echo "    $cidr 1;"
            done
            echo "}"
            ;;
        apache)
            echo "# Apache .htaccess format"
            jq -r '.cidr' "$input_file" | sort -u | while read -r cidr; do
                echo "Require ip $cidr"
            done
            ;;
        *)
            jq -r '.cidr' "$input_file" | sort -u
            ;;
    esac
}

# Function to show summary
show_summary() {
    local temp_filtered=$1
    
    local total_count=$(jq -r '.cidr' "$temp_filtered" | sort -u | wc -l)
    local ipv4_count=$(jq -r '.cidr' "$temp_filtered" | grep -v ":" | sort -u | wc -l)
    local ipv6_count=$(jq -r '.cidr' "$temp_filtered" | grep ":" | sort -u | wc -l)
    local products=$(jq -r '.product[]?' "$temp_filtered" | sort -u | paste -sd ", " -)
    local regions=$(jq -r '.region[]?' "$temp_filtered" | sort -u | paste -sd ", " -)
    
    echo -e "\n${GREEN}========================================${NC}" >&2
    echo -e "${GREEN}Atlassian IP Range Summary${NC}" >&2
    echo -e "${GREEN}========================================${NC}" >&2
    echo -e "Fetch Date: $(date -Iseconds)" >&2
    echo -e "Source: $ATLASSIAN_URL" >&2
    echo -e "Total Unique CIDR Blocks: ${BLUE}$total_count${NC}" >&2
    echo -e "  - IPv4: $ipv4_count" >&2
    echo -e "  - IPv6: $ipv6_count" >&2
    echo -e "Products: $products" >&2
    echo -e "Regions: $regions" >&2
    echo -e "${GREEN}========================================${NC}" >&2
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--product)
            if [ -n "$PRODUCTS" ]; then
                PRODUCTS="$PRODUCTS,$2"
            else
                PRODUCTS="$2"
            fi
            shift 2
            ;;
        -d|--direction)
            DIRECTION="$2"
            shift 2
            ;;
        -r|--region)
            REGION="$2"
            shift 2
            ;;
        -4|--ipv4-only)
            IPV4_ONLY=true
            shift
            ;;
        -6|--ipv6-only)
            IPV6_ONLY=true
            shift
            ;;
        -f|--format)
            OUTPUT_FORMAT="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        --jira-webhooks)
            PRODUCTS="jira"
            DIRECTION="egress"
            shift
            ;;
        --all-webhooks)
            DIRECTION="egress"
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            show_help
            exit 1
            ;;
    esac
done

# Main execution
check_dependencies
fetch_ip_ranges

# Build and apply filter
FILTER=$(build_jq_filter)
TEMP_FILTERED="/tmp/atlassian_filtered_$$.json"

# Apply filter and save to temp file
jq "$FILTER" "$TEMP_FILE" > "$TEMP_FILTERED"

# Check if any results
if [ ! -s "$TEMP_FILTERED" ] || [ "$(jq 'length' "$TEMP_FILTERED")" -eq 0 ]; then
    echo -e "${RED}No IP ranges found matching the criteria${NC}" >&2
    rm -f "$TEMP_FILE" "$TEMP_FILTERED"
    exit 1
fi

# Generate output
if [ -n "$OUTPUT_FILE" ]; then
    format_output "$OUTPUT_FORMAT" "$TEMP_FILTERED" > "$OUTPUT_FILE"
    echo -e "${GREEN}Output written to $OUTPUT_FILE${NC}" >&2
    show_summary "$TEMP_FILTERED"
else
    format_output "$OUTPUT_FORMAT" "$TEMP_FILTERED"
fi

# Cleanup
rm -f "$TEMP_FILE" "$TEMP_FILTERED"
