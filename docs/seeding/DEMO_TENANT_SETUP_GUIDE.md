# Comprehensive Demo Tenant Setup Guide

## Overview

This guide provides complete instructions for setting up 3 fully-configured demo tenants with comprehensive feature coverage:

1. **Acme Tech Solutions** - Enterprise software & cloud solutions
2. **Global Finance Corp** - Financial services & banking  
3. **Real Estate Ventures** - Commercial & residential real estate

Each tenant includes:
- ✅ 13+ custom field types (covering all available types)
- ✅ Calculated fields with CEL expressions
- ✅ Business policies (DENY and WARN severity)
- ✅ 15-20 sample customers
- ✅ 20 sample contacts
- ✅ 15+ opportunities with workflow progression
- ✅ 18-20 activities across the pipeline
- ✅ Company branding (logo, colors, description)
- ✅ Tenant settings fully configured

## Quick Start

### Prerequisites

```bash
# 1. Start infrastructure (includes Postgres, RabbitMQ, and OPA sidecar)
docker compose up -d

# 2. Start backend (from backend/)
./gradlew bootRun

# 3. Start frontend (from frontend/) - optional for API testing
npm run dev
```

> **OPA sidecar**: Policy rules use [Open Policy Agent](https://www.openpolicyagent.org/) for Rego evaluation. The `docker compose up -d` command starts the OPA container automatically. If policy rules aren't triggering, verify OPA is running: `docker compose ps opa`

### Run the Setup

**Option 1: HTTP Test File (Recommended for Tenant 1)**

```bash
# Open IntelliJ/VSCode REST Client
# File: backend/api-tests/12-demo-tenants-comprehensive-setup.http
# Run all requests in sequence
# Takes ~2 minutes to complete Tenant 1
```

**Option 2: Complete Setup via API Client (cURL or Postman)**

```bash
# See API Endpoints section below
```

**Option 3: Use the Frontend Onboarding Wizard**

```
1. Navigate to http://localhost:5173/onboarding
2. Login with demo@bocrm.com / demo123
3. Create new tenant → enter org bio
4. Select suggested custom fields/policies
5. Confirm to apply
```

---

## Tenant 1: Acme Tech Solutions - COMPLETE

**Status**: ✅ Ready to run from `12-demo-tenants-comprehensive-setup.http`

### Setup Summary

```
Branding:
  - Logo: https://cdn.dribbble.com/users/402324/screenshots/3500966/acme_logo.png
  - Colors: #0066CC (primary), #00D9FF (secondary)
  - Bio: "Leading enterprise software solutions provider..."

Custom Fields (13 total):
  ✓ Industry (text)
  ✓ Company Size (number)
  ✓ Annual Revenue (currency)
  ✓ Risk Level (select)
  ✓ Certification Expiry (date)
  ✓ Has Active Contract (boolean)
  ✓ Technical Notes (textarea)
  ✓ Website (url)
  ✓ Primary Contact Email (email)
  ✓ Support Phone (phone)
  ✓ Growth Rate (percentage)
  ✓ Decision Factors (multiselect - on Opportunity)
  ✓ Sales Stage (workflow - on Opportunity)

Calculated Fields (2 total):
  ✓ Engagement Score = min(100, (company_size/100) + (growth_rate/2))
  ✓ Deal Progression = sales_stage.currentIndex * 20

Policies (3 total):
  ✓ DENY: Block archiving customers with $10M+ revenue
  ✓ WARN: Warn on updates to critical-risk customers
  ✓ DENY: Block opportunity stage regression from negotiation+

Sample Data:
  ✓ 15 Customers (TechCore, DataFlow, SecureVault, CloudNine, etc.)
  ✓ 20 Contacts
  ✓ 15 Opportunities (with workflow stages 0-4)
  ✓ 18 Activities (discovery → proposal → negotiation stages)
```

---

## Tenant 2: Global Finance Corp - TEMPLATE

### Setup Instructions

#### Step 1: Create Tenant & Login

```http
POST http://localhost:8080/api/admin/tenants
Authorization: Bearer {{adminAccessToken}}
Content-Type: application/json

{
  "name": "Global Finance Corp"
}
```

**Save the returned `id` as `{{tenant2Id}}`**

Then login to get `{{tenant2AccessToken}}`

#### Step 2: Update Branding & Settings

```http
PUT http://localhost:8080/api/tenant-settings
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "orgName": "Global Finance Corp",
  "orgBio": "Leading global financial services firm specializing in corporate banking, investment management, and risk solutions. We provide sophisticated financial infrastructure to institutional clients worldwide, with 50+ years of market expertise and offices across 35 countries.",
  "primaryColor": "#1B4A2E",
  "secondaryColor": "#4CAF50",
  "logoUrl": "https://cdn.dribbble.com/users/123456/screenshots/banking-logo.png",
  "websiteUrl": "https://globalfinancecorp.com"
}
```

#### Step 3: Create Custom Fields

**3.1 Customer Fields (Financial Industry)**

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "account_type",
  "label": "Account Type",
  "type": "select",
  "required": true,
  "displayInTable": true,
  "config": {
    "options": ["Commercial", "Institutional", "Investment", "Private Banking"]
  }
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "aum",
  "label": "CustomRecords Under Management",
  "type": "currency",
  "required": false,
  "displayInTable": true,
  "config": {
    "currencySymbol": "$",
    "decimalPlaces": 2
  }
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "credit_rating",
  "label": "Credit Rating",
  "type": "select",
  "required": false,
  "displayInTable": true,
  "config": {
    "options": ["AAA", "AA", "A", "BBB", "BB", "B", "CCC"]
  }
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "regulatory_status",
  "label": "Regulatory Status",
  "type": "text",
  "required": false,
  "config": {
    "placeholder": "e.g., Licensed, Pending, Restricted"
  }
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "last_audit_date",
  "label": "Last Audit Date",
  "type": "date",
  "required": false
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "is_regulated",
  "label": "Is Regulated",
  "type": "boolean",
  "required": false,
  "displayInTable": true,
  "config": {"defaultValue": true}
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "compliance_notes",
  "label": "Compliance Notes",
  "type": "textarea",
  "required": false,
  "config": {"maxLength": 1000, "rows": 4}
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "website",
  "label": "Website",
  "type": "url",
  "required": false
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "contact_email",
  "label": "Primary Contact Email",
  "type": "email",
  "required": false
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "phone",
  "label": "Main Phone",
  "type": "phone",
  "required": false
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "leverage_ratio",
  "label": "Leverage Ratio (%)",
  "type": "percentage",
  "required": false,
  "displayInTable": true,
  "config": {"min": 0, "max": 500}
}
```

**3.2 Opportunity Fields**

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "entityType": "Opportunity",
  "key": "product_category",
  "label": "Product Category",
  "type": "multiselect",
  "required": false,
  "config": {
    "options": ["Corporate Lending", "Treasury", "Investment Banking", "Risk Management", "FX", "Derivatives"]
  }
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "entityType": "Opportunity",
  "key": "structured_overview",
  "label": "Deal Structure Overview",
  "type": "richtext",
  "required": false,
  "config": {"maxLength": 5000}
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "entityType": "Opportunity",
  "key": "deal_stage",
  "label": "Deal Lifecycle",
  "type": "workflow",
  "required": false,
  "displayInTable": true,
  "config": {
    "milestones": ["RFP", "Due Diligence", "Term Sheet", "Negotiation", "Closed Won"]
  }
}
```

