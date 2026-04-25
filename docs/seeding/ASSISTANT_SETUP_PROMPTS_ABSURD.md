# Absurdist Business Universe - Assistant Setup Prompts

Three hilarious demo tenants themed around the most ridiculous business ideas imaginable.

---

## Prompt 1: Toast Distribution Network (TDN)

```
I'm setting up a new CRM workspace for "Toast Distribution Network" (TDN) - the world's leading distributor of pre-made toast across 47 states, with a mission to ensure no one ever has to make their own toast again.

Please help me set up this workspace completely. Here's what I need:

**Company Branding:**
- Logo: https://companieslogo.com/img/orig/TOST-7d2110c1.png?t=1720244494&download=true
- Primary Color: #D2691E
- Secondary Color: #FFD700
- Website: https://toastdistribution.com
- Bio: "Toast Distribution Network revolutionizes the distribution of pre-made toast. Our proprietary "keep-it-toasted" technology ensures every slice arrives in perfect condition. From sourdough to Wonder Bread, we've got the crust game covered. Crunch is our promise."

**Custom Fields for Customers (Toast Retailers/Restaurants):**
1. "Toast Preference Level" (select field, required) - options: Lightly Toasted, Medium Brown, Dark and Crispy, Burnt (Classic), Charcoal Black
2. "Monthly Toast Consumption" (number field) - slices per month
3. "Average Crust Thickness" (percentage field, 0-100) - thickness preference
4. "Butter Budget (USD)" (currency field) - butter spending
5. "Bread Type Preference" (multiselect) - options: White, Wheat, Rye, Sourdough, Pumpernickel, English Muffin, Bagel
6. "Jam Partnership Status" (boolean field) - use our partner jam
7. "Toast Temperature (Fahrenheit)" (number field) - preferred arrival temp
8. "Crumb Spillage Tolerance" (percentage field) - crumb acceptance
9. "Premium Member" (boolean field) - subscription status
10. "Last Toast Delivery Date" (date field) - last delivery
11. "Toast Freshness Rating" (number field, 0-10) - satisfaction score
12. "Special Requests" (textarea field) - custom toast notes
13. "Toast Journey Progress" (workflow field) - milestones: Order Placed, Toasting, Bagging, Shipping, Delivered

**Custom Fields for Opportunities (Toast Orders/Contracts):**
1. "Toast Batch Size" (select field, required) - options: Single Slice, Half Dozen, Dozen, Case (24), Pallet (1000)
2. "Urgency Level" (select field) - options: Leisurely, Standard, Express, TOAST EMERGENCY
3. "Special Instructions" (richtext) - detailed toast specs
4. "Expected Crunch Factor" (percentage field) - crunch prediction
5. "Toast Contract Progression" (workflow field) - milestones: Negotiation, Manufacturing, Quality Check, Logistics, Consumed

**Calculated Fields:**
1. For Customer: "Toast Loyalty Score" = (MonthlyToastConsumption / 1000) * 50 + (if PremiumMember then 25 else 0) + (ToastFreshnessRating * 5)
2. For Opportunity: "Toast Readiness %" = if ToastContractProgression exists then (ToastContractProgression.currentIndex + 1) * 20 else 0

**Business Policies** (use Rego syntax - multiple conditions on separate lines):
1. DENY policy: Cannot ship toast with Crumb Spillage Tolerance < 20%
   ```
   input.entity.crumb_spillage_tolerance < 20
   ```
2. WARN policy: Warn if ordering Charcoal Black toast after 6 PM
   ```
   input.entity.toast_preference_level == "Charcoal Black"
   input.entity.order_time > "18:00"
   ```
3. DENY policy: Block "TOAST EMERGENCY" orders without Premium membership
   ```
   input.entity.urgency_level == "TOAST EMERGENCY"
   input.entity.premium_member == false
   ```

**Sample Data - 15 Toast Retailers (Customers):**

1. Sunrise Breakfast Diner
   - Toast Preference Level: Medium Brown
   - Monthly Toast Consumption: 450
   - Average Crust Thickness: 65%
   - Butter Budget: $500
   - Bread Type Preference: Wheat, White
   - Jam Partnership Status: true
   - Toast Temperature: 185°F
   - Crumb Spillage Tolerance: 85%
   - Premium Member: true
   - Toast Freshness Rating: 8.5
   - Special Requests: Slightly buttered, no jelly drip
   - Toast Journey Progress: Delivered (index 4)

2. The Crunch Palace
   - Toast Preference Level: Dark and Crispy
   - Monthly Toast Consumption: 1200
   - Average Crust Thickness: 95%
   - Butter Budget: $1200
   - Bread Type Preference: Sourdough
   - Jam Partnership Status: false
   - Toast Temperature: 210°F
   - Crumb Spillage Tolerance: 45%
   - Premium Member: true
   - Toast Freshness Rating: 9.2
   - Special Requests: Maximum crunch, no soft centers
   - Toast Journey Progress: Delivered (index 4)

3. Carb Counter Cafe
   - Toast Preference Level: Lightly Toasted
   - Monthly Toast Consumption: 320
   - Average Crust Thickness: 40%
   - Butter Budget: $150
   - Bread Type Preference: White
   - Jam Partnership Status: false
   - Toast Temperature: 160°F
   - Crumb Spillage Tolerance: 92%
   - Premium Member: false
   - Toast Freshness Rating: 7.8
   - Special Requests: Minimal butter, health conscious
   - Toast Journey Progress: Delivered (index 4)

4. Burnt to Perfection Restaurant
   - Toast Preference Level: Charcoal Black
   - Monthly Toast Consumption: 890
   - Average Crust Thickness: 100%
   - Butter Budget: $800
   - Bread Type Preference: Rye
   - Jam Partnership Status: true
   - Toast Temperature: 230°F
   - Crumb Spillage Tolerance: 25%
   - Premium Member: true
   - Toast Freshness Rating: 9.8
   - Special Requests: Dark as night, crispy texture essential
   - Toast Journey Progress: Delivered (index 4)

5. Golden Brown Bistro
   - Toast Preference Level: Medium Brown
   - Monthly Toast Consumption: 670
   - Average Crust Thickness: 70%
   - Butter Budget: $700
   - Bread Type Preference: Sourdough
   - Jam Partnership Status: true
   - Toast Temperature: 190°F
   - Crumb Spillage Tolerance: 80%
   - Premium Member: true
   - Toast Freshness Rating: 8.9
   - Special Requests: Artisanal quality expected
   - Toast Journey Progress: Delivered (index 4)

6. Quick Toast Express
   - Toast Preference Level: Lightly Toasted
   - Monthly Toast Consumption: 1100
   - Average Crust Thickness: 35%
   - Butter Budget: $400
   - Bread Type Preference: White
   - Jam Partnership Status: false
   - Toast Temperature: 155°F
   - Crumb Spillage Tolerance: 88%
   - Premium Member: false
   - Toast Freshness Rating: 8.1
   - Special Requests: Fast turnaround, budget conscious
   - Toast Journey Progress: Delivered (index 4)

7. The Crispy Corner
   - Toast Preference Level: Dark and Crispy
   - Monthly Toast Consumption: 445
   - Average Crust Thickness: 90%
   - Butter Budget: $500
   - Bread Type Preference: Pumpernickel
   - Jam Partnership Status: true
   - Toast Temperature: 205°F
   - Crumb Spillage Tolerance: 50%
   - Premium Member: true
   - Toast Freshness Rating: 9.1
   - Special Requests: Specialty breads preferred
   - Toast Journey Progress: Delivered (index 4)

8. Butterville Bakehouse
   - Toast Preference Level: Medium Brown
   - Monthly Toast Consumption: 580
   - Average Crust Thickness: 68%
   - Butter Budget: $1100
   - Bread Type Preference: English Muffin
   - Jam Partnership Status: true
   - Toast Temperature: 188°F
   - Crumb Spillage Tolerance: 78%
   - Premium Member: false
   - Toast Freshness Rating: 8.3
   - Special Requests: Heavy butter application expected
   - Toast Journey Progress: Delivered (index 4)

9. Toast & Jam Junction
   - Toast Preference Level: Dark and Crispy
   - Monthly Toast Consumption: 240
   - Average Crust Thickness: 88%
   - Butter Budget: $600
   - Bread Type Preference: Sourdough, Multigrain
   - Jam Partnership Status: true
   - Toast Temperature: 200°F
   - Crumb Spillage Tolerance: 60%
   - Premium Member: true
   - Toast Freshness Rating: 9.5
   - Special Requests: Jam-ready surface texture
   - Toast Journey Progress: Delivered (index 4)

10. Whole Grain Wellness
    - Toast Preference Level: Lightly Toasted
    - Monthly Toast Consumption: 380
    - Average Crust Thickness: 38%
    - Butter Budget: $80
    - Bread Type Preference: Wheat
    - Jam Partnership Status: false
    - Toast Temperature: 158°F
    - Crumb Spillage Tolerance: 95%
    - Premium Member: false
    - Toast Freshness Rating: 8.0
    - Special Requests: Whole grain only, minimal butter
    - Toast Journey Progress: Delivered (index 4)

11. The Toaster's Table
    - Toast Preference Level: Charcoal Black
    - Monthly Toast Consumption: 920
    - Average Crust Thickness: 100%
    - Butter Budget: $950
    - Bread Type Preference: Sourdough
    - Jam Partnership Status: false
    - Toast Temperature: 225°F
    - Crumb Spillage Tolerance: 20%
    - Premium Member: true
    - Toast Freshness Rating: 9.9
    - Special Requests: Extreme darkness, premium quality
    - Toast Journey Progress: Delivered (index 4)

12. Bread & Breakfast Co
    - Toast Preference Level: Medium Brown
    - Monthly Toast Consumption: 620
    - Average Crust Thickness: 72%
    - Butter Budget: $550
    - Bread Type Preference: Rye
    - Jam Partnership Status: true
    - Toast Temperature: 192°F
    - Crumb Spillage Tolerance: 82%
    - Premium Member: false
    - Toast Freshness Rating: 8.7
    - Special Requests: Traditional breakfast quality
    - Toast Journey Progress: Delivered (index 4)

13. Sourdough Dreams
    - Toast Preference Level: Dark and Crispy
    - Monthly Toast Consumption: 510
    - Average Crust Thickness: 87%
    - Butter Budget: $800
    - Bread Type Preference: Sourdough
    - Jam Partnership Status: true
    - Toast Temperature: 202°F
    - Crumb Spillage Tolerance: 55%
    - Premium Member: true
    - Toast Freshness Rating: 9.3
    - Special Requests: Artisan sourdough only
    - Toast Journey Progress: Delivered (index 4)

14. Simple Slice Cafe
    - Toast Preference Level: Lightly Toasted
    - Monthly Toast Consumption: 290
    - Average Crust Thickness: 42%
    - Butter Budget: $120
    - Bread Type Preference: White
    - Jam Partnership Status: false
    - Toast Temperature: 162°F
    - Crumb Spillage Tolerance: 90%
    - Premium Member: false
    - Toast Freshness Rating: 7.5
    - Special Requests: Simple, no frills
    - Toast Journey Progress: Delivered (index 4)

15. Premium Toast Society
    - Toast Preference Level: Charcoal Black
    - Monthly Toast Consumption: 1800
    - Average Crust Thickness: 100%
    - Butter Budget: $2500
    - Bread Type Preference: Sourdough
    - Jam Partnership Status: true
    - Toast Temperature: 235°F
    - Crumb Spillage Tolerance: 15%
    - Premium Member: true
    - Toast Freshness Rating: 10.0
    - Special Requests: VIP treatment, perfection required
    - Toast Journey Progress: Delivered (index 4)

**Sample Data - 20 Contacts:**
- Chef Butterworth (Head Toaster) at Sunrise Diner
- Crispetta Jones (Toast Quality Manager) at Crunch Palace
- Bernard Breadsworth (Owner) at Carb Counter
- Stella Scorch (Head Chef) at Burnt to Perfection
- Marina Marmalade (Jam Coordinator) at Toast & Jam Junction
- Derek Dough (Operations Manager) at Quick Toast Express
- Cinnamon Sugar (Pastry Lead) at Buttersville Bakehouse
- Monty Muffin (Product Specialist) at Golden Brown Bistro
- Petra Pumpernickel (Supply Manager) at Crispy Corner
- Reginald Rye (Grain Specialist) at Bread & Breakfast
- Toasty McToastface (Brand Ambassador) at Toast Distribution Network
- Bran Whitney (Nutrition Advisor) at Whole Grain Wellness
- Yeast Williams (Fermentation Expert) at Sourdough Dreams
- Gluten Green (Allergen Manager) at TDN HQ
- Caramelina Brown (Head Toaster) at Toaster's Table
- Sesame Sally (Seed Specialist) at Premium Toast Society
- Wheat Wilson (Grain Procurement) at TDN Sourcing
- Crust Custard (Quality Assurance) at TDN Quality
- Poppy Seedwell (Distribution Manager) at TDN Logistics
- Sourdough Steve (Master Toaster) at TDN Production

**Sample Data - 15 Toast Orders (Opportunities Linked to Retailers):**

1. Sunrise Diner Weekly Shipment
   - Retailer: Sunrise Breakfast Diner
   - Toast Batch Size: Half Dozen
   - Urgency Level: Standard
   - Special Instructions: Buttered, consistent temperature
   - Expected Crunch Factor: 85%
   - Toast Contract Progression: Logistics (index 3)

2. TOAST EMERGENCY - Event at 6:00 PM
   - Retailer: Premium Toast Society
   - Toast Batch Size: Pallet
   - Urgency Level: TOAST EMERGENCY
   - Special Instructions: Drop everything, VIP event
   - Expected Crunch Factor: 95%
   - Toast Contract Progression: Manufacturing (index 1)

3. Burnt to Perfection Monthly Bulk
   - Retailer: Burnt to Perfection Restaurant
   - Toast Batch Size: Case
   - Urgency Level: Standard
   - Special Instructions: Maximum darkness, charcoal level
   - Expected Crunch Factor: 99%
   - Toast Contract Progression: Quality Check (index 2)

4. Crunch Palace Black Toast Event
   - Retailer: The Crunch Palace
   - Toast Batch Size: Dozen
   - Urgency Level: Express
   - Special Instructions: Peak crunchiness, dark color
   - Expected Crunch Factor: 100%
   - Toast Contract Progression: Logistics (index 3)

5. Whole Grain Wellness Subscription
   - Retailer: Whole Grain Wellness
   - Toast Batch Size: Single Slice
   - Urgency Level: Leisurely
   - Special Instructions: Whole grain, minimal butter
   - Expected Crunch Factor: 75%
   - Toast Contract Progression: Manufacturing (index 1)

6. Toast & Jam Junction Sourdough Rush
   - Retailer: Toast & Jam Junction
   - Toast Batch Size: Case
   - Urgency Level: Express
   - Special Instructions: Sourdough specialty, jam-ready
   - Expected Crunch Factor: 89%
   - Toast Contract Progression: Negotiation (index 0)

7. The Toaster's Table Charcoal Deluxe
   - Retailer: The Toaster's Table
   - Toast Batch Size: Pallet
   - Urgency Level: Standard
   - Special Instructions: Maximum darkness, premium quality
   - Expected Crunch Factor: 99%
   - Toast Contract Progression: Consumed (index 4)

8. Quick Toast Express Daily Restocking
   - Retailer: Quick Toast Express
   - Toast Batch Size: Dozen
   - Urgency Level: Leisurely
   - Special Instructions: Fast delivery, budget friendly
   - Expected Crunch Factor: 80%
   - Toast Contract Progression: Logistics (index 3)

9. Golden Brown Bistro Sourdough Bulk
   - Retailer: Golden Brown Bistro
   - Toast Batch Size: Case
   - Urgency Level: Standard
   - Special Instructions: Artisanal sourdough, medium brown
   - Expected Crunch Factor: 87%
   - Toast Contract Progression: Manufacturing (index 1)

10. Premium Toast Society VIP Order
    - Retailer: Premium Toast Society
    - Toast Batch Size: Pallet
    - Urgency Level: Express
    - Special Instructions: VIP treatment, perfection required
    - Expected Crunch Factor: 100%
    - Toast Contract Progression: Consumed (index 4)

11. Bread & Breakfast Rye Special
    - Retailer: Bread & Breakfast Co
    - Toast Batch Size: Case
    - Urgency Level: Standard
    - Special Instructions: Rye bread, traditional style
    - Expected Crunch Factor: 85%
    - Toast Contract Progression: Quality Check (index 2)

12. Buttersville Muffin Toast Order
    - Retailer: Butterville Bakehouse
    - Toast Batch Size: Half Dozen
    - Urgency Level: Express
    - Special Instructions: English muffin base, heavy butter
    - Expected Crunch Factor: 81%
    - Toast Contract Progression: Negotiation (index 0)

13. Sourdough Dreams Artisan Batch
    - Retailer: Sourdough Dreams
    - Toast Batch Size: Dozen
    - Urgency Level: Leisurely
    - Special Instructions: Artisan quality, dark and crispy
    - Expected Crunch Factor: 92%
    - Toast Contract Progression: Manufacturing (index 1)

14. Simple Slice Cafe Weekly Needs
    - Retailer: Simple Slice Cafe
    - Toast Batch Size: Single Slice
    - Urgency Level: Leisurely
    - Special Instructions: Simple, no frills, basic quality
    - Expected Crunch Factor: 75%
    - Toast Contract Progression: Logistics (index 3)

15. The Crispy Corner Pumpernickel Rush
    - Retailer: The Crispy Corner
    - Toast Batch Size: Case
    - Urgency Level: TOAST EMERGENCY
    - Special Instructions: Specialty bread, express delivery
    - Expected Crunch Factor: 91%
    - Toast Contract Progression: Quality Check (index 2)

**Sample Data - 18 Activities:**

1. Sunrise Diner Toasting Session - Activity, 120 minutes, 2024-01-15, Completed, Medium brown batch
2. Crunch Palace Quality Taste Test - Meeting, 30 minutes, 2024-01-16, Scheduled, Dark crispy evaluation
3. Toast Butter Application Review - Meeting, 15 minutes, 2024-01-18, Scheduled, Application technique
4. Carb Counter Crumb Cleanup - Activity, 10 minutes, 2024-01-19, Completed, Spillage minimization
5. Temperature Monitoring Check - Activity, 5 minutes, 2024-01-22, Completed, Heat consistency
6. Premium Toast Society Call - Call, 20 minutes, 2024-01-23, Scheduled, Satisfaction survey
7. Toast Poetry Reading - Meeting, 45 minutes, 2024-01-24, Completed, Rhyming appreciation
8. Jam Pairing Consultation - Meeting, 30 minutes, 2024-01-25, Scheduled, Flavor matching
9. Burnt to Perfection Toasting - Activity, 120 minutes, 2024-01-29, Completed, Charcoal batch production
10. Golden Brown Quality Test - Meeting, 30 minutes, 2024-01-30, Completed, Medium brown standards
11. Quick Toast Express Restocking - Activity, 60 minutes, 2024-02-01, Scheduled, Inventory replenishment
12. Whole Grain Wellness Consultation - Call, 20 minutes, 2024-02-02, Completed, Health check-in
13. Toast & Jam Special Order - Meeting, 30 minutes, 2024-02-05, Scheduled, Sourdough jam pairing
14. The Toaster's Table VIP Call - Call, 20 minutes, 2024-02-06, Completed, Premium service
15. Butterville Butter Application - Meeting, 15 minutes, 2024-02-08, Scheduled, Heavy butter technique
16. Sourdough Dreams Artisan Review - Meeting, 30 minutes, 2024-02-09, Completed, Specialty assessment
17. Simple Slice Temperature Check - Activity, 5 minutes, 2024-02-12, Completed, Light toast verification
18. Emergency Toast Protocol Meeting - Meeting, 45 minutes, 2024-02-15, Scheduled, TOAST EMERGENCY training

**Standard Field Mapping (apply when creating all records):**
- Customer `status`: Derive from context — Premium Member = true → `active`; non-premium with regular orders → `prospect`; inactive/no recent orders or Crumb Spillage Tolerance < 20% (flagged) → `inactive`. Aim for ~60% active, 25% prospect, 15% inactive.
- Opportunity `stage`: Map from Toast Contract Progression index — 0 = `prospecting`, 1 = `qualification`, 2 = `proposal`, 3 = `negotiation`, 4 = `closed_won`. Use `closed_lost` for any burned/cancelled orders.
- Activity `status`: Extract from each description — "Completed" → `completed`, "Scheduled"/"Pending" → `pending`, "Cancelled" → `cancelled`.

**Sample Data - 10 CustomRecords (use bulkCreateCustomRecords):**
1. Industrial Toaster Array — 48-Slice Commercial Grade - type: equipment, status: active, serialNumber: TOAST-48-001, customer: The Toaster's Table
2. Premium Butter Churning Station - type: equipment, status: active, serialNumber: BUTTER-CH-001, customer: Butterville Butter
3. Sourdough Fermentation Chamber (Climate-Controlled) - type: hardware, status: active, serialNumber: FERM-SOUR-001, customer: Sourdough Dreams
4. Delivery Fleet Vehicle (Toast Insulated) — 10 units - type: vehicle, status: active, serialNumber: DELIV-TOAST-010
5. Jam Processing Apparatus — Berry Grade - type: equipment, status: active, serialNumber: JAM-BERRY-001, customer: Toast & Jam Special
6. Crumb Collection System (Advanced) - type: hardware, status: active, serialNumber: CRUMB-ADV-001, customer: Quick Toast Express
7. Artisan Oven Temperature Calibration Kit - type: equipment, status: maintenance, serialNumber: OVN-CAL-001, customer: Whole Grain Wellness
8. Bread Moisture Meter Fleet (x50) - type: equipment, status: active, serialNumber: MOIST-FLT-050
9. Toast Delivery Box Fleet (Temperature-Sealed) — 500 units - type: inventory, status: active, serialNumber: BOX-SEALED-500
10. Crumb Spillage Prevention Vacuum System - type: hardware, status: inactive, serialNumber: VAC-CRUMB-001, customer: Simple Slice

**Sample Data - 6 Orders:**
1. The Toaster's Table - "Premium Toast Supplies Bundle", status: DELIVERED, totalAmount: 2800, currency: USD
2. Butterville Butter - "Butter Churn Maintenance & Upgrade", status: CONFIRMED, totalAmount: 1200, currency: USD
3. Sourdough Dreams - "Artisan Fermentation Chamber Lease", status: SHIPPED, totalAmount: 950, currency: USD
4. Quick Toast Express - "Quarterly Inventory Restock", status: DRAFT, totalAmount: 3400, currency: USD
5. Whole Grain Wellness - "Organic Toast Grain Supply", status: CANCELLED, totalAmount: 680, currency: USD
6. Toast & Jam Special - "Specialty Jam Ingredients Bundle", status: DELIVERED, totalAmount: 1100, currency: USD

**Sample Data - 6 Invoices:**
1. The Toaster's Table - status: PAID, totalAmount: 2800, currency: USD, paymentTerms: NET-30
2. Butterville Butter - status: SENT, totalAmount: 1200, currency: USD, paymentTerms: NET-30
3. Sourdough Dreams - status: PAID, totalAmount: 950, currency: USD, paymentTerms: NET-14
4. Quick Toast Express - status: DRAFT, totalAmount: 3400, currency: USD, paymentTerms: NET-30
5. Whole Grain Wellness - status: OVERDUE, totalAmount: 680, currency: USD, paymentTerms: NET-30
6. Toast & Jam Special - status: PAID, totalAmount: 1100, currency: USD, paymentTerms: NET-30

**Document Templates (3 custom styles — use createDocumentTemplate for each):**
1. "Toast Consumption Report" — templateType: slide_deck, description: "Golden-brown themed consumption analytics deck for retailer meetings", styleJson: {"layout":"corporate","accentColor":"#C65911","backgroundColor":"#1A0F00","h1Color":"#FFB84D"}
2. "Retailer Toast Profile" — templateType: one_pager, description: "Single-page toast partner profile and engagement summary", styleJson: {"layout":"light","accentColor":"#C65911","includeCustomFields":true}
3. "Toast Retailer Export" — templateType: csv_export, description: "Full retailer export with toast consumption and premium member data", styleJson: {"includeFields":["name","status","premium_member","annual_toast_consumption","avg_order_value","crumb_spillage_tolerance"]}

**Notification Templates (3 toast delivery alert templates — use createNotificationTemplate for each):**
1. name: "Order Ready for Delivery", notificationType: ORDER_CREATED, isActive: true
   subject: "Toast Order Ready: {{customerName}} — {{amount}} Slices"
   body: "A fresh toast order is ready for delivery.\n\nRetailer: {{customerName}}\nOrder: {{opportunityName}}\nSlices: {{amount}}\nDelivery Expected: {{dueDate}}\n\nTrack: {{link}}"
2. name: "Premium Member Alert", notificationType: CUSTOMER_UPDATED, isActive: true
   subject: "Premium Member Status: {{customerName}}"
   body: "A retailer's premium membership has been updated.\n\nRetailer: {{customerName}}\nStatus: {{status}}\nUpdated by: {{assignee}}\n\nView: {{link}}"
3. name: "Crumb Spillage Alert", notificationType: ACTIVITY_CREATED, isActive: true
   subject: "Crumb Spillage Tolerance Alert: {{customerName}}"
   body: "A retailer's crumb spillage tolerance is approaching critical threshold.\n\nRetailer: {{customerName}}\nActivity: {{opportunityName}}\nDate: {{dueDate}}\n\nAction required: {{link}}"

Please execute this complete setup now, creating all fields, policies, retailers, contacts, orders, activities, customRecords, orders, invoices, document templates, and notification templates. Update the tenant settings with the branding information provided. Let's get this toast delivered!
```

