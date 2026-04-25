# Seed Data Improvements

**Date**: April 2026  
**Overview**: Enhanced ASSISTANT_SETUP_PROMPTS.md with proper entity linking, tenant settings configuration, and entity label customization.

## Changes Made

### 1. **Company Bio & Tenant Settings Configuration**

All three prompts (Acme, Global Finance, Real Estate) now include explicit instructions to call `updateTenantSettings` after creating the first entity:

```
updateTenantSettings(
  primaryColor="<hex-color>",
  orgBio="<organization-bio-text>"
)
```

**Why**: Ensures company bio is properly stored in tenant settings for use by AI assistant context and tenant branding.

### 2. **Entity Label Customization**

Each prompt now includes optional instructions to rename entity types using `updateEntityLabels`:

- **Acme Tech**: Rename "Opportunity" → "Deal" (suggested)
- **Global Finance**: Rename "Opportunity" → "Deal", "Order" → "Transaction", "Invoice" → "Statement"
- **Real Estate**: Rename "Customer" → "Property", "Contact" → "Stakeholder", "Opportunity" → "Investment", "Order" → "Transaction", "Invoice" → "Statement"

```
updateEntityLabels(
  opportunityLabel="Deal",
  orderLabel="Transaction",
  invoiceLabel="Statement"
)
```

**Why**: Allows organizations to use domain-appropriate terminology while keeping the same underlying entities.

### 3. **Explicit Order & Invoice Customer Linking**

Orders and invoices now include explicit customer references:

**Before**:
```
1. TechCore Inc - "Q1 Infrastructure Consulting Package", status: DELIVERED, totalAmount: 185000
```

**After**:
```
1. TechCore Inc (Customer #1) - "Q1 Infrastructure Consulting Package", status: DELIVERED, totalAmount: 185000
```

**Why**: Makes it crystal clear which customer each order/invoice belongs to, preventing linking errors.

### 4. **Execution Order & Entity Linking Guidelines**

Added explicit sequencing for all three prompts:

1. Custom field definitions
2. Calculated field definitions
3. Policy rules
4. **Customers/Properties** (IDs needed for next steps)
5. Contacts (linked to customers)
6. Opportunities (linked to customers)
7. Activities (linked to customers and opportunities)
8. CustomRecords (linked to customers)
9. **Tenant settings** (branding, bio, labels)
10. Orders (linked to customers by ID)
11. Invoices (linked to customers by ID)
12. Document templates
13. Notification templates

**Critical guidance**:
- When creating contacts, explicitly specify which customer they belong to (use customer ID)
- When creating opportunities, explicitly specify which customer they belong to (use customer ID)
- When creating orders/invoices, link them to the appropriate customer using the customer's ID
- When creating activities, link them to both a customer and an opportunity
- When creating customRecords, link them to the appropriate customer

**Why**: Prevents orphaned records and ensures all relationships are properly established during bulk seeding.

## Impact

### Before
- Orders/invoices weren't clearly linked to customers in seed data
- No guidance on when to set company bio and entity labels
- No clear execution order for entity dependencies
- Potential for orphaned records or missing relationships

### After
- All orders and invoices explicitly reference their customer by ID
- Tenant branding (bio, primary color) is set via `updateTenantSettings`
- Entity labels can be customized per organization context
- Clear execution order prevents dependency issues
- All entity relationships are properly established

## Usage

Each prompt now includes a dedicated "**Execution Order**" section with:
1. Step-by-step sequencing
2. Why each step matters
3. Critical linking guidelines
4. Confirmation step for customer IDs

When using the prompts:
1. Follow the execution order strictly
2. Let the assistant know you're following the order
3. Pay attention to customer ID references for orders/invoices
4. Call `updateTenantSettings` after step 8 before creating orders/invoices
5. Optionally call `updateEntityLabels` if you want domain-specific naming

## Files Modified

- `/docs/seeding/ASSISTANT_SETUP_PROMPTS.md` — All three prompts (Acme, Global Finance, Real Estate)

## Tools Used

This improvement leverages existing AdminTools methods:
- `updateTenantSettings(primaryColor, language, orgBio)` — Set company branding
- `updateEntityLabels(customerLabel, contactLabel, opportunityLabel, activityLabel, customRecordLabel, orderLabel, invoiceLabel)` — Customize entity type names

Both methods are idempotent and safe to call multiple times.
