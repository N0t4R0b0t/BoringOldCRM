# Quick Demo Tenant Setup - 3 Minutes to Production Demo

## TL;DR

You have 3 perfectly crafted AI prompts in `ASSISTANT_SETUP_PROMPTS.md` that will set up complete, production-ready demo tenants automatically.

---

## Quick Start

### Step 1: Start Backend & Frontend
```bash
# Terminal 1: Backend
cd backend
./gradlew bootRun

# Terminal 2: Frontend  
cd frontend
npm run dev

# Terminal 3: Infrastructure (if not running)
docker compose up -d
```

### Step 2: Copy a Prompt
Open `ASSISTANT_SETUP_PROMPTS.md` and copy **Prompt 1, 2, or 3** (the full text in the code blocks)

### Step 3: Go to Onboarding
1. Open http://localhost:5173/onboarding
2. Login: `demo@bocrm.com` / `demo123`
3. Create new tenant (any name, you'll override it)
4. Skip the form → Click "Go to Chat"

### Step 4: Paste & Execute
1. Paste the entire prompt into the chat
2. Hit Enter
3. Watch it execute automatically (auto mode)
4. Takes 3-5 minutes per tenant

### Step 5: Repeat for Other Tenants
Switch to a new tenant and repeat with Prompt 2 and 3

---

## What Gets Created

Each prompt automatically creates:

- ✅ Tenant settings (name, bio, logo, colors)
- ✅ 11-13 custom fields (all types: text, number, select, multiselect, date, boolean, textarea, email, phone, url, currency, percentage, richtext, workflow)
- ✅ 2 calculated fields (CEL expressions)
- ✅ 2-3 business policies (DENY/WARN rules, evaluated by OPA/Rego)
- ✅ 15 sample customers with full custom field data
- ✅ 20 sample contacts across customers
- ✅ 15 sample opportunities with workflow stages
- ✅ 18-20 sample activities across the pipeline

---

## The 3 Tenants

### Tenant 1: Acme Tech Solutions
**Tech/SaaS focused** with cloud infrastructure customers
- Custom fields: Industry, Company Size, Revenue, Risk Level, Certifications, Growth Rate, etc.
- Policies: Block high-value customer archival, Warn on critical risk updates
- Data: 15 tech companies (TechCore, DataFlow, SecureVault, etc.)

### Tenant 2: Global Finance Corp
**Financial services** with institutional banking clients
- Custom fields: Account Type, AUM, Credit Rating, Regulatory Status, Leverage Ratio, etc.
- Policies: Prevent archiving regulated accounts, Warn on high leverage, Block deals without due diligence
- Data: 15 banks/investment firms (JP Morgan, Goldman Sachs, Barclays, etc.)

### Tenant 3: Real Estate Ventures
**Real estate** with commercial development projects
- Custom fields: Property Type, Acquisition Price, Square Footage, Occupancy, Zoning, Sustainability, etc.
- Policies: Lock completed properties, Warn on $50M+ transactions
- Data: 15 properties across major US cities (Seattle, San Francisco, NYC, etc.)

---

## Feature Coverage

Each tenant demonstrates:

### Custom Fields
- **Text**: Industry, Zoning, Regulatory Status
- **Number**: Company Size, Square Footage
- **Currency**: Annual Revenue, Acquisition Price, AUM
- **Date**: Certification Expiry, Last Audit Date, Completion Date
- **Select**: Risk Level, Account Type, Property Type, Credit Rating
- **Multiselect**: Decision Factors, Product Category, Sustainability Features
- **Boolean**: Has Active Contract, Is Regulated, Construction Complete
- **Textarea**: Technical Notes, Compliance Notes, Property Description
- **Email**: Contact Email addresses
- **Phone**: Support Phone, Main Phone
- **URL**: Website URLs
- **Percentage**: Growth Rate, Occupancy Rate, Leverage Ratio
- **Richtext**: Detailed descriptions, Deal structures
- **Workflow**: Sales Pipeline, Deal Lifecycle, Development Phases

### Calculated Fields
- Revenue-based engagement scoring
- Workflow stage percentage completion
- Cost per square foot calculations
- Risk score computations

### Policies (OPA/Rego engine)
- **DENY**: Hard blocks (prevent high-value customer archival, lock completed properties)
- **WARN**: Soft blocks requiring confirmation (critical risk updates, $50M+ transactions)
- **UPDATE operations**: Prevent invalid state changes
- Expressions use Rego syntax: `input.entity.*`, `input.previous.*`, `input.operation`
- Supported entity types: Customer, Contact, Opportunity, Activity, CustomRecord, Order, Invoice

### Sample Data Relationships
- Customers → Contacts (2-3 per customer)
- Customers → Opportunities (1-2 per customer)
- Opportunities → Activities (various types and stages)
- Full workflow progression visible in opportunities

---

## Verify It Worked

After each prompt executes:

1. **Check Dashboard**
   - Visit http://localhost:5173/dashboard
   - See customer count, opportunity pipeline, activity feed

2. **View Custom Fields**
   - Go to Customers page
   - Scroll right to see all custom fields populated
   - Check calculated fields are computed

3. **Check Workflow Progress**
   - Open an Opportunity
   - See "Sales Pipeline Stage" (Acme), "Deal Lifecycle" (Finance), or "Development Phases" (Real Estate)
   - Workflow stepper shows current stage with percentage complete

4. **Test a Policy**
   - Try to update a customer that would trigger a DENY policy
   - Should see error message blocking the action
   - Try WARN policy action → should show confirmation modal

5. **Explore Activities**
   - Click Activities tab on a customer
   - See full timeline of calls, meetings, emails
   - Review realistic sales cycle progression

---

## Troubleshooting

**Issue**: "Token expired" or 401 error
- **Solution**: Login again at http://localhost:5173/chat first

**Issue**: Assistant fails to create fields
- **Solution**: Check internet connection, backend logs for errors
- **Note**: Some fields might fail; assistant will continue with next items

**Issue**: Partial data created
- **Solution**: Prompts are designed to be fault-tolerant; re-run the prompt to complete
- **Alternative**: Manual setup via DEMO_TENANT_SETUP_GUIDE.md

**Issue**: Calculations show "error" in calculated field
- **Solution**: Reload the page; CEL evaluation caches the first occurrence
- **Note**: This is normal; appears in rare cases

---

## Pro Tips

1. **Use 3 browser tabs** - Keep each tenant open for easy switching
2. **Turn on notifications** - Watch the activity feed update in real-time
3. **Try AI features** - Ask assistant for slide decks, exports, insights
4. **Check the policy rules** - Go to Admin → Policy Rules to see what was created
5. **Review custom field config** - Admin → Custom Fields to see all configurations
6. **Document templates** - Admin → Document Templates shows default templates for reports
7. **Set up an integration** - Admin → Integrations → configure Slack or Webhook to receive live CRM events

---

## File Reference

- **Setup Prompts**: `ASSISTANT_SETUP_PROMPTS.md` ← Copy from here
- **Detailed Guide**: `DEMO_TENANT_SETUP_GUIDE.md` (API endpoint reference)
- **This File**: `QUICK_DEMO_SETUP.md` (you are here)

---

## One-Liner Commands

```bash
# Check backend is running
curl http://localhost:8080/api/auth/login

# Check frontend is running
curl http://localhost:5173

# View backend logs
cd backend && ./gradlew bootRun 2>&1 | tail -20

# Restart infrastructure
docker compose restart
```

---

## Expected Timeline

| Step | Time |
|------|------|
| Start services | 1 min |
| Login + go to chat | 1 min |
| Paste + execute Prompt 1 | 4 min |
| Tenant 1 complete | 6 min |
| Paste + execute Prompt 2 | 4 min |
| Tenant 2 complete | 10 min |
| Paste + execute Prompt 3 | 4 min |
| All 3 tenants ready | 14 min |

**Total time to fully functional demo: ~15 minutes**

---

## What Makes These Prompts Special

✅ **Perfectly sequenced** - Each tool call is in the right order (fields before policies, policies before data)

✅ **Self-healing** - If one field fails, assistant continues with next items

✅ **Realistic data** - Company names, deal amounts, contact titles are all realistic

✅ **Feature-complete** - Covers all custom field types, both calculated and basic fields, DENY/WARN policies

✅ **Relationship-aware** - Contacts linked to customers, opportunities linked to customers with matching amounts, activities span timeline

✅ **Workflow-integrated** - Opportunities progress through defined workflow stages with percentages

✅ **Business logic** - Policies demonstrate real-world constraints (regulatory blocks, approval thresholds)

✅ **Cross-functional** - Data reflects realistic CRM usage (sales calls → demos → proposals → negotiations → closed)

---

## Next: Advanced Features

Once tenants are set up:

- **AI Assistant**: Ask it to generate reports, slide decks, or CSV exports; upload PDFs/images/CSVs for bulk import
- **Bulk actions**: Select multiple opportunities and update workflow stage
- **Document templates**: Generate branded documents for customers
- **Dashboard insights**: See AI-powered pipeline observations
- **Access control**: Set up row-level permissions for records
- **CustomRecord management**: Upload and link files to opportunities
- **Notifications**: Configure alerts for policy violations
- **Integrations**: Connect Slack, HubSpot, Zapier, or Webhook to receive real-time CRM event notifications (Admin → Integrations)

---

## Questions?

- **How do I customize the data?** Edit `ASSISTANT_SETUP_PROMPTS.md` and change company names, amounts, etc.
- **Can I skip the onboarding?** Yes, use the API directly with auto mode
- **How do I delete and start over?** Delete the tenant via Admin panel → all data cascades
- **Can I run multiple tenants in parallel?** No, run sequentially; backend is single-threaded for demo
- **Do I need real data?** No, this is fully self-contained demo data; delete to start fresh

---

## Success Checklist

After all 3 prompts:

- [ ] 3 tenants visible in tenant switcher
- [ ] Each tenant has 15 customers
- [ ] Each customer has 1-2 contacts
- [ ] Each customer has 1+ opportunities
- [ ] Opportunities show workflow progress (not all at 0%)
- [ ] Custom fields are populated across all customers
- [ ] Calculated fields show computed values
- [ ] Can switch between tenants without errors
- [ ] Dashboard shows correct customer count per tenant
- [ ] Policy rules page shows rules for each tenant

**Once all checked: ✅ You're ready for demos!**

