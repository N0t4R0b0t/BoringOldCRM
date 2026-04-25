# Demo Tenant Setup - Complete Guide

This directory contains everything you need to set up 3 fully-configured, production-ready demo tenants for BOCRM with comprehensive feature coverage.

## 📋 Files in This Setup

### 1. **QUICK_DEMO_SETUP.md** ← START HERE
- 3-minute quick reference
- Step-by-step instructions to set up all 3 tenants
- Troubleshooting guide
- Success checklist

### 2. **ASSISTANT_SETUP_PROMPTS.md** ← THE MAIN RESOURCE
- 3 perfectly crafted AI assistant prompts
- Copy-paste ready into the chat interface
- Each prompt sets up a complete tenant end-to-end
- Fully detailed with all custom fields, policies, and sample data

### 3. **DEMO_TENANT_SETUP_GUIDE.md** ← DETAILED REFERENCE
- Comprehensive setup guide
- API endpoint reference
- Field type reference
- Validation rules
- Testing procedures

---

## 🚀 Ultra-Quick Start

```bash
# 1. Start services
cd backend && ./gradlew bootRun  # Terminal 1
cd frontend && npm run dev       # Terminal 2

# 2. Go to chat
# Open http://localhost:5173/onboarding

# 3. Paste & run
# Copy entire prompt from ASSISTANT_SETUP_PROMPTS.md
# Paste into chat
# Hit Enter (executes in ~4 minutes)

# 4. Repeat for 3 tenants
# Create tenant 2 → paste Prompt 2
# Create tenant 3 → paste Prompt 3
```

**Total time: ~15 minutes for all 3 tenants**

---

## 📊 What You Get

### 3 Complete Tenants

#### Tenant 1: Acme Tech Solutions
- Industry: Enterprise Software & Cloud
- 15 customers (TechCore, DataFlow, SecureVault, etc.)
- 20 contacts
- 15 opportunities ($380K - $6.5M deals)
- 18 activities across sales cycle
- Custom branding (blue theme)

#### Tenant 2: Global Finance Corp
- Industry: Banking & Financial Services
- 15 customers (JP Morgan, Goldman Sachs, Barclays, etc.)
- 20 contacts
- 15 opportunities ($80M - $500M deals)
- 20 activities (executive reviews, due diligence, etc.)
- Custom branding (green theme)

#### Tenant 3: Real Estate Ventures
- Industry: Real Estate Development
- 15 properties (Seattle, SF, NYC, LA, Chicago, etc.)
- 20 contacts
- 15 projects ($35M - $320M developments)
- 20 activities (site visits, design reviews, etc.)
- Custom branding (brown/tan theme)

### Feature Coverage

**Custom Fields (39 total, all 14 types):**
- ✅ Text, Number, Date, Select
- ✅ Multiselect, Boolean, Textarea
- ✅ Email, Phone, URL, Currency
- ✅ Percentage, RichText, Workflow

**Calculated Fields (6 total):**
- ✅ Engagement Score (Acme)
- ✅ Deal Progression % (Acme)
- ✅ Risk Score (Finance)
- ✅ Deal Completion % (Finance)
- ✅ Cost Per Sq Ft (Real Estate)
- ✅ Project Progress % (Real Estate)

**Policies (7 total, OPA/Rego engine):**
- ✅ 4 DENY policies (hard blocks)
- ✅ 3 WARN policies (confirmation required)
- Rego expressions: `input.entity.*`, `input.previous.*`, `input.operation`
- Entity types supported: Customer, Contact, Opportunity, Activity, CustomRecord, Order, Invoice

**Sample Data:**
- ✅ 45 customers/properties
- ✅ 60 contacts
- ✅ 45 opportunities/projects
- ✅ 54 activities with full timeline

**Integrations (bonus demo feature):**
- ✅ Slack, Webhook, HubSpot, Zapier adapters
- ✅ 12 CRM event types available for subscription
- ✅ Admin UI at `/admin/integrations` to configure per-tenant

---

## 🎯 How the Prompts Work

Each prompt is designed to be pasted into the AI assistant chat interface and executed automatically:

1. **Tenant Setup** - Creates tenant, updates branding
2. **Custom Fields** - Creates 11-13 fields with proper configs
3. **Calculated Fields** - Creates 2 computed fields with CEL
4. **Policies** - Creates 2-3 business rules (DENY/WARN)
5. **Sample Data** - Populates customers, contacts, opportunities, activities

The assistant handles all the heavy lifting. You just paste and watch it execute.

---

## 📖 Documentation

### For Implementation
- **DEMO_TENANT_SETUP_GUIDE.md** - Detailed field-by-field setup guide with API examples
- **backend/api-tests/12-demo-tenants-comprehensive-setup.http** - HTTP test file for manual setup (Tenant 1 only)

### For Operations
- **ASSISTANT_SETUP_PROMPTS.md** - Ready-to-use prompts for each tenant
- **QUICK_DEMO_SETUP.md** - Quick reference and troubleshooting

