# Absurdist Business Universe - Staged Assistant Setup Prompts

Decomposed versions of each absurd tenant prompt, broken into 10 smaller steps for smaller AI models.

---

## Toast Distribution Network (TDN) — 10 Steps

### Step 1 of 10 — Customer Custom Fields

```
I'm setting up a CRM workspace for "Toast Distribution Network" (TDN). In this step, please create the custom fields for Customers (Toast Retailers/Restaurants).

Create the following custom fields for the Customer entity:
1. "Toast Preference Level" (select field, required) — options: Lightly Toasted, Medium Brown, Dark and Crispy, Burnt (Classic), Charcoal Black
2. "Monthly Toast Consumption" (number field) — slices per month
3. "Average Crust Thickness" (percentage field, 0-100) — thickness preference
4. "Butter Budget (USD)" (currency field) — butter spending
5. "Bread Type Preference" (multiselect) — options: White, Wheat, Rye, Sourdough, Pumpernickel, English Muffin, Bagel
6. "Jam Partnership Status" (boolean field) — use our partner jam
7. "Toast Temperature (Fahrenheit)" (number field) — preferred arrival temp
8. "Crumb Spillage Tolerance" (percentage field) — crumb acceptance
9. "Premium Member" (boolean field) — subscription status
10. "Last Toast Delivery Date" (date field) — last delivery
11. "Toast Freshness Rating" (number field, 0-10) — satisfaction score
12. "Special Requests" (textarea field) — custom toast notes
13. "Toast Journey Progress" (workflow field) — milestones: Order Placed, Toasting, Bagging, Shipping, Delivered

Confirm when all 13 customer custom fields have been created.
```

---

### Step 2 of 10 — Opportunity Custom Fields

```
I'm continuing setup for Toast Distribution Network (TDN). Customer custom fields were created in the previous step. Now create the custom fields for the Opportunity entity (Toast Orders/Contracts).

Create the following custom fields for the Opportunity entity:
1. "Toast Batch Size" (select field, required) — options: Single Slice, Half Dozen, Dozen, Case (24), Pallet (1000)
2. "Urgency Level" (select field) — options: Leisurely, Standard, Express, TOAST EMERGENCY
3. "Special Instructions" (richtext) — detailed toast specs
4. "Expected Crunch Factor" (percentage field) — crunch prediction
5. "Toast Contract Progression" (workflow field) — milestones: Negotiation, Manufacturing, Quality Check, Logistics, Consumed

Confirm when all 5 opportunity custom fields have been created.
```

---

### Step 3 of 10 — Calculated Fields & Business Policies

```
I'm continuing setup for Toast Distribution Network (TDN). Custom fields for Customers and Opportunities are done. Now create the calculated fields and business policies.

**Calculated Fields:**
1. For Customer — "Toast Loyalty Score":
   Expression: (Monthly_Toast_Consumption / 1000) * 50 + (if Premium_Member then 25 else 0) + (Toast_Freshness_Rating * 5)

2. For Opportunity — "Toast Readiness %":
   Expression: if Toast_Contract_Progression exists then (Toast_Contract_Progression.currentIndex + 1) * 20 else 0

**Business Policies (Rego syntax — each condition on a separate line):**
1. DENY — "No Low-Tolerance Shipment":
   input.entity.crumb_spillage_tolerance < 20

2. WARN — "Late Night Charcoal Order":
   input.entity.toast_preference_level == "Charcoal Black"
   input.entity.order_time > "18:00"

3. DENY — "TOAST EMERGENCY Requires Premium":
   input.entity.urgency_level == "TOAST EMERGENCY"
   input.entity.premium_member == false

Confirm when calculated fields and policies are created.
```

---

### Step 4 of 10 — Tenant Settings & Branding

```
I'm continuing setup for Toast Distribution Network (TDN). Now update the tenant settings with the following branding:

- Company Name: Toast Distribution Network
- Logo URL: https://companieslogo.com/img/orig/TOST-7d2110c1.png?t=1720244494&download=true
- Primary Color: #D2691E
- Secondary Color: #FFD700
- Website: https://toastdistribution.com
- Bio: "Toast Distribution Network revolutionizes the distribution of pre-made toast. Our proprietary "keep-it-toasted" technology ensures every slice arrives in perfect condition. From sourdough to Wonder Bread, we've got the crust game covered. Crunch is our promise."

Confirm when tenant settings have been updated.
```

---

### Step 5 of 10 — Customers 1–8

```
I'm continuing setup for Toast Distribution Network (TDN). Now create the first 8 toast retailer customers with all their custom field values.

Create the following customers:

1. Name: Sunrise Breakfast Diner, status: active
   Custom fields: Toast Preference Level=Medium Brown, Monthly Toast Consumption=450, Average Crust Thickness=65, Butter Budget=500, Bread Type Preference=[Wheat, White], Jam Partnership Status=true, Toast Temperature=185, Crumb Spillage Tolerance=85, Premium Member=true, Toast Freshness Rating=8.5, Special Requests="Slightly buttered, no jelly drip", Toast Journey Progress currentIndex=4

2. Name: The Crunch Palace, status: active
   Custom fields: Toast Preference Level=Dark and Crispy, Monthly Toast Consumption=1200, Average Crust Thickness=95, Butter Budget=1200, Bread Type Preference=[Sourdough], Jam Partnership Status=false, Toast Temperature=210, Crumb Spillage Tolerance=45, Premium Member=true, Toast Freshness Rating=9.2, Special Requests="Maximum crunch, no soft centers", Toast Journey Progress currentIndex=4

3. Name: Carb Counter Cafe, status: prospect
   Custom fields: Toast Preference Level=Lightly Toasted, Monthly Toast Consumption=320, Average Crust Thickness=40, Butter Budget=150, Bread Type Preference=[White], Jam Partnership Status=false, Toast Temperature=160, Crumb Spillage Tolerance=92, Premium Member=false, Toast Freshness Rating=7.8, Special Requests="Minimal butter, health conscious", Toast Journey Progress currentIndex=4

4. Name: Burnt to Perfection Restaurant, status: active
   Custom fields: Toast Preference Level=Charcoal Black, Monthly Toast Consumption=890, Average Crust Thickness=100, Butter Budget=800, Bread Type Preference=[Rye], Jam Partnership Status=true, Toast Temperature=230, Crumb Spillage Tolerance=25, Premium Member=true, Toast Freshness Rating=9.8, Special Requests="Dark as night, crispy texture essential", Toast Journey Progress currentIndex=4

5. Name: Golden Brown Bistro, status: active
   Custom fields: Toast Preference Level=Medium Brown, Monthly Toast Consumption=670, Average Crust Thickness=70, Butter Budget=700, Bread Type Preference=[Sourdough], Jam Partnership Status=true, Toast Temperature=190, Crumb Spillage Tolerance=80, Premium Member=true, Toast Freshness Rating=8.9, Special Requests="Artisanal quality expected", Toast Journey Progress currentIndex=4

6. Name: Quick Toast Express, status: prospect
   Custom fields: Toast Preference Level=Lightly Toasted, Monthly Toast Consumption=1100, Average Crust Thickness=35, Butter Budget=400, Bread Type Preference=[White], Jam Partnership Status=false, Toast Temperature=155, Crumb Spillage Tolerance=88, Premium Member=false, Toast Freshness Rating=8.1, Special Requests="Fast turnaround, budget conscious", Toast Journey Progress currentIndex=4

7. Name: The Crispy Corner, status: active
   Custom fields: Toast Preference Level=Dark and Crispy, Monthly Toast Consumption=445, Average Crust Thickness=90, Butter Budget=500, Bread Type Preference=[Pumpernickel], Jam Partnership Status=true, Toast Temperature=205, Crumb Spillage Tolerance=50, Premium Member=true, Toast Freshness Rating=9.1, Special Requests="Specialty breads preferred", Toast Journey Progress currentIndex=4

8. Name: Butterville Bakehouse, status: prospect
   Custom fields: Toast Preference Level=Medium Brown, Monthly Toast Consumption=580, Average Crust Thickness=68, Butter Budget=1100, Bread Type Preference=[English Muffin], Jam Partnership Status=true, Toast Temperature=188, Crumb Spillage Tolerance=78, Premium Member=false, Toast Freshness Rating=8.3, Special Requests="Heavy butter application expected", Toast Journey Progress currentIndex=4

Confirm when all 8 customers are created.
```

---

### Step 6 of 10 — Customers 9–15

