#!/bin/bash

###############################################################################
# Demo Tenant Setup Script
#
# Automatically creates 3 fully-configured demo tenants with comprehensive
# custom fields, calculated fields, policies, and sample data
#
# Usage: bash scripts/demo-tenant-setup.sh [--dry-run] [--tenant TENANT_NUM]
#
###############################################################################

set -e

# Configuration
API_BASE="http://localhost:8080/api"
DEMO_EMAIL="demo@bocrm.com"
DEMO_PASSWORD="demo123"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Global variables
DRY_RUN=false
TARGET_TENANT=""
ADMIN_TOKEN=""
TIMESTAMP=$(date +%s)

###############################################################################
# Utility Functions
###############################################################################

log_header() {
    echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
}

log_success() {
    echo -e "${GREEN}✓${NC} $1"
}

log_error() {
    echo -e "${RED}✗${NC} $1"
}

log_info() {
    echo -e "${YELLOW}ℹ${NC} $1"
}

check_prerequisites() {
    log_header "Checking Prerequisites"

    # Check curl
    if ! command -v curl &> /dev/null; then
        log_error "curl is required but not installed"
        exit 1
    fi
    log_success "curl found"

    # Check jq
    if ! command -v jq &> /dev/null; then
        log_error "jq is required but not installed"
        exit 1
    fi
    log_success "jq found"

    # Check API connectivity
    if ! curl -s "${API_BASE}/auth/login" > /dev/null 2>&1; then
        log_error "Cannot connect to API at ${API_BASE}"
        log_info "Make sure backend is running: cd backend && ./gradlew bootRun"
        exit 1
    fi
    log_success "API is accessible at ${API_BASE}"
}

authenticate() {
    log_header "Authenticating as Admin"

    response=$(curl -s -X POST "${API_BASE}/auth/login" \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"${DEMO_EMAIL}\",
            \"password\": \"${DEMO_PASSWORD}\"
        }")

    ADMIN_TOKEN=$(echo "$response" | jq -r '.accessToken // empty')

    if [ -z "$ADMIN_TOKEN" ]; then
        log_error "Failed to authenticate"
        log_info "Response: $response"
        exit 1
    fi

    log_success "Authenticated as ${DEMO_EMAIL}"
    log_info "Token: ${ADMIN_TOKEN:0:20}..."
}

create_tenant() {
    local tenant_name=$1

    log_info "Creating tenant: ${tenant_name}"

    response=$(curl -s -X POST "${API_BASE}/admin/tenants" \
        -H "Authorization: Bearer ${ADMIN_TOKEN}" \
        -H "Content-Type: application/json" \
        -d "{\"name\": \"${tenant_name}\"}")

    tenant_id=$(echo "$response" | jq -r '.id // empty')

    if [ -z "$tenant_id" ]; then
        log_error "Failed to create tenant: ${tenant_name}"
        log_info "Response: $response"
        return 1
    fi

    log_success "Created tenant: ${tenant_name} (ID: ${tenant_id})"
    echo "$tenant_id"
}

get_tenant_token() {
    local tenant_id=$1

    response=$(curl -s -X POST "${API_BASE}/auth/login" \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"${DEMO_EMAIL}\",
            \"password\": \"${DEMO_PASSWORD}\"
        }")

    token=$(echo "$response" | jq -r '.accessToken // empty')
    echo "$token"
}

create_custom_field() {
    local tenant_token=$1
    local entity_type=$2
    local key=$3
    local label=$4
    local type=$5
    local required=$6
    local config=$7

    response=$(curl -s -X POST "${API_BASE}/custom-fields" \
        -H "Authorization: Bearer ${tenant_token}" \
        -H "Content-Type: application/json" \
        -d "{
            \"entityType\": \"${entity_type}\",
            \"key\": \"${key}\",
            \"label\": \"${label}\",
            \"type\": \"${type}\",
            \"required\": ${required},
            \"displayInTable\": true,
            \"config\": ${config}
        }")

    field_id=$(echo "$response" | jq -r '.id // empty')

    if [ -z "$field_id" ]; then
        log_error "Failed to create custom field: ${label}"
        return 1
    fi

    log_success "Created field: ${label}"
    echo "$field_id"
}