#### Step 4: Create Calculated Fields

```http
POST http://localhost:8080/api/calculated-fields
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "risk_score",
  "label": "Overall Risk Score",
  "expression": "T(java.lang.Integer).parseInt(customField_credit_rating.replaceAll('[^0-9]', '') * 10) + (leverage_ratio / 5)",
  "returnType": "number"
}
```

```http
POST http://localhost:8080/api/calculated-fields
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "entityType": "Opportunity",
  "key": "deal_completion_stage",
  "label": "Deal Completion %",
  "expression": "customField_deal_stage != null ? (customField_deal_stage.currentIndex + 1) * 20 : 0",
  "returnType": "number"
}
```

#### Step 5: Create Policies

```http
POST http://localhost:8080/api/policy-rules/definitions
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "name": "Prevent Archiving Regulated Accounts",
  "entityType": "Customer",
  "operations": "UPDATE",
  "expression": "input.entity.customFields.is_regulated == true\ninput.entity.status == \"archived\"",
  "severity": "DENY",
  "description": "Cannot archive accounts with active regulatory oversight"
}
```

```http
POST http://localhost:8080/api/policy-rules/definitions
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "name": "Warn on High Leverage Updates",
  "entityType": "Customer",
  "operations": "UPDATE",
  "expression": "input.entity.customFields.leverage_ratio > 250",
  "severity": "WARN",
  "description": "Requires confirmation when modifying high-leverage accounts"
}
```