```
I'm continuing setup for Toast Distribution Network (TDN). The first 8 customers were created. Now create customers 9–15.

9. Name: Toast & Jam Junction, status: active
   Custom fields: Toast Preference Level=Dark and Crispy, Monthly Toast Consumption=240, Average Crust Thickness=88, Butter Budget=600, Bread Type Preference=[Sourdough], Jam Partnership Status=true, Toast Temperature=200, Crumb Spillage Tolerance=60, Premium Member=true, Toast Freshness Rating=9.5, Special Requests="Jam-ready surface texture", Toast Journey Progress currentIndex=4

10. Name: Whole Grain Wellness, status: prospect
    Custom fields: Toast Preference Level=Lightly Toasted, Monthly Toast Consumption=380, Average Crust Thickness=38, Butter Budget=80, Bread Type Preference=[Wheat], Jam Partnership Status=false, Toast Temperature=158, Crumb Spillage Tolerance=95, Premium Member=false, Toast Freshness Rating=8.0, Special Requests="Whole grain only, minimal butter", Toast Journey Progress currentIndex=4

11. Name: The Toaster's Table, status: active
    Custom fields: Toast Preference Level=Charcoal Black, Monthly Toast Consumption=920, Average Crust Thickness=100, Butter Budget=950, Bread Type Preference=[Sourdough], Jam Partnership Status=false, Toast Temperature=225, Crumb Spillage Tolerance=20, Premium Member=true, Toast Freshness Rating=9.9, Special Requests="Extreme darkness, premium quality", Toast Journey Progress currentIndex=4

12. Name: Bread & Breakfast Co, status: active
    Custom fields: Toast Preference Level=Medium Brown, Monthly Toast Consumption=620, Average Crust Thickness=72, Butter Budget=550, Bread Type Preference=[Rye], Jam Partnership Status=true, Toast Temperature=192, Crumb Spillage Tolerance=82, Premium Member=false, Toast Freshness Rating=8.7, Special Requests="Traditional breakfast quality", Toast Journey Progress currentIndex=4

13. Name: Sourdough Dreams, status: active
    Custom fields: Toast Preference Level=Dark and Crispy, Monthly Toast Consumption=510, Average Crust Thickness=87, Butter Budget=800, Bread Type Preference=[Sourdough], Jam Partnership Status=true, Toast Temperature=202, Crumb Spillage Tolerance=55, Premium Member=true, Toast Freshness Rating=9.3, Special Requests="Artisan sourdough only", Toast Journey Progress currentIndex=4

14. Name: Simple Slice Cafe, status: inactive
    Custom fields: Toast Preference Level=Lightly Toasted, Monthly Toast Consumption=290, Average Crust Thickness=42, Butter Budget=120, Bread Type Preference=[White], Jam Partnership Status=false, Toast Temperature=162, Crumb Spillage Tolerance=90, Premium Member=false, Toast Freshness Rating=7.5, Special Requests="Simple, no frills", Toast Journey Progress currentIndex=4

15. Name: Premium Toast Society, status: active
    Custom fields: Toast Preference Level=Charcoal Black, Monthly Toast Consumption=1800, Average Crust Thickness=100, Butter Budget=2500, Bread Type Preference=[Sourdough], Jam Partnership Status=true, Toast Temperature=235, Crumb Spillage Tolerance=15, Premium Member=true, Toast Freshness Rating=10.0, Special Requests="VIP treatment, perfection required", Toast Journey Progress currentIndex=4

Confirm when all 7 customers are created.
```

---

### Step 7 of 10 — Contacts

```
I'm continuing setup for Toast Distribution Network (TDN). 15 customers are created. Now create 20 contacts linked to the customers below.

Create these contacts (name, title, linked customer):
1. Chef Butterworth — Head Toaster at Sunrise Breakfast Diner
2. Crispetta Jones — Toast Quality Manager at The Crunch Palace
3. Bernard Breadsworth — Owner at Carb Counter Cafe
4. Stella Scorch — Head Chef at Burnt to Perfection Restaurant
5. Marina Marmalade — Jam Coordinator at Toast & Jam Junction
6. Derek Dough — Operations Manager at Quick Toast Express
7. Cinnamon Sugar — Pastry Lead at Butterville Bakehouse
8. Monty Muffin — Product Specialist at Golden Brown Bistro
9. Petra Pumpernickel — Supply Manager at The Crispy Corner
10. Reginald Rye — Grain Specialist at Bread & Breakfast Co
11. Toasty McToastface — Brand Ambassador (no linked customer, company: Toast Distribution Network)
12. Bran Whitney — Nutrition Advisor at Whole Grain Wellness
13. Yeast Williams — Fermentation Expert at Sourdough Dreams
14. Gluten Green — Allergen Manager (company: TDN HQ)
15. Caramelina Brown — Head Toaster at The Toaster's Table
16. Sesame Sally — Seed Specialist at Premium Toast Society
17. Wheat Wilson — Grain Procurement (company: TDN Sourcing)
18. Crust Custard — Quality Assurance (company: TDN Quality)
19. Poppy Seedwell — Distribution Manager (company: TDN Logistics)
20. Sourdough Steve — Master Toaster (company: TDN Production)

Confirm when all 20 contacts are created.
```

---

### Step 8 of 10 — Opportunities (Toast Orders)

```
I'm continuing setup for Toast Distribution Network (TDN). Now create 15 toast orders as Opportunities linked to the retailers. Map Toast Contract Progression index to stage: 0=prospecting, 1=qualification, 2=proposal, 3=negotiation, 4=closed_won.

1. Name: Sunrise Diner Weekly Shipment, customer: Sunrise Breakfast Diner, stage: negotiation
   Custom fields: Toast Batch Size=Half Dozen, Urgency Level=Standard, Special Instructions="Buttered, consistent temperature", Expected Crunch Factor=85, Toast Contract Progression currentIndex=3

2. Name: TOAST EMERGENCY - Event at 6:00 PM, customer: Premium Toast Society, stage: qualification
   Custom fields: Toast Batch Size=Pallet, Urgency Level=TOAST EMERGENCY, Special Instructions="Drop everything, VIP event", Expected Crunch Factor=95, Toast Contract Progression currentIndex=1

3. Name: Burnt to Perfection Monthly Bulk, customer: Burnt to Perfection Restaurant, stage: proposal
   Custom fields: Toast Batch Size=Case, Urgency Level=Standard, Special Instructions="Maximum darkness, charcoal level", Expected Crunch Factor=99, Toast Contract Progression currentIndex=2

4. Name: Crunch Palace Black Toast Event, customer: The Crunch Palace, stage: negotiation
   Custom fields: Toast Batch Size=Dozen, Urgency Level=Express, Special Instructions="Peak crunchiness, dark color", Expected Crunch Factor=100, Toast Contract Progression currentIndex=3

5. Name: Whole Grain Wellness Subscription, customer: Whole Grain Wellness, stage: qualification
   Custom fields: Toast Batch Size=Single Slice, Urgency Level=Leisurely, Special Instructions="Whole grain, minimal butter", Expected Crunch Factor=75, Toast Contract Progression currentIndex=1

6. Name: Toast & Jam Junction Sourdough Rush, customer: Toast & Jam Junction, stage: prospecting
   Custom fields: Toast Batch Size=Case, Urgency Level=Express, Special Instructions="Sourdough specialty, jam-ready", Expected Crunch Factor=89, Toast Contract Progression currentIndex=0

7. Name: The Toaster's Table Charcoal Deluxe, customer: The Toaster's Table, stage: closed_won
   Custom fields: Toast Batch Size=Pallet, Urgency Level=Standard, Special Instructions="Maximum darkness, premium quality", Expected Crunch Factor=99, Toast Contract Progression currentIndex=4

8. Name: Quick Toast Express Daily Restocking, customer: Quick Toast Express, stage: negotiation
   Custom fields: Toast Batch Size=Dozen, Urgency Level=Leisurely, Special Instructions="Fast delivery, budget friendly", Expected Crunch Factor=80, Toast Contract Progression currentIndex=3

9. Name: Golden Brown Bistro Sourdough Bulk, customer: Golden Brown Bistro, stage: qualification
   Custom fields: Toast Batch Size=Case, Urgency Level=Standard, Special Instructions="Artisanal sourdough, medium brown", Expected Crunch Factor=87, Toast Contract Progression currentIndex=1

10. Name: Premium Toast Society VIP Order, customer: Premium Toast Society, stage: closed_won
    Custom fields: Toast Batch Size=Pallet, Urgency Level=Express, Special Instructions="VIP treatment, perfection required", Expected Crunch Factor=100, Toast Contract Progression currentIndex=4

11. Name: Bread & Breakfast Rye Special, customer: Bread & Breakfast Co, stage: proposal
    Custom fields: Toast Batch Size=Case, Urgency Level=Standard, Special Instructions="Rye bread, traditional style", Expected Crunch Factor=85, Toast Contract Progression currentIndex=2

12. Name: Buttersville Muffin Toast Order, customer: Butterville Bakehouse, stage: prospecting
    Custom fields: Toast Batch Size=Half Dozen, Urgency Level=Express, Special Instructions="English muffin base, heavy butter", Expected Crunch Factor=81, Toast Contract Progression currentIndex=0

13. Name: Sourdough Dreams Artisan Batch, customer: Sourdough Dreams, stage: qualification
    Custom fields: Toast Batch Size=Dozen, Urgency Level=Leisurely, Special Instructions="Artisan quality, dark and crispy", Expected Crunch Factor=92, Toast Contract Progression currentIndex=1

14. Name: Simple Slice Cafe Weekly Needs, customer: Simple Slice Cafe, stage: negotiation
    Custom fields: Toast Batch Size=Single Slice, Urgency Level=Leisurely, Special Instructions="Simple, no frills, basic quality", Expected Crunch Factor=75, Toast Contract Progression currentIndex=3

15. Name: The Crispy Corner Pumpernickel Rush, customer: The Crispy Corner, stage: proposal
    Custom fields: Toast Batch Size=Case, Urgency Level=TOAST EMERGENCY, Special Instructions="Specialty bread, express delivery", Expected Crunch Factor=91, Toast Contract Progression currentIndex=2

Confirm when all 15 opportunities are created.
```

---

### Step 9 of 10 — Activities