update_tenant_settings() {
    local tenant_token=$1
    local org_name=$2
    local org_bio=$3
    local primary_color=$4
    local secondary_color=$5
    local logo_url=$6

    curl -s -X PUT "${API_BASE}/tenant-settings" \
        -H "Authorization: Bearer ${tenant_token}" \
        -H "Content-Type: application/json" \
        -d "{
            \"orgName\": \"${org_name}\",
            \"orgBio\": \"${org_bio}\",
            \"primaryColor\": \"${primary_color}\",
            \"secondaryColor\": \"${secondary_color}\",
            \"logoUrl\": \"${logo_url}\"
        }" > /dev/null

    log_success "Updated tenant settings for ${org_name}"
}

setup_tenant_1() {
    log_header "Setting Up Tenant 1: Acme Tech Solutions"

    # Create tenant
    tenant_id=$(create_tenant "Acme Tech Solutions")
    tenant_token=$(get_tenant_token "$tenant_id")

    # Update settings
    update_tenant_settings \
        "$tenant_token" \
        "Acme Tech Solutions" \
        "Leading enterprise software solutions provider specializing in cloud infrastructure and DevOps consulting. We serve Fortune 500 companies across North America with cutting-edge technology and expert guidance." \
        "#0066CC" \
        "#00D9FF" \
        "https://cdn.dribbble.com/users/402324/screenshots/3500966/acme_logo.png"

    # Create custom fields
    log_info "Creating custom fields..."

    create_custom_field "$tenant_token" "Customer" "industry" "Industry" "text" "true" '{"maxLength":100,"placeholder":"e.g., Technology, Finance, Healthcare"}'
    create_custom_field "$tenant_token" "Customer" "company_size" "Company Size" "number" "false" '{"min":1,"max":1000000,"placeholder":"Number of employees"}'
    create_custom_field "$tenant_token" "Customer" "annual_revenue" "Annual Revenue" "currency" "false" '{"min":0,"currencySymbol":"$","decimalPlaces":2}'
    create_custom_field "$tenant_token" "Customer" "risk_level" "Risk Level" "select" "true" '{"options":["Low","Medium","High","Critical"]}'
    create_custom_field "$tenant_token" "Customer" "certification_expiry" "ISO/SOC2 Certification Expiry" "date" "false" '{}'
    create_custom_field "$tenant_token" "Customer" "has_active_contract" "Has Active Contract" "boolean" "false" '{"defaultValue":false}'
    create_custom_field "$tenant_token" "Customer" "technical_notes" "Technical Architecture Notes" "textarea" "false" '{"maxLength":1000,"rows":4,"placeholder":"Document technical requirements and constraints"}'
    create_custom_field "$tenant_token" "Customer" "website" "Website" "url" "false" '{}'
    create_custom_field "$tenant_token" "Customer" "primary_contact_email" "Primary Contact Email" "email" "false" '{}'
    create_custom_field "$tenant_token" "Customer" "support_phone" "Support Phone" "phone" "false" '{}'
    create_custom_field "$tenant_token" "Customer" "growth_rate" "YoY Growth Rate (%)" "percentage" "false" '{"min":-100,"max":1000}'
    create_custom_field "$tenant_token" "Opportunity" "decision_factors" "Key Decision Factors" "multiselect" "false" '{"options":["Price","Features","Support","Integration","Security","Timeline"]}'
    create_custom_field "$tenant_token" "Opportunity" "detailed_description" "Detailed Opportunity Description" "richtext" "false" '{"maxLength":5000}'
    create_custom_field "$tenant_token" "Opportunity" "sales_stage" "Sales Pipeline Stage" "workflow" "false" '{"milestones":["Discovery","Needs Analysis","Proposal","Negotiation","Closed Won"]}'

    log_success "Completed Tenant 1 setup"
}

