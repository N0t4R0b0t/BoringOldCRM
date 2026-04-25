# Standard Tenants — Staged Setup Prompts

Break each large setup into 9–10 focused steps. Paste one step at a time, wait for completion, then continue.

---

## How to Use

1. Go to `/chat` or `/onboarding` → Go to Chat
2. Paste **Step 1**, wait for the assistant to finish
3. Paste **Step 2**, and so on
4. Each step references prior work by entity name — the assistant will look up IDs automatically

---

## Tenant 1: Acme Tech Solutions

### Step 1/10 — Customer Custom Fields

```
I'm starting to set up a CRM workspace for "Acme Tech Solutions". Please create these custom fields for Customers:

1. "Industry" — text, required
2. "Company Size" — number (min: 1, max: 1000000)
3. "Annual Revenue" — currency (USD)
4. "Risk Level" — select, required, options: Low, Medium, High, Critical
5. "ISO/SOC2 Certification Expiry" — date
6. "Has Active Contract" — boolean, default: false
7. "Technical Architecture Notes" — textarea
8. "Website" — url
9. "Primary Contact Email" — email
10. "Support Phone" — phone
11. "YoY Growth Rate (%)" — percentage

Create all 11 fields now.
```

---

### Step 2/10 — Opportunity Custom Fields

```
I'm continuing setup for Acme Tech Solutions. Customer custom fields are done. Now create these custom fields for Opportunities:

1. "Key Decision Factors" — multiselect, options: Price, Features, Support, Integration, Security, Timeline
2. "Detailed Opportunity Description" — richtext
3. "Sales Pipeline Stage" — workflow, milestones: Discovery, Needs Analysis, Proposal, Negotiation, Closed Won
```

---

### Step 3/10 — Calculated Fields & Business Policies

```
I'm continuing setup for Acme Tech Solutions. All custom fields are done. Now create:

**Calculated Fields:**
1. For Customer — "Engagement Score"
   Expression: customField_company_size != null && customField_yoy_growth_rate_percentage != null ? (customField_company_size / 100 + customField_yoy_growth_rate_percentage / 2 > 100 ? 100 : customField_company_size / 100 + customField_yoy_growth_rate_percentage / 2) : 0

2. For Opportunity — "Deal Progression (%)"
   Expression: customField_sales_pipeline_stage != null && customField_sales_pipeline_stage.currentIndex != null ? (customField_sales_pipeline_stage.currentIndex + 1) * 20 : 0

**Business Policies (Rego syntax, each condition on its own line):**
1. DENY on UPDATE — "Block Archiving High-Value Customers"
   input.entity.annual_revenue > 10000000

2. WARN on UPDATE — "Critical Customer Modification Warning"
   input.entity.risk_level == "Critical"

3. DENY on UPDATE — "Block Opportunity Stage Regression"
   input.previous.sales_pipeline_stage.currentIndex >= 3
   input.entity.sales_pipeline_stage.currentIndex < input.previous.sales_pipeline_stage.currentIndex
```

---

### Step 4/10 — Tenant Settings

```
Update tenant settings for Acme Tech Solutions:
- primaryColor: "#0066CC"
- orgBio: "Leading enterprise software solutions provider specializing in cloud infrastructure and DevOps consulting. We serve Fortune 500 companies across North America with cutting-edge technology and expert guidance."
```

---

### Step 5/10 — Customers (1–8)

```
I'm continuing setup for Acme Tech Solutions. Create the following customers with their custom field values:

1. TechCore Inc — status: active
   Industry: Cloud Computing, Company Size: 450, Annual Revenue: 125000000, Risk Level: Low, YoY Growth Rate: 45.5, Has Active Contract: true
   Website: https://techcore.io, Email: sales@techcore.io, Phone: +1-415-555-0001
   Technical Notes: AWS-first infrastructure, multi-region deployment, 99.99% SLA requirement

2. DataFlow Systems — status: active
   Industry: Data Analytics, Company Size: 320, Annual Revenue: 87500000, Risk Level: Low, YoY Growth Rate: 62.0, Has Active Contract: true
   Website: https://dataflow-sys.com, Email: enterprise@dataflow-sys.com, Phone: +1-408-555-0002
   Technical Notes: Real-time streaming pipeline, custom ML integrations, Kafka cluster

3. SecureVault Corp — status: active
   Industry: Cybersecurity, Company Size: 285, Annual Revenue: 95500000, Risk Level: Medium, YoY Growth Rate: 38.2, Has Active Contract: true
   Website: https://securevault.io, Email: partnerships@securevault.io, Phone: +1-202-555-0003

4. CloudNine Solutions — status: prospect
   Industry: Cloud Infrastructure, Company Size: 150, Annual Revenue: 32000000, Risk Level: Low, YoY Growth Rate: 75.5, Has Active Contract: false
   Website: https://cloudnine.io, Email: info@cloudnine.io, Phone: +1-510-555-0004

5. FinanceFlow Ltd — status: active
   Industry: Financial Services, Company Size: 580, Annual Revenue: 245000000, Risk Level: Critical, YoY Growth Rate: 12.3, Has Active Contract: true
   Website: https://financeflow.uk, Email: enterprise@financeflow.uk, Phone: +44-207-555-0005

6. MediTech Innovations — status: active
   Industry: Healthcare Technology, Company Size: 420, Annual Revenue: 156000000, Risk Level: High, YoY Growth Rate: 28.5, Has Active Contract: true
   Website: https://meditech.io, Email: sales@meditech.io, Phone: +1-617-555-0006

7. RetailMax Group — status: active
   Industry: Retail & E-commerce, Company Size: 890, Annual Revenue: 520000000, Risk Level: Low, YoY Growth Rate: 33.7, Has Active Contract: true
   Website: https://retailmax.com, Email: b2b@retailmax.com, Phone: +1-312-555-0007

8. LogiChain Global — status: prospect
   Industry: Logistics & Supply Chain, Company Size: 2100, Annual Revenue: 1250000000, Risk Level: Medium, YoY Growth Rate: 18.2, Has Active Contract: false
   Website: https://logichain.com, Email: partnerships@logichain.com, Phone: +1-713-555-0008
```

---

### Step 6/10 — Customers (9–15)

```
I'm continuing setup for Acme Tech Solutions. Create these additional customers:

9. EduConnect Platform — status: active
   Industry: Education Technology, Company Size: 210, Annual Revenue: 48000000, Risk Level: Low, YoY Growth Rate: 95.3, Has Active Contract: true
   Website: https://educonnect.io, Email: enterprise@educonnect.io, Phone: +1-206-555-0009

10. EnergyOptimize Inc — status: active
    Industry: Energy Management, Company Size: 340, Annual Revenue: 112000000, Risk Level: Low, YoY Growth Rate: 41.8, Has Active Contract: true
    Website: https://energyoptimize.com, Email: enterprise@energyoptimize.com, Phone: +1-512-555-0010

11. BuildSmart Solutions — status: prospect
    Industry: Construction Technology, Company Size: 175, Annual Revenue: 38000000, Risk Level: Medium, YoY Growth Rate: 52.4, Has Active Contract: false
    Website: https://buildsmart.io, Email: contact@buildsmart.io, Phone: +1-720-555-0011

12. LabAnalytics Pro — status: active
    Industry: Scientific Research, Company Size: 95, Annual Revenue: 22000000, Risk Level: Low, YoY Growth Rate: 67.2, Has Active Contract: true
    Website: https://labanalytics.io, Email: partnerships@labanalytics.io, Phone: +1-858-555-0012

13. MediaSense Network — status: active
    Industry: Media & Entertainment, Company Size: 650, Annual Revenue: 380000000, Risk Level: Low, YoY Growth Rate: 29.5, Has Active Contract: true
    Website: https://mediasense.io, Email: b2b@mediasense.io, Phone: +1-424-555-0013

14. TravelHub Innovations — status: active
    Industry: Travel & Hospitality, Company Size: 520, Annual Revenue: 215000000, Risk Level: Medium, YoY Growth Rate: 35.8, Has Active Contract: true
    Website: https://travelhub.io, Email: enterprise@travelhub.io, Phone: +1-201-555-0014

15. AgriSmartTech — status: inactive
    Industry: Agriculture Technology, Company Size: 140, Annual Revenue: 29000000, Risk Level: Low, YoY Growth Rate: 88.5, Has Active Contract: false
    Website: https://agrismarttech.io, Email: sales@agrismarttech.io, Phone: +1-515-555-0015
```

---

### Step 7/10 — Contacts