```
I'm continuing setup for Toast Distribution Network (TDN). Now create 18 activities. Map status: "Completed" → completed, "Scheduled" → pending.

1. Subject: Sunrise Diner Toasting Session, type: task, duration: 120 min, date: 2024-01-15, status: completed, notes: "Medium brown batch"
2. Subject: Crunch Palace Quality Taste Test, type: meeting, duration: 30 min, date: 2024-01-16, status: pending, notes: "Dark crispy evaluation"
3. Subject: Toast Butter Application Review, type: meeting, duration: 15 min, date: 2024-01-18, status: pending, notes: "Application technique"
4. Subject: Carb Counter Crumb Cleanup, type: task, duration: 10 min, date: 2024-01-19, status: completed, notes: "Spillage minimization"
5. Subject: Temperature Monitoring Check, type: task, duration: 5 min, date: 2024-01-22, status: completed, notes: "Heat consistency"
6. Subject: Premium Toast Society Call, type: call, duration: 20 min, date: 2024-01-23, status: pending, notes: "Satisfaction survey"
7. Subject: Toast Poetry Reading, type: meeting, duration: 45 min, date: 2024-01-24, status: completed, notes: "Rhyming appreciation"
8. Subject: Jam Pairing Consultation, type: meeting, duration: 30 min, date: 2024-01-25, status: pending, notes: "Flavor matching"
9. Subject: Burnt to Perfection Toasting, type: task, duration: 120 min, date: 2024-01-29, status: completed, notes: "Charcoal batch production"
10. Subject: Golden Brown Quality Test, type: meeting, duration: 30 min, date: 2024-01-30, status: completed, notes: "Medium brown standards"
11. Subject: Quick Toast Express Restocking, type: task, duration: 60 min, date: 2024-02-01, status: pending, notes: "Inventory replenishment"
12. Subject: Whole Grain Wellness Consultation, type: call, duration: 20 min, date: 2024-02-02, status: completed, notes: "Health check-in"
13. Subject: Toast & Jam Special Order, type: meeting, duration: 30 min, date: 2024-02-05, status: pending, notes: "Sourdough jam pairing"
14. Subject: The Toaster's Table VIP Call, type: call, duration: 20 min, date: 2024-02-06, status: completed, notes: "Premium service"
15. Subject: Butterville Butter Application, type: meeting, duration: 15 min, date: 2024-02-08, status: pending, notes: "Heavy butter technique"
16. Subject: Sourdough Dreams Artisan Review, type: meeting, duration: 30 min, date: 2024-02-09, status: completed, notes: "Specialty assessment"
17. Subject: Simple Slice Temperature Check, type: task, duration: 5 min, date: 2024-02-12, status: completed, notes: "Light toast verification"
18. Subject: Emergency Toast Protocol Meeting, type: meeting, duration: 45 min, date: 2024-02-15, status: pending, notes: "TOAST EMERGENCY training"

Confirm when all 18 activities are created.
```

---

### Step 10 of 10 — CustomRecords, Orders, Invoices & Templates

```
I'm continuing setup for Toast Distribution Network (TDN). This is the final step. Create customRecords, orders, invoices, document templates, and notification templates.

**CustomRecords (use bulkCreateCustomRecords):**
1. name: "Industrial Toaster Array — 48-Slice Commercial Grade", type: equipment, status: active, serialNumber: TOAST-48-001, customer: The Toaster's Table
2. name: "Premium Butter Churning Station", type: equipment, status: active, serialNumber: BUTTER-CH-001
3. name: "Sourdough Fermentation Chamber (Climate-Controlled)", type: hardware, status: active, serialNumber: FERM-SOUR-001, customer: Sourdough Dreams
4. name: "Delivery Fleet Vehicle (Toast Insulated) — 10 units", type: vehicle, status: active, serialNumber: DELIV-TOAST-010
5. name: "Jam Processing Apparatus — Berry Grade", type: equipment, status: active, serialNumber: JAM-BERRY-001, customer: Toast & Jam Junction
6. name: "Crumb Collection System (Advanced)", type: hardware, status: active, serialNumber: CRUMB-ADV-001, customer: Quick Toast Express
7. name: "Artisan Oven Temperature Calibration Kit", type: equipment, status: maintenance, serialNumber: OVN-CAL-001, customer: Whole Grain Wellness
8. name: "Bread Moisture Meter Fleet (x50)", type: equipment, status: active, serialNumber: MOIST-FLT-050
9. name: "Toast Delivery Box Fleet (Temperature-Sealed) — 500 units", type: inventory, status: active, serialNumber: BOX-SEALED-500
10. name: "Crumb Spillage Prevention Vacuum System", type: hardware, status: inactive, serialNumber: VAC-CRUMB-001, customer: Simple Slice Cafe

**Orders:**
1. customer: The Toaster's Table, name: "Premium Toast Supplies Bundle", status: DELIVERED, totalAmount: 2800, currency: USD
2. customer: Butterville Bakehouse, name: "Butter Churn Maintenance & Upgrade", status: CONFIRMED, totalAmount: 1200, currency: USD
3. customer: Sourdough Dreams, name: "Artisan Fermentation Chamber Lease", status: SHIPPED, totalAmount: 950, currency: USD
4. customer: Quick Toast Express, name: "Quarterly Inventory Restock", status: DRAFT, totalAmount: 3400, currency: USD
5. customer: Whole Grain Wellness, name: "Organic Toast Grain Supply", status: CANCELLED, totalAmount: 680, currency: USD
6. customer: Toast & Jam Junction, name: "Specialty Jam Ingredients Bundle", status: DELIVERED, totalAmount: 1100, currency: USD

**Invoices:**
1. customer: The Toaster's Table, status: PAID, totalAmount: 2800, currency: USD, paymentTerms: NET-30
2. customer: Butterville Bakehouse, status: SENT, totalAmount: 1200, currency: USD, paymentTerms: NET-30
3. customer: Sourdough Dreams, status: PAID, totalAmount: 950, currency: USD, paymentTerms: NET-14
4. customer: Quick Toast Express, status: DRAFT, totalAmount: 3400, currency: USD, paymentTerms: NET-30
5. customer: Whole Grain Wellness, status: OVERDUE, totalAmount: 680, currency: USD, paymentTerms: NET-30
6. customer: Toast & Jam Junction, status: PAID, totalAmount: 1100, currency: USD, paymentTerms: NET-30

**Document Templates (use createDocumentTemplate for each):**
1. name: "Toast Consumption Report", templateType: slide_deck, description: "Golden-brown themed consumption analytics deck for retailer meetings", styleJson: {"layout":"corporate","accentColor":"#C65911","backgroundColor":"#1A0F00","h1Color":"#FFB84D"}
2. name: "Retailer Toast Profile", templateType: one_pager, description: "Single-page toast partner profile and engagement summary", styleJson: {"layout":"light","accentColor":"#C65911","includeCustomFields":true}
3. name: "Toast Retailer Export", templateType: csv_export, description: "Full retailer export with toast consumption and premium member data", styleJson: {"includeFields":["name","status","premium_member","monthly_toast_consumption","avg_order_value","crumb_spillage_tolerance"]}

**Notification Templates (use createNotificationTemplate for each):**
1. name: "Order Ready for Delivery", notificationType: ORDER_CREATED, isActive: true
   subject: "Toast Order Ready: {{customerName}} — {{amount}} Slices"
   body: "A fresh toast order is ready for delivery.\n\nRetailer: {{customerName}}\nOrder: {{opportunityName}}\nSlices: {{amount}}\nDelivery Expected: {{dueDate}}\n\nTrack: {{link}}"

2. name: "Premium Member Alert", notificationType: CUSTOMER_UPDATED, isActive: true
   subject: "Premium Member Status: {{customerName}}"
   body: "A retailer's premium membership has been updated.\n\nRetailer: {{customerName}}\nStatus: {{status}}\nUpdated by: {{assignee}}\n\nView: {{link}}"

3. name: "Crumb Spillage Alert", notificationType: ACTIVITY_CREATED, isActive: true
   subject: "Crumb Spillage Tolerance Alert: {{customerName}}"
   body: "A retailer's crumb spillage tolerance is approaching critical threshold.\n\nRetailer: {{customerName}}\nActivity: {{opportunityName}}\nDate: {{dueDate}}\n\nAction required: {{link}}"

Confirm when all customRecords, orders, invoices, document templates, and notification templates are created. TDN setup complete!
```

---

## Paperclip Optimization Corporation (PORC) — 10 Steps

### Step 1 of 10 — Customer Custom Fields

```
I'm setting up a CRM workspace for "Paperclip Optimization Corporation" (PORC). In this step, please create the custom fields for Customers (Office Supply Retailers).

Create the following custom fields for the Customer entity:
1. "Clip Size Preference" (select field, required) — options: Micro (28mm), Standard (32mm), Jumbo (50mm), Giant (100mm), Industrial (200mm)
2. "Monthly Paperclip Consumption" (number field) — clips per month
3. "Metal Type Preference" (select field) — options: Steel, Plastic-Coated, Copper, Gold-Plated, Titanium
4. "Pile Height Capacity (sheets)" (number field) — max paper thickness
5. "Color Preference" (multiselect) — options: Silver, Black, Red, Blue, Rainbow, Neon
6. "Rust Resistance Rating" (percentage field, 0-100) — durability need
7. "Ergonomic Score" (number field, 0-10) — comfort preference
8. "Tangle Tolerance" (percentage field) — acceptable tangling rate
9. "Bulk Discount Eligible" (boolean field) — volume buyer
10. "Last Shipment Date" (date field) — delivery tracking
11. "Satisfaction Rating" (number field, 0-10) — client happiness
12. "Special Requirements" (textarea field) — custom needs
13. "Paperclip Acquisition Journey" (workflow field) — milestones: Interest, Proposal, Trial, Bulk Order, Loyalty Program

Confirm when all 13 customer custom fields have been created.
```

---

### Step 2 of 10 — Opportunity Custom Fields

```
I'm continuing setup for Paperclip Optimization Corporation (PORC). Customer custom fields were created. Now create the custom fields for the Opportunity entity (Paperclip Contracts).

Create the following custom fields for the Opportunity entity:
1. "Order Quantity (Boxes)" (number field, required) — quantity ordered
2. "Urgency Level" (select field) — options: Routine Restocking, Standard Order, Priority Rush, CRITICAL CLIP SHORTAGE
3. "Customization Type" (select field) — options: Standard, Logo Engraved, Color Custom, Material Upgrade
4. "Expected ROI (Paper Bound/Dollar)" (number field) — efficiency metric
5. "Acquisition Stage" (workflow field) — milestones: Quote, Negotiation, Order Confirmation, Manufacturing, Shipping, Stocked

Confirm when all 5 opportunity custom fields have been created.
```

---

### Step 3 of 10 — Calculated Fields & Business Policies