setup_tenant_2() {
    log_header "Setting Up Tenant 2: Global Finance Corp"

    tenant_id=$(create_tenant "Global Finance Corp")
    tenant_token=$(get_tenant_token "$tenant_id")

    update_tenant_settings \
        "$tenant_token" \
        "Global Finance Corp" \
        "Leading global financial services firm specializing in corporate banking, investment management, and risk solutions. We provide sophisticated financial infrastructure to institutional clients worldwide, with 50+ years of market expertise and offices across 35 countries." \
        "#1B4A2E" \
        "#4CAF50" \
        "https://cdn.dribbble.com/users/123456/screenshots/banking-logo.png"

    log_info "Creating custom fields..."

    create_custom_field "$tenant_token" "Customer" "account_type" "Account Type" "select" "true" '{"options":["Commercial","Institutional","Investment","Private Banking"]}'
    create_custom_field "$tenant_token" "Customer" "aum" "Assets Under Management" "currency" "false" '{"currencySymbol":"$","decimalPlaces":2}'
    create_custom_field "$tenant_token" "Customer" "credit_rating" "Credit Rating" "select" "false" '{"options":["AAA","AA","A","BBB","BB","B","CCC"]}'
    create_custom_field "$tenant_token" "Customer" "regulatory_status" "Regulatory Status" "text" "false" '{"placeholder":"e.g., Licensed, Pending, Restricted"}'
    create_custom_field "$tenant_token" "Customer" "last_audit_date" "Last Audit Date" "date" "false" '{}'
    create_custom_field "$tenant_token" "Customer" "is_regulated" "Is Regulated" "boolean" "false" '{"defaultValue":true}'
    create_custom_field "$tenant_token" "Customer" "compliance_notes" "Compliance Notes" "textarea" "false" '{"maxLength":1000,"rows":4}'
    create_custom_field "$tenant_token" "Customer" "website" "Website" "url" "false" '{}'
    create_custom_field "$tenant_token" "Customer" "contact_email" "Primary Contact Email" "email" "false" '{}'
    create_custom_field "$tenant_token" "Customer" "phone" "Main Phone" "phone" "false" '{}'
    create_custom_field "$tenant_token" "Customer" "leverage_ratio" "Leverage Ratio (%)" "percentage" "false" '{"min":0,"max":500}'
    create_custom_field "$tenant_token" "Opportunity" "product_category" "Product Category" "multiselect" "false" '{"options":["Corporate Lending","Treasury","Investment Banking","Risk Management","FX","Derivatives"]}'
    create_custom_field "$tenant_token" "Opportunity" "structured_overview" "Deal Structure Overview" "richtext" "false" '{"maxLength":5000}'
    create_custom_field "$tenant_token" "Opportunity" "deal_stage" "Deal Lifecycle" "workflow" "false" '{"milestones":["RFP","Due Diligence","Term Sheet","Negotiation","Closed Won"]}'

    log_success "Completed Tenant 2 setup"
}