```http
POST http://localhost:8080/api/policy-rules/definitions
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "name": "Block Deal Closure Without Due Diligence",
  "entityType": "Opportunity",
  "operations": "UPDATE",
  "expression": "input.entity.status == \"closed\"\ninput.previous.customFields.deal_stage.currentIndex < 1",
  "severity": "DENY",
  "description": "Cannot close deals without completing due diligence phase"
}
```

#### Step 6: Create Sample Data

**15 Sample Customers** (repeat for each):

```http
POST http://localhost:8080/api/customers
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "name": "JP Morgan Capital",
  "status": "active",
  "customFields": {
    "account_type": "Commercial",
    "aum": 250000000000,
    "credit_rating": "AAA",
    "regulatory_status": "Licensed",
    "is_regulated": true,
    "leverage_ratio": 8.5,
    "website": "https://jpmorgan.com",
    "contact_email": "corporate@jpmorgan.com",
    "phone": "+1-212-270-6000"
  }
}
```

Continue with 14 more similar entries for:
- Goldman Sachs Investment Partnership
- Barclays Capital Management
- HSBC Global Banking
- Deutsche Bank Securities
- BNY Mellon Trust
- Morgan Stanley Advisory
- Citigroup Corporate
- UBS Wealth Partners
- Credit Suisse Private
- Bank of America Commercial
- Wells Fargo Institutional
- Charles Schwab Investment
- Fidelity Capital Partners
- BlackRock Investment Management

**20 Sample Contacts** - 2-3 per customer (same pattern as Tenant 1)

**15+ Sample Opportunities** with deal stages:

```http
POST http://localhost:8080/api/opportunities
Authorization: Bearer {{tenant2AccessToken}}
Content-Type: application/json

{
  "customerId": 1,
  "name": "Structured Credit Facility - $500M",
  "description": "Mezzanine financing for acquisition",
  "amount": 500000000,
  "probability": 75,
  "expectedCloseDate": "2024-06-30",
  "status": "active",
  "customFields": {
    "product_category": ["Corporate Lending", "Risk Management"],
    "deal_stage": {
      "currentIndex": 2
    }
  }
}
```

Continue with 14 more opportunities covering:
- Investment banking advisory
- Treasury services
- FX hedging programs
- Derivative strategies
- Loan syndication
- M&A financing
- Capital markets access
- And more...

**18-20 Sample Activities** - Same pattern as Tenant 1

---

## Tenant 3: Real Estate Ventures - TEMPLATE

### Setup Instructions

#### Step 1: Create Tenant

```http
POST http://localhost:8080/api/admin/tenants
Authorization: Bearer {{adminAccessToken}}
Content-Type: application/json

{
  "name": "Real Estate Ventures"
}
```

#### Step 2: Update Branding

```http
PUT http://localhost:8080/api/tenant-settings
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "orgName": "Real Estate Ventures",
  "orgBio": "Premier commercial and residential real estate development firm with $2B+ portfolio across North America. Specializing in mixed-use developments, sustainable construction, and institutional investment opportunities in high-growth markets.",
  "primaryColor": "#8B4513",
  "secondaryColor": "#D2691E",
  "logoUrl": "https://cdn.dribbble.com/users/real-estate/screenshots/building-logo.png",
  "websiteUrl": "https://realestateventures.com"
}
```

#### Step 3: Custom Fields for Real Estate