```
I'm continuing setup for Paperclip Optimization Corporation (PORC). Custom fields are done. Now create the calculated fields and business policies.

**Calculated Fields:**
1. For Customer — "Clip Loyalty Index":
   Expression: (Monthly_Paperclip_Consumption / 5000) * 40 + (Satisfaction_Rating * 6) + (if Bulk_Discount_Eligible then 20 else 0)

2. For Opportunity — "Order Fulfillment %":
   Expression: if Acquisition_Stage exists then (Acquisition_Stage.currentIndex + 1) * 16.6 else 0

**Business Policies (Rego syntax — each condition on a separate line):**
1. DENY — "No Standard Clips for Titanium Preference":
   input.entity.metal_type_preference == "Titanium"
   input.entity.clip_type == "Standard"

2. WARN — "CRITICAL SHORTAGE Without Bulk Discount":
   input.entity.urgency_level == "CRITICAL CLIP SHORTAGE"
   input.entity.bulk_discount_eligible == false

3. DENY — "Logo Engraving Minimum Order":
   input.entity.customization_type == "Logo Engraved"
   input.entity.order_quantity < 10

Confirm when calculated fields and policies are created.
```

---

### Step 4 of 10 — Tenant Settings & Branding

```
I'm continuing setup for Paperclip Optimization Corporation (PORC). Now update the tenant settings with the following branding:

- Company Name: Paperclip Optimization Corporation
- Logo URL: https://e7.pngegg.com/pngimages/532/291/png-clipart-computer-icons-encapsulated-postscript-email-attachment-paperclips-blue-text-thumbnail.png
- Primary Color: #696969
- Secondary Color: #C0C0C0
- Website: https://paperclipoptimization.com
- Bio: "Paperclip Optimization Corporation pioneers the future of office supply chains. Our proprietary MetalClip™ technology ensures optimal binding performance across all document sizes. From SMB to enterprise, PORC connects papers in ways they've never been connected before."

Confirm when tenant settings have been updated.
```

---

### Step 5 of 10 — Customers 1–8

```
I'm continuing setup for Paperclip Optimization Corporation (PORC). Now create the first 8 customers.

1. Name: Office Supplies Superstore, status: active
   Custom fields: Clip Size Preference=Standard (32mm), Monthly Paperclip Consumption=50000, Metal Type Preference=Steel, Pile Height Capacity=500, Color Preference=[Silver], Rust Resistance Rating=85, Ergonomic Score=8.5, Tangle Tolerance=92, Bulk Discount Eligible=true, Satisfaction Rating=9.2, Special Requirements="High volume, consistent quality", Paperclip Acquisition Journey currentIndex=4

2. Name: The Clip Shop, status: active
   Custom fields: Clip Size Preference=Jumbo (50mm), Monthly Paperclip Consumption=12000, Metal Type Preference=Steel, Pile Height Capacity=250, Color Preference=[Black], Rust Resistance Rating=78, Ergonomic Score=7.9, Tangle Tolerance=65, Bulk Discount Eligible=true, Satisfaction Rating=8.8, Special Requirements="Premium appearance, specialty colors", Paperclip Acquisition Journey currentIndex=4

3. Name: Corporate Essentials Distributor, status: active
   Custom fields: Clip Size Preference=Standard (32mm), Monthly Paperclip Consumption=85000, Metal Type Preference=Steel, Pile Height Capacity=400, Color Preference=[Silver, Red, Blue], Rust Resistance Rating=88, Ergonomic Score=8.8, Tangle Tolerance=88, Bulk Discount Eligible=true, Satisfaction Rating=9.5, Special Requirements="Rainbow variety packs, large scale distribution", Paperclip Acquisition Journey currentIndex=4

4. Name: Mom & Pop Stationary Store, status: prospect
   Custom fields: Clip Size Preference=Micro (28mm), Monthly Paperclip Consumption=2500, Metal Type Preference=Steel, Pile Height Capacity=150, Color Preference=[Silver], Rust Resistance Rating=72, Ergonomic Score=7.2, Tangle Tolerance=80, Bulk Discount Eligible=false, Satisfaction Rating=7.9, Special Requirements="Small orders, personal service", Paperclip Acquisition Journey currentIndex=2

5. Name: Tech Startup Office, status: active
   Custom fields: Clip Size Preference=Standard (32mm), Monthly Paperclip Consumption=3200, Metal Type Preference=Titanium, Pile Height Capacity=300, Color Preference=[Silver], Rust Resistance Rating=95, Ergonomic Score=9.2, Tangle Tolerance=85, Bulk Discount Eligible=true, Satisfaction Rating=9.7, Special Requirements="Premium materials, modern aesthetic", Paperclip Acquisition Journey currentIndex=3

6. Name: Government Procurement Office, status: active
   Custom fields: Clip Size Preference=Industrial (200mm), Monthly Paperclip Consumption=200000, Metal Type Preference=Steel, Pile Height Capacity=600, Color Preference=[Silver], Rust Resistance Rating=92, Ergonomic Score=8.1, Tangle Tolerance=70, Bulk Discount Eligible=true, Satisfaction Rating=9.1, Special Requirements="Heavy duty, compliance certified", Paperclip Acquisition Journey currentIndex=4

7. Name: University Supply Hub, status: active
   Custom fields: Clip Size Preference=Standard (32mm), Monthly Paperclip Consumption=28000, Metal Type Preference=Steel, Pile Height Capacity=350, Color Preference=[Silver, Red, Blue, Black], Rust Resistance Rating=80, Ergonomic Score=8.0, Tangle Tolerance=82, Bulk Discount Eligible=true, Satisfaction Rating=8.4, Special Requirements="Multi-color assortments, student use", Paperclip Acquisition Journey currentIndex=3

8. Name: Hospital Administration Supply, status: prospect
   Custom fields: Clip Size Preference=Micro (28mm), Monthly Paperclip Consumption=15000, Metal Type Preference=Plastic-Coated, Pile Height Capacity=200, Color Preference=[Silver], Rust Resistance Rating=90, Ergonomic Score=8.3, Tangle Tolerance=88, Bulk Discount Eligible=false, Satisfaction Rating=8.6, Special Requirements="Hypoallergenic, medical grade", Paperclip Acquisition Journey currentIndex=2

Confirm when all 8 customers are created.
```

---

### Step 6 of 10 — Customers 9–15

```
I'm continuing setup for Paperclip Optimization Corporation (PORC). First 8 customers are done. Now create customers 9–15.

9. Name: Law Firm Document Management, status: active
   Custom fields: Clip Size Preference=Jumbo (50mm), Monthly Paperclip Consumption=8500, Metal Type Preference=Gold-Plated, Pile Height Capacity=350, Color Preference=[Silver], Rust Resistance Rating=98, Ergonomic Score=9.1, Tangle Tolerance=60, Bulk Discount Eligible=true, Satisfaction Rating=9.9, Special Requirements="Premium appearance, large case files", Paperclip Acquisition Journey currentIndex=4

10. Name: Print Shop Operations, status: active
    Custom fields: Clip Size Preference=Standard (32mm), Monthly Paperclip Consumption=42000, Metal Type Preference=Steel, Pile Height Capacity=400, Color Preference=[Silver], Rust Resistance Rating=86, Ergonomic Score=8.4, Tangle Tolerance=89, Bulk Discount Eligible=true, Satisfaction Rating=9.3, Special Requirements="High volume, consistent supply", Paperclip Acquisition Journey currentIndex=4

11. Name: School District Supply Center, status: active
    Custom fields: Clip Size Preference=Standard (32mm), Monthly Paperclip Consumption=35000, Metal Type Preference=Steel, Pile Height Capacity=350, Color Preference=[Silver, Red, Blue, Black], Rust Resistance Rating=82, Ergonomic Score=7.9, Tangle Tolerance=84, Bulk Discount Eligible=true, Satisfaction Rating=8.7, Special Requirements="Student-friendly, colorful options", Paperclip Acquisition Journey currentIndex=3

12. Name: Architecture Firm, status: active
    Custom fields: Clip Size Preference=Jumbo (50mm), Monthly Paperclip Consumption=6800, Metal Type Preference=Copper, Pile Height Capacity=280, Color Preference=[Silver], Rust Resistance Rating=85, Ergonomic Score=8.6, Tangle Tolerance=75, Bulk Discount Eligible=true, Satisfaction Rating=9.4, Special Requirements="Design aesthetic, blueprint organization", Paperclip Acquisition Journey currentIndex=3

13. Name: Financial Services Archive, status: active
    Custom fields: Clip Size Preference=Industrial (200mm), Monthly Paperclip Consumption=95000, Metal Type Preference=Steel, Pile Height Capacity=550, Color Preference=[Silver], Rust Resistance Rating=93, Ergonomic Score=8.2, Tangle Tolerance=72, Bulk Discount Eligible=true, Satisfaction Rating=9.6, Special Requirements="Heavy duty archive, compliance", Paperclip Acquisition Journey currentIndex=4

14. Name: Creative Agency Studio, status: active
    Custom fields: Clip Size Preference=Micro (28mm), Monthly Paperclip Consumption=4200, Metal Type Preference=Steel, Pile Height Capacity=180, Color Preference=[Neon], Rust Resistance Rating=75, Ergonomic Score=8.7, Tangle Tolerance=78, Bulk Discount Eligible=true, Satisfaction Rating=8.9, Special Requirements="Creative colors, fun designs", Paperclip Acquisition Journey currentIndex=3

15. Name: Enterprise Data Center, status: active
    Custom fields: Clip Size Preference=Industrial (200mm), Monthly Paperclip Consumption=150000, Metal Type Preference=Titanium, Pile Height Capacity=600, Color Preference=[Silver], Rust Resistance Rating=99, Ergonomic Score=8.9, Tangle Tolerance=68, Bulk Discount Eligible=true, Satisfaction Rating=9.8, Special Requirements="Maximum durability, enterprise scale", Paperclip Acquisition Journey currentIndex=4

Confirm when all 7 customers are created.
```

---

### Step 7 of 10 — Contacts