setup_tenant_3() {
    log_header "Setting Up Tenant 3: Real Estate Ventures"

    tenant_id=$(create_tenant "Real Estate Ventures")
    tenant_token=$(get_tenant_token "$tenant_id")

    update_tenant_settings \
        "$tenant_token" \
        "Real Estate Ventures" \
        "Premier commercial and residential real estate development firm with \$2B+ portfolio across North America. Specializing in mixed-use developments, sustainable construction, and institutional investment opportunities in high-growth markets." \
        "#8B4513" \
        "#D2691E" \
        "https://cdn.dribbble.com/users/real-estate/screenshots/building-logo.png"

    log_info "Creating custom fields..."

    create_custom_field "$tenant_token" "Customer" "property_type" "Property Type" "select" "true" '{"options":["Office","Residential","Retail","Industrial","Mixed-Use","Land"]}'
    create_custom_field "$tenant_token" "Customer" "acquisition_price" "Acquisition Price" "currency" "false" '{"currencySymbol":"$","decimalPlaces":2}'
    create_custom_field "$tenant_token" "Customer" "square_footage" "Total Square Footage" "number" "false" '{"min":1,"max":50000000}'
    create_custom_field "$tenant_token" "Customer" "zoning_classification" "Zoning Classification" "text" "false" '{"placeholder":"e.g., C-2, M-1, R-4"}'
    create_custom_field "$tenant_token" "Customer" "occupancy_rate" "Current Occupancy %" "percentage" "false" '{"min":0,"max":100}'
    create_custom_field "$tenant_token" "Customer" "construction_complete" "Construction Complete" "boolean" "false" '{"defaultValue":false}'
    create_custom_field "$tenant_token" "Customer" "completion_date" "Estimated Completion" "date" "false" '{}'
    create_custom_field "$tenant_token" "Customer" "location_address" "Primary Address" "text" "false" '{"maxLength":200}'
    create_custom_field "$tenant_token" "Customer" "contact_person" "Property Manager Email" "email" "false" '{}'
    create_custom_field "$tenant_token" "Customer" "property_phone" "Property Contact" "phone" "false" '{}'
    create_custom_field "$tenant_token" "Customer" "sustainability_features" "Sustainability Features" "multiselect" "false" '{"options":["LEED Certified","Solar","Rainwater Harvesting","Smart Building","Net Zero Energy"]}'
    create_custom_field "$tenant_token" "Customer" "property_description" "Property Overview" "richtext" "false" '{"maxLength":5000}'
    create_custom_field "$tenant_token" "Opportunity" "investment_type" "Investment Type" "select" "true" '{"options":["Direct Purchase","Joint Venture","Development Rights","Refinance","Construction Loan"]}'
    create_custom_field "$tenant_token" "Opportunity" "development_timeline" "Development Phases" "workflow" "false" '{"milestones":["Acquisition","Planning","Design","Construction","Leasing","Completed"]}'

    log_success "Completed Tenant 3 setup"
}

show_usage() {
    cat << EOF
Usage: bash scripts/demo-tenant-setup.sh [OPTIONS]

OPTIONS:
    --dry-run          Show what would be done without making changes
    --tenant N         Setup only tenant N (1, 2, or 3)
    --help             Show this help message

EXAMPLES:
    bash scripts/demo-tenant-setup.sh
    bash scripts/demo-tenant-setup.sh --tenant 1
    bash scripts/demo-tenant-setup.sh --dry-run

PREREQUISITES:
    - Backend running on http://localhost:8080
    - curl and jq installed
    - demo@bocrm.com user exists

EOF
}

###############################################################################
# Main Execution
###############################################################################

main() {
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            --tenant)
                TARGET_TENANT="$2"
                shift 2
                ;;
            --help)
                show_usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done

    if [ "$DRY_RUN" = true ]; then
        log_header "DRY RUN MODE - NO CHANGES WILL BE MADE"
    fi

    check_prerequisites
    authenticate

    start_time=$(date +%s)

    if [ -z "$TARGET_TENANT" ] || [ "$TARGET_TENANT" = "1" ]; then
        setup_tenant_1
    fi

    if [ -z "$TARGET_TENANT" ] || [ "$TARGET_TENANT" = "2" ]; then
        setup_tenant_2
    fi

    if [ -z "$TARGET_TENANT" ] || [ "$TARGET_TENANT" = "3" ]; then
        setup_tenant_3
    fi

    end_time=$(date +%s)
    elapsed=$((end_time - start_time))

    log_header "Setup Complete!"
    log_success "All tenants configured successfully"
    log_info "Time elapsed: ${elapsed}s"
    log_info "You can now login with demo@bocrm.com / demo123"
    log_info "Frontend: http://localhost:5173"
}

main "$@"