```
I'm continuing setup for Acme Tech Solutions. Create the following 20 contacts, each linked to the named customer (look up the customer ID by name):

1. John Mitchell — VP Engineering, TechCore Inc
2. Sarah Chen — CTO, TechCore Inc
3. Mark Rodriguez — Director Ops, DataFlow Systems
4. Lisa Park — Procurement Lead, DataFlow Systems
5. David Kumar — CISO, SecureVault Corp
6. Jennifer Wong — VP Sales, SecureVault Corp
7. James O'Brien — CEO, CloudNine Solutions
8. Patricia Anderson — Product Manager, CloudNine Solutions
9. Robert Thompson — Chief Risk Officer, FinanceFlow Ltd
10. Emma Clarke — VP Technology, FinanceFlow Ltd
11. Michael Hassan — Chief Medical Officer, MediTech Innovations
12. Amanda Lewis — Compliance Officer, MediTech Innovations
13. Christopher Davis — VP Digital Transformation, RetailMax Group
14. Victoria Martinez — Director Supply Chain, RetailMax Group
15. Alexander Schmidt — President, LogiChain Global
16. Nicole Johnson — Technology Director, LogiChain Global
17. Gregory Patel — Chief Academic Officer, EduConnect Platform
18. Rachel White — VP Partnerships, EduConnect Platform
19. Timothy Brown — VP Solutions, EnergyOptimize Inc
20. Sophia Garcia — Implementation Manager, EnergyOptimize Inc
```

---

### Step 8/10 — Opportunities

```
I'm continuing setup for Acme Tech Solutions. Create these 15 opportunities. Link each to the named customer. Set the "Sales Pipeline Stage" workflow field's currentIndex as specified:

1. Infrastructure Platform Migration — customer: TechCore Inc, amount: 2500000, probability: 75, stage: proposal, Sales Pipeline Stage currentIndex: 2, Decision Factors: Integration, Security, Support
2. Real-time Analytics Platform Enhancement — customer: DataFlow Systems, amount: 1800000, probability: 85, stage: negotiation, Sales Pipeline Stage currentIndex: 3, Decision Factors: Features, Price
3. SOC 2 Type II Compliance Consulting — customer: SecureVault Corp, amount: 450000, probability: 65, stage: qualification, Sales Pipeline Stage currentIndex: 1, Decision Factors: Support, Timeline
4. Premium Enterprise Support Package — customer: CloudNine Solutions, amount: 380000, probability: 55, stage: qualification, Sales Pipeline Stage currentIndex: 1, Decision Factors: Support, Price
5. RegTech Integration & Automation — customer: FinanceFlow Ltd, amount: 5200000, probability: 90, stage: negotiation, Sales Pipeline Stage currentIndex: 3, Decision Factors: Integration, Security
6. HIPAA-Compliant Architecture Redesign — customer: MediTech Innovations, amount: 3100000, probability: 70, stage: proposal, Sales Pipeline Stage currentIndex: 2, Decision Factors: Security, Timeline, Support
7. Unified Omnichannel Commerce Platform — customer: RetailMax Group, amount: 4800000, probability: 60, stage: proposal, Sales Pipeline Stage currentIndex: 2, Decision Factors: Features, Integration
8. End-to-End Supply Chain Visibility — customer: LogiChain Global, amount: 6500000, probability: 45, stage: qualification, Sales Pipeline Stage currentIndex: 1, Decision Factors: Features, Price, Timeline
9. Advanced Learning Analytics & AI Tutoring — customer: EduConnect Platform, amount: 980000, probability: 75, stage: proposal, Sales Pipeline Stage currentIndex: 2, Decision Factors: Features, Support
10. Smart Grid Integration & Predictive Maintenance — customer: EnergyOptimize Inc, amount: 2200000, probability: 70, stage: proposal, Sales Pipeline Stage currentIndex: 2, Decision Factors: Features, Integration
11. Integrated Project & Cost Management Suite — customer: BuildSmart Solutions, amount: 650000, probability: 40, stage: qualification, Sales Pipeline Stage currentIndex: 1, Decision Factors: Features, Support
12. Laboratory Information Management System — customer: LabAnalytics Pro, amount: 580000, probability: 80, stage: negotiation, Sales Pipeline Stage currentIndex: 3, Decision Factors: Features, Integration
13. Global Content Delivery & Monetization Platform — customer: MediaSense Network, amount: 3400000, probability: 65, stage: proposal, Sales Pipeline Stage currentIndex: 2, Decision Factors: Features, Integration, Support
14. Next-Gen Travel Booking & Reservation Engine — customer: TravelHub Innovations, amount: 2900000, probability: 55, stage: proposal, Sales Pipeline Stage currentIndex: 2, Decision Factors: Features, Price
15. Precision Agriculture IoT & Analytics Platform — customer: AgriSmartTech, amount: 1200000, probability: 50, stage: qualification, Sales Pipeline Stage currentIndex: 1, Decision Factors: Features, Integration
```

---

### Step 9/10 — Activities

```
I'm continuing setup for Acme Tech Solutions. Create 18 activities across customers and opportunities. Spread dates across January–February 2024:

1. TechCore - Discovery Call — call, 30 min, 2024-01-15, completed, linked to TechCore Inc
2. DataFlow - Technical Assessment — meeting, 90 min, 2024-01-16, completed, linked to DataFlow Systems
3. SecureVault - Compliance Demo — meeting, 60 min, 2024-01-18, completed, linked to SecureVault Corp
4. CloudNine - Discovery Call — call, 45 min, 2024-01-19, completed, linked to CloudNine Solutions
5. FinanceFlow - Proposal Submission — email, 15 min, 2024-01-22, completed, linked to FinanceFlow Ltd
6. MediTech - Architecture Review — meeting, 90 min, 2024-01-23, pending, linked to MediTech Innovations
7. RetailMax - Demo Session — meeting, 60 min, 2024-01-24, pending, linked to RetailMax Group
8. LogiChain - Needs Analysis — meeting, 60 min, 2024-01-25, pending, linked to LogiChain Global
9. EduConnect - Follow-up Discussion — call, 45 min, 2024-01-29, completed, linked to EduConnect Platform
10. EnergyOptimize - Technical Assessment — meeting, 60 min, 2024-01-30, completed, linked to EnergyOptimize Inc
11. TechCore - Negotiation Meeting — meeting, 90 min, 2024-02-01, pending, linked to TechCore Inc
12. DataFlow - Quarterly Review — call, 45 min, 2024-02-02, pending, linked to DataFlow Systems
13. FinanceFlow - Negotiation — meeting, 90 min, 2024-02-05, pending, linked to FinanceFlow Ltd
14. LabAnalytics - Proposal Submission — email, 20 min, 2024-02-06, completed, linked to LabAnalytics Pro
15. MediaSense - Demo Session — meeting, 60 min, 2024-02-08, pending, linked to MediaSense Network
16. TravelHub - Discovery Call — call, 45 min, 2024-02-09, completed, linked to TravelHub Innovations
17. BuildSmart - Initial Call — call, 30 min, 2024-02-12, completed, linked to BuildSmart Solutions
18. EduConnect - Negotiation — meeting, 60 min, 2024-02-15, pending, linked to EduConnect Platform
```

---

### Step 10/10 — CustomRecords, Orders, Invoices & Templates