### For Features
- **CLAUDE.md** - Architecture and patterns used in BOCRM
- **docs/** - Additional documentation

---

## 🎮 Step-by-Step Usage

### Prerequisites
```bash
# Docker running
docker compose up -d

# Backend started
cd backend
./gradlew bootRun
# Watch for: "Started Application in X seconds"

# Frontend ready (optional)
cd frontend
npm run dev
# Watch for: "Local: http://localhost:5173"
```

### Setup Flow

**Step 1: Login**
```
Go to: http://localhost:5173/onboarding
Email: demo@bocrm.com
Password: demo123
```

**Step 2: Create Tenant**
```
Click: Create New Workspace
Name: Acme Tech Solutions (gets overridden by prompt)
Click: Create
```

**Step 3: Skip Wizard**
```
Skip the initial onboarding form
Click: Go to Chat
```

**Step 4: Paste Prompt**
```
Open: ASSISTANT_SETUP_PROMPTS.md
Copy: Entire Prompt 1 (from opening ``` to closing ```)
Paste: Into chat input
Press: Enter
```

**Step 5: Watch Execution**
```
Assistant will:
1. Set tenant settings
2. Create all custom fields (shows ✓ for each)
3. Create calculated fields
4. Create policies
5. Create sample customers
6. Create contacts
7. Create opportunities
8. Create activities

Time: ~4 minutes
Watch: Messages show progress
```

**Step 6: Repeat for Tenant 2 & 3**
```
Create second tenant
Go to chat
Paste Prompt 2
Execute
Create third tenant
Go to chat
Paste Prompt 3
Execute
```

### Verify Setup
```
Dashboard: http://localhost:5173/dashboard
Should show:
- 3 tenants in switcher
- Each has 15 customers
- Customer list shows custom fields populated
- Opportunities show workflow progress
- Activities show full timeline

Try it:
- Click a customer → see custom fields
- Click an opportunity → see workflow stage
- Toggle opportunity to next stage → see progress %
- Try action that triggers policy → see DENY/WARN message
```

---

## 🔧 Customization

### Want Different Data?
Edit the prompts in `ASSISTANT_SETUP_PROMPTS.md`:
- Change customer names
- Adjust deal amounts
- Modify contact details
- Update company descriptions
- Change logo URLs or colors

Then re-run the modified prompt.

### Want Different Fields?
Edit the custom field sections:
- Add/remove fields
- Change field types
- Modify validation rules
- Adjust labels

Keep the same structure and the assistant will handle it.

### Want Different Industry?
Replace the entire company context:
- Change company name and bio
- Update industry-specific fields
- Modify sample data to match
- Adjust policies for the industry

---

## ✅ Success Criteria

After running all 3 prompts, you should have:

- [ ] Dashboard shows 45+ total customers
- [ ] Each tenant has unique branding colors
- [ ] Opportunity list shows workflow stages
- [ ] Custom fields visible in customer detail
- [ ] Calculated fields show computed values
- [ ] Activity feeds populated across records
- [ ] Can switch tenants in sidebar
- [ ] Policies page shows configured rules
- [ ] All data persists after page reload
- [ ] No console errors in browser dev tools

---

## 🐛 Troubleshooting

### Setup Fails to Start

**Problem**: "Cannot connect to API"
```
Solution: 
1. Check backend is running: curl http://localhost:8080/api/health
2. Check port 8080 is not in use: lsof -i :8080
3. Restart backend: Ctrl+C in backend terminal, ./gradlew bootRun
```

**Problem**: "Authentication failed"
```
Solution:
1. Clear browser localStorage: Cmd+Shift+Delete → Storage → Clear All
2. Reload page
3. Login again with demo@bocrm.com / demo123
```

### Partial Data Creation

**Problem**: "Some fields created, then stopped"
```
Solution:
1. Check backend logs for error messages
2. The assistant stops on first hard error
3. Fix the error in the prompt and re-run
4. Or manually create remaining fields via Admin UI
```

**Problem**: "Policy rule didn't apply"
```
Solution:
1. Reload page (cache issue)
2. Check policy Rego expression syntax (`input.entity.fieldName == "value"`)
3. Go to Admin → Policy Rules to verify creation
4. Test with data that matches the expression
```

### Data Not Visible

**Problem**: "Created 15 customers but only see 3"
```
Solution:
1. Check page size selector (top right of list)
2. Default page size is 10; set to 50+
3. Or scroll to next page
4. Check filters aren't hiding records
```

**Problem**: "Custom fields don't show in list"
```
Solution:
1. Go to Admin → Custom Fields
2. Set `displayInTable: true` for fields you want visible
3. Reload customer list
4. Fields should now appear in table
```

### Performance Issues

**Problem**: "Page is slow after setup"
```
Solution:
1. This is normal with 1,000+ records and calculated fields
2. Try filtering to a single customer
3. Pagination defaults to 10 records; acceptable for demo
4. For production, add database indexes (not needed for demo)
```

---

## 🎓 Learning Resources

### Understanding the Features

**Custom Fields**: CLAUDE.md → Custom Field Types section
- All 14 types documented with examples
- Configuration options explained
- Use cases for each type

**Calculated Fields**: CLAUDE.md → Calculated Field Pattern
- CEL expression syntax
- Available variables
- Performance considerations

**Policies**: CLAUDE.md → Business Policy Rules
- Rego expression reference (`input.entity.*`, `input.previous.*`, `input.operation`)
- DENY vs WARN severity
- UPDATE operation handling

**Workflows**: CLAUDE.md → Workflow Field Type
- Milestone configuration
- Current index semantics
- UI interaction patterns

### API Documentation

**Custom Fields Endpoint**: `DEMO_TENANT_SETUP_GUIDE.md` → API Endpoint Reference
- POST /custom-fields
- GET /custom-fields
- PUT /custom-fields/{id}

**All Endpoints**: See `docs/api.md` or backend OpenAPI at `/api/docs/swagger-ui.html`

---

## 🚢 Next Steps

### For Demos
1. Run all 3 tenants setup
2. Take screenshots of each tenant's dashboard
3. Record a walkthrough video of the features
4. Show policy violations in action
5. Generate a sample document with AI

### For Testing
1. Verify each custom field type is working
2. Test calculated field expressions
3. Trigger each policy rule (DENY and WARN)
4. Load test with 3 tenants of data
5. Check database growth

### For Development
1. Review custom field implementation
2. Understand policy evaluation flow
3. Study calculated field CEL evaluation
4. Examine workflow state transitions
5. Profile performance bottlenecks

---

## 📞 Support

### Common Questions

**Q: Can I run prompts in parallel?**
A: No, run sequentially. Backend is single-threaded for demo.

**Q: Can I modify a tenant after setup?**
A: Yes, fully editable. Add/remove fields, create more records, etc.

**Q: How do I delete a tenant?**
A: Admin → Tenants → Delete. All data cascades.

**Q: Can I backup the data?**
A: Docker Postgres is in-memory. For persistence, use named volume: `docker-compose.yml` → postgres → volumes.

**Q: How do I export the data?**
A: Use AI assistant: "Export all customers to CSV" → Downloads file.

### Reporting Issues

If setup fails:
1. Check backend logs: `tail -50 backend/build/logs/`
2. Check browser console: F12 → Console tab
3. Check network tab: F12 → Network → see failed requests
4. Try with a fresh tenant
5. Report with error logs and steps to reproduce

---

## 📝 Files Summary

```
BoringOldCRM/
├── ASSISTANT_SETUP_PROMPTS.md          ← MAIN: 3 ready-to-use prompts
├── QUICK_DEMO_SETUP.md                 ← Quick reference guide
├── DEMO_TENANT_SETUP_GUIDE.md          ← Detailed setup guide
├── README_DEMO_SETUP.md                ← This file
├── CLAUDE.md                           ← Architecture & patterns
├── backend/
│   ├── api-tests/
│   │   └── 12-demo-tenants-comprehensive-setup.http
│   ├── src/main/java/.../
│   │   ├── service/AssistantService.java
│   │   ├── service/CustomFieldDefinitionService.java
│   │   ├── tools/AdminTools.java
│   │   └── ...
│   └── README.md
├── frontend/
│   ├── src/
│   │   ├── pages/
│   │   │   ├── OnboardingPage.tsx
│   │   │   ├── ChatPage.tsx
│   │   │   └── DashboardPage.tsx
│   │   └── api/apiClient.ts
│   └── README.md
└── docker-compose.yml
```

---

## 🎯 Final Checklist

Before you start:
- [ ] Docker is running
- [ ] Backend can start without errors
- [ ] Frontend can start without errors
- [ ] You have `ASSISTANT_SETUP_PROMPTS.md` open
- [ ] You're logged in as demo@bocrm.com
- [ ] You understand the 5 prompts (Prompt 1, 2, 3 + variants)

Ready? Start with `QUICK_DEMO_SETUP.md` and follow the steps!

---

## 💡 Tips for Great Demos

1. **Show Feature Progression**: Start with Tenant 1 (tech), explain custom fields → calculated fields → policies
2. **Live Policy Violation**: Try to archive a high-value customer, show DENY error
3. **Workflow Advancement**: Click opportunity, advance sales stage, show progress % calculate
4. **AI Integration**: Ask assistant to generate a slide deck of the opportunities; upload a CSV to bulk-import contacts
5. **Multi-Tenancy**: Switch between tenants to show complete isolation and branding
6. **Integrations**: Show Admin → Integrations; set up a Slack webhook or use webhook.site to demonstrate live event delivery
6. **Real Data**: The sample data is realistic; use it to explain real-world use cases
7. **Comparative**: Show how Finance tenant is structured differently than Tech tenant

---

Generated for BOCRM Demo Setup | v1.0 | 2026-04-01