```
I'm continuing setup for Paperclip Optimization Corporation (PORC). 15 customers are created. Now create 20 contacts.

1. Sterling Silver — Sales Director at Office Supplies Superstore
2. Clipper Mendez — Procurement Manager at Corporate Essentials Distributor
3. Rusty Rings — Quality Assurance (company: PORC Manufacturing)
4. Dr. Metal Fatigue — Research & Development (company: PORC Labs)
5. Jennifer Stapler — Competitor Liaison at The Clip Shop
6. Marcus Binder — Volume Sales at Government Procurement Office
7. Clarice Paper — Account Manager at Law Firm Document Management
8. Phillip Metal — Product Engineering (company: PORC Engineering)
9. Betty Document — Supply Chain at Hospital Administration Supply
10. Coil Spring — Operations Manager (company: PORC Operations)
11. Tension Taylor — Design Specialist (company: PORC Design)
12. Grip Gripper — Customer Success (company: PORC Customer Service)
13. Stack Holmes — Inventory Manager at University Supply Hub
14. Fastener Frank — B2B Specialist at Print Shop Operations
15. Wire Whitney — Technical Support (company: PORC Tech Support)
16. Bind Blackwell — Contract Negotiator (company: PORC Sales)
17. Clasp Clarkson — Marketing Director (company: PORC Marketing)
18. Connector Carl — Distribution Manager (company: PORC Logistics)
19. Loop Larson — Packaging Specialist (company: PORC Packaging)
20. Hold Harris — CEO (company: PORC Headquarters)

Confirm when all 20 contacts are created.
```

---

### Step 8 of 10 — Opportunities (Paperclip Orders)

```
I'm continuing setup for Paperclip Optimization Corporation (PORC). Now create 15 paperclip orders as Opportunities. Map Acquisition Stage index: 0=prospecting, 1=qualification, 2=proposal, 3=negotiation, 4=closed_won, 5=closed_won (stocked).

1. Name: Corporate Essentials Bulk Order, customer: Corporate Essentials Distributor, stage: prospecting
   Custom fields: Order Quantity=1000, Urgency Level=Routine Restocking, Customization Type=Color Custom, Expected ROI=9.2, Acquisition Stage currentIndex=0

2. Name: CRITICAL CLIP SHORTAGE - Government, customer: Government Procurement Office, stage: negotiation
   Custom fields: Order Quantity=5000, Urgency Level=CRITICAL CLIP SHORTAGE, Customization Type=Standard, Expected ROI=9.8, Acquisition Stage currentIndex=3

3. Name: Law Firm Custom Logo Clips, customer: Law Firm Document Management, stage: qualification
   Custom fields: Order Quantity=50, Urgency Level=Standard Order, Customization Type=Logo Engraved, Expected ROI=8.5, Acquisition Stage currentIndex=1

4. Name: Tech Startup Titanium Rush, customer: Tech Startup Office, stage: proposal
   Custom fields: Order Quantity=200, Urgency Level=Priority Rush, Customization Type=Material Upgrade, Expected ROI=9.7, Acquisition Stage currentIndex=2

5. Name: University Multi-Color Distribution, customer: University Supply Hub, stage: closed_won
   Custom fields: Order Quantity=750, Urgency Level=Routine Restocking, Customization Type=Color Custom, Expected ROI=8.3, Acquisition Stage currentIndex=4

6. Name: Print Shop Weekly Standard, customer: Print Shop Operations, stage: prospecting
   Custom fields: Order Quantity=900, Urgency Level=Routine Restocking, Customization Type=Standard, Expected ROI=9.1, Acquisition Stage currentIndex=0

7. Name: Architecture Firm Copper Custom, customer: Architecture Firm, stage: negotiation
   Custom fields: Order Quantity=150, Urgency Level=Standard Order, Customization Type=Material Upgrade, Expected ROI=9.2, Acquisition Stage currentIndex=3

8. Name: Financial Services Archive, customer: Financial Services Archive, stage: closed_won
   Custom fields: Order Quantity=3000, Urgency Level=Routine Restocking, Customization Type=Standard, Expected ROI=9.5, Acquisition Stage currentIndex=5

9. Name: Hospital Hypoallergenic Micro, customer: Hospital Administration Supply, stage: qualification
   Custom fields: Order Quantity=400, Urgency Level=Standard Order, Customization Type=Material Upgrade, Expected ROI=8.7, Acquisition Stage currentIndex=1

10. Name: Creative Agency Neon Colors, customer: Creative Agency Studio, stage: prospecting
    Custom fields: Order Quantity=150, Urgency Level=Standard Order, Customization Type=Color Custom, Expected ROI=8.6, Acquisition Stage currentIndex=0

11. Name: Enterprise Data Center Titanium, customer: Enterprise Data Center, stage: negotiation
    Custom fields: Order Quantity=2500, Urgency Level=Priority Rush, Customization Type=Material Upgrade, Expected ROI=9.9, Acquisition Stage currentIndex=3

12. Name: School District Budget Order, customer: School District Supply Center, stage: closed_won
    Custom fields: Order Quantity=800, Urgency Level=Routine Restocking, Customization Type=Color Custom, Expected ROI=8.4, Acquisition Stage currentIndex=4

13. Name: Mom & Pop Silver Micro, customer: Mom & Pop Stationary Store, stage: qualification
    Custom fields: Order Quantity=50, Urgency Level=Routine Restocking, Customization Type=Standard, Expected ROI=7.8, Acquisition Stage currentIndex=1

14. Name: Government Rust-Resistant Bulk, customer: Government Procurement Office, stage: closed_won
    Custom fields: Order Quantity=4000, Urgency Level=Routine Restocking, Customization Type=Material Upgrade, Expected ROI=9.3, Acquisition Stage currentIndex=5

15. Name: Startup Sustainability Initiative, customer: Tech Startup Office, stage: prospecting
    Custom fields: Order Quantity=100, Urgency Level=Standard Order, Customization Type=Material Upgrade, Expected ROI=8.9, Acquisition Stage currentIndex=0

Confirm when all 15 opportunities are created.
```

---

### Step 9 of 10 — Activities

```
I'm continuing setup for Paperclip Optimization Corporation (PORC). Now create 18 activities. Map: "Completed" → completed, "Scheduled" → pending.

1. Subject: Superstore Clip Stress Testing, type: task, duration: 120 min, date: 2024-01-15, status: completed, notes: "Standard clips under load"
2. Subject: Distributor Paper Binding Trials, type: task, duration: 60 min, date: 2024-01-16, status: completed, notes: "Multi-color clip durability"
3. Subject: Metal Durability Assessment, type: meeting, duration: 30 min, date: 2024-01-17, status: pending, notes: "Steel vs titanium analysis"
4. Subject: Rust Resistance Benchmarking, type: task, duration: 180 min, date: 2024-01-18, status: completed, notes: "Environmental exposure test"
5. Subject: Customer Satisfaction Survey Call, type: call, duration: 15 min, date: 2024-01-19, status: completed, notes: "Feedback collection"
6. Subject: Supply Chain Optimization Review, type: meeting, duration: 60 min, date: 2024-01-22, status: pending, notes: "Procurement efficiency"
7. Subject: Government Procurement Shortage Crisis, type: task, duration: 240 min, date: 2024-01-23, status: completed, notes: "Emergency response coordination"
8. Subject: Competitor Clip Comparison Meeting, type: meeting, duration: 45 min, date: 2024-01-24, status: completed, notes: "Market analysis"
9. Subject: Tech Startup Titanium Testing, type: task, duration: 90 min, date: 2024-01-25, status: completed, notes: "Premium material evaluation"
10. Subject: University Supply Multicolor Order Review, type: meeting, duration: 30 min, date: 2024-01-26, status: pending, notes: "Color assortment planning"
11. Subject: Hospital Hypoallergenic Certification, type: task, duration: 120 min, date: 2024-01-29, status: completed, notes: "Medical compliance validation"
12. Subject: Law Firm Logo Engraving Quality Check, type: task, duration: 45 min, date: 2024-01-30, status: completed, notes: "Customization standards"
13. Subject: Print Shop High Volume Capacity, type: meeting, duration: 30 min, date: 2024-02-01, status: pending, notes: "Volume handling assessment"
14. Subject: Financial Services Archive Bulk Organization, type: task, duration: 150 min, date: 2024-02-02, status: completed, notes: "Large scale storage testing"
15. Subject: Creative Agency Neon Color Appeal, type: meeting, duration: 20 min, date: 2024-02-05, status: completed, notes: "Design aesthetic review"
16. Subject: Enterprise Data Center Installation, type: task, duration: 180 min, date: 2024-02-06, status: completed, notes: "Industrial strength validation"
17. Subject: School District Multi-Color Logistics, type: call, duration: 20 min, date: 2024-02-08, status: pending, notes: "Distribution coordination"
18. Subject: Annual Clip Industry Conference, type: meeting, duration: 120 min, date: 2024-02-15, status: completed, notes: "Innovation and standards update"

Confirm when all 18 activities are created.
```

---

### Step 10 of 10 — CustomRecords, Orders, Invoices & Templates