```
I'm finishing setup for Acme Tech Solutions. Create all of the following:

**10 CustomRecords (bulkCreateCustomRecords):**
1. Cloud Infrastructure License — software, active, LIC-TCORE-2024, TechCore Inc
2. Enterprise Security Appliance — hardware, active, ESA-SVLT-001, SecureVault Corp
3. Dedicated Support Server — hardware, active, SRV-MEDI-001, MediTech Innovations
4. Analytics Platform License — software, active, LIC-DFS-2024, DataFlow Systems
5. CloudNine Backup Appliance — hardware, maintenance, BK-CN-001, CloudNine Solutions
6. FinanceFlow Compliance Module — software, active, LIC-FF-COMP, FinanceFlow Ltd
7. RetailMax POS Integration Module — software, active, LIC-RM-POS, RetailMax Group
8. LogiChain IoT Gateway Hub — hardware, inactive, GW-LC-HUB2, LogiChain Global
9. EduConnect Platform License — software, active, LIC-EDU-PLAT, EduConnect Platform
10. Smart Sensor Array Kit — hardware, active, SNS-EO-ARR1, EnergyOptimize Inc

**6 Orders:**
1. TechCore Inc — "Q1 Infrastructure Consulting Package", DELIVERED, $185,000 USD
2. DataFlow Systems — "Advanced Analytics Expansion Bundle", CONFIRMED, $92,000 USD
3. SecureVault Corp — "Security Audit & Remediation Services", SHIPPED, $45,000 USD
4. FinanceFlow Ltd — "RegTech Module Annual License", DRAFT, $520,000 USD
5. MediTech Innovations — "HIPAA Compliance Toolkit Renewal", CANCELLED, $78,000 USD
6. RetailMax Group — "Omnichannel Integration Bundle", DELIVERED, $230,000 USD

**6 Invoices (same customers as orders above):**
1. TechCore Inc — PAID, $185,000 USD, NET-30
2. DataFlow Systems — SENT, $92,000 USD, NET-45
3. SecureVault Corp — PAID, $45,000 USD, NET-30
4. FinanceFlow Ltd — DRAFT, $520,000 USD, NET-60
5. MediTech Innovations — OVERDUE, $78,000 USD, NET-30
6. RetailMax Group — PAID, $230,000 USD, NET-30

**3 Document Templates:**
1. "Tech Sales Executive Deck" — slide_deck, {"layout":"corporate","accentColor":"#0066CC","backgroundColor":"#0A1929","h1Color":"#00D9FF"}
2. "Deal One-Pager (Light)" — one_pager, {"layout":"light","accentColor":"#0066CC","includeCustomFields":true}
3. "Customer Pipeline Export" — csv_export, {"includeFields":["name","status","industry","annual_revenue","risk_level"]}

**3 Notification Templates:**
1. "New Opportunity Alert" — OPPORTUNITY_CREATED, subject: "New Opportunity: {{opportunityName}} ({{customerName}})", body: "New opportunity created.\n\nCustomer: {{customerName}}\nAmount: {{amount}}\nStage: {{stage}}\n\nView: {{link}}"
2. "Deal Stage Change" — OPPORTUNITY_UPDATED, subject: "Deal Updated: {{opportunityName}} → {{stage}}", body: "Stage updated.\n\nCustomer: {{customerName}}\nNew Stage: {{stage}}\nAmount: {{amount}}\n\n{{link}}"
3. "New Customer Onboarding" — CUSTOMER_CREATED, subject: "New Customer Added: {{customerName}}", body: "Customer added.\n\nName: {{customerName}}\nStatus: {{status}}\n\nProfile: {{link}}"
```

---

## Tenant 2: Global Finance Corp

### Step 1/10 — Customer Custom Fields

```
I'm starting to set up a CRM workspace for "Global Finance Corp". Create these custom fields for Customers:

1. "Account Type" — select, required, options: Commercial, Institutional, Investment, Private Banking
2. "Assets Under Management" — currency (USD)
3. "Credit Rating" — select, options: AAA, AA, A, BBB, BB, B, CCC
4. "Regulatory Status" — text
5. "Last Audit Date" — date
6. "Is Regulated" — boolean, default: true
7. "Compliance Notes" — textarea
8. "Website" — url
9. "Primary Contact Email" — email
10. "Main Phone" — phone
11. "Leverage Ratio (%)" — percentage
```

---

### Step 2/10 — Opportunity & Financial Instrument Custom Fields

```
I'm continuing setup for Global Finance Corp. Create these custom fields for Opportunities and Financial Instruments (CustomRecords):

**Opportunity custom fields:**
1. "Product Category" — multiselect, options: Corporate Lending, Treasury, Investment Banking, Risk Management, FX, Derivatives
2. "Deal Structure Overview" — richtext
3. "Deal Lifecycle" — workflow, milestones: RFP, Due Diligence, Term Sheet, Negotiation, Closed Won
4. "Compliance Checklist" — workflow, milestones: Initial Review, KYC Check, Legal Review, Risk Assessment, Approved

**Financial Instrument (CustomRecord) custom fields:**
5. "Instrument Type" — select, required, options: License, Hardware, Platform, System, Module
6. "Annual Maintenance Cost" — currency (USD)
7. "Contract Expiry" — date
8. "Vendor" — text
9. "Is Business Critical" — boolean
10. "SLA Response Time" — select, options: 1 Hour, 4 Hours, 8 Hours, Next Business Day
11. "Support Notes" — textarea
12. "Vendor Contact Email" — email
```

---

### Step 3/10 — Calculated Fields & Policies

```
I'm continuing setup for Global Finance Corp. Create:

**Calculated Fields:**
1. For Customer — "Risk Score"
   Expression: customField_credit_rating != null && customField_leverage_ratio_percentage != null ? (customField_leverage_ratio_percentage > 12.0 ? 3 : customField_leverage_ratio_percentage > 9.0 ? 2 : 1) : 0

2. For Customer — "Portfolio Tier"
   Expression: customField_assets_under_management != null ? (customField_assets_under_management >= 200000000000.0 ? 5 : customField_assets_under_management >= 100000000000.0 ? 4 : customField_assets_under_management >= 50000000000.0 ? 3 : customField_assets_under_management >= 10000000000.0 ? 2 : 1) : 0

3. For Customer — "Compliance Health"
   Expression: customField_is_regulated != null && customField_leverage_ratio_percentage != null ? (customField_is_regulated ? (customField_leverage_ratio_percentage <= 9.0 ? 3 : customField_leverage_ratio_percentage <= 12.0 ? 2 : 1) : 0) : 0

4. For Opportunity — "Deal Completion (%)"
   Expression: customField_deal_lifecycle != null && customField_deal_lifecycle.currentIndex != null ? (customField_deal_lifecycle.currentIndex + 1) * 20 : 0

5. For Opportunity — "Compliance Progress (%)"
   Expression: customField_compliance_checklist != null && customField_compliance_checklist.currentIndex != null ? (customField_compliance_checklist.currentIndex + 1) * 20 : 0

6. For Financial Instrument (CustomRecord) — "Annual Cost"
   Expression: customField_annual_maintenance_cost != null ? customField_annual_maintenance_cost : 0

**Business Policies (Rego syntax, each condition on its own line):**
1. DENY on UPDATE — "Block Archiving Regulated Accounts"
   input.entity.is_regulated == true

2. WARN on UPDATE — "High Leverage Account Warning"
   input.entity.leverage_ratio_percentage > 12

3. DENY on UPDATE — "Block Closing Pre-Due-Diligence Deals"
   input.entity.deal_lifecycle.currentIndex < 1

4. DENY on DELETE — "Protect Regulated Accounts from Deletion"
   input.entity.is_regulated == true

5. WARN on CREATE — "Large Deal Committee Review"
   input.entity.value > 250000000

6. WARN on UPDATE — "Leverage Ratio Increase Warning"
   input.entity.leverage_ratio_percentage > 13
   input.previous.leverage_ratio_percentage <= 13

7. WARN on UPDATE — "Product Category Change on Advanced Deal"
   input.entity.deal_lifecycle.currentIndex >= 2
```

---

### Step 4/10 — Tenant Settings

```
Update tenant settings for Global Finance Corp:
- primaryColor: "#1B4A2E"
- orgBio: "Leading global financial services firm specializing in corporate banking, investment management, and risk solutions. We provide sophisticated financial infrastructure to institutional clients worldwide, with 50+ years of market expertise and offices across 35 countries."
```

---

### Step 5/10 — Customers (1–8)