**Property/Customer Fields:**

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "property_type",
  "label": "Property Type",
  "type": "select",
  "required": true,
  "displayInTable": true,
  "config": {
    "options": ["Office", "Residential", "Retail", "Industrial", "Mixed-Use", "Land"]
  }
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "acquisition_price",
  "label": "Acquisition Price",
  "type": "currency",
  "required": false,
  "displayInTable": true,
  "config": {"currencySymbol": "$", "decimalPlaces": 2}
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "square_footage",
  "label": "Total Square Footage",
  "type": "number",
  "required": false,
  "displayInTable": true,
  "config": {"min": 1, "max": 50000000}
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "zoning_classification",
  "label": "Zoning Classification",
  "type": "text",
  "required": false,
  "config": {"placeholder": "e.g., C-2, M-1, R-4"}
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "occupancy_rate",
  "label": "Current Occupancy %",
  "type": "percentage",
  "required": false,
  "displayInTable": true,
  "config": {"min": 0, "max": 100}
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "construction_complete",
  "label": "Construction Complete",
  "type": "boolean",
  "required": false,
  "displayInTable": true,
  "config": {"defaultValue": false}
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "completion_date",
  "label": "Estimated Completion",
  "type": "date",
  "required": false
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "location_address",
  "label": "Primary Address",
  "type": "text",
  "required": false,
  "config": {"maxLength": 200}
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "contact_person",
  "label": "Property Manager Email",
  "type": "email",
  "required": false
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "property_phone",
  "label": "Property Contact",
  "type": "phone",
  "required": false
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "sustainability_features",
  "label": "Sustainability Features",
  "type": "multiselect",
  "required": false,
  "config": {
    "options": ["LEED Certified", "Solar", "Rainwater Harvesting", "Smart Building", "Net Zero Energy"]
  }
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "property_description",
  "label": "Property Overview",
  "type": "richtext",
  "required": false,
  "config": {"maxLength": 5000}
}
```

**Opportunity Fields:**

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "entityType": "Opportunity",
  "key": "investment_type",
  "label": "Investment Type",
  "type": "select",
  "required": true,
  "displayInTable": true,
  "config": {
    "options": ["Direct Purchase", "Joint Venture", "Development Rights", "Refinance", "Construction Loan"]
  }
}
```

```http
POST http://localhost:8080/api/custom-fields
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "entityType": "Opportunity",
  "key": "development_timeline",
  "label": "Development Phases",
  "type": "workflow",
  "required": false,
  "displayInTable": true,
  "config": {
    "milestones": ["Acquisition", "Planning", "Design", "Construction", "Leasing", "Completed"]
  }
}
```

#### Step 4: Calculated Fields

```http
POST http://localhost:8080/api/calculated-fields
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "entityType": "Customer",
  "key": "cost_per_sqft",
  "label": "Cost Per Square Foot",
  "expression": "customField_acquisition_price / customField_square_footage",
  "returnType": "number"
}
```

```http
POST http://localhost:8080/api/calculated-fields
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "entityType": "Opportunity",
  "key": "project_progress",
  "label": "Project Progress %",
  "expression": "customField_development_timeline != null ? (customField_development_timeline.currentIndex + 1) * 16 : 0",
  "returnType": "number"
}
```

#### Step 5: Policies

```http
POST http://localhost:8080/api/policy-rules/definitions
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "name": "Lock Completed Properties",
  "entityType": "Customer",
  "operations": "UPDATE",
  "expression": "input.entity.customFields.construction_complete == true\ninput.entity.status == \"archived\"",
  "severity": "DENY",
  "description": "Cannot modify details of completed properties"
}
```

```http
POST http://localhost:8080/api/policy-rules/definitions
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "name": "Warn High-Value Transactions",
  "entityType": "Opportunity",
  "operations": "CREATE,UPDATE",
  "expression": "input.entity.amount > 50000000",
  "severity": "WARN",
  "description": "Transactions exceeding $50M require senior approval"
}
```

#### Step 6: Sample Data

**15 Properties/Customers:**

```http
POST http://localhost:8080/api/customers
Authorization: Bearer {{tenant3AccessToken}}
Content-Type: application/json

{
  "name": "Downtown Tech Plaza - Seattle",
  "status": "active",
  "customFields": {
    "property_type": "Mixed-Use",
    "acquisition_price": 85000000,
    "square_footage": 450000,
    "zoning_classification": "C-2",
    "occupancy_rate": 92,
    "construction_complete": true,
    "location_address": "815 Mercer Street, Seattle, WA",
    "contact_person": "leasing@downtowntech.com",
    "property_phone": "+1-206-555-1234",
    "sustainability_features": ["LEED Certified", "Smart Building"],
    "property_description": "<p>Premium mixed-use development with 180k office + 120k retail space. Ground floor retail with tech tenant anchor. Class A office space with modern amenities.</p>"
  }
}
```