```
I'm continuing setup for Paperclip Optimization Corporation (PORC). This is the final step.

**CustomRecords (use bulkCreateCustomRecords):**
1. name: "Clip Manufacturing Line Alpha", type: equipment, status: active, serialNumber: MANUF-ALPHA-001
2. name: "Industrial Shredder — Recycling Grade", type: equipment, status: active, serialNumber: SHRED-REC-001
3. name: "Clip Quality Inspection Scanner (Automated)", type: hardware, status: active, serialNumber: QI-AUTO-001, customer: Creative Agency Studio
4. name: "Bulk Storage Warehouse Climate Control", type: hardware, status: active, serialNumber: CLIMATE-STOR-001, customer: Government Procurement Office
5. name: "Clip Counting Calibration Machine", type: equipment, status: maintenance, serialNumber: COUNT-CAL-001, customer: Financial Services Archive
6. name: "Transport Fleet (Refrigerated) — 15 units", type: vehicle, status: active, serialNumber: TRANSP-REF-015
7. name: "Paperclip Design CAD System (Proprietary)", type: software, status: active, serialNumber: CAD-PROP-001
8. name: "Clip Durability Testing Apparatus", type: equipment, status: active, serialNumber: TEST-DURA-001
9. name: "Safety Packaging Machine Array — 50 units", type: inventory, status: active, serialNumber: PKG-ARRAY-050
10. name: "Neon Color Anodizing Equipment", type: hardware, status: inactive, serialNumber: ANODIZE-NEON-001, customer: Creative Agency Studio

**Orders:**
1. customer: Corporate Essentials Distributor, name: "Bulk Standard Clip Shipment", status: DELIVERED, totalAmount: 28000, currency: USD
2. customer: Government Procurement Office, name: "Rust-Resistant Clip Supply", status: CONFIRMED, totalAmount: 52000, currency: USD
3. customer: Creative Agency Studio, name: "Neon Color Specialty Batch", status: SHIPPED, totalAmount: 8500, currency: USD
4. customer: Financial Services Archive, name: "Industrial Archive Clips", status: DRAFT, totalAmount: 15000, currency: USD
5. customer: Tech Startup Office, name: "Custom Startup Clip Set", status: CANCELLED, totalAmount: 3200, currency: USD
6. customer: Mom & Pop Stationary Store, name: "Retail Refill Packs", status: DELIVERED, totalAmount: 2100, currency: USD

**Invoices:**
1. customer: Corporate Essentials Distributor, status: PAID, totalAmount: 28000, currency: USD, paymentTerms: NET-30
2. customer: Government Procurement Office, status: SENT, totalAmount: 52000, currency: USD, paymentTerms: NET-60
3. customer: Creative Agency Studio, status: PAID, totalAmount: 8500, currency: USD, paymentTerms: NET-30
4. customer: Financial Services Archive, status: DRAFT, totalAmount: 15000, currency: USD, paymentTerms: NET-45
5. customer: Tech Startup Office, status: OVERDUE, totalAmount: 3200, currency: USD, paymentTerms: NET-30
6. customer: Mom & Pop Stationary Store, status: PAID, totalAmount: 2100, currency: USD, paymentTerms: NET-30

**Document Templates (use createDocumentTemplate for each):**
1. name: "Clip Industry Report", templateType: slide_deck, description: "Serious corporate-grade clip market analysis deck for management review", styleJson: {"layout":"corporate","accentColor":"#424242","backgroundColor":"#1A1A1A","h1Color":"#BDBDBD"}
2. name: "Clip Supplier Profile", templateType: one_pager, description: "Single-page supplier evaluation for procurement teams", styleJson: {"layout":"light","accentColor":"#424242","includeCustomFields":true}
3. name: "Clip Inventory Export", templateType: csv_export, description: "Full clip inventory export with color, durability, and cost data", styleJson: {"includeFields":["name","status","material_grade","color_variant","durability_rating","unit_cost"]}

**Notification Templates (use createNotificationTemplate for each):**
1. name: "Clip Order Confirmed", notificationType: ORDER_CREATED, isActive: true
   subject: "Clip Order Confirmed: {{customerName}} — {{amount}} Units"
   body: "A clip order has been confirmed and queued for manufacturing.\n\nCustomer: {{customerName}}\nClip Type: {{opportunityName}}\nQuantity: {{amount}} units\nShipment Date: {{dueDate}}\n\nTrack: {{link}}"

2. name: "Supplier Status Update", notificationType: CUSTOMER_UPDATED, isActive: true
   subject: "Supplier Record Updated: {{customerName}}"
   body: "A key supplier's record has been modified.\n\nSupplier: {{customerName}}\nStatus: {{status}}\nUpdated: {{assignee}}\n\nReview: {{link}}"

3. name: "Neon Batch Ready", notificationType: ACTIVITY_CREATED, isActive: true
   subject: "Specialty Neon Clip Batch Ready: {{customerName}}"
   body: "A specialty neon color clip batch is ready for delivery.\n\nCustomer: {{customerName}}\nBatch: {{opportunityName}}\nColor: Neon {{stage}}\nQuantity: {{amount}}\n\n{{link}}"

Confirm when all customRecords, orders, invoices, document templates, and notification templates are created. PORC setup complete!
```

---

## Disposable Undergarment Enterprises (DUE) — 10 Steps

### Step 1 of 10 — Customer Custom Fields

```
I'm setting up a CRM workspace for "Disposable Undergarment Enterprises" (DUE). In this step, please create the custom fields for Customers (Retailers/Institutions).

Create the following custom fields for the Customer entity:
1. "Customer Type" (select field, required) — options: Retail Chain, Hospital, Nursing Home, Travel Industry, Corporate, Niche Online
2. "Target Demographics" (multiselect) — options: Travel, Healthcare, Elderly, Athletes, New Parents, Outdoors Enthusiasts
3. "Monthly Unit Consumption" (number field) — units per month
4. "Size Range Preference" (multiselect) — options: XS, S, M, L, XL, XXL, Universal Fit
5. "Material Preference" (select field) — options: Cotton Blend, Bamboo, Recycled Plastic, Premium Synthetic, Moisture-Wicking
6. "Comfort Rating (Customer)" (number field, 0-10) — satisfaction on comfort
7. "Packaging Preference" (select field) — options: Individual Wrapped, Box of 12, Box of 24, Bulk Bin
8. "Sustainability Concern" (percentage field) — eco-conscious rating
9. "Wholesale Account" (boolean field) — bulk purchase eligible
10. "Last Reorder Date" (date field) — purchase history
11. "Price Sensitivity" (select field) — options: Budget, Mid-Range, Premium, No Concern
12. "Special Instructions" (textarea field) — custom requests
13. "Customer Retention Progress" (workflow field) — milestones: New Customer, First Order, Repeat Buyer, Loyal Subscriber, Advocate

Confirm when all 13 customer custom fields have been created.
```

---

### Step 2 of 10 — Opportunity Custom Fields

```
I'm continuing setup for Disposable Undergarment Enterprises (DUE). Customer custom fields are done. Now create the custom fields for the Opportunity entity (Sales Orders/Contracts).

Create the following custom fields for the Opportunity entity:
1. "Order Type" (select field, required) — options: One-Time Purchase, Subscription Monthly, Bulk Corporate, Hospital/Medical, Retail Partnership
2. "Unit Quantity (Each)" (number field) — individual units ordered
3. "Urgency Level" (select field) — options: Standard, Expedited, Urgent, EMERGENCY SITUATION
4. "Margin Target (%)" (number field) — profit expectation
5. "Sales Progression" (workflow field) — milestones: Inquiry, Quotation, Negotiation, Order Placed, Fulfillment, Satisfaction

Confirm when all 5 opportunity custom fields have been created.
```

---

### Step 3 of 10 — Calculated Fields & Business Policies

```
I'm continuing setup for Disposable Undergarment Enterprises (DUE). Custom fields are done. Now create the calculated fields and business policies.

**Calculated Fields:**
1. For Customer — "Lifetime Value Score":
   Expression: (Monthly_Unit_Consumption * 1.5) + (if Wholesale_Account then 50 else 0) + (Comfort_Rating * 8) + (Sustainability_Concern / 2)

2. For Opportunity — "Sales Completion %":
   Expression: if Sales_Progression exists then (Sales_Progression.currentIndex + 1) * 16.6 else 0

**Business Policies (Rego syntax — each condition on a separate line):**
1. DENY — "No Premium Synthetic for Eco Customers":
   input.entity.material_preference == "Premium Synthetic"
   input.entity.sustainability_concern > 75

2. WARN — "Bulk Order Without Wholesale Pricing":
   input.entity.order_type == "Bulk Corporate"
   input.entity.pricing_tier == "Retail"

3. DENY — "EMERGENCY Orders Minimum Quantity":
   input.entity.urgency_level == "EMERGENCY SITUATION"
   input.entity.unit_quantity < 1000

Confirm when calculated fields and policies are created.
```

---

### Step 4 of 10 — Tenant Settings & Branding

```
I'm continuing setup for Disposable Undergarment Enterprises (DUE). Now update the tenant settings with the following branding:

- Company Name: Disposable Undergarment Enterprises
- Logo URL: https://www.mountainside-medical.com/cdn/shop/products/Prevail-Protective-Underwear_1800x1800.jpg?v=1600375517
- Primary Color: #FFB6C1
- Secondary Color: #87CEEB
- Website: https://disposableunderwear.com
- Bio: "Disposable Undergarment Enterprises (DUE) disrupts the intimate apparel industry with single-use undergarments designed for convenience, hygiene, and modern lifestyles. From travel to hospital applications, DUE provides comfort where it counts. Wear once. Toss responsibly."

Confirm when tenant settings have been updated.
```

---

### Step 5 of 10 — Customers 1–8