```
I'm continuing setup for Global Finance Corp. Create these customers:

1. JP Morgan Capital — status: active, Account Type: Commercial, AUM: 250000000000, Credit Rating: AAA, Regulatory Status: Licensed, Is Regulated: true, Leverage Ratio: 8.5, Website: https://jpmorgan.com, Email: corporate@jpmorgan.com, Phone: +1-212-270-6000, Compliance Notes: Full regulatory oversight, quarterly audits
2. Goldman Sachs Investment Partnership — status: active, Account Type: Investment, AUM: 180000000000, Credit Rating: AAA, Regulatory Status: Licensed, Is Regulated: true, Leverage Ratio: 12, Website: https://goldmansachs.com, Email: enterprise@goldmansachs.com, Phone: +1-212-902-1000, Compliance Notes: SEC regulated, fully compliant
3. Barclays Capital Management — status: active, Account Type: Institutional, AUM: 165000000000, Credit Rating: AA, Regulatory Status: Licensed, Is Regulated: true, Leverage Ratio: 10, Website: https://barclays.com, Email: capital@barclays.com, Phone: +44-20-7116-1000, Compliance Notes: FCA regulated entity
4. HSBC Global Banking — status: active, Account Type: Commercial, AUM: 220000000000, Credit Rating: AA, Regulatory Status: Licensed, Is Regulated: true, Leverage Ratio: 9, Website: https://hsbc.com, Email: global@hsbc.com, Phone: +44-20-7919-1000, Compliance Notes: Multi-jurisdiction compliance
5. Deutsche Bank Securities — status: active, Account Type: Investment, AUM: 145000000000, Credit Rating: A, Regulatory Status: Licensed, Is Regulated: true, Leverage Ratio: 11, Website: https://deutschebank.com, Email: securities@db.com, Phone: +49-69-910-0, Compliance Notes: BaFin regulated
6. BNY Mellon Trust — status: active, Account Type: Institutional, AUM: 195000000000, Credit Rating: AAA, Regulatory Status: Licensed, Is Regulated: true, Leverage Ratio: 8, Website: https://bnymellon.com, Email: trust@bnymellon.com, Phone: +1-212-635-1500, Compliance Notes: Fiduciary standards compliance
7. Morgan Stanley Advisory — status: active, Account Type: Investment, AUM: 155000000000, Credit Rating: AA, Regulatory Status: Licensed, Is Regulated: true, Leverage Ratio: 13, Website: https://morganstanley.com, Email: advisory@morganstanley.com, Phone: +1-212-761-4000, Compliance Notes: FINRA regulated advisor
8. Citigroup Corporate — status: active, Account Type: Commercial, AUM: 210000000000, Credit Rating: AA, Regulatory Status: Licensed, Is Regulated: true, Leverage Ratio: 9.5, Website: https://citigroup.com, Email: corporate@citigroup.com, Phone: +1-212-559-1000, Compliance Notes: OCC supervised entity
```

---

### Step 6/10 — Customers (9–15)

```
I'm continuing setup for Global Finance Corp. Create these additional customers:

9. UBS Wealth Partners — status: active, Account Type: Private Banking, AUM: 280000000000, Credit Rating: AAA, Regulatory Status: Licensed, Is Regulated: true, Leverage Ratio: 7, Website: https://ubs.com, Email: wealth@ubs.com, Phone: +41-44-234-85-00, Compliance Notes: Swiss regulatory compliant
10. Credit Suisse Private — status: active, Account Type: Private Banking, AUM: 125000000000, Credit Rating: A, Regulatory Status: Licensed, Is Regulated: true, Leverage Ratio: 14, Website: https://creditsuisse.com, Email: private@creditsuisse.com, Phone: +41-44-333-84-84, Compliance Notes: Swiss banking oversight, elevated leverage ratio
11. Bank of America Commercial — status: active, Account Type: Commercial, AUM: 240000000000, Credit Rating: AA, Regulatory Status: Licensed, Is Regulated: true, Leverage Ratio: 10, Website: https://bankofamerica.com, Email: commercial@bofa.com, Phone: +1-704-386-5000
12. Wells Fargo Institutional — status: active, Account Type: Institutional, AUM: 175000000000, Credit Rating: AA, Regulatory Status: Licensed, Is Regulated: true, Leverage Ratio: 9, Website: https://wellsfargo.com, Email: institutional@wellsfargo.com, Phone: +1-866-869-3557
13. Charles Schwab Investment — status: active, Account Type: Investment, AUM: 95000000000, Credit Rating: AAA, Regulatory Status: Licensed, Is Regulated: true, Leverage Ratio: 6, Website: https://schwab.com, Email: investment@schwab.com, Phone: +1-415-667-8050
14. Fidelity Capital Partners — status: prospect, Account Type: Investment, AUM: 108000000000, Credit Rating: AAA, Regulatory Status: Licensed, Is Regulated: true, Leverage Ratio: 5.5, Website: https://fidelity.com, Email: capital@fidelity.com, Phone: +1-617-563-7000
15. BlackRock Investment Management — status: prospect, Account Type: Institutional, AUM: 310000000000, Credit Rating: AAA, Regulatory Status: Licensed, Is Regulated: true, Leverage Ratio: 8.2, Website: https://blackrock.com, Email: institutional@blackrock.com, Phone: +1-212-810-5000
```

---

### Step 7/10 — Contacts

```
I'm continuing setup for Global Finance Corp. Create 20 contacts linked to named customers:

1. Robert Harrison — Managing Director, JP Morgan Capital
2. Katherine Williams — Head of Treasury, JP Morgan Capital
3. Michael Torres — VP Client Services, Goldman Sachs Investment Partnership
4. Jennifer Lee — Risk Management Lead, Goldman Sachs Investment Partnership
5. David Martinez — Director of Compliance, Barclays Capital Management
6. Sarah Johnson — VP Corporate Banking, Barclays Capital Management
7. Christopher Chen — Senior Relationship Manager, HSBC Global Banking
8. Elizabeth Brown — Investment Advisor, HSBC Global Banking
9. Andreas Mueller — Head of Derivatives, Deutsche Bank Securities
10. Nicole Schmidt — Compliance Officer, Deutsche Bank Securities
11. Patrick O'Connor — Executive VP, BNY Mellon Trust
12. Margaret Kelly — Treasury Specialist, BNY Mellon Trust
13. Thomas Anderson — Managing Director, Morgan Stanley Advisory
14. Victoria Campbell — VP Advisory, Morgan Stanley Advisory
15. Richard Foster — SVP Commercial, Citigroup Corporate
16. Amanda White — Regulatory Affairs, Citigroup Corporate
17. Stefan Keller — Wealth Manager, UBS Wealth Partners
18. Natasha Volkov — Portfolio Manager, UBS Wealth Partners
19. James Mitchell — Head of Private Banking, Credit Suisse Private
20. Sophia Zhang — Investment Director, Credit Suisse Private
```

---

### Step 8/10 — Opportunities

```
I'm continuing setup for Global Finance Corp. Create these 15 opportunities. Link each to the named customer. Set both "Deal Lifecycle" and "Compliance Checklist" workflow currentIndex as specified:

1. Structured Credit Facility — JP Morgan Capital, $500M, prob: 75, stage: proposal, Deal Lifecycle currentIndex: 2, Compliance Checklist currentIndex: 1, Product Category: Corporate Lending, Treasury
2. Investment Banking Advisory — Goldman Sachs Investment Partnership, $150M, prob: 85, stage: negotiation, Deal Lifecycle currentIndex: 3, Compliance Checklist currentIndex: 3, Product Category: Investment Banking, Risk Management
3. Syndicated Loan Program — Barclays Capital Management, $300M, prob: 65, stage: qualification, Deal Lifecycle currentIndex: 1, Compliance Checklist currentIndex: 1, Product Category: Corporate Lending
4. FX Hedging Services — HSBC Global Banking, $80M, prob: 55, stage: prospecting, Deal Lifecycle currentIndex: 0, Compliance Checklist currentIndex: 0, Product Category: FX, Treasury
5. Derivative Strategy — Deutsche Bank Securities, $120M, prob: 90, stage: negotiation, Deal Lifecycle currentIndex: 3, Compliance Checklist currentIndex: 4, Product Category: Derivatives, Risk Management
6. Institutional Custody Services — BNY Mellon Trust, $95M, prob: 70, stage: proposal, Deal Lifecycle currentIndex: 2, Compliance Checklist currentIndex: 2, Product Category: Corporate Lending, Treasury
7. M&A Financing — Morgan Stanley Advisory, $450M, prob: 60, stage: qualification, Deal Lifecycle currentIndex: 1, Compliance Checklist currentIndex: 0, Product Category: Investment Banking, Corporate Lending
8. Treasury Solutions — Citigroup Corporate, $200M, prob: 45, stage: prospecting, Deal Lifecycle currentIndex: 0, Compliance Checklist currentIndex: 0, Product Category: Treasury, Risk Management
9. Wealth Management Platform — UBS Wealth Partners, $180M, prob: 75, stage: proposal, Deal Lifecycle currentIndex: 2, Compliance Checklist currentIndex: 2, Product Category: Investment Banking
10. Private Banking Tech — Credit Suisse Private, $140M, prob: 80, stage: negotiation, Deal Lifecycle currentIndex: 3, Compliance Checklist currentIndex: 3, Product Category: Investment Banking, Risk Management
11. Commercial Lending — Bank of America Commercial, $350M, prob: 70, stage: proposal, Deal Lifecycle currentIndex: 2, Compliance Checklist currentIndex: 1, Product Category: Corporate Lending
12. Institutional Services — Wells Fargo Institutional, $220M, prob: 55, stage: qualification, Deal Lifecycle currentIndex: 1, Compliance Checklist currentIndex: 0, Product Category: Treasury, Risk Management
13. Investment Platform Access — Charles Schwab Investment, $100M, prob: 65, stage: proposal, Deal Lifecycle currentIndex: 2, Compliance Checklist currentIndex: 2, Product Category: Investment Banking
14. Capital Markets Infrastructure — Fidelity Capital Partners, $175M, prob: 50, stage: prospecting, Deal Lifecycle currentIndex: 0, Compliance Checklist currentIndex: 0, Product Category: Investment Banking, Derivatives
15. Risk Management Integration — BlackRock Investment Management, $280M, prob: 40, stage: qualification, Deal Lifecycle currentIndex: 1, Compliance Checklist currentIndex: 1, Product Category: Risk Management, Investment Banking
```