Continue with 14 more properties for cities like:
- San Francisco, Los Angeles, New York, Boston, Chicago, Austin, Denver, Dallas, Portland, Miami, Atlanta, Phoenix, Las Vegas, Nashville, Charlotte

**20 Contacts** (developers, leasing managers, investors)

**15 Opportunities** (acquisitions, developments, refinances)

**20 Activities** (site visits, inspections, due diligence, tours)

---

## Custom Field Types Reference

All 14 field types included in demo setup:

| Type | Use Case | Config Options |
|------|----------|-----------------|
| **text** | Short text fields | maxLength, placeholder |
| **number** | Numeric values | min, max |
| **date** | Date selection | none |
| **select** | Single choice dropdown | options array |
| **multiselect** | Multiple choice | options array |
| **boolean** | Yes/No | defaultValue |
| **textarea** | Long text | maxLength, rows |
| **email** | Email validation | none |
| **phone** | Phone numbers | none |
| **url** | Website URLs | none |
| **currency** | Monetary values | currencySymbol, decimalPlaces |
| **percentage** | Percentage (0-100+) | min, max |
| **richtext** | HTML content | maxLength |
| **workflow** | Milestone tracker | milestones array |

---

## Calculated Field Expression Examples

Calculated fields use **CEL (Common Expression Language)** — not SpEL.

```javascript
// Revenue-based scoring (uses math functions)
math.min(100.0, customField_company_size / 100.0 + customField_growth_rate / 2.0)

// Workflow stage percentage  
customField_sales_stage != null ? (customField_sales_stage.currentIndex + 1) * 20 : 0

// Cost calculations
customField_acquisition_price / customField_square_footage

// Risk assessment (conditional expression)
customField_risk_level == 'Critical' ? 100 : customField_risk_level == 'High' ? 75 : 0

// String check
size(name) > 10 ? "Long name" : "Short name"
```

> **Note**: CEL is the expression language for calculated fields. Policies use **Rego** (OPA). These are different languages — do not mix them.

---

## API Endpoint Reference

### Tenant Management
```http
POST /admin/tenants
GET /admin/tenants
PUT /tenant-settings
```

### Custom Fields
```http
POST /custom-fields
GET /custom-fields?entityType=Customer
PUT /custom-fields/{id}
DELETE /custom-fields/{id}
```

### Calculated Fields
```http
POST /calculated-fields
GET /calculated-fields?entityType=Customer
PUT /calculated-fields/{id}
DELETE /calculated-fields/{id}
```

### Policies
```http
POST /policy-rules/definitions
GET /policy-rules/definitions
PUT /policy-rules/definitions/{id}
DELETE /policy-rules/definitions/{id}
```

### Sample Data
```http
POST /customers
POST /contacts
POST /opportunities
POST /activities
```

---

## Validation Rules

- **Custom field key**: Alphanumeric + underscore, unique per entity type per tenant
- **Calculated field expression**: Valid CEL expression; access custom fields via `customField_<key>`, built-in fields by name (`name`, `status`, `amount`, etc.)
- **Policy expression**: Valid Rego condition body; use `input.entity.*`, `input.previous.*`, `input.operation`; supported entity types: Customer, Contact, Opportunity, Activity, CustomRecord, Order, Invoice
- **Amount**: Positive number, decimal for currency
- **Probability**: 0-100
- **Dates**: ISO 8601 format (YYYY-MM-DD or YYYY-MM-DDTHH:mm:ssZ)

---

## Testing the Setup

Once all tenants are created:

### 1. Login to Each Tenant
```bash
POST http://localhost:8080/api/auth/login
{
  "email": "demo@bocrm.com",
  "password": "demo123"
}
```

### 2. Verify Custom Fields
```bash
GET http://localhost:8080/api/custom-fields?entityType=Customer
```

### 3. View Calculated Field Values
```bash
GET http://localhost:8080/api/customers
```

### 4. Test Policies
```bash
PUT http://localhost:8080/api/customers/{id}
{
  "status": "archived"
  # Should trigger policy DENY if conditions met
}
```

### 5. Frontend Dashboard
Navigate to http://localhost:5173
- View customers with all custom fields populated
- See calculated fields in table view
- Try actions that trigger policies
- Review activities timeline
- Check opportunities with workflow stages