```
I'm continuing setup for Disposable Undergarment Enterprises (DUE). Now create the first 8 customers.

1. Name: Travel Smart Outfitters, status: prospect
   Custom fields: Customer Type=Retail Chain, Target Demographics=[Travel], Monthly Unit Consumption=25000, Size Range Preference=[M, L, XL], Material Preference=Cotton Blend, Comfort Rating=8.9, Packaging Preference=Individual Wrapped, Sustainability Concern=45, Wholesale Account=false, Last Reorder Date=2024-01-10, Price Sensitivity=Mid-Range, Customer Retention Progress currentIndex=2

2. Name: Metro Hospital System, status: active
   Custom fields: Customer Type=Hospital, Target Demographics=[Healthcare], Monthly Unit Consumption=45000, Size Range Preference=[Universal Fit], Material Preference=Premium Synthetic, Comfort Rating=9.2, Packaging Preference=Bulk Bin, Sustainability Concern=55, Wholesale Account=true, Last Reorder Date=2024-01-12, Price Sensitivity=No Concern, Customer Retention Progress currentIndex=3

3. Name: Grandview Senior Living, status: active
   Custom fields: Customer Type=Nursing Home, Target Demographics=[Elderly], Monthly Unit Consumption=12000, Size Range Preference=[L, XL, XXL], Material Preference=Moisture-Wicking, Comfort Rating=8.5, Packaging Preference=Box of 24, Sustainability Concern=50, Wholesale Account=false, Last Reorder Date=2024-01-08, Price Sensitivity=Budget, Customer Retention Progress currentIndex=2

4. Name: Marathon Runners Club, status: active
   Custom fields: Customer Type=Corporate, Target Demographics=[Athletes], Monthly Unit Consumption=8900, Size Range Preference=[S, M, L], Material Preference=Moisture-Wicking, Comfort Rating=9.1, Packaging Preference=Individual Wrapped, Sustainability Concern=65, Wholesale Account=true, Last Reorder Date=2024-01-11, Price Sensitivity=Premium, Customer Retention Progress currentIndex=3

5. Name: New Parent Supply Co, status: prospect
   Custom fields: Customer Type=Niche Online, Target Demographics=[New Parents], Monthly Unit Consumption=5500, Size Range Preference=[XS, S, M], Material Preference=Cotton Blend, Comfort Rating=8.7, Packaging Preference=Individual Wrapped, Sustainability Concern=70, Wholesale Account=false, Last Reorder Date=2024-01-09, Price Sensitivity=Mid-Range, Customer Retention Progress currentIndex=1

6. Name: Summit Adventure Gear, status: active
   Custom fields: Customer Type=Retail Chain, Target Demographics=[Outdoors Enthusiasts], Monthly Unit Consumption=16200, Size Range Preference=[M, L, XL], Material Preference=Bamboo, Comfort Rating=9.3, Packaging Preference=Individual Wrapped, Sustainability Concern=85, Wholesale Account=true, Last Reorder Date=2024-01-13, Price Sensitivity=Premium, Customer Retention Progress currentIndex=3

7. Name: Children's Hospital Network, status: active
   Custom fields: Customer Type=Hospital, Target Demographics=[Healthcare], Monthly Unit Consumption=32000, Size Range Preference=[XS, S, M], Material Preference=Cotton Blend, Comfort Rating=9.4, Packaging Preference=Box of 12, Sustainability Concern=60, Wholesale Account=true, Last Reorder Date=2024-01-14, Price Sensitivity=No Concern, Customer Retention Progress currentIndex=4

8. Name: Bright Futures Academy, status: active
   Custom fields: Customer Type=Corporate, Target Demographics=[Healthcare], Monthly Unit Consumption=18500, Size Range Preference=[S, M, L], Material Preference=Cotton Blend, Comfort Rating=8.8, Packaging Preference=Box of 24, Sustainability Concern=55, Wholesale Account=false, Last Reorder Date=2024-01-07, Price Sensitivity=Mid-Range, Customer Retention Progress currentIndex=3

Confirm when all 8 customers are created.
```

---

### Step 6 of 10 — Customers 9–15

```
I'm continuing setup for Disposable Undergarment Enterprises (DUE). First 8 customers are done. Now create customers 9–15.

9. Name: Premium Wellness Boutique, status: active
   Custom fields: Customer Type=Retail Chain, Target Demographics=[Healthcare], Monthly Unit Consumption=3200, Size Range Preference=[XS, S, M, L], Material Preference=Bamboo, Comfort Rating=9.7, Packaging Preference=Individual Wrapped, Sustainability Concern=80, Wholesale Account=false, Last Reorder Date=2024-01-15, Price Sensitivity=Premium, Customer Retention Progress currentIndex=4

10. Name: International Airlines Group, status: active
    Custom fields: Customer Type=Corporate, Target Demographics=[Travel], Monthly Unit Consumption=120000, Size Range Preference=[Universal Fit], Material Preference=Premium Synthetic, Comfort Rating=9.0, Packaging Preference=Bulk Bin, Sustainability Concern=40, Wholesale Account=true, Last Reorder Date=2024-01-06, Price Sensitivity=No Concern, Customer Retention Progress currentIndex=4

11. Name: Athletic Endurance Training, status: active
    Custom fields: Customer Type=Corporate, Target Demographics=[Athletes], Monthly Unit Consumption=12800, Size Range Preference=[S, M, L, XL], Material Preference=Moisture-Wicking, Comfort Rating=9.5, Packaging Preference=Individual Wrapped, Sustainability Concern=70, Wholesale Account=true, Last Reorder Date=2024-01-12, Price Sensitivity=Mid-Range, Customer Retention Progress currentIndex=3

12. Name: Elder Care Alliance, status: active
    Custom fields: Customer Type=Nursing Home, Target Demographics=[Elderly], Monthly Unit Consumption=22500, Size Range Preference=[L, XL, XXL], Material Preference=Moisture-Wicking, Comfort Rating=8.6, Packaging Preference=Bulk Bin, Sustainability Concern=50, Wholesale Account=true, Last Reorder Date=2024-01-11, Price Sensitivity=Budget, Customer Retention Progress currentIndex=3

13. Name: Budget Travel Hostels, status: prospect
    Custom fields: Customer Type=Retail Chain, Target Demographics=[Travel], Monthly Unit Consumption=9200, Size Range Preference=[M, L, XL], Material Preference=Cotton Blend, Comfort Rating=7.9, Packaging Preference=Individual Wrapped, Sustainability Concern=35, Wholesale Account=false, Last Reorder Date=2024-01-05, Price Sensitivity=Budget, Customer Retention Progress currentIndex=1

14. Name: University Campus Health, status: active
    Custom fields: Customer Type=Corporate, Target Demographics=[Healthcare], Monthly Unit Consumption=31000, Size Range Preference=[S, M, L, XL], Material Preference=Cotton Blend, Comfort Rating=8.9, Packaging Preference=Box of 24, Sustainability Concern=60, Wholesale Account=true, Last Reorder Date=2024-01-14, Price Sensitivity=Mid-Range, Customer Retention Progress currentIndex=3

15. Name: Eco-Conscious Company Store, status: inactive
    Custom fields: Customer Type=Niche Online, Target Demographics=[Travel], Monthly Unit Consumption=4800, Size Range Preference=[M, L, XL], Material Preference=Recycled Plastic, Comfort Rating=8.4, Packaging Preference=Individual Wrapped, Sustainability Concern=95, Wholesale Account=false, Last Reorder Date=2024-01-16, Price Sensitivity=Premium, Customer Retention Progress currentIndex=2

Confirm when all 7 customers are created.
```

---

### Step 7 of 10 — Contacts

```
I'm continuing setup for Disposable Undergarment Enterprises (DUE). 15 customers are created. Now create 20 contacts.

1. Comfort Champion — VP Sales at Travel Smart Outfitters
2. Dr. Hygiene — Procurement Manager at Metro Hospital System
3. Elderly Enabler — Supply Director at Grandview Senior Living
4. Speed Runner — Account Manager at Marathon Runners Club
5. Pamela Parent — Owner at New Parent Supply Co
6. Peak Performance — Sales Lead at Summit Adventure Gear
7. Dr. Wellness — Chief Medical at Children's Hospital Network
8. Campus Coordinator — Supply Manager at Bright Futures Academy
9. Luxury Louise — Premium Brand Manager at Premium Wellness Boutique
10. Flight Captain — Procurement Lead at International Airlines Group
11. Athlete Advocate — Training Director at Athletic Endurance Training
12. Care Coordinator — Operations Manager at Elder Care Alliance
13. Budget Barry — Account Manager at Budget Travel Hostels
14. Student Services — Health Director at University Campus Health
15. Eco Ellen — Sustainability Officer at Eco-Conscious Company Store
16. Comfort Carl — Quality Assurance (company: DUE Headquarters)
17. Discreet Dan — Logistics Manager (company: DUE Distribution)
18. Fit Fiona — Product Designer (company: DUE Design)
19. Market Melissa — Marketing Director (company: DUE Marketing)
20. Founder Fernando — CEO (company: DUE Headquarters)

Confirm when all 20 contacts are created.
```

---

### Step 8 of 10 — Opportunities (Sales Orders)

```
I'm continuing setup for Disposable Undergarment Enterprises (DUE). Now create 15 sales orders as Opportunities. Map Sales Progression index: 0=prospecting, 1=qualification, 2=proposal, 3=negotiation, 4=closed_won, 5=closed_won (satisfaction).

1. Name: Travel Smart Monthly Subscription, customer: Travel Smart Outfitters, stage: prospecting
   Custom fields: Order Type=Subscription Monthly, Unit Quantity=5000, Urgency Level=Standard, Margin Target=9.2, Sales Progression currentIndex=0

2. Name: Metro Hospital Bulk Medical Order, customer: Metro Hospital System, stage: closed_won
   Custom fields: Order Type=Hospital/Medical, Unit Quantity=45000, Urgency Level=Expedited, Margin Target=18.5, Sales Progression currentIndex=4

3. Name: Marathon Runners Corporate Event, customer: Marathon Runners Club, stage: qualification
   Custom fields: Order Type=Bulk Corporate, Unit Quantity=8900, Urgency Level=Urgent, Margin Target=22.3, Sales Progression currentIndex=1

4. Name: EMERGENCY SITUATION - Airlines, customer: International Airlines Group, stage: negotiation
   Custom fields: Order Type=Subscription Monthly, Unit Quantity=120000, Urgency Level=EMERGENCY SITUATION, Margin Target=15.0, Sales Progression currentIndex=3

5. Name: Grandview Senior Quarterly, customer: Grandview Senior Living, stage: proposal
   Custom fields: Order Type=One-Time Purchase, Unit Quantity=12000, Urgency Level=Standard, Margin Target=16.8, Sales Progression currentIndex=2

6. Name: Summit Adventure Retail Partnership, customer: Summit Adventure Gear, stage: qualification
   Custom fields: Order Type=Retail Partnership, Unit Quantity=16200, Urgency Level=Standard, Margin Target=32.5, Sales Progression currentIndex=1

7. Name: Children's Hospital Warehouse Stock, customer: Children's Hospital Network, stage: closed_won
   Custom fields: Order Type=Hospital/Medical, Unit Quantity=32000, Urgency Level=Expedited, Margin Target=19.2, Sales Progression currentIndex=4

8. Name: Bright Futures Academy Semester Supply, customer: Bright Futures Academy, stage: negotiation
   Custom fields: Order Type=Subscription Monthly, Unit Quantity=18500, Urgency Level=Standard, Margin Target=20.1, Sales Progression currentIndex=3

9. Name: Premium Wellness VIP Order, customer: Premium Wellness Boutique, stage: closed_won
   Custom fields: Order Type=One-Time Purchase, Unit Quantity=3200, Urgency Level=Urgent, Margin Target=35.8, Sales Progression currentIndex=5

10. Name: Elder Care Alliance Monthly, customer: Elder Care Alliance, stage: closed_won
    Custom fields: Order Type=Subscription Monthly, Unit Quantity=22500, Urgency Level=Standard, Margin Target=17.4, Sales Progression currentIndex=4

11. Name: Budget Travel Bulk Purchase, customer: Budget Travel Hostels, stage: negotiation
    Custom fields: Order Type=Bulk Corporate, Unit Quantity=9200, Urgency Level=Standard, Margin Target=12.3, Sales Progression currentIndex=3

12. Name: Athletic Endurance Training Sponsorship, customer: Athletic Endurance Training, stage: proposal
    Custom fields: Order Type=Bulk Corporate, Unit Quantity=12800, Urgency Level=Expedited, Margin Target=24.5, Sales Progression currentIndex=2

13. Name: University Campus Health Emergency, customer: University Campus Health, stage: negotiation
    Custom fields: Order Type=Subscription Monthly, Unit Quantity=31000, Urgency Level=Urgent, Margin Target=18.9, Sales Progression currentIndex=3

14. Name: Eco-Conscious Store Specialty Order, customer: Eco-Conscious Company Store, stage: qualification
    Custom fields: Order Type=Retail Partnership, Unit Quantity=4800, Urgency Level=Standard, Margin Target=28.0, Sales Progression currentIndex=1

15. Name: International Airways Restock, customer: International Airlines Group, stage: closed_won
    Custom fields: Order Type=Subscription Monthly, Unit Quantity=50000, Urgency Level=Standard, Margin Target=14.7, Sales Progression currentIndex=4

Confirm when all 15 opportunities are created.
```