---

### Step 9/10 — Activities

```
I'm continuing setup for Global Finance Corp. Create these 20 activities. Link each to the named customer:

1. JP Morgan Executive Briefing — meeting, 120 min, 2024-01-15, pending, JP Morgan Capital
2. Goldman Sachs RFP Response — email, 30 min, 2024-01-16, completed, Goldman Sachs Investment Partnership
3. Barclays Due Diligence Session — meeting, 90 min, 2024-01-18, pending, Barclays Capital Management
4. HSBC Regulatory Consultation — call, 60 min, 2024-01-19, completed, HSBC Global Banking
5. Deutsche Bank Term Sheet Discussion — meeting, 75 min, 2024-01-22, pending, Deutsche Bank Securities
6. BNY Mellon Contract Negotiation — meeting, 120 min, 2024-01-23, pending, BNY Mellon Trust
7. Morgan Stanley Executive Briefing — meeting, 120 min, 2024-01-24, completed, Morgan Stanley Advisory
8. Citigroup RFP Review — email, 25 min, 2024-01-25, completed, Citigroup Corporate
9. UBS Due Diligence Session — meeting, 90 min, 2024-01-29, pending, UBS Wealth Partners
10. Credit Suisse Term Sheet Discussion — meeting, 90 min, 2024-01-30, pending, Credit Suisse Private
11. Bank of America Quarterly Review — call, 60 min, 2024-02-01, pending, Bank of America Commercial
12. Wells Fargo Due Diligence Session — meeting, 90 min, 2024-02-02, pending, Wells Fargo Institutional
13. Charles Schwab Executive Briefing — meeting, 120 min, 2024-02-05, completed, Charles Schwab Investment
14. Fidelity RFP Response — email, 30 min, 2024-02-06, completed, Fidelity Capital Partners
15. BlackRock Regulatory Consultation — call, 60 min, 2024-02-08, completed, BlackRock Investment Management
16. JP Morgan Contract Negotiation — meeting, 120 min, 2024-02-09, pending, JP Morgan Capital
17. Goldman Sachs Quarterly Review — call, 60 min, 2024-02-12, pending, Goldman Sachs Investment Partnership
18. Barclays Executive Briefing — meeting, 120 min, 2024-02-13, completed, Barclays Capital Management
19. HSBC Term Sheet Discussion — meeting, 75 min, 2024-02-15, pending, HSBC Global Banking
20. Deutsche Bank Contract Negotiation — meeting, 120 min, 2024-02-16, pending, Deutsche Bank Securities
```

---

### Step 10/10 — CustomRecords, Orders, Invoices & Templates

```
I'm finishing setup for Global Finance Corp. Create all of the following:

**10 Financial Instruments (bulkCreateCustomRecords):**
1. Bloomberg Terminal License — software, active, BBG-JPMORG-2024, JP Morgan Capital
   Instrument Type: License, Annual Maintenance Cost: $125,000, Vendor: Bloomberg LP, Is Business Critical: true, SLA Response Time: 1 Hour, Contract Expiry: 2025-01-15, Vendor Contact Email: enterprise@bloomberg.com, Support Notes: 24/7 mission-critical trading data feed
2. Compliance Monitoring System — software, active, CMS-GS-001, Goldman Sachs Investment Partnership
   Instrument Type: System, Annual Maintenance Cost: $85,000, Vendor: NICE Actimize, Is Business Critical: true, SLA Response Time: 4 Hours, Contract Expiry: 2025-03-01, Vendor Contact Email: support@niceactimize.com, Support Notes: AML and fraud detection, fully integrated
3. Secure Trading Workstation — hardware, active, STW-BARC-001, Barclays Capital Management
   Instrument Type: Hardware, Annual Maintenance Cost: $45,000, Vendor: Dell Technologies, Is Business Critical: true, SLA Response Time: 4 Hours, Contract Expiry: 2025-06-30, Vendor Contact Email: enterprise@dell.com
4. Risk Analytics Platform License — software, active, RAP-HSBC-2024, HSBC Global Banking
   Instrument Type: Platform, Annual Maintenance Cost: $210,000, Vendor: Moody's Analytics, Is Business Critical: true, SLA Response Time: 1 Hour, Contract Expiry: 2025-02-28, Vendor Contact Email: support@moodys.com, Support Notes: Global credit risk and market risk analytics
5. Encrypted Document Vault — software, active, EDV-DB-001, Deutsche Bank Securities
   Instrument Type: System, Annual Maintenance Cost: $62,000, Vendor: Thales, Is Business Critical: true, SLA Response Time: 4 Hours, Contract Expiry: 2025-04-30, Vendor Contact Email: enterprise@thales.com
6. Custody Management System — software, active, CMS-BNY-002, BNY Mellon Trust
   Instrument Type: System, Annual Maintenance Cost: $175,000, Vendor: FIS Global, Is Business Critical: true, SLA Response Time: 1 Hour, Contract Expiry: 2025-01-31, Vendor Contact Email: support@fisglobal.com, Support Notes: Core custody and settlement infrastructure
7. Regulatory Reporting Engine — software, maintenance, RRE-MS-001, Morgan Stanley Advisory
   Instrument Type: Module, Annual Maintenance Cost: $95,000, Vendor: Wolters Kluwer, Is Business Critical: true, SLA Response Time: 4 Hours, Contract Expiry: 2024-12-31, Vendor Contact Email: support@wolterskluwer.com, Support Notes: Under maintenance — upgrade to v6.2 in progress
8. HSM Hardware Security Module — hardware, active, HSM-CITI-001, Citigroup Corporate
   Instrument Type: Hardware, Annual Maintenance Cost: $38,000, Vendor: Thales, Is Business Critical: true, SLA Response Time: 1 Hour, Contract Expiry: 2025-05-31, Vendor Contact Email: enterprise@thales.com
9. Trade Surveillance Server — hardware, inactive, TSS-CS-001, Credit Suisse Private
   Instrument Type: Hardware, Annual Maintenance Cost: $52,000, Vendor: Nasdaq, Is Business Critical: false, SLA Response Time: Next Business Day, Contract Expiry: 2024-11-30, Vendor Contact Email: surveillance@nasdaq.com, Support Notes: Decommission scheduled — replacement under review
10. AML Transaction Monitor — software, active, AML-UBS-2024, UBS Wealth Partners
    Instrument Type: System, Annual Maintenance Cost: $145,000, Vendor: NICE Actimize, Is Business Critical: true, SLA Response Time: 1 Hour, Contract Expiry: 2025-03-31, Vendor Contact Email: support@niceactimize.com, Support Notes: Real-time transaction screening for wealth clients

**6 Orders:**
1. JP Morgan Capital — "Enterprise Risk Suite Annual License", DELIVERED, $2,800,000 USD
2. Goldman Sachs Investment Partnership — "Portfolio Analytics Platform", CONFIRMED, $1,750,000 USD
3. Barclays Capital Management — "Regulatory Reporting Module", SHIPPED, $920,000 GBP
4. HSBC Global Banking — "AML Compliance System Upgrade", DRAFT, $3,200,000 USD
5. Deutsche Bank Securities — "Trade Surveillance Solution", CANCELLED, $680,000 EUR
6. BNY Mellon Trust — "Fiduciary Management Platform", DELIVERED, $1,450,000 USD

**6 Invoices:**
1. JP Morgan Capital — PAID, $2,800,000 USD, NET-30
2. Goldman Sachs Investment Partnership — SENT, $1,750,000 USD, NET-45
3. Barclays Capital Management — PAID, $920,000 GBP, NET-30
4. HSBC Global Banking — DRAFT, $3,200,000 USD, NET-60
5. Deutsche Bank Securities — OVERDUE, $680,000 EUR, NET-30
6. BNY Mellon Trust — PAID, $1,450,000 USD, NET-30

**3 Document Templates:**
1. "Financial Deal Brief" — slide_deck, {"layout":"corporate","accentColor":"#1B4A2E","backgroundColor":"#0D1E15","h1Color":"#4CAF50"}
2. "Client Relationship Summary" — one_pager, {"layout":"light","accentColor":"#1B4A2E","includeCustomFields":true}
3. "Portfolio Export (Full)" — csv_export, {"includeFields":["name","status","account_type","aum","credit_rating","is_regulated"]}

**3 Notification Templates:**
1. "New Deal Alert" — OPPORTUNITY_CREATED, subject: "New Deal Opened: {{opportunityName}} ({{customerName}})", body: "New deal entered.\n\nClient: {{customerName}}\nAmount: {{amount}}\nStage: {{stage}}\n\nView: {{link}}"
2. "Compliance Flag" — CUSTOMER_UPDATED, subject: "Client Record Updated: {{customerName}}", body: "Regulated client modified.\n\nClient: {{customerName}}\nUpdated by: {{assignee}}\n\nReview: {{link}}"
3. "Invoice Due Alert" — INVOICE_CREATED, subject: "Invoice Issued: {{customerName}} — {{amount}}", body: "New invoice issued.\n\nClient: {{customerName}}\nAmount: {{amount}}\nPayment Terms: {{paymentTerms}}\nDue: {{dueDate}}\n\nView: {{link}}"
```