---

## Prompt 2: Paperclip Optimization Corporation

```
I'm setting up a new CRM workspace for "Paperclip Optimization Corporation" (PORC) - the leading industry disruptor in paperclip manufacturing, distribution, and life-cycle management.

Please help me set up this workspace completely. Here's what I need:

**Company Branding:**
- Logo: https://e7.pngegg.com/pngimages/532/291/png-clipart-computer-icons-encapsulated-postscript-email-attachment-paperclips-blue-text-thumbnail.png
- Primary Color: #696969
- Secondary Color: #C0C0C0
- Website: https://paperclipoptimization.com
- Bio: "Paperclip Optimization Corporation pioneers the future of office supply chains. Our proprietary MetalClip™ technology ensures optimal binding performance across all document sizes. From SMB to enterprise, PORC connects papers in ways they've never been connected before."

**Custom Fields for Customers (Office Supply Retailers):**
1. "Clip Size Preference" (select field, required) - options: Micro (28mm), Standard (32mm), Jumbo (50mm), Giant (100mm), Industrial (200mm)
2. "Monthly Paperclip Consumption" (number field) - clips per month
3. "Metal Type Preference" (select field) - options: Steel, Plastic-Coated, Copper, Gold-Plated, Titanium
4. "Pile Height Capacity (sheets)" (number field) - max paper thickness
5. "Color Preference" (multiselect) - options: Silver, Black, Red, Blue, Rainbow, Neon
6. "Rust Resistance Rating" (percentage field, 0-100) - durability need
7. "Ergonomic Score" (number field, 0-10) - comfort preference
8. "Tangle Tolerance" (percentage field) - acceptable tangling rate
9. "Bulk Discount Eligible" (boolean field) - volume buyer
10. "Last Shipment Date" (date field) - delivery tracking
11. "Satisfaction Rating" (number field, 0-10) - client happiness
12. "Special Requirements" (textarea field) - custom needs
13. "Paperclip Acquisition Journey" (workflow field) - milestones: Interest, Proposal, Trial, Bulk Order, Loyalty Program

**Custom Fields for Opportunities (Paperclip Contracts):**
1. "Order Quantity (Boxes)" (number field, required) - quantity ordered
2. "Urgency Level" (select field) - options: Routine Restocking, Standard Order, Priority Rush, CRITICAL CLIP SHORTAGE
3. "Customization Type" (select field) - options: Standard, Logo Engraved, Color Custom, Material Upgrade
4. "Expected ROI (Paper Bound/Dollar)" (number field) - efficiency metric
5. "Acquisition Stage" (workflow field) - milestones: Quote, Negotiation, Order Confirmation, Manufacturing, Shipping, Stocked

**Calculated Fields:**
1. For Customer: "Clip Loyalty Index" = (MonthlyConsumption / 5000) * 40 + (SatisfactionRating * 6) + (if BulkDiscountEligible then 20 else 0)
2. For Opportunity: "Order Fulfillment %" = if AcquisitionStage exists then (AcquisitionStage.currentIndex + 1) * 16.6 else 0

**Business Policies:**
1. DENY policy: Cannot offer Standard clips to Titanium preference customers (MetalTypePreference = "Titanium" AND ClipType = "Standard")
2. WARN policy: Warn on "CRITICAL CLIP SHORTAGE" orders without bulk discount (UrgencyLevel = "CRITICAL CLIP SHORTAGE" AND BulkDiscountEligible = false)
3. DENY policy: Block logo engraving for orders under 10 boxes (CustomizationType = "Logo Engraved" AND OrderQuantity < 10)

**Sample Data - 15 Paperclip Retailers:**

1. Office Supplies Superstore
   - Clip Size Preference: Standard (32mm)
   - Monthly Paperclip Consumption: 50000
   - Metal Type Preference: Steel
   - Pile Height Capacity: 500 sheets
   - Color Preference: Silver
   - Rust Resistance Rating: 85%
   - Ergonomic Score: 8.5
   - Tangle Tolerance: 92%
   - Bulk Discount Eligible: true
   - Satisfaction Rating: 9.2
   - Special Requirements: High volume, consistent quality
   - Paperclip Acquisition Journey: Loyalty Program (index 4)

2. The Clip Shop
   - Clip Size Preference: Jumbo (50mm)
   - Monthly Paperclip Consumption: 12000
   - Metal Type Preference: Steel
   - Pile Height Capacity: 250 sheets
   - Color Preference: Black
   - Rust Resistance Rating: 78%
   - Ergonomic Score: 7.9
   - Tangle Tolerance: 65%
   - Bulk Discount Eligible: true
   - Satisfaction Rating: 8.8
   - Special Requirements: Premium appearance, specialty colors
   - Paperclip Acquisition Journey: Loyalty Program (index 4)

3. Corporate Essentials Distributor
   - Clip Size Preference: Standard (32mm)
   - Monthly Paperclip Consumption: 85000
   - Metal Type Preference: Steel
   - Pile Height Capacity: 400 sheets
   - Color Preference: Silver, Red, Blue
   - Rust Resistance Rating: 88%
   - Ergonomic Score: 8.8
   - Tangle Tolerance: 88%
   - Bulk Discount Eligible: true
   - Satisfaction Rating: 9.5
   - Special Requirements: Rainbow variety packs, large scale distribution
   - Paperclip Acquisition Journey: Loyalty Program (index 4)

4. Mom & Pop Stationary Store
   - Clip Size Preference: Micro (28mm)
   - Monthly Paperclip Consumption: 2500
   - Metal Type Preference: Steel
   - Pile Height Capacity: 150 sheets
   - Color Preference: Silver
   - Rust Resistance Rating: 72%
   - Ergonomic Score: 7.2
   - Tangle Tolerance: 80%
   - Bulk Discount Eligible: false
   - Satisfaction Rating: 7.9
   - Special Requirements: Small orders, personal service
   - Paperclip Acquisition Journey: Trial (index 2)

5. Tech Startup Office
   - Clip Size Preference: Standard (32mm)
   - Monthly Paperclip Consumption: 3200
   - Metal Type Preference: Titanium
   - Pile Height Capacity: 300 sheets
   - Color Preference: Silver
   - Rust Resistance Rating: 95%
   - Ergonomic Score: 9.2
   - Tangle Tolerance: 85%
   - Bulk Discount Eligible: true
   - Satisfaction Rating: 9.7
   - Special Requirements: Premium materials, modern aesthetic
   - Paperclip Acquisition Journey: Bulk Order (index 3)

6. Government Procurement Office
   - Clip Size Preference: Industrial (200mm)
   - Monthly Paperclip Consumption: 200000
   - Metal Type Preference: Steel
   - Pile Height Capacity: 600 sheets
   - Color Preference: Silver
   - Rust Resistance Rating: 92%
   - Ergonomic Score: 8.1
   - Tangle Tolerance: 70%
   - Bulk Discount Eligible: true
   - Satisfaction Rating: 9.1
   - Special Requirements: Heavy duty, compliance certified
   - Paperclip Acquisition Journey: Loyalty Program (index 4)

7. University Supply Hub
   - Clip Size Preference: Standard (32mm)
   - Monthly Paperclip Consumption: 28000
   - Metal Type Preference: Steel
   - Pile Height Capacity: 350 sheets
   - Color Preference: Silver, Red, Blue, Black
   - Rust Resistance Rating: 80%
   - Ergonomic Score: 8.0
   - Tangle Tolerance: 82%
   - Bulk Discount Eligible: true
   - Satisfaction Rating: 8.4
   - Special Requirements: Multi-color assortments, student use
   - Paperclip Acquisition Journey: Bulk Order (index 3)

8. Hospital Administration Supply
   - Clip Size Preference: Micro (28mm)
   - Monthly Paperclip Consumption: 15000
   - Metal Type Preference: Plastic-Coated
   - Pile Height Capacity: 200 sheets
   - Color Preference: Silver
   - Rust Resistance Rating: 90%
   - Ergonomic Score: 8.3
   - Tangle Tolerance: 88%
   - Bulk Discount Eligible: false
   - Satisfaction Rating: 8.6
   - Special Requirements: Hypoallergenic, medical grade
   - Paperclip Acquisition Journey: Trial (index 2)

9. Law Firm Document Management
   - Clip Size Preference: Jumbo (50mm)
   - Monthly Paperclip Consumption: 8500
   - Metal Type Preference: Gold-Plated
   - Pile Height Capacity: 350 sheets
   - Color Preference: Silver
   - Rust Resistance Rating: 98%
   - Ergonomic Score: 9.1
   - Tangle Tolerance: 60%
   - Bulk Discount Eligible: true
   - Satisfaction Rating: 9.9
   - Special Requirements: Premium appearance, large case files
   - Paperclip Acquisition Journey: Loyalty Program (index 4)

10. Print Shop Operations
    - Clip Size Preference: Standard (32mm)
    - Monthly Paperclip Consumption: 42000
    - Metal Type Preference: Steel
    - Pile Height Capacity: 400 sheets
    - Color Preference: Silver
    - Rust Resistance Rating: 86%
    - Ergonomic Score: 8.4
    - Tangle Tolerance: 89%
    - Bulk Discount Eligible: true
    - Satisfaction Rating: 9.3
    - Special Requirements: High volume, consistent supply
    - Paperclip Acquisition Journey: Loyalty Program (index 4)

11. School District Supply Center
    - Clip Size Preference: Standard (32mm)
    - Monthly Paperclip Consumption: 35000
    - Metal Type Preference: Steel
    - Pile Height Capacity: 350 sheets
    - Color Preference: Silver, Red, Blue, Black
    - Rust Resistance Rating: 82%
    - Ergonomic Score: 7.9
    - Tangle Tolerance: 84%
    - Bulk Discount Eligible: true
    - Satisfaction Rating: 8.7
    - Special Requirements: Student-friendly, colorful options
    - Paperclip Acquisition Journey: Bulk Order (index 3)

12. Architecture Firm
    - Clip Size Preference: Jumbo (50mm)
    - Monthly Paperclip Consumption: 6800
    - Metal Type Preference: Copper
    - Pile Height Capacity: 280 sheets
    - Color Preference: Silver
    - Rust Resistance Rating: 85%
    - Ergonomic Score: 8.6
    - Tangle Tolerance: 75%
    - Bulk Discount Eligible: true
    - Satisfaction Rating: 9.4
    - Special Requirements: Design aesthetic, blueprint organization
    - Paperclip Acquisition Journey: Bulk Order (index 3)

13. Financial Services Archive
    - Clip Size Preference: Industrial (200mm)
    - Monthly Paperclip Consumption: 95000
    - Metal Type Preference: Steel
    - Pile Height Capacity: 550 sheets
    - Color Preference: Silver
    - Rust Resistance Rating: 93%
    - Ergonomic Score: 8.2
    - Tangle Tolerance: 72%
    - Bulk Discount Eligible: true
    - Satisfaction Rating: 9.6
    - Special Requirements: Heavy duty archive, compliance
    - Paperclip Acquisition Journey: Loyalty Program (index 4)

14. Creative Agency Studio
    - Clip Size Preference: Micro (28mm)
    - Monthly Paperclip Consumption: 4200
    - Metal Type Preference: Steel
    - Pile Height Capacity: 180 sheets
    - Color Preference: Neon
    - Rust Resistance Rating: 75%
    - Ergonomic Score: 8.7
    - Tangle Tolerance: 78%
    - Bulk Discount Eligible: true
    - Satisfaction Rating: 8.9
    - Special Requirements: Creative colors, fun designs
    - Paperclip Acquisition Journey: Bulk Order (index 3)

15. Enterprise Data Center
    - Clip Size Preference: Industrial (200mm)
    - Monthly Paperclip Consumption: 150000
    - Metal Type Preference: Titanium
    - Pile Height Capacity: 600 sheets
    - Color Preference: Silver
    - Rust Resistance Rating: 99%
    - Ergonomic Score: 8.9
    - Tangle Tolerance: 68%
    - Bulk Discount Eligible: true
    - Satisfaction Rating: 9.8
    - Special Requirements: Maximum durability, enterprise scale
    - Paperclip Acquisition Journey: Loyalty Program (index 4)

**Sample Data - 20 Contacts:**
- Sterling Silver (Sales Director) at Office Supplies Superstore
- Clipper Mendez (Procurement Manager) at Corporate Essentials
- Rusty Rings (Quality Assurance) at PORC Manufacturing
- Dr. Metal Fatigue (Research & Development) at PORC Labs
- Jennifer Stapler (Competitor Liaison) at The Clip Shop
- Marcus Binder (Volume Sales) at Government Procurement
- Clarice Paper (Account Manager) at Law Firm Services
- Phillip Metal (Product Engineering) at PORC Engineering
- Betty Document (Supply Chain) at Hospital Administration
- Coil Spring (Operations Manager) at PORC Operations
- Tension Taylor (Design Specialist) at PORC Design
- Grip Gripper (Customer Success) at PORC Customer Service
- Stack Holmes (Inventory Manager) at University Supply
- Fastener Frank (B2B Specialist) at Print Shop
- Wire Whitney (Technical Support) at PORC Tech Support
- Bind Blackwell (Contract Negotiator) at PORC Sales
- Clasp Clarkson (Marketing Director) at PORC Marketing
- Connector Carl (Distribution Manager) at PORC Logistics
- Loop Larson (Packaging Specialist) at PORC Packaging
- Hold Harris (CEO) at PORC Headquarters

**Sample Data - 15 Orders:**

1. Corporate Essentials Bulk Order
   - Retailer: Corporate Essentials Distributor
   - Order Quantity: 1000 boxes
   - Urgency Level: Routine Restocking
   - Customization Type: Color Custom
   - Expected ROI: 9.2
   - Acquisition Stage: Quote (index 0)

2. CRITICAL CLIP SHORTAGE - Government
   - Retailer: Government Procurement Office
   - Order Quantity: 5000 boxes
   - Urgency Level: CRITICAL CLIP SHORTAGE
   - Customization Type: Standard
   - Expected ROI: 9.8
   - Acquisition Stage: Manufacturing (index 3)

3. Law Firm Custom Logo Clips
   - Retailer: Law Firm Document Management
   - Order Quantity: 50 boxes
   - Urgency Level: Standard Order
   - Customization Type: Logo Engraved
   - Expected ROI: 8.5
   - Acquisition Stage: Negotiation (index 1)

4. Tech Startup Titanium Rush
   - Retailer: Tech Startup Office
   - Order Quantity: 200 boxes
   - Urgency Level: Priority Rush
   - Customization Type: Material Upgrade
   - Expected ROI: 9.7
   - Acquisition Stage: Order Confirmation (index 2)

5. University Multi-Color Distribution
   - Retailer: University Supply Hub
   - Order Quantity: 750 boxes
   - Urgency Level: Routine Restocking
   - Customization Type: Color Custom
   - Expected ROI: 8.3
   - Acquisition Stage: Shipping (index 4)

6. Print Shop Weekly Standard
   - Retailer: Print Shop Operations
   - Order Quantity: 900 boxes
   - Urgency Level: Routine Restocking
   - Customization Type: Standard
   - Expected ROI: 9.1
   - Acquisition Stage: Quote (index 0)

7. Architecture Firm Copper Custom
   - Retailer: Architecture Firm
   - Order Quantity: 150 boxes
   - Urgency Level: Standard Order
   - Customization Type: Material Upgrade
   - Expected ROI: 9.2
   - Acquisition Stage: Manufacturing (index 3)

8. Financial Services Archive
   - Retailer: Financial Services Archive
   - Order Quantity: 3000 boxes
   - Urgency Level: Routine Restocking
   - Customization Type: Standard
   - Expected ROI: 9.5
   - Acquisition Stage: Stocked (index 5)

9. Hospital Hypoallergenic Micro
   - Retailer: Hospital Administration Supply
   - Order Quantity: 400 boxes
   - Urgency Level: Standard Order
   - Customization Type: Material Upgrade
   - Expected ROI: 8.7
   - Acquisition Stage: Negotiation (index 1)

10. Creative Agency Neon Colors
    - Retailer: Creative Agency Studio
    - Order Quantity: 150 boxes
    - Urgency Level: Standard Order
    - Customization Type: Color Custom
    - Expected ROI: 8.6
    - Acquisition Stage: Quote (index 0)

11. Enterprise Data Center Titanium
    - Retailer: Enterprise Data Center
    - Order Quantity: 2500 boxes
    - Urgency Level: Priority Rush
    - Customization Type: Material Upgrade
    - Expected ROI: 9.9
    - Acquisition Stage: Manufacturing (index 3)

12. School District Budget Order
    - Retailer: School District Supply Center
    - Order Quantity: 800 boxes
    - Urgency Level: Routine Restocking
    - Customization Type: Color Custom
    - Expected ROI: 8.4
    - Acquisition Stage: Shipping (index 4)

13. Mom & Pop Silver Micro
    - Retailer: Mom & Pop Stationary Store
    - Order Quantity: 50 boxes
    - Urgency Level: Routine Restocking
    - Customization Type: Standard
    - Expected ROI: 7.8
    - Acquisition Stage: Negotiation (index 1)

14. Government Rust-Resistant Bulk
    - Retailer: Government Procurement Office
    - Order Quantity: 4000 boxes
    - Urgency Level: Routine Restocking
    - Customization Type: Material Upgrade
    - Expected ROI: 9.3
    - Acquisition Stage: Stocked (index 5)

15. Startup Sustainability Initiative
    - Retailer: Tech Startup Office
    - Order Quantity: 100 boxes
    - Urgency Level: Standard Order
    - Customization Type: Material Upgrade
    - Expected ROI: 8.9
    - Acquisition Stage: Quote (index 0)

**Sample Data - 18 Activities:**

1. Superstore Clip Stress Testing - Activity, 120 minutes, 2024-01-15, Completed, Standard clips under load
2. Distributor Paper Binding Trials - Activity, 60 minutes, 2024-01-16, Completed, Multi-color clip durability
3. Metal Durability Assessment - Meeting, 30 minutes, 2024-01-17, Scheduled, Steel vs titanium analysis
4. Rust Resistance Benchmarking - Activity, 180 minutes, 2024-01-18, Completed, Environmental exposure test
5. Customer Satisfaction Survey Call - Call, 15 minutes, 2024-01-19, Completed, Feedback collection
6. Supply Chain Optimization Review - Meeting, 60 minutes, 2024-01-22, Scheduled, Procurement efficiency
7. Government Procurement Shortage Crisis - Activity, 240 minutes, 2024-01-23, Completed, Emergency response coordination
8. Competitor Clip Comparison Meeting - Meeting, 45 minutes, 2024-01-24, Completed, Market analysis
9. Tech Startup Titanium Testing - Activity, 90 minutes, 2024-01-25, Completed, Premium material evaluation
10. University Supply Multicolor Order Review - Meeting, 30 minutes, 2024-01-26, Scheduled, Color assortment planning
11. Hospital Hypoallergenic Certification - Activity, 120 minutes, 2024-01-29, Completed, Medical compliance validation
12. Law Firm Logo Engraving Quality Check - Activity, 45 minutes, 2024-01-30, Completed, Customization standards
13. Print Shop High Volume Capacity - Meeting, 30 minutes, 2024-02-01, Scheduled, Volume handling assessment
14. Financial Services Archive Bulk Organization - Activity, 150 minutes, 2024-02-02, Completed, Large scale storage testing
15. Creative Agency Neon Color Appeal - Meeting, 20 minutes, 2024-02-05, Completed, Design aesthetic review
16. Enterprise Data Center Installation - Activity, 180 minutes, 2024-02-06, Completed, Industrial strength validation
17. School District Multi-Color Logistics - Call, 20 minutes, 2024-02-08, Scheduled, Distribution coordination
18. Annual Clip Industry Conference - Meeting, 120 minutes, 2024-02-15, Completed, Innovation and standards update

Make these hilariously overly-serious with dates across "Clip Quarter" (Jan-Feb).

**Standard Field Mapping (apply when creating all records):**
- Customer `status`: Derive from context — active paper clip consumers with regular orders and Premium Member = true → `active`; newer or occasional consumers → `prospect`; inactive accounts or those with supply issues → `inactive`. Aim for ~60% active, 25% prospect, 15% inactive.
- Opportunity `stage`: Map from the contract workflow index — 0 = `prospecting`, 1 = `qualification`, 2 = `proposal`, 3 = `negotiation`, 4 = `closed_won`. Use `closed_lost` for lost/cancelled clip orders.
- Activity `status`: Extract from each description — "Completed" → `completed`, "Scheduled"/"Pending" → `pending`, "Cancelled" → `cancelled`.

**Sample Data - 10 CustomRecords (use bulkCreateCustomRecords):**
1. Clip Manufacturing Line Alpha - type: equipment, status: active, serialNumber: MANUF-ALPHA-001, customer: Paperclip Optimization HQ
2. Industrial Shredder — Recycling Grade - type: equipment, status: active, serialNumber: SHRED-REC-001
3. Clip Quality Inspection Scanner (Automated) - type: hardware, status: active, serialNumber: QI-AUTO-001, customer: Creative Agency
4. Bulk Storage Warehouse Climate Control - type: hardware, status: active, serialNumber: CLIMATE-STOR-001, customer: Government Procurement Office
5. Clip Counting Calibration Machine - type: equipment, status: maintenance, serialNumber: COUNT-CAL-001, customer: Financial Services Archive
6. Transport Fleet (Refrigerated) — 15 units - type: vehicle, status: active, serialNumber: TRANSP-REF-015
7. Paperclip Design CAD System (Proprietary) - type: software, status: active, serialNumber: CAD-PROP-001
8. Clip Durability Testing Apparatus - type: equipment, status: active, serialNumber: TEST-DURA-001
9. Safety Packaging Machine Array — 50 units - type: inventory, status: active, serialNumber: PKG-ARRAY-050
10. Neon Color Anodizing Equipment - type: hardware, status: inactive, serialNumber: ANODIZE-NEON-001, customer: Creative Agency

**Sample Data - 6 Orders:**
1. Paperclip Optimization HQ - "Bulk Standard Clip Shipment", status: DELIVERED, totalAmount: 28000, currency: USD
2. Government Procurement Office - "Rust-Resistant Clip Supply", status: CONFIRMED, totalAmount: 52000, currency: USD
3. Creative Agency - "Neon Color Specialty Batch", status: SHIPPED, totalAmount: 8500, currency: USD
4. Financial Services Archive - "Industrial Archive Clips", status: DRAFT, totalAmount: 15000, currency: USD
5. Tech Startup Office - "Custom Startup Clip Set", status: CANCELLED, totalAmount: 3200, currency: USD
6. Mom & Pop Stationary Store - "Retail Refill Packs", status: DELIVERED, totalAmount: 2100, currency: USD

**Sample Data - 6 Invoices:**
1. Paperclip Optimization HQ - status: PAID, totalAmount: 28000, currency: USD, paymentTerms: NET-30
2. Government Procurement Office - status: SENT, totalAmount: 52000, currency: USD, paymentTerms: NET-60
3. Creative Agency - status: PAID, totalAmount: 8500, currency: USD, paymentTerms: NET-30
4. Financial Services Archive - status: DRAFT, totalAmount: 15000, currency: USD, paymentTerms: NET-45
5. Tech Startup Office - status: OVERDUE, totalAmount: 3200, currency: USD, paymentTerms: NET-30
6. Mom & Pop Stationary Store - status: PAID, totalAmount: 2100, currency: USD, paymentTerms: NET-30

**Document Templates (3 custom styles — use createDocumentTemplate for each):**
1. "Clip Industry Report" — templateType: slide_deck, description: "Serious corporate-grade clip market analysis deck for management review", styleJson: {"layout":"corporate","accentColor":"#424242","backgroundColor":"#1A1A1A","h1Color":"#BDBDBD"}
2. "Clip Supplier Profile" — templateType: one_pager, description: "Single-page supplier evaluation for procurement teams", styleJson: {"layout":"light","accentColor":"#424242","includeCustomFields":true}
3. "Clip Inventory Export" — templateType: csv_export, description: "Full clip inventory export with color, durability, and cost data", styleJson: {"includeFields":["name","status","material_grade","color_variant","durability_rating","unit_cost"]}

**Notification Templates (3 clip dispatch templates — use createNotificationTemplate for each):**
1. name: "Clip Order Confirmed", notificationType: ORDER_CREATED, isActive: true
   subject: "Clip Order Confirmed: {{customerName}} — {{amount}} Units"
   body: "A clip order has been confirmed and queued for manufacturing.\n\nCustomer: {{customerName}}\nClip Type: {{opportunityName}}\nQuantity: {{amount}} units\nShipment Date: {{dueDate}}\n\nTrack: {{link}}"
2. name: "Supplier Status Update", notificationType: CUSTOMER_UPDATED, isActive: true
   subject: "Supplier Record Updated: {{customerName}}"
   body: "A key supplier's record has been modified.\n\nSupplier: {{customerName}}\nStatus: {{status}}\nUpdated: {{assignee}}\n\nReview: {{link}}"
3. name: "Neon Batch Ready", notificationType: ACTIVITY_CREATED, isActive: true
   subject: "Specialty Neon Clip Batch Ready: {{customerName}}"
   body: "A specialty neon color clip batch is ready for delivery.\n\nCustomer: {{customerName}}\nBatch: {{opportunityName}}\nColor: Neon {{stage}}\nQuantity: {{amount}}\n\n{{link}}"

Please execute this complete setup now, creating all fields, policies, retailers, contacts, orders, activities, customRecords, orders, invoices, document templates, and notification templates. Update the tenant settings with the branding information provided. One clip at a time!
```