---

### Step 9 of 10 — Activities

```
I'm continuing setup for Disposable Undergarment Enterprises (DUE). Now create 18 activities. Map: "Completed" → completed, "Scheduled" → pending.

1. Subject: Travel Smart Comfort Testing, type: task, duration: 120 min, date: 2024-01-15, status: completed, notes: "Travel segment durability eval"
2. Subject: Metro Hospital Material Assessment, type: meeting, duration: 45 min, date: 2024-01-16, status: completed, notes: "Medical grade review"
3. Subject: Customer Satisfaction Survey Call, type: call, duration: 20 min, date: 2024-01-17, status: pending, notes: "General feedback collection"
4. Subject: Grandview Senior Sizing Review, type: task, duration: 60 min, date: 2024-01-18, status: completed, notes: "Elderly fit verification"
5. Subject: Summit Adventure Packaging Study, type: meeting, duration: 60 min, date: 2024-01-19, status: pending, notes: "Sustainability packaging"
6. Subject: Athletic Endurance Analysis, type: meeting, duration: 90 min, date: 2024-01-22, status: completed, notes: "Performance material testing"
7. Subject: International Airlines Emergency Restock, type: task, duration: 240 min, date: 2024-01-23, status: completed, notes: "Crisis coordination"
8. Subject: Children's Hospital Compliance Check, type: task, duration: 90 min, date: 2024-01-24, status: completed, notes: "Medical certification"
9. Subject: Premium Wellness VIP Consultation, type: call, duration: 30 min, date: 2024-01-25, status: completed, notes: "Premium service review"
10. Subject: Budget Travel Bulk Logistics, type: meeting, duration: 45 min, date: 2024-01-26, status: pending, notes: "Cost optimization"
11. Subject: Bright Futures Academy Semester Planning, type: task, duration: 120 min, date: 2024-01-29, status: completed, notes: "Academic supply planning"
12. Subject: Elder Care Alliance Quarterly Review, type: meeting, duration: 60 min, date: 2024-01-30, status: completed, notes: "Elderly care standards"
13. Subject: University Campus Health Delivery, type: task, duration: 75 min, date: 2024-02-01, status: pending, notes: "Campus logistics"
14. Subject: Eco-Conscious Store Sustainability Impact, type: meeting, duration: 90 min, date: 2024-02-02, status: completed, notes: "Environmental metrics"
15. Subject: Marathon Runners Event Preparation, type: call, duration: 20 min, date: 2024-02-05, status: completed, notes: "Event coordination"
16. Subject: New Parent Supply Quality Assessment, type: task, duration: 60 min, date: 2024-02-06, status: completed, notes: "Infant safety testing"
17. Subject: Annual DUE Conference, type: meeting, duration: 120 min, date: 2024-02-15, status: completed, notes: "Industry standards and innovation"
18. Subject: Discreet Delivery Protocol Training, type: task, duration: 45 min, date: 2024-02-20, status: pending, notes: "Privacy and logistics"

Confirm when all 18 activities are created.
```

---

### Step 10 of 10 — CustomRecords, Orders, Invoices & Templates

```
I'm continuing setup for Disposable Undergarment Enterprises (DUE). This is the final step.

**CustomRecords (use bulkCreateCustomRecords):**
1. name: "Comfort Manufacturing Plant Alpha", type: facility, status: active, serialNumber: MFG-ALPHA-001, customer: Premium Wellness Boutique
2. name: "Automated Comfort Product Quality Assurance Scanner", type: hardware, status: active, serialNumber: QA-AUTO-001
3. name: "Discretion Packaging & Sealing Equipment", type: equipment, status: active, serialNumber: PKG-DISC-001, customer: International Airlines Group
4. name: "Subscription Logistics Distribution Center", type: facility, status: active, serialNumber: DIST-SUB-001, customer: Metro Hospital System
5. name: "Comfort Material Testing Laboratory", type: equipment, status: maintenance, serialNumber: LAB-TEST-001, customer: Grandview Senior Living
6. name: "Monthly Delivery Fleet (Discreet Vehicles) — 20 units", type: vehicle, status: active, serialNumber: FLEET-DISC-020
7. name: "Comfort Product Research Database (Proprietary)", type: software, status: active, serialNumber: DB-PROP-001
8. name: "Compliance Documentation Archive System", type: hardware, status: active, serialNumber: ARCH-COMP-001
9. name: "Privacy-Grade Packaging Supply (1M units)", type: inventory, status: active, serialNumber: PKG-PRIV-1M
10. name: "Medical Grade Certification Analyzer", type: hardware, status: inactive, serialNumber: CERT-MED-001, customer: Children's Hospital Network

**Orders:**
1. customer: Premium Wellness Boutique, name: "Executive Comfort Subscription Package", status: DELIVERED, totalAmount: 18500, currency: USD
2. customer: International Airlines Group, name: "Crew Comfort Supply Bundle", status: CONFIRMED, totalAmount: 42000, currency: USD
3. customer: Metro Hospital System, name: "Medical-Grade Comfort Product", status: SHIPPED, totalAmount: 28000, currency: USD
4. customer: Grandview Senior Living, name: "Elderly Comfort Comprehensive Kit", status: DRAFT, totalAmount: 35000, currency: USD
5. customer: Budget Travel Hostels, name: "Economy Comfort Package", status: CANCELLED, totalAmount: 9200, currency: USD
6. customer: Athletic Endurance Training, name: "Performance Comfort Sponsorship", status: DELIVERED, totalAmount: 12800, currency: USD

**Invoices:**
1. customer: Premium Wellness Boutique, status: PAID, totalAmount: 18500, currency: USD, paymentTerms: NET-30
2. customer: International Airlines Group, status: SENT, totalAmount: 42000, currency: USD, paymentTerms: NET-60
3. customer: Metro Hospital System, status: PAID, totalAmount: 28000, currency: USD, paymentTerms: NET-45
4. customer: Grandview Senior Living, status: DRAFT, totalAmount: 35000, currency: USD, paymentTerms: NET-60
5. customer: Budget Travel Hostels, status: OVERDUE, totalAmount: 9200, currency: USD, paymentTerms: NET-30
6. customer: Athletic Endurance Training, status: PAID, totalAmount: 12800, currency: USD, paymentTerms: NET-30

**Document Templates (use createDocumentTemplate for each):**
1. name: "Comfort Analytics Report", templateType: slide_deck, description: "Discreet formal comfort product analytics deck for corporate review", styleJson: {"layout":"corporate","accentColor":"#5C6BC0","backgroundColor":"#1A1320","h1Color":"#9FA8DA"}
2. name: "Customer Comfort Profile", templateType: one_pager, description: "Single-page confidential customer comfort profile for medical review", styleJson: {"layout":"light","accentColor":"#5C6BC0","includeCustomFields":true}
3. name: "Comfort Product Export", templateType: csv_export, description: "Full comfort product export with subscription and medical compliance data", styleJson: {"includeFields":["name","status","comfort_level","subscription_type","medical_certification","customer_satisfaction_score"]}

**Notification Templates (use createNotificationTemplate for each):**
1. name: "Subscription Renewed", notificationType: ORDER_CREATED, isActive: true
   subject: "Monthly Comfort Subscription Renewed: {{customerName}}"
   body: "Your comfort subscription has been renewed.\n\nCustomer: {{customerName}}\nPackage: {{opportunityName}}\nAmount: {{amount}}\nNext Delivery: {{dueDate}}\n\nManage subscription: {{link}}"

2. name: "Customer Care Update", notificationType: CUSTOMER_UPDATED, isActive: true
   subject: "Account Update: {{customerName}}"
   body: "Your comfort account has been updated.\n\nCustomer: {{customerName}}\nStatus: {{status}}\n\nView account: {{link}}"

3. name: "Delivery Scheduled", notificationType: ACTIVITY_CREATED, isActive: true
   subject: "Discreet Delivery Scheduled: {{customerName}}"
   body: "Your monthly comfort delivery has been scheduled.\n\nScheduled for: {{dueDate}}\nDelivery method: Discreet and confidential\nAssigned to: {{assignee}}\n\nTrack delivery: {{link}}"

Confirm when all customRecords, orders, invoices, document templates, and notification templates are created. DUE setup complete!
```