---

## Tenant 3: Real Estate Ventures

### Step 1/10 — Customer Custom Fields

```
I'm starting to set up a CRM workspace for "Real Estate Ventures". Create these custom fields for Customers (Properties):

1. "Property Type" — select, required, options: Office, Residential, Retail, Industrial, Mixed-Use, Land
2. "Acquisition Price" — currency (USD)
3. "Total Square Footage" — number
4. "Zoning Classification" — text
5. "Current Occupancy (%)" — percentage
6. "Construction Complete" — boolean, default: false
7. "Estimated Completion" — date
8. "Primary Address" — text
9. "Property Manager Email" — email
10. "Property Contact" — phone
11. "Sustainability Features" — multiselect, options: LEED Certified, Solar, Rainwater Harvesting, Smart Building, Net Zero Energy
```

---

### Step 2/10 — Opportunity & Property Asset Custom Fields

```
I'm continuing setup for Real Estate Ventures. Create these custom fields for Investments (Opportunities) and Property Assets (CustomRecords):

**Investment (Opportunity) custom fields:**
1. "Investment Type" — select, required, options: Direct Purchase, Joint Venture, Development Rights, Refinance, Construction Loan
2. "Deal Memo" — richtext
3. "Development Phases" — workflow, milestones: Acquisition, Planning, Design, Construction, Leasing, Completed
4. "Approval Track" — workflow, milestones: Site Survey, Environmental Review, Legal Sign-off, Board Approval, Funded

**Property Asset (CustomRecord) custom fields:**
5. "Asset Category" — select, required, options: Equipment, Infrastructure, Documentation, Permit, Fixture
6. "Purchase Value" — currency (USD)
7. "Installation Date" — date
8. "Warranty Expiry" — date
9. "Condition" — select, options: Excellent, Good, Fair, Poor, Decommissioned
10. "Insured" — boolean
11. "Maintenance Notes" — textarea
12. "Assigned Technician Email" — email
```

---

### Step 3/10 — Calculated Fields & Policies

```
I'm continuing setup for Real Estate Ventures. Create:

**Calculated Fields:**
1. For Customer — "Cost Per Square Foot"
   Expression: customField_acquisition_price != null && customField_total_square_footage != null && customField_total_square_footage > 0 ? customField_acquisition_price / customField_total_square_footage : 0

2. For Customer — "Portfolio Value Tier"
   Expression: customField_acquisition_price != null ? (customField_acquisition_price >= 200000000.0 ? 5 : customField_acquisition_price >= 100000000.0 ? 4 : customField_acquisition_price >= 50000000.0 ? 3 : customField_acquisition_price >= 20000000.0 ? 2 : 1) : 0

3. For Customer — "Occupancy Health"
   Expression: customField_current_occupancy_percentage != null ? (customField_current_occupancy_percentage >= 90.0 ? 3 : customField_current_occupancy_percentage >= 75.0 ? 2 : 1) : 0

4. For Opportunity — "Project Progress (%)"
   Expression: customField_development_phases != null && customField_development_phases.currentIndex != null ? (customField_development_phases.currentIndex + 1) * 16 : 0

5. For Opportunity — "Approval Progress (%)"
   Expression: customField_approval_track != null && customField_approval_track.currentIndex != null ? (customField_approval_track.currentIndex + 1) * 20 : 0

6. For Property Asset (CustomRecord) — "Condition Score"
   Expression: customField_condition != null ? (customField_condition == "Excellent" ? 5 : customField_condition == "Good" ? 4 : customField_condition == "Fair" ? 3 : customField_condition == "Poor" ? 2 : 1) : 0

**Business Policies (Rego syntax, each condition on its own line):**
1. DENY on UPDATE — "Block Modifying Completed Properties"
   input.entity.construction_complete == true

2. WARN on CREATE — "Large Transaction Senior Approval Required"
   input.entity.value > 50000000

3. DENY on DELETE — "Protect Active Properties from Deletion"
   input.entity.status == "active"

4. WARN on UPDATE — "Low Occupancy Rate Warning"
   input.entity.current_occupancy_percentage < 70

5. WARN on CREATE — "Large Joint Venture Approval Required"
   input.entity.investment_type == "Joint Venture"
   input.entity.value > 100000000

6. DENY on UPDATE — "Block Value Reduction on Advanced Deals"
   input.entity.development_phases.currentIndex >= 2
   input.entity.value < input.previous.value

7. WARN on CREATE — "No Sustainability Features Flag"
   input.entity.sustainability_features == null
```

---

### Step 4/10 — Tenant Settings

```
Update tenant settings for Real Estate Ventures:
- primaryColor: "#8B4513"
- orgBio: "Premier commercial and residential real estate development firm with $2B+ portfolio across North America. Specializing in mixed-use developments, sustainable construction, and institutional investment opportunities in high-growth markets."
```

---

### Step 5/10 — Customers (Properties 1–8)

```
I'm continuing setup for Real Estate Ventures. Create these properties as customers:

1. Downtown Tech Plaza — status: active, Property Type: Mixed-Use, Acquisition Price: 85000000, Sq Ft: 450000, Zoning: Commercial-Mixed Use, Occupancy: 92, Construction Complete: true, Address: 1001 Eastlake Avenue Seattle WA, Email: manager@downtowntech.com, Phone: +1-206-555-0101, Sustainability: LEED Certified, Smart Building
2. Silicon Valley Office Tower — status: active, Property Type: Office, Acquisition Price: 125000000, Sq Ft: 380000, Zoning: Commercial Office, Occupancy: 88, Construction Complete: true, Address: 100 Oracle Parkway San Jose CA, Email: manager@svtower.com, Phone: +1-408-555-0102, Sustainability: LEED Certified, Solar
3. Manhattan Residential Complex — status: active, Property Type: Residential, Acquisition Price: 320000000, Sq Ft: 520000, Zoning: Residential-Luxury, Occupancy: 95, Construction Complete: true, Address: 450 Park Avenue New York NY, Email: manager@mresidential.com, Phone: +1-212-555-0103, Sustainability: Net Zero Energy, Smart Building
4. Los Angeles Retail Corridor — status: active, Property Type: Retail, Acquisition Price: 95000000, Sq Ft: 280000, Zoning: Commercial Retail, Occupancy: 85, Construction Complete: true, Address: 9900 Wilshire Boulevard Los Angeles CA, Email: manager@laretail.com, Phone: +1-310-555-0104, Sustainability: Solar, Smart Building
5. Chicago Mixed-Use Development — status: active, Property Type: Mixed-Use, Acquisition Price: 180000000, Sq Ft: 620000, Zoning: Commercial-Mixed Use, Occupancy: 78, Construction Complete: true, Address: 233 South Wacker Drive Chicago IL, Email: manager@chicagomixed.com, Phone: +1-312-555-0105, Sustainability: LEED Certified, Rainwater Harvesting
6. Miami Waterfront Project — status: prospect, Property Type: Residential, Acquisition Price: 210000000, Sq Ft: 450000, Zoning: Residential-Waterfront, Occupancy: 82, Construction Complete: false, Estimated Completion: 2024-06-30, Address: 101 Biscayne Boulevard Miami FL, Email: manager@miamiwater.com, Phone: +1-305-555-0106, Sustainability: Rainwater Harvesting, Net Zero Energy
7. Austin Tech Hub — status: active, Property Type: Office, Acquisition Price: 110000000, Sq Ft: 340000, Zoning: Commercial Office, Occupancy: 91, Construction Complete: true, Address: 100 Congress Avenue Austin TX, Email: manager@austintech.com, Phone: +1-512-555-0107, Sustainability: LEED Certified, Solar
8. Denver Innovation District — status: prospect, Property Type: Mixed-Use, Acquisition Price: 145000000, Sq Ft: 500000, Zoning: Commercial-Mixed Use, Occupancy: 80, Construction Complete: false, Estimated Completion: 2024-08-15, Address: 1800 Larimer Street Denver CO, Email: manager@denverinnovation.com, Phone: +1-303-555-0108, Sustainability: Net Zero Energy, Smart Building
```