---

## Troubleshooting

**Issue**: Custom field creation fails with 400 error
- **Solution**: Check field key format (alphanumeric + underscore only)
- **Check**: Entity type must be Customer|Contact|Opportunity|Activity

**Issue**: Policy rule not triggering
- **Solution**: Verify Rego expression syntax — strings require double quotes, not single quotes
- **Check**: Use `input.entity.fieldName`, not `entity.fieldName`
- **Check**: For UPDATE operations, use `input.previous.fieldName` for old values
- **Check**: OPA sidecar must be running (`docker compose up -d opa`)

**Issue**: Calculated field shows error
- **Solution**: Check CEL expression syntax (CEL, not SpEL — these are different languages)
- **Check**: Referenced custom field keys must exist and use `customField_<key>` prefix
- **Check**: For optional fields, use null check: `field != null ? field : 0`
- **Check**: Math functions: use `math.min()`, `math.max()` — not `T(java.lang.Math).min()`

**Issue**: Sample data creation slow
- **Solution**: Reduce scope if needed
- **Check**: You can run individual customer/opportunity posts in parallel

---

## Performance Notes

- Each tenant setup takes ~2 minutes for complete setup
- 15 customers × 20 contacts × 15 opportunities × 20 activities = ~3,800 records per tenant
- 3 tenants = ~11,400 total records for demo
- Database size: ~50-100MB for full setup with indexes

---

## Integration Framework (Optional Demo Feature)

BOCRM supports real-time event distribution to external systems via the **Integrations** panel (Admin → Integrations).

### Supported Adapters

| Adapter | Use Case | Required Config |
|---------|----------|-----------------|
| **Slack** | Post CRM events as channel messages | Webhook URL |
| **Webhook** | Generic HTTP POST to any endpoint | URL, optional HMAC secret |
| **HubSpot** | Sync Customers/Contacts/Opportunities | API Key |
| **Zapier** | Trigger Zaps from CRM events | Zapier Catch Hook URL |

### Events Available

`CUSTOMER_CREATED`, `CUSTOMER_UPDATED`, `CUSTOMER_DELETED`, `CONTACT_CREATED`, `CONTACT_UPDATED`, `CONTACT_DELETED`, `OPPORTUNITY_CREATED`, `OPPORTUNITY_UPDATED`, `OPPORTUNITY_DELETED`, `ACTIVITY_CREATED`, `ACTIVITY_UPDATED`, `ACTIVITY_DELETED`

### Quick Demo Setup (Slack)

```http
POST http://localhost:8080/api/integrations
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "name": "Slack - Sales Alerts",
  "adapterType": "SLACK",
  "enabled": true,
  "credentials": {
    "webhookUrl": "https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK"
  },
  "eventTypes": ["OPPORTUNITY_CREATED", "OPPORTUNITY_UPDATED", "CUSTOMER_CREATED"]
}
```

### Quick Demo Setup (Webhook — use webhook.site for testing)

```http
POST http://localhost:8080/api/integrations
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "name": "Test Webhook",
  "adapterType": "WEBHOOK",
  "enabled": true,
  "credentials": {
    "url": "https://webhook.site/your-unique-url"
  },
  "eventTypes": ["CUSTOMER_CREATED", "OPPORTUNITY_CREATED"]
}
```

After creating an integration, trigger a CRM event (create a customer or opportunity) and verify the payload arrives at the target system. Events are dispatched every 30 seconds by the integration poller.

---

## Next Steps After Setup

1. **Login as demo user** → See all 3 tenants in selector
2. **Switch between tenants** → Verify isolation and custom branding
3. **Add more data** → Use frontend forms to add more records
4. **Test AI Assistant** → Try prompts like:
   - "Create a customer report for Q1"
   - "Generate an opportunity slide deck"
   - "Suggest new policies for high-value deals"
5. **Configure preferences** → Set notification rules, AI tier selection

---

## Files Reference

- **HTTP Test File**: `backend/api-tests/12-demo-tenants-comprehensive-setup.http` (Tenant 1 only)
- **This Guide**: `DEMO_TENANT_SETUP_GUIDE.md`
- **API Tests**: `backend/api-tests/` directory
- **Backend**: `backend/src/main/java/com/bocrm/backend/`
- **Frontend**: `frontend/src/`