---

## Prompt 3: Disposable Undergarment Enterprises (DUE)

```
I'm setting up a new CRM workspace for "Disposable Undergarment Enterprises" (DUE) - the disruptive innovator revolutionizing the single-use undergarment market across North America.

Please help me set up this workspace completely. Here's what I need:

**Company Branding:**
- Logo: https://www.mountainside-medical.com/cdn/shop/products/Prevail-Protective-Underwear_1800x1800.jpg?v=1600375517
- Primary Color: #FFB6C1
- Secondary Color: #87CEEB
- Website: https://disposableunderwear.com
- Bio: "Disposable Undergarment Enterprises (DUE) disrupts the intimate apparel industry with single-use undergarments designed for convenience, hygiene, and modern lifestyles. From travel to hospital applications, DUE provides comfort where it counts. Wear once. Toss responsibly."

**Custom Fields for Customers (Retailers/Institutions):**
1. "Customer Type" (select field, required) - options: Retail Chain, Hospital, Nursing Home, Travel Industry, Corporate, Niche Online
2. "Target Demographics" (multiselect) - options: Travel, Healthcare, Elderly, Athletes, New Parents, Outdoors Enthusiasts
3. "Monthly Unit Consumption" (number field) - units per month
4. "Size Range Preference" (multiselect) - options: XS, S, M, L, XL, XXL, Universal Fit
5. "Material Preference" (select field) - options: Cotton Blend, Bamboo, Recycled Plastic, Premium Synthetic, Moisture-Wicking
6. "Comfort Rating (Customer)" (number field, 0-10) - satisfaction on comfort
7. "Packaging Preference" (select field) - options: Individual Wrapped, Box of 12, Box of 24, Bulk Bin
8. "Sustainability Concern" (percentage field) - eco-conscious rating
9. "Wholesale Account" (boolean field) - bulk purchase eligible
10. "Last Reorder Date" (date field) - purchase history
11. "Price Sensitivity" (select field) - options: Budget, Mid-Range, Premium, No Concern
12. "Special Instructions" (textarea field) - custom requests
13. "Customer Retention Progress" (workflow field) - milestones: New Customer, First Order, Repeat Buyer, Loyal Subscriber, Advocate

**Custom Fields for Opportunities (Sales Orders/Contracts):**
1. "Order Type" (select field, required) - options: One-Time Purchase, Subscription Monthly, Bulk Corporate, Hospital/Medical, Retail Partnership
2. "Unit Quantity (Each)" (number field) - individual units ordered
3. "Urgency Level" (select field) - options: Standard, Expedited, Urgent, EMERGENCY SITUATION
4. "Margin Target (%)" (number field) - profit expectation
5. "Sales Progression" (workflow field) - milestones: Inquiry, Quotation, Negotiation, Order Placed, Fulfillment, Satisfaction

**Calculated Fields:**
1. For Customer: "Lifetime Value Score" = (MonthlyConsumption * 1.5) + (if WholesaleAccount then 50 else 0) + (ComfortRating * 8) + (SustainabilityConcern / 2)
2. For Opportunity: "Sales Completion %" = if SalesProgression exists then (SalesProgression.currentIndex + 1) * 16.6 else 0

**Business Policies:**
1. DENY policy: Cannot offer "Premium Synthetic" to high sustainability concern customers (MaterialPreference = "Premium Synthetic" AND SustainabilityConcern > 75)
2. WARN policy: Warn on bulk orders without wholesale pricing (OrderType = "Bulk Corporate" AND PricingTier = "Retail")
3. DENY policy: Block EMERGENCY orders under 1000 units (UrgencyLevel = "EMERGENCY SITUATION" AND Quantity < 1000)

**Sample Data - 15 Retailers/Institutions:**

1. Travel Smart Outfitters
   - Customer Type: Retail Chain
   - Target Demographics: Travel
   - Monthly Unit Consumption: 25000
   - Size Range Preference: M, L, XL
   - Material Preference: Cotton Blend
   - Comfort Rating: 8.9
   - Packaging Preference: Individual Wrapped
   - Sustainability Concern: 45%
   - Wholesale Account: false
   - Price Sensitivity: Mid-Range
   - Last Reorder Date: 2024-01-10
   - Customer Retention Progress: Repeat Buyer (index 2)

2. Metro Hospital System
   - Customer Type: Hospital
   - Target Demographics: Healthcare
   - Monthly Unit Consumption: 45000
   - Size Range Preference: Universal Fit
   - Material Preference: Premium Synthetic
   - Comfort Rating: 9.2
   - Packaging Preference: Bulk Bin
   - Sustainability Concern: 55%
   - Wholesale Account: true
   - Price Sensitivity: No Concern
   - Last Reorder Date: 2024-01-12
   - Customer Retention Progress: Loyal Subscriber (index 3)

3. Grandview Senior Living
   - Customer Type: Nursing Home
   - Target Demographics: Elderly
   - Monthly Unit Consumption: 12000
   - Size Range Preference: L, XL, XXL
   - Material Preference: Moisture-Wicking
   - Comfort Rating: 8.5
   - Packaging Preference: Box of 24
   - Sustainability Concern: 50%
   - Wholesale Account: false
   - Price Sensitivity: Budget
   - Last Reorder Date: 2024-01-08
   - Customer Retention Progress: Repeat Buyer (index 2)

4. Marathon Runners Club
   - Customer Type: Corporate
   - Target Demographics: Athletes
   - Monthly Unit Consumption: 8900
   - Size Range Preference: S, M, L
   - Material Preference: Moisture-Wicking
   - Comfort Rating: 9.1
   - Packaging Preference: Individual Wrapped
   - Sustainability Concern: 65%
   - Wholesale Account: true
   - Price Sensitivity: Premium
   - Last Reorder Date: 2024-01-11
   - Customer Retention Progress: Loyal Subscriber (index 3)

5. New Parent Supply Co
   - Customer Type: Niche Online
   - Target Demographics: New Parents
   - Monthly Unit Consumption: 5500
   - Size Range Preference: XS, S, M
   - Material Preference: Cotton Blend
   - Comfort Rating: 8.7
   - Packaging Preference: Individual Wrapped
   - Sustainability Concern: 70%
   - Wholesale Account: false
   - Price Sensitivity: Mid-Range
   - Last Reorder Date: 2024-01-09
   - Customer Retention Progress: First Order (index 1)

6. Summit Adventure Gear
   - Customer Type: Retail Chain
   - Target Demographics: Outdoors Enthusiasts
   - Monthly Unit Consumption: 16200
   - Size Range Preference: M, L, XL
   - Material Preference: Bamboo
   - Comfort Rating: 9.3
   - Packaging Preference: Individual Wrapped
   - Sustainability Concern: 85%
   - Wholesale Account: true
   - Price Sensitivity: Premium
   - Last Reorder Date: 2024-01-13
   - Customer Retention Progress: Loyal Subscriber (index 3)

7. Children's Hospital Network
   - Customer Type: Hospital
   - Target Demographics: Healthcare
   - Monthly Unit Consumption: 32000
   - Size Range Preference: XS, S, M
   - Material Preference: Cotton Blend
   - Comfort Rating: 9.4
   - Packaging Preference: Box of 12
   - Sustainability Concern: 60%
   - Wholesale Account: true
   - Price Sensitivity: No Concern
   - Last Reorder Date: 2024-01-14
   - Customer Retention Progress: Advocate (index 4)

8. Bright Futures Academy
   - Customer Type: Corporate
   - Target Demographics: Healthcare
   - Monthly Unit Consumption: 18500
   - Size Range Preference: S, M, L
   - Material Preference: Cotton Blend
   - Comfort Rating: 8.8
   - Packaging Preference: Box of 24
   - Sustainability Concern: 55%
   - Wholesale Account: false
   - Price Sensitivity: Mid-Range
   - Last Reorder Date: 2024-01-07
   - Customer Retention Progress: Loyal Subscriber (index 3)

9. Premium Wellness Boutique
   - Customer Type: Retail Chain
   - Target Demographics: Healthcare
   - Monthly Unit Consumption: 3200
   - Size Range Preference: XS, S, M, L
   - Material Preference: Bamboo
   - Comfort Rating: 9.7
   - Packaging Preference: Individual Wrapped
   - Sustainability Concern: 80%
   - Wholesale Account: false
   - Price Sensitivity: Premium
   - Last Reorder Date: 2024-01-15
   - Customer Retention Progress: Advocate (index 4)

10. International Airlines Group
    - Customer Type: Corporate
    - Target Demographics: Travel
    - Monthly Unit Consumption: 120000
    - Size Range Preference: Universal Fit
    - Material Preference: Premium Synthetic
    - Comfort Rating: 9.0
    - Packaging Preference: Bulk Bin
    - Sustainability Concern: 40%
    - Wholesale Account: true
    - Price Sensitivity: No Concern
    - Last Reorder Date: 2024-01-06
    - Customer Retention Progress: Advocate (index 4)

11. Athletic Endurance Training
    - Customer Type: Corporate
    - Target Demographics: Athletes
    - Monthly Unit Consumption: 12800
    - Size Range Preference: S, M, L, XL
    - Material Preference: Moisture-Wicking
    - Comfort Rating: 9.5
    - Packaging Preference: Individual Wrapped
    - Sustainability Concern: 70%
    - Wholesale Account: true
    - Price Sensitivity: Mid-Range
    - Last Reorder Date: 2024-01-12
    - Customer Retention Progress: Loyal Subscriber (index 3)

12. Elder Care Alliance
    - Customer Type: Nursing Home
    - Target Demographics: Elderly
    - Monthly Unit Consumption: 22500
    - Size Range Preference: L, XL, XXL
    - Material Preference: Moisture-Wicking
    - Comfort Rating: 8.6
    - Packaging Preference: Bulk Bin
    - Sustainability Concern: 50%
    - Wholesale Account: true
    - Price Sensitivity: Budget
    - Last Reorder Date: 2024-01-11
    - Customer Retention Progress: Loyal Subscriber (index 3)

13. Budget Travel Hostels
    - Customer Type: Retail Chain
    - Target Demographics: Travel
    - Monthly Unit Consumption: 9200
    - Size Range Preference: M, L, XL
    - Material Preference: Cotton Blend
    - Comfort Rating: 7.9
    - Packaging Preference: Individual Wrapped
    - Sustainability Concern: 35%
    - Wholesale Account: false
    - Price Sensitivity: Budget
    - Last Reorder Date: 2024-01-05
    - Customer Retention Progress: First Order (index 1)

14. University Campus Health
    - Customer Type: Corporate
    - Target Demographics: Healthcare
    - Monthly Unit Consumption: 31000
    - Size Range Preference: S, M, L, XL
    - Material Preference: Cotton Blend
    - Comfort Rating: 8.9
    - Packaging Preference: Box of 24
    - Sustainability Concern: 60%
    - Wholesale Account: true
    - Price Sensitivity: Mid-Range
    - Last Reorder Date: 2024-01-14
    - Customer Retention Progress: Loyal Subscriber (index 3)

15. Eco-Conscious Company Store
    - Customer Type: Niche Online
    - Target Demographics: Travel
    - Monthly Unit Consumption: 4800
    - Size Range Preference: M, L, XL
    - Material Preference: Recycled Plastic
    - Comfort Rating: 8.4
    - Packaging Preference: Individual Wrapped
    - Sustainability Concern: 95%
    - Wholesale Account: false
    - Price Sensitivity: Premium
    - Last Reorder Date: 2024-01-16
    - Customer Retention Progress: Repeat Buyer (index 2)

**Sample Data - 20 Contacts:**
- Comfort Champion (VP Sales) at Travel Smart
- Dr. Hygiene (Procurement Manager) at Metro Hospital
- Elderly Enabler (Supply Director) at Grandview Senior
- Speed Runner (Account Manager) at Marathon Club
- Pamela Parent (Owner) at New Parent Supply
- Peak Performance (Sales Lead) at Summit Adventure
- Dr. Wellness (Chief Medical) at Children's Hospital
- Campus Coordinator (Supply Manager) at Bright Futures
- Luxury Louise (Premium Brand Manager) at Premium Wellness
- Flight Captain (Procurement Lead) at International Airlines
- Athlete Advocate (Training Director) at Athletic Endurance
- Care Coordinator (Operations Manager) at Elder Care Alliance
- Budget Barry (Account Manager) at Budget Travel
- Student Services (Health Director) at University Campus
- Eco Ellen (Sustainability Officer) at Eco-Conscious Store
- Comfort Carl (Quality Assurance) at DUE Headquarters
- Discreet Dan (Logistics Manager) at DUE Distribution
- Fit Fiona (Product Designer) at DUE Design
- Market Melissa (Marketing Director) at DUE Marketing
- Founder Fernando (CEO) at DUE Headquarters

**Sample Data - 15 Orders:**

1. Travel Smart Monthly Subscription
   - Retailer: Travel Smart Outfitters
   - Order Type: Subscription Monthly
   - Unit Quantity: 5000
   - Urgency Level: Standard
   - Margin Target: 9.2
   - Sales Progression: Inquiry (index 0)

2. Metro Hospital Bulk Medical Order
   - Retailer: Metro Hospital System
   - Order Type: Hospital/Medical
   - Unit Quantity: 45000
   - Urgency Level: Expedited
   - Margin Target: 18.5
   - Sales Progression: Fulfillment (index 4)

3. Marathon Runners Corporate Event
   - Retailer: Marathon Runners Club
   - Order Type: Bulk Corporate
   - Unit Quantity: 8900
   - Urgency Level: Urgent
   - Margin Target: 22.3
   - Sales Progression: Quotation (index 1)

4. EMERGENCY SITUATION - Airlines
   - Retailer: International Airlines Group
   - Order Type: Subscription Monthly
   - Unit Quantity: 120000
   - Urgency Level: EMERGENCY SITUATION
   - Margin Target: 15.0
   - Sales Progression: Order Placed (index 3)

5. Grandview Senior Quarterly
   - Retailer: Grandview Senior Living
   - Order Type: One-Time Purchase
   - Unit Quantity: 12000
   - Urgency Level: Standard
   - Margin Target: 16.8
   - Sales Progression: Negotiation (index 2)

6. Summit Adventure Retail Partnership
   - Retailer: Summit Adventure Gear
   - Order Type: Retail Partnership
   - Unit Quantity: 16200
   - Urgency Level: Standard
   - Margin Target: 32.5
   - Sales Progression: Quotation (index 1)

7. Children's Hospital Warehouse Stock
   - Retailer: Children's Hospital Network
   - Order Type: Hospital/Medical
   - Unit Quantity: 32000
   - Urgency Level: Expedited
   - Margin Target: 19.2
   - Sales Progression: Fulfillment (index 4)

8. Bright Futures Academy Semester Supply
   - Retailer: Bright Futures Academy
   - Order Type: Subscription Monthly
   - Unit Quantity: 18500
   - Urgency Level: Standard
   - Margin Target: 20.1
   - Sales Progression: Order Placed (index 3)

9. Premium Wellness VIP Order
   - Retailer: Premium Wellness Boutique
   - Order Type: One-Time Purchase
   - Unit Quantity: 3200
   - Urgency Level: Urgent
   - Margin Target: 35.8
   - Sales Progression: Satisfaction (index 5)

10. Elder Care Alliance Monthly
    - Retailer: Elder Care Alliance
    - Order Type: Subscription Monthly
    - Unit Quantity: 22500
    - Urgency Level: Standard
    - Margin Target: 17.4
    - Sales Progression: Fulfillment (index 4)

11. Budget Travel Bulk Purchase
    - Retailer: Budget Travel Hostels
    - Order Type: Bulk Corporate
    - Unit Quantity: 9200
    - Urgency Level: Standard
    - Margin Target: 12.3
    - Sales Progression: Order Placed (index 3)

12. Athletic Endurance Training Sponsorship
    - Retailer: Athletic Endurance Training
    - Order Type: Bulk Corporate
    - Unit Quantity: 12800
    - Urgency Level: Expedited
    - Margin Target: 24.5
    - Sales Progression: Negotiation (index 2)

13. University Campus Health Emergency
    - Retailer: University Campus Health
    - Order Type: Subscription Monthly
    - Unit Quantity: 31000
    - Urgency Level: Urgent
    - Margin Target: 18.9
    - Sales Progression: Order Placed (index 3)

14. Eco-Conscious Store Specialty Order
    - Retailer: Eco-Conscious Company Store
    - Order Type: Retail Partnership
    - Unit Quantity: 4800
    - Urgency Level: Standard
    - Margin Target: 28.0
    - Sales Progression: Quotation (index 1)

15. International Airways Restock
    - Retailer: International Airlines Group
    - Order Type: Subscription Monthly
    - Unit Quantity: 50000
    - Urgency Level: Standard
    - Margin Target: 14.7
    - Sales Progression: Fulfillment (index 4)

**Sample Data - 18 Activities:**

1. Travel Smart Comfort Testing - Activity, 120 minutes, 2024-01-15, Completed, Travel segment durability eval
2. Metro Hospital Material Assessment - Meeting, 45 minutes, 2024-01-16, Completed, Medical grade review
3. Customer Satisfaction Survey Call - Call, 20 minutes, 2024-01-17, Scheduled, General feedback collection
4. Grandview Senior Sizing Review - Activity, 60 minutes, 2024-01-18, Completed, Elderly fit verification
5. Summit Adventure Packaging Study - Meeting, 60 minutes, 2024-01-19, Scheduled, Sustainability packaging
6. Athletic Endurance Analysis - Meeting, 90 minutes, 2024-01-22, Completed, Performance material testing
7. International Airlines Emergency Restock - Activity, 240 minutes, 2024-01-23, Completed, Crisis coordination
8. Children's Hospital Compliance Check - Activity, 90 minutes, 2024-01-24, Completed, Medical certification
9. Premium Wellness VIP Consultation - Call, 30 minutes, 2024-01-25, Completed, Premium service review
10. Budget Travel Bulk Logistics - Meeting, 45 minutes, 2024-01-26, Scheduled, Cost optimization
11. Bright Futures Academy Semester Planning - Activity, 120 minutes, 2024-01-29, Completed, Academic supply planning
12. Elder Care Alliance Quarterly Review - Meeting, 60 minutes, 2024-01-30, Completed, Elderly care standards
13. University Campus Health Delivery - Activity, 75 minutes, 2024-02-01, Scheduled, Campus logistics
14. Eco-Conscious Store Sustainability Impact - Meeting, 90 minutes, 2024-02-02, Completed, Environmental metrics
15. Marathon Runners Event Preparation - Call, 20 minutes, 2024-02-05, Completed, Event coordination
16. New Parent Supply Quality Assessment - Activity, 60 minutes, 2024-02-06, Completed, Infant safety testing
17. Annual DUE Conference - Meeting, 120 minutes, 2024-02-15, Completed, Industry standards and innovation
18. Discreet Delivery Protocol Training - Activity, 45 minutes, 2024-02-20, Scheduled, Privacy and logistics

Make these amusingly deadpan with dates across "Comfort Quarter" (Jan-Feb).

**Standard Field Mapping (apply when creating all records):**
- Customer `status`: Derive from context — active comfort seekers with subscriptions → `active`; new/prospective comfort enthusiasts → `prospect`; churned/inactive/comfort-resistant → `inactive`. Aim for ~60% active, 25% prospect, 15% inactive.
- Opportunity `stage`: Map from the comfort workflow index — 0 = `prospecting`, 1 = `qualification`, 2 = `proposal`, 3 = `negotiation`, 4 = `closed_won`. Use `closed_lost` for cancelled/refused comfort orders.
- Activity `status`: Extract from each description — "Completed" → `completed`, "Scheduled"/"Pending" → `pending`, "Cancelled" → `cancelled`.

**Sample Data - 10 CustomRecords (use bulkCreateCustomRecords):**
1. Comfort Manufacturing Plant Alpha - type: facility, status: active, serialNumber: MFG-ALPHA-001, customer: Premium Wellness
2. Automated Comfort Product Quality Assurance Scanner - type: hardware, status: active, serialNumber: QA-AUTO-001
3. Discretion Packaging & Sealing Equipment - type: equipment, status: active, serialNumber: PKG-DISC-001, customer: International Airlines
4. Subscription Logistics Distribution Center - type: facility, status: active, serialNumber: DIST-SUB-001, customer: Metro Hospital
5. Comfort Material Testing Laboratory - type: equipment, status: maintenance, serialNumber: LAB-TEST-001, customer: Grandview Senior Living
6. Monthly Delivery Fleet (Discreet Vehicles) — 20 units - type: vehicle, status: active, serialNumber: FLEET-DISC-020
7. Comfort Product Research Database (Proprietary) - type: software, status: active, serialNumber: DB-PROP-001
8. Compliance Documentation Archive System - type: hardware, status: active, serialNumber: ARCH-COMP-001
9. Privacy-Grade Packaging Supply (1M units) - type: inventory, status: active, serialNumber: PKG-PRIV-1M
10. Medical Grade Certification Analyzer - type: hardware, status: inactive, serialNumber: CERT-MED-001, customer: Children's Hospital

**Sample Data - 6 Orders:**
1. Premium Wellness - "Executive Comfort Subscription Package", status: DELIVERED, totalAmount: 18500, currency: USD
2. International Airlines - "Crew Comfort Supply Bundle", status: CONFIRMED, totalAmount: 42000, currency: USD
3. Metro Hospital - "Medical-Grade Comfort Product", status: SHIPPED, totalAmount: 28000, currency: USD
4. Grandview Senior Living - "Elderly Comfort Comprehensive Kit", status: DRAFT, totalAmount: 35000, currency: USD
5. Budget Travel Co - "Economy Comfort Package", status: CANCELLED, totalAmount: 9200, currency: USD
6. Athletic Endurance Training - "Performance Comfort Sponsorship", status: DELIVERED, totalAmount: 12800, currency: USD

**Sample Data - 6 Invoices:**
1. Premium Wellness - status: PAID, totalAmount: 18500, currency: USD, paymentTerms: NET-30
2. International Airlines - status: SENT, totalAmount: 42000, currency: USD, paymentTerms: NET-60
3. Metro Hospital - status: PAID, totalAmount: 28000, currency: USD, paymentTerms: NET-45
4. Grandview Senior Living - status: DRAFT, totalAmount: 35000, currency: USD, paymentTerms: NET-60
5. Budget Travel Co - status: OVERDUE, totalAmount: 9200, currency: USD, paymentTerms: NET-30
6. Athletic Endurance Training - status: PAID, totalAmount: 12800, currency: USD, paymentTerms: NET-30

**Document Templates (3 custom styles — use createDocumentTemplate for each):**
1. "Comfort Analytics Report" — templateType: slide_deck, description: "Discreet formal comfort product analytics deck for corporate review", styleJson: {"layout":"corporate","accentColor":"#5C6BC0","backgroundColor":"#1A1320","h1Color":"#9FA8DA"}
2. "Customer Comfort Profile" — templateType: one_pager, description: "Single-page confidential customer comfort profile for medical review", styleJson: {"layout":"light","accentColor":"#5C6BC0","includeCustomFields":true}
3. "Comfort Product Export" — templateType: csv_export, description: "Full comfort product export with subscription and medical compliance data", styleJson: {"includeFields":["name","status","comfort_level","subscription_type","medical_certification","customer_satisfaction_score"]}

**Notification Templates (3 discreet notification templates — use createNotificationTemplate for each):**
1. name: "Subscription Renewed", notificationType: ORDER_CREATED, isActive: true
   subject: "Monthly Comfort Subscription Renewed: {{customerName}}"
   body: "Your comfort subscription has been renewed.\n\nCustomer: {{customerName}}\nPackage: {{opportunityName}}\nAmount: {{amount}}\nNext Delivery: {{dueDate}}\n\nManage subscription: {{link}}"
2. name: "Customer Care Update", notificationType: CUSTOMER_UPDATED, isActive: true
   subject: "Account Update: {{customerName}}"
   body: "Your comfort account has been updated.\n\nCustomer: {{customerName}}\nStatus: {{status}}\n\nView account: {{link}}"
3. name: "Delivery Scheduled", notificationType: ACTIVITY_CREATED, isActive: true
   subject: "Discreet Delivery Scheduled: {{customerName}}"
   body: "Your monthly comfort delivery has been scheduled.\n\nScheduled for: {{dueDate}}\nDelivery method: Discreet and confidential\nAssigned to: {{assignee}}\n\nTrack delivery: {{link}}"

Please execute this complete setup now, creating all fields, policies, retailers, contacts, orders, activities, customRecords, orders, invoices, document templates, and notification templates. Update the tenant settings with the branding information provided. Comfort awaits!
```

---

## 📖 How to Use These Absurd Prompts

1. **Pick your favorite absurd business**: Toast, Paperclips, or Disposable Underwear
2. **Copy the entire prompt** from this file
3. **Go to chat**: http://localhost:5173/onboarding
4. **Create new tenant** → Skip wizard → Go to Chat
5. **Paste the prompt** and hit Enter
6. **Wait ~4-5 minutes** (executes automatically)
7. **Laugh as your absurd CRM builds**

**Each prompt creates**:
- ✅ 13 unique custom fields (themed to the absurd business)
- ✅ 2 calculated fields with ridiculous SpEL expressions
- ✅ 2-3 business policies with silly constraints
- ✅ 15 sample customers/retailers
- ✅ 20 hilariously-named contacts
- ✅ 15 ridiculous orders/contracts
- ✅ 18+ activities with absurd operation types
- ✅ Branded company settings with terrible names

**Total setup time**: ~15 minutes for all 3 absurd tenants

---

## 🎭 The Absurdity Awaits

May these prompts bring laughter to your testing. May your toast be warm, your clips be straight, and your undergarments be... well... disposable.