---

### Step 6/10 — Customers (Properties 9–15)

```
I'm continuing setup for Real Estate Ventures. Create these additional properties:

9. Portland Green Building — status: active, Property Type: Office, Acquisition Price: 65000000, Sq Ft: 220000, Zoning: Commercial Office, Occupancy: 75, Construction Complete: true, Address: 1211 SW Alder Street Portland OR, Email: manager@pdxgreen.com, Phone: +1-503-555-0109, Sustainability: LEED Certified, Solar, Rainwater Harvesting
10. Phoenix Development Site — status: prospect, Property Type: Land, Acquisition Price: 42000000, Sq Ft: 850000, Zoning: Commercial-Zoned, Occupancy: 0, Construction Complete: false, Estimated Completion: 2025-03-31, Address: Desert Sky Boulevard Phoenix AZ, Email: manager@phoenixdev.com, Phone: +1-602-555-0110
11. Las Vegas Casino Resort — status: active, Property Type: Mixed-Use, Acquisition Price: 280000000, Sq Ft: 750000, Zoning: Commercial-Entertainment, Occupancy: 88, Construction Complete: true, Address: 3355 Las Vegas Boulevard Las Vegas NV, Email: manager@lvresort.com, Phone: +1-702-555-0111, Sustainability: Smart Building
12. Nashville Music District — status: active, Property Type: Retail, Acquisition Price: 75000000, Sq Ft: 310000, Zoning: Commercial Retail, Occupancy: 83, Construction Complete: true, Address: 5th Avenue North Nashville TN, Email: manager@nashvillemusic.com, Phone: +1-615-555-0112, Sustainability: LEED Certified
13. Atlanta Corporate Campus — status: active, Property Type: Office, Acquisition Price: 155000000, Sq Ft: 420000, Zoning: Commercial Office, Occupancy: 87, Construction Complete: true, Address: 100 Peachtree Street Atlanta GA, Email: manager@atlantacorp.com, Phone: +1-404-555-0113, Sustainability: LEED Certified, Solar
14. Boston Biotech Park — status: active, Property Type: Office, Acquisition Price: 130000000, Sq Ft: 380000, Zoning: Commercial Research, Occupancy: 92, Construction Complete: true, Address: 300 Kendall Street Cambridge MA, Email: manager@bostonbiotech.com, Phone: +1-617-555-0114, Sustainability: Net Zero Energy, LEED Certified
15. Charlotte Office Tower — status: inactive, Property Type: Office, Acquisition Price: 105000000, Sq Ft: 360000, Zoning: Commercial Office, Occupancy: 89, Construction Complete: true, Address: 301 South Tryon Street Charlotte NC, Email: manager@charlotteofc.com, Phone: +1-704-555-0115, Sustainability: LEED Certified
```

---

### Step 7/10 — Contacts

```
I'm continuing setup for Real Estate Ventures. Create 20 contacts linked to named properties:

1. James Richards — VP Development, Downtown Tech Plaza
2. Patricia Moore — Leasing Manager, Downtown Tech Plaza
3. Michael Chang — VP Real Estate, Silicon Valley Office Tower
4. Lisa Anderson — Site Manager, Silicon Valley Office Tower
5. Robert Williams — Director Acquisitions, Manhattan Residential Complex
6. Jennifer Davis — Construction Lead, Manhattan Residential Complex
7. Christopher Martinez — VP Retail, Los Angeles Retail Corridor
8. Victoria Brown — Operations Manager, Los Angeles Retail Corridor
9. David Kumar — Chief Development Officer, Chicago Mixed-Use Development
10. Sarah Wilson — Project Manager, Chicago Mixed-Use Development
11. Thomas Garcia — VP Development, Miami Waterfront Project
12. Amanda Taylor — Sustainability Officer, Miami Waterfront Project
13. Gregory Rodriguez — Director Operations, Austin Tech Hub
14. Rachel Cohen — Leasing Director, Austin Tech Hub
15. Alexander Thompson — SVP Development, Denver Innovation District
16. Nicole Zhang — Architect Lead, Denver Innovation District
17. James Mitchell — VP Portland, Portland Green Building
18. Elizabeth Campbell — Facilities Manager, Portland Green Building
19. Steven Price — VP Phoenix, Phoenix Development Site
20. Margaret Hall — Land Manager, Phoenix Development Site
```

---

### Step 8/10 — Opportunities

```
I'm continuing setup for Real Estate Ventures. Create these 15 opportunities. Set both "Development Phases" and "Approval Track" workflow currentIndex as specified:

1. Downtown Tech Expansion — Downtown Tech Plaza, $65M, prob: 70, stage: qualification, Dev Phases currentIndex: 1, Approval Track currentIndex: 1, Investment Type: Joint Venture
2. Silicon Valley Tower 2 — Silicon Valley Office Tower, $140M, prob: 75, stage: proposal, Dev Phases currentIndex: 2, Approval Track currentIndex: 2, Investment Type: Development Rights
3. Manhattan Luxury Residences — Manhattan Residential Complex, $320M, prob: 60, stage: prospecting, Dev Phases currentIndex: 0, Approval Track currentIndex: 0, Investment Type: Direct Purchase
4. LA Retail Renovation — Los Angeles Retail Corridor, $45M, prob: 65, stage: proposal, Dev Phases currentIndex: 2, Approval Track currentIndex: 2, Investment Type: Refinance
5. Chicago Expansion Phase — Chicago Mixed-Use Development, $95M, prob: 80, stage: qualification, Dev Phases currentIndex: 1, Approval Track currentIndex: 1, Investment Type: Development Rights
6. Miami Waterfront Phase 2 — Miami Waterfront Project, $180M, prob: 55, stage: negotiation, Dev Phases currentIndex: 3, Approval Track currentIndex: 3, Investment Type: Construction Loan
7. Austin Tech Campus Extension — Austin Tech Hub, $85M, prob: 70, stage: qualification, Dev Phases currentIndex: 1, Approval Track currentIndex: 1, Investment Type: Joint Venture
8. Denver Innovation Phase 2 — Denver Innovation District, $120M, prob: 50, stage: proposal, Dev Phases currentIndex: 2, Approval Track currentIndex: 2, Investment Type: Direct Purchase
9. Portland Renovation Project — Portland Green Building, $35M, prob: 75, stage: negotiation, Dev Phases currentIndex: 3, Approval Track currentIndex: 4, Investment Type: Refinance
10. Phoenix Development — Phoenix Development Site, $200M, prob: 40, stage: prospecting, Dev Phases currentIndex: 0, Approval Track currentIndex: 0, Investment Type: Direct Purchase
11. Las Vegas Resort Expansion — Las Vegas Casino Resort, $150M, prob: 65, stage: qualification, Dev Phases currentIndex: 1, Approval Track currentIndex: 1, Investment Type: Construction Loan
12. Nashville District Expansion — Nashville Music District, $55M, prob: 60, stage: proposal, Dev Phases currentIndex: 2, Approval Track currentIndex: 2, Investment Type: Joint Venture
13. Atlanta Campus Phase 2 — Atlanta Corporate Campus, $110M, prob: 70, stage: qualification, Dev Phases currentIndex: 1, Approval Track currentIndex: 1, Investment Type: Development Rights
14. Boston Biotech Park Expansion — Boston Biotech Park, $105M, prob: 75, stage: proposal, Dev Phases currentIndex: 2, Approval Track currentIndex: 3, Investment Type: Direct Purchase
15. Charlotte Corporate Campus — Charlotte Office Tower, $125M, prob: 55, stage: prospecting, Dev Phases currentIndex: 0, Approval Track currentIndex: 0, Investment Type: Development Rights
```

---

### Step 9/10 — Activities

```
I'm continuing setup for Real Estate Ventures. Create 20 activities linked to named properties:

1. Downtown Tech Site Assessment — meeting, 120 min, 2024-01-15, pending, Downtown Tech Plaza
2. Silicon Valley Architectural Review — meeting, 90 min, 2024-01-16, completed, Silicon Valley Office Tower
3. Manhattan Investor Presentation — meeting, 90 min, 2024-01-18, pending, Manhattan Residential Complex
4. LA Retail Zoning Consultation — meeting, 60 min, 2024-01-19, completed, Los Angeles Retail Corridor
5. Chicago Permitting Follow-up — call, 30 min, 2024-01-22, pending, Chicago Mixed-Use Development
6. Miami Construction Progress Review — meeting, 60 min, 2024-01-23, completed, Miami Waterfront Project
7. Austin Campus Design Review — meeting, 90 min, 2024-01-24, pending, Austin Tech Hub
8. Denver Investor Presentation — meeting, 120 min, 2024-01-25, completed, Denver Innovation District
9. Portland Sustainability Tracking — email, 25 min, 2024-01-29, completed, Portland Green Building
10. Phoenix Site Assessment — meeting, 120 min, 2024-01-30, pending, Phoenix Development Site
11. Las Vegas Tenant Lease Signing — meeting, 60 min, 2024-02-01, pending, Las Vegas Casino Resort
12. Nashville Architectural Review — meeting, 90 min, 2024-02-02, completed, Nashville Music District
13. Atlanta Zoning Consultation — meeting, 75 min, 2024-02-05, pending, Atlanta Corporate Campus
14. Boston Construction Progress Review — meeting, 60 min, 2024-02-06, completed, Boston Biotech Park
15. Charlotte Permitting Follow-up — call, 30 min, 2024-02-08, completed, Charlotte Office Tower
16. Downtown Tech Investor Presentation — meeting, 90 min, 2024-02-09, pending, Downtown Tech Plaza
17. Silicon Valley Tenant Lease Signing — meeting, 60 min, 2024-02-12, pending, Silicon Valley Office Tower
18. Manhattan Zoning Consultation — meeting, 90 min, 2024-02-13, completed, Manhattan Residential Complex
19. LA Retail Permitting Follow-up — email, 20 min, 2024-02-15, completed, Los Angeles Retail Corridor
20. Chicago Site Assessment — meeting, 120 min, 2024-02-16, pending, Chicago Mixed-Use Development
```

---

### Step 10/10 — CustomRecords, Orders, Invoices & Templates

```
I'm finishing setup for Real Estate Ventures. Create all of the following:

**10 Property Assets (bulkCreateCustomRecords):**
1. Manhattan Tower - CAD Blueprints Package — document, active, DOCS-MANH-001, Manhattan Residential Complex
   Asset Category: Documentation, Purchase Value: $12,000, Condition: Excellent, Insured: true, Maintenance Notes: Digital archive with annual backup and off-site copy
2. LA Commercial Plaza - Security Camera System — hardware, active, SEC-LACP-001, Los Angeles Retail Corridor
   Asset Category: Infrastructure, Purchase Value: $285,000, Installation Date: 2022-03-15, Warranty Expiry: 2025-03-15, Condition: Good, Insured: true, Assigned Technician Email: security@laretail.com
3. Chicago Mixed-Use - HVAC Control Unit — hardware, maintenance, HVAC-CHI-002, Chicago Mixed-Use Development
   Asset Category: Equipment, Purchase Value: $420,000, Installation Date: 2021-08-01, Warranty Expiry: 2024-08-01, Condition: Fair, Insured: true, Maintenance Notes: Annual servicing overdue — Q2 service window scheduled, Assigned Technician Email: facilities@chicagomixed.com
4. Miami Waterfront - Marina Equipment Set — equipment, active, MARN-MIA-001, Miami Waterfront Project
   Asset Category: Equipment, Purchase Value: $950,000, Installation Date: 2023-11-01, Warranty Expiry: 2026-11-01, Condition: Excellent, Insured: true, Assigned Technician Email: marina@miamiwater.com
5. Silicon Valley Campus - Fiber Network Equipment — hardware, active, NET-SVC-001, Silicon Valley Office Tower
   Asset Category: Infrastructure, Purchase Value: $380,000, Installation Date: 2022-06-15, Warranty Expiry: 2025-06-15, Condition: Excellent, Insured: true, Assigned Technician Email: network@svtower.com
6. Boston Biotech - Access Control System — hardware, active, ACS-BOS-001, Boston Biotech Park
   Asset Category: Infrastructure, Purchase Value: $195,000, Installation Date: 2022-01-10, Warranty Expiry: 2025-01-10, Condition: Good, Insured: true, Assigned Technician Email: security@bostonbiotech.com
7. Denver Tech Park - Solar Panel Array — equipment, active, SOL-DEN-001, Denver Innovation District
   Asset Category: Equipment, Purchase Value: $520,000, Installation Date: 2023-07-01, Warranty Expiry: 2033-07-01, Condition: Excellent, Insured: true, Assigned Technician Email: facilities@denverinnovation.com
8. NYC Retail Corridor - POS Infrastructure — hardware, inactive, POS-NYC-002, Las Vegas Casino Resort
   Asset Category: Equipment, Purchase Value: $78,000, Installation Date: 2020-05-01, Warranty Expiry: 2023-05-01, Condition: Poor, Insured: false, Maintenance Notes: Out of warranty — decommission approval pending, Assigned Technician Email: it@lvresort.com
9. Seattle Waterfront - Environmental Monitor — hardware, active, ENV-SEA-001, Downtown Tech Plaza
   Asset Category: Equipment, Purchase Value: $65,000, Installation Date: 2023-02-15, Warranty Expiry: 2026-02-15, Condition: Good, Insured: true, Assigned Technician Email: facilities@downtowntech.com
10. Austin Innovation Hub - Smart Building Controller — hardware, active, SBC-AUS-001, Austin Tech Hub
    Asset Category: Infrastructure, Purchase Value: $240,000, Installation Date: 2022-09-01, Warranty Expiry: 2025-09-01, Condition: Excellent, Insured: true, Assigned Technician Email: smartbldg@austintech.com

**6 Orders:**
1. Manhattan Residential Complex — "Lobby Renovation Package", DELIVERED, $3,200,000 USD
2. Los Angeles Retail Corridor — "HVAC Replacement Project", CONFIRMED, $850,000 USD
3. Chicago Mixed-Use Development — "Smart Building Upgrade", SHIPPED, $540,000 USD
4. Miami Waterfront Project — "Marina Expansion Equipment", DRAFT, $1,200,000 USD
5. Silicon Valley Office Tower — "Network Infrastructure Refresh", CANCELLED, $290,000 USD
6. Boston Biotech Park — "Security System Overhaul", DELIVERED, $680,000 USD

**6 Invoices:**
1. Manhattan Residential Complex — PAID, $3,200,000 USD, NET-30
2. Los Angeles Retail Corridor — SENT, $850,000 USD, NET-45
3. Chicago Mixed-Use Development — PAID, $540,000 USD, NET-30
4. Miami Waterfront Project — DRAFT, $1,200,000 USD, NET-60
5. Silicon Valley Office Tower — OVERDUE, $290,000 USD, NET-30
6. Boston Biotech Park — PAID, $680,000 USD, NET-30

**3 Document Templates:**
1. "Property Portfolio Deck" — slide_deck, {"layout":"corporate","accentColor":"#2E7D32","backgroundColor":"#1A2E1A","h1Color":"#81C784"}
2. "Property One-Pager" — one_pager, {"layout":"light","accentColor":"#2E7D32","includeCustomFields":true}
3. "Portfolio CSV Export" — csv_export, {"includeFields":["name","status","property_type","current_valuation","occupancy_rate","zoning_classification"]}

**3 Notification Templates:**
1. "New Property Listed" — CUSTOMER_CREATED, subject: "New Property Added: {{customerName}}", body: "New property added.\n\nProperty: {{customerName}}\nStatus: {{status}}\n\nView: {{link}}"
2. "Deal Stage Update" — OPPORTUNITY_UPDATED, subject: "Transaction Update: {{opportunityName}} → {{stage}}", body: "Transaction updated.\n\nProperty: {{customerName}}\nNew Stage: {{stage}}\nValue: {{amount}}\n\n{{link}}"
3. "Order Confirmed" — ORDER_CREATED, subject: "Service Order Confirmed: {{customerName}}", body: "Service order created.\n\nProperty: {{customerName}}\nTotal: {{amount}}\n\nView: {{link}}"
```
