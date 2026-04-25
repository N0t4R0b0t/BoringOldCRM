# Harry Potter Universe - Assistant Setup Prompts

Three richly-detailed demo tenants themed around the Harry Potter magical universe.

---

## Prompt 1: Diagon Alley Business Alliance (DABA)

```
I'm setting up a new CRM workspace for "Diagon Alley Business Alliance" (DABA) - the central commerce and networking organization managing all legitimate merchant activities, trade agreements, and magical supply chains across Diagon Alley.

Please help me set up this workspace completely. Here's what I need:

**Company Branding:**
- Logo: https://via.placeholder.com/200x100?text=Diagon+Alley
- Primary Color: #8B0000
- Secondary Color: #FFD700
- Website: https://diagon-alley-alliance.magic
- Bio: "Diagon Alley Business Alliance coordinates magical commerce, merchant partnerships, and trade logistics across the wizarding world. We facilitate supply chains for potion ingredients, spell components, magical creatures, and enchanted artifacts. DABA ensures legitimate magical enterprise flourishes across all of Europe."

**Custom Fields for Customers (Merchants/Vendors):**
1. "Shop Category" (select field, required) - options: Potion Supplies, Books & Scrolls, Wand Crafting, Creature Care, Enchanted Objects, Robes & Fashion, Apothecary, Magical Food
2. "Specialization Skill" (text field) - area of expertise
3. "Wizard Registration Level" (number field, 1-10) - Ministry standing
4. "Galleon Annual Revenue" (currency field) - yearly earnings
5. "Potion License Status" (select field) - options: Licensed, Restricted, Apprentice, Master Brewer
6. "Creature Care Certification" (select field) - options: None, Beginner, Intermediate, Advanced, Master
7. "Member Since (Year)" (date field) - how long in alliance
8. "Hex-Free Business Record" (boolean field) - clean compliance history
9. "Staff Wand Training Completion" (percentage field) - employee competency
10. "Dark Arts Supply Restrictions" (text field) - banned products list
11. "Owl Delivery Coverage" (select field) - options: Local, Regional, National, International, Multiple Continents
12. "Magical Artifact Authentication" (boolean field) - can authenticate items
13. "Business Relationship Status" (workflow field) - milestones: Inquiry, Application, Approval, Active Member, Master Tradesman

**Custom Fields for Opportunities (Trade Agreements/Contracts):**
1. "Trade Type" (select field, required) - options: Supplier Agreement, Distribution Partnership, Apprenticeship, Equipment Supply, Potion Procurement, Creature Trade
2. "Galleon Value (Annual)" (currency field) - contract value
3. "Potion Ingredient Specialty" (multiselect) - options: Moonstone, Unicorn Hair, Phoenix Feathers, Dragon Blood, Essence of Murtlap, Powdered Moonstone
4. "Delivery Method" (select field) - options: Owl Post, Floo Network, Portkey, Magical Courier, Apparition
5. "Contract Duration (Years)" (number field) - agreement length
6. "Agreement Progress" (workflow field) - milestones: Negotiation, Approval, Implementation, Active Trade, Renewal

**Calculated Fields:**
1. For Customer: "Merchant Prestige Index" = (WizardRegistrationLevel * 8) + (YearsAsMember * 2) + (if HexFreeRecord then 25 else -10)
2. For Opportunity: "Trade Completion %" = if AgreementProgress exists then (AgreementProgress.currentIndex + 1) * 20 else 0

**Business Policies:**
1. DENY policy: Cannot trade Dark Arts supplies (DarkArtsSupplies contains any banned item AND TradeType = "Potion Procurement")
2. WARN policy: Warn on high-value trades without Master Tradesman status (GalleonValue > 50000 AND PrestigeIndex < 50)
3. DENY policy: Block Apprenticeship agreements for unregistered wizards (WizardRegistrationLevel < 3 AND TradeType = "Apprenticeship")

**Sample Data - 15 Merchants:**

1. Ollivander's Wand Shop
   - Shop Category: Wand Crafting
   - Specialization Skill: Wandcore matching and selection
   - Wizard Registration Level: 10
   - Galleon Annual Revenue: 200000
   - Potion License Status: Master Brewer
   - Creature Care Certification: None
   - Member Since: 1812
   - Hex-Free Business Record: true
   - Staff Wand Training Completion: 95%
   - Dark Arts Supply Restrictions: All dark artifacts banned
   - Owl Delivery Coverage: International
   - Magical Artifact Authentication: true
   - Business Relationship Status: Master Tradesman (index 4)

2. Gringotts Wizarding Bank
   - Shop Category: Enchanted Objects
   - Specialization Skill: Financial services and vault security
   - Wizard Registration Level: 10
   - Galleon Annual Revenue: 5000000
   - Potion License Status: Master Brewer
   - Creature Care Certification: Master
   - Member Since: 1473
   - Hex-Free Business Record: true
   - Staff Wand Training Completion: 98%
   - Dark Arts Supply Restrictions: No dark funds
   - Owl Delivery Coverage: International
   - Magical Artifact Authentication: true
   - Business Relationship Status: Master Tradesman (index 4)

3. Weasleys' Wizard Wheezes
   - Shop Category: Enchanted Objects
   - Specialization Skill: Magical novelty and joke items
   - Wizard Registration Level: 7
   - Galleon Annual Revenue: 85000
   - Potion License Status: Apprentice
   - Creature Care Certification: Beginner
   - Member Since: 1998
   - Hex-Free Business Record: true
   - Staff Wand Training Completion: 72%
   - Dark Arts Supply Restrictions: No hexes or curses
   - Owl Delivery Coverage: National
   - Magical Artifact Authentication: false
   - Business Relationship Status: Active Member (index 3)

4. Flourish and Blotts
   - Shop Category: Books & Scrolls
   - Specialization Skill: Magical literature and spell texts
   - Wizard Registration Level: 9
   - Galleon Annual Revenue: 150000
   - Potion License Status: Licensed
   - Creature Care Certification: Intermediate
   - Member Since: 1627
   - Hex-Free Business Record: true
   - Staff Wand Training Completion: 88%
   - Dark Arts Supply Restrictions: Limited dark magic texts
   - Owl Delivery Coverage: National
   - Magical Artifact Authentication: false
   - Business Relationship Status: Active Member (index 3)

5. Eeylops Owl Emporium
   - Shop Category: Creature Care
   - Specialization Skill: Owl breeding and care
   - Wizard Registration Level: 8
   - Galleon Annual Revenue: 95000
   - Potion License Status: Restricted
   - Creature Care Certification: Advanced
   - Member Since: 1742
   - Hex-Free Business Record: true
   - Staff Wand Training Completion: 85%
   - Dark Arts Supply Restrictions: No harmful creature trade
   - Owl Delivery Coverage: National
   - Magical Artifact Authentication: false
   - Business Relationship Status: Active Member (index 3)

6. The Leaky Cauldron
   - Shop Category: Magical Food
   - Specialization Skill: Magical beverage and food service
   - Wizard Registration Level: 6
   - Galleon Annual Revenue: 120000
   - Potion License Status: Licensed
   - Creature Care Certification: Beginner
   - Member Since: 1500
   - Hex-Free Business Record: true
   - Staff Wand Training Completion: 78%
   - Dark Arts Supply Restrictions: No dark potions
   - Owl Delivery Coverage: Regional
   - Magical Artifact Authentication: false
   - Business Relationship Status: Active Member (index 3)

7. Magical Menagerie
   - Shop Category: Creature Care
   - Specialization Skill: Exotic magical creature sales
   - Wizard Registration Level: 7
   - Galleon Annual Revenue: 88000
   - Potion License Status: Restricted
   - Creature Care Certification: Intermediate
   - Member Since: 1890
   - Hex-Free Business Record: true
   - Staff Wand Training Completion: 81%
   - Dark Arts Supply Restrictions: No dangerous creatures
   - Owl Delivery Coverage: Regional
   - Magical Artifact Authentication: false
   - Business Relationship Status: Active Member (index 3)

8. Madam Malkin's Robes for All Occasions
   - Shop Category: Robes & Fashion
   - Specialization Skill: Magical robe tailoring and design
   - Wizard Registration Level: 8
   - Galleon Annual Revenue: 110000
   - Potion License Status: Licensed
   - Creature Care Certification: None
   - Member Since: 1764
   - Hex-Free Business Record: true
   - Staff Wand Training Completion: 84%
   - Dark Arts Supply Restrictions: No dark enchantments
   - Owl Delivery Coverage: National
   - Magical Artifact Authentication: false
   - Business Relationship Status: Active Member (index 3)

9. Apothecary Shop
   - Shop Category: Potion Supplies
   - Specialization Skill: Potion ingredients and brewing
   - Wizard Registration Level: 9
   - Galleon Annual Revenue: 180000
   - Potion License Status: Master Brewer
   - Creature Care Certification: Advanced
   - Member Since: 1450
   - Hex-Free Business Record: true
   - Staff Wand Training Completion: 91%
   - Dark Arts Supply Restrictions: No dark potion ingredients
   - Owl Delivery Coverage: International
   - Magical Artifact Authentication: true
   - Business Relationship Status: Master Tradesman (index 4)

10. Quality Quidditch Supplies
    - Shop Category: Enchanted Objects
    - Specialization Skill: Quidditch equipment manufacturing
    - Wizard Registration Level: 7
    - Galleon Annual Revenue: 75000
    - Potion License Status: Apprentice
    - Creature Care Certification: Intermediate
    - Member Since: 1876
    - Hex-Free Business Record: true
    - Staff Wand Training Completion: 68%
    - Dark Arts Supply Restrictions: No cursed equipment
    - Owl Delivery Coverage: National
    - Magical Artifact Authentication: false
    - Business Relationship Status: Approval (index 2)

11. Scribbulus Writing Implements
    - Shop Category: Books & Scrolls
    - Specialization Skill: Magical quills and parchment
    - Wizard Registration Level: 6
    - Galleon Annual Revenue: 55000
    - Potion License Status: Licensed
    - Creature Care Certification: None
    - Member Since: 1802
    - Hex-Free Business Record: true
    - Staff Wand Training Completion: 72%
    - Dark Arts Supply Restrictions: None
    - Owl Delivery Coverage: Regional
    - Magical Artifact Authentication: false
    - Business Relationship Status: Active Member (index 3)

12. Cauldrons & Kettles Co
    - Shop Category: Apothecary
    - Specialization Skill: Cauldron manufacturing
    - Wizard Registration Level: 7
    - Galleon Annual Revenue: 98000
    - Potion License Status: Licensed
    - Creature Care Certification: Beginner
    - Member Since: 1723
    - Hex-Free Business Record: true
    - Staff Wand Training Completion: 76%
    - Dark Arts Supply Restrictions: Standard cauldrons only
    - Owl Delivery Coverage: National
    - Magical Artifact Authentication: false
    - Business Relationship Status: Active Member (index 3)

13. Twilfitt and Tattings
    - Shop Category: Robes & Fashion
    - Specialization Skill: Luxury robes and high-end fashion
    - Wizard Registration Level: 8
    - Galleon Annual Revenue: 140000
    - Potion License Status: Licensed
    - Creature Care Certification: None
    - Member Since: 1703
    - Hex-Free Business Record: true
    - Staff Wand Training Completion: 87%
    - Dark Arts Supply Restrictions: No dark fashion
    - Owl Delivery Coverage: International
    - Magical Artifact Authentication: false
    - Business Relationship Status: Active Member (index 3)

14. The Owl Post
    - Shop Category: Enchanted Objects
    - Specialization Skill: Magical mail and delivery services
    - Wizard Registration Level: 7
    - Galleon Annual Revenue: 102000
    - Potion License Status: Licensed
    - Creature Care Certification: Advanced
    - Member Since: 1850
    - Hex-Free Business Record: true
    - Staff Wand Training Completion: 79%
    - Dark Arts Supply Restrictions: No cursed parcels
    - Owl Delivery Coverage: International
    - Magical Artifact Authentication: false
    - Business Relationship Status: Active Member (index 3)

15. Dragon Hide Suppliers Ltd
    - Shop Category: Enchanted Objects
    - Specialization Skill: Exotic dragon materials and supplies
    - Wizard Registration Level: 9
    - Galleon Annual Revenue: 320000
    - Potion License Status: Master Brewer
    - Creature Care Certification: Master
    - Member Since: 1567
    - Hex-Free Business Record: true
    - Staff Wand Training Completion: 93%
    - Dark Arts Supply Restrictions: Limited dragon black market
    - Owl Delivery Coverage: International
    - Magical Artifact Authentication: true
    - Business Relationship Status: Master Tradesman (index 4)

**Sample Data - 20 Contacts:**
- Garrick Ollivander (Master Wandmaker) at Ollivander's
- Gringott (Bank Director) at Gringotts
- George Weasley (Operations Manager) at Wheezes
- Fred Weasley (Creative Lead) at Wheezes
- Florean Fortescue (Ice Cream Purveyor) at Ice Cream Parlour
- Gilderoy Lockhart (Book Consultant) at Flourish & Blotts
- Eeylop (Creature Specialist) at Eeylops
- Madam Malkin (Robes Designer) at Madam Malkin's
- Elixir Expert (Senior Brewer) at Apothecary
- Hogsmeade Manager (Regional Director) at DABA
- Godric's Hollow Correspondent (Field Agent) at DABA
- Dragon Trainer (Supply Manager) at Dragon Hide Suppliers
- Knockturn Alley Liaison (Acquisition) at DABA Operations
- Enchantment Expert (Quality Assurance) at DABA QA
- Magical Commerce Officer (Compliance) at DABA Regulatory
- Registry Keeper (Records Manager) at Ministry Liaison
- Hex Detection Specialist (Compliance Officer) at DABA Compliance
- Cauldron Craftsman (Master Artisan) at Cauldrons & Kettles
- Exotic Material Procurer (Sourcing Lead) at Dragon Hide Suppliers
- Alliance Director (CEO) at DABA Headquarters

**Sample Data - 15 Trade Agreements:**

1. Potion Ingredient Supply Chain
   - Merchant: Apothecary Shop
   - Trade Type: Supplier Agreement
   - Galleon Value: 185000
   - Potion Ingredient Specialty: Essence of Murtlap, Phoenix Feathers
   - Delivery Method: Floo Network
   - Contract Duration: 5
   - Agreement Progress: Active Trade (index 3)

2. Wand Crafting Equipment Partnership
   - Merchant: Ollivander's Wand Shop
   - Trade Type: Distribution Partnership
   - Galleon Value: 95000
   - Potion Ingredient Specialty: Unicorn Hair, Phoenix Feathers
   - Delivery Method: Owl Post
   - Contract Duration: 3
   - Agreement Progress: Implementation (index 2)

3. Apothecary Master Apprenticeship
   - Merchant: Apothecary Shop
   - Trade Type: Apprenticeship
   - Galleon Value: 45000
   - Potion Ingredient Specialty: Essence of Murtlap
   - Delivery Method: Apparition
   - Contract Duration: 2
   - Agreement Progress: Active Trade (index 3)

4. Dragon Hide Procurement Exclusive
   - Merchant: Dragon Hide Suppliers Ltd
   - Trade Type: Equipment Supply
   - Galleon Value: 320000
   - Potion Ingredient Specialty: Dragon Blood, Powdered Moonstone
   - Delivery Method: Magical Courier
   - Contract Duration: 10
   - Agreement Progress: Active Trade (index 3)

5. Spell Component Distribution
   - Merchant: Scribbulus Writing Implements
   - Trade Type: Supplier Agreement
   - Galleon Value: 125000
   - Potion Ingredient Specialty: Powdered Moonstone, Essence of Newt
   - Delivery Method: Owl Post
   - Contract Duration: 4
   - Agreement Progress: Implementation (index 2)

6. Quidditch Supplies Franchise
   - Merchant: Quality Quidditch Supplies
   - Trade Type: Distribution Partnership
   - Galleon Value: 75000
   - Potion Ingredient Specialty: Unicorn Hair
   - Delivery Method: Portkey
   - Contract Duration: 5
   - Agreement Progress: Active Trade (index 3)

7. Creature Feed Wholesale
   - Merchant: Eeylops Owl Emporium
   - Trade Type: Potion Procurement
   - Galleon Value: 62000
   - Potion Ingredient Specialty: Essence of Newt
   - Delivery Method: Portkey
   - Contract Duration: 3
   - Agreement Progress: Implementation (index 2)

8. Magical Creature Exchange Program
   - Merchant: Magical Menagerie
   - Trade Type: Creature Trade
   - Galleon Value: 88000
   - Potion Ingredient Specialty: Phoenix Feathers
   - Delivery Method: Magical Courier
   - Contract Duration: 2
   - Agreement Progress: Negotiation (index 0)

9. Enchanted Object Certification
   - Merchant: Weasleys' Wizard Wheezes
   - Trade Type: Apprenticeship
   - Galleon Value: 52000
   - Potion Ingredient Specialty: Unicorn Hair
   - Delivery Method: Apparition
   - Contract Duration: 2
   - Agreement Progress: Active Trade (index 3)

10. Luxury Robes Manufacturing
    - Merchant: Twilfitt and Tattings
    - Trade Type: Supplier Agreement
    - Galleon Value: 140000
    - Potion Ingredient Specialty: Phoenix Feathers
    - Delivery Method: Floo Network
    - Contract Duration: 6
    - Agreement Progress: Active Trade (index 3)

11. Brewing Supplies Exclusive
    - Merchant: Cauldrons & Kettles Co
    - Trade Type: Equipment Supply
    - Galleon Value: 98000
    - Potion Ingredient Specialty: Powdered Moonstone
    - Delivery Method: Magical Courier
    - Contract Duration: 4
    - Agreement Progress: Active Trade (index 3)

12. Book Publishing Partnership
    - Merchant: Flourish and Blotts
    - Trade Type: Distribution Partnership
    - Galleon Value: 110000
    - Potion Ingredient Specialty: Essence of Murtlap
    - Delivery Method: Owl Post
    - Contract Duration: 5
    - Agreement Progress: Implementation (index 2)

13. Owl Delivery Network Expansion
    - Merchant: The Owl Post
    - Trade Type: Distribution Partnership
    - Galleon Value: 102000
    - Potion Ingredient Specialty: Essence of Newt
    - Delivery Method: Portkey
    - Contract Duration: 5
    - Agreement Progress: Active Trade (index 3)

14. Exotic Potion Ingredients Import
    - Merchant: Dragon Hide Suppliers Ltd
    - Trade Type: Supplier Agreement
    - Galleon Value: 175000
    - Potion Ingredient Specialty: Dragon Blood, Essence of Murtlap
    - Delivery Method: Portkey
    - Contract Duration: 7
    - Agreement Progress: Active Trade (index 3)

15. Cauldron Manufacturing & Supply
    - Merchant: Cauldrons & Kettles Co
    - Trade Type: Equipment Supply
    - Galleon Value: 98000
    - Potion Ingredient Specialty: Powdered Moonstone
    - Delivery Method: Owl Post
    - Contract Duration: 4
    - Agreement Progress: Active Trade (index 3)

**Sample Data - 18 Activities:**

1. Ministry Approval Meeting - Meeting, 90 minutes, 2024-01-15, Completed, DABA charter renewal
2. Ollivander Hex-Testing Inspection - Activity, 120 minutes, 2024-01-16, Completed, Wand safety verification
3. Apothecary Potion Ingredient Verification - Meeting, 60 minutes, 2024-01-17, Scheduled, Phoenix feather audit
4. Wand Compatibility Assessment - Activity, 45 minutes, 2024-01-18, Completed, Core material evaluation
5. Magical Menagerie Welfare Audit - Meeting, 75 minutes, 2024-01-19, Completed, Creature care standards
6. Floo Network Maintenance Check - Activity, 60 minutes, 2024-01-22, Completed, Connectivity verification
7. Dark Arts Compliance Review - Meeting, 60 minutes, 2024-01-23, Scheduled, Supply restrictions audit
8. Dragon Hide Authentication - Activity, 90 minutes, 2024-01-24, Completed, Material verification
9. Gringotts Financial Audit - Meeting, 120 minutes, 2024-01-25, Completed, Vault security check
10. Quidditch Supplies Inspection - Activity, 75 minutes, 2024-01-26, Completed, Equipment safety standards
11. Robes Quality Assessment - Meeting, 45 minutes, 2024-01-29, Scheduled, Madam Malkin standards
12. Cauldron Manufacturing Review - Activity, 90 minutes, 2024-01-30, Completed, Production capacity
13. Flourish & Blotts Inventory Check - Meeting, 60 minutes, 2024-02-01, Completed, Book stock verification
14. Eeylops Owl Care Protocol - Activity, 75 minutes, 2024-02-02, Completed, Animal welfare standards
15. Regional Delivery Network Meeting - Meeting, 90 minutes, 2024-02-05, Scheduled, Logistics expansion
16. Magical Artifact Authentication Training - Activity, 120 minutes, 2024-02-06, Completed, Expert certification
17. Annual Diagon Alley Commerce Conference - Meeting, 180 minutes, 2024-02-15, Completed, Industry standards update
18. Dark Arts Black Market Investigation - Activity, 240 minutes, 2024-02-20, Completed, Compliance enforcement

Make these appropriately magical with dates across "Wizarding Year 1995-2005".

**Standard Field Mapping (apply when creating all records):**
- Customer `status`: Derive from context — Business Relationship Status index 3-4 (Active Member / Master Tradesman) → `active`; index 1-2 (Application / Approval) → `prospect`; index 0 (Inquiry) or Hex-Free = false → `inactive`. Aim for ~60% active, 25% prospect, 15% inactive.
- Opportunity `stage`: Map from Agreement Progress index — 0 = `prospecting`, 1 = `qualification`, 2 = `proposal`, 3 = `negotiation`, 4 = `closed_won`. Use `closed_lost` for any collapsed trade deals.
- Activity `status`: Extract from each description — "Completed" → `completed`, "Scheduled" → `pending`, "Cancelled" → `cancelled`.

**Sample Data - 10 CustomRecords (use bulkCreateCustomRecords):**
1. Elder Wand Replica (Display) - type: artifact, status: active, serialNumber: WAND-OO-001, customer: Ollivanders Wand Shop
2. Cauldron Stock — Grade 7 Pewter (24 units) - type: inventory, status: active, serialNumber: CAU-BORGIN-001, customer: Borgin and Burkes
3. Gringotts Vault Security Crystal - type: hardware, status: active, serialNumber: SEC-GRING-007, customer: Gringotts Wizarding Bank
4. Firewhisky Kegs — Aged 12yr (6 casks) - type: inventory, status: active, serialNumber: WHI-3B-001, customer: The Three Broomsticks
5. Magical Greenhouse Equipment Set - type: equipment, status: active, serialNumber: GRN-SPR-001, customer: Slug & Jiggers Apothecary
6. Restricted Section Scroll Chest - type: artifact, status: maintenance, serialNumber: SCR-FLOUR-001, customer: Flourish and Blotts
7. Quidditch Equipment Crate — Brooms x5 - type: inventory, status: active, serialNumber: QD-QUAL-001, customer: Quality Quidditch Supplies
8. Honeydukes Enchanted Display Case - type: hardware, status: active, serialNumber: DSP-HOND-001, customer: Honeydukes Sweet Shop
9. Magical Creature Transport Cage (Dragon-Grade) - type: equipment, status: inactive, serialNumber: CGE-MGZOO-002, customer: Magical Menagerie
10. Potions Distillation Apparatus — Master Set - type: equipment, status: active, serialNumber: POT-POTT-001, customer: Potage's Cauldron Shop

**Sample Data - 6 Orders:**
1. Ollivanders Wand Shop - "Rare Wand Wood Shipment", status: DELIVERED, totalAmount: 8400, currency: GBP
2. Gringotts Wizarding Bank - "Vault Expansion Hardware", status: CONFIRMED, totalAmount: 52000, currency: GBP
3. Honeydukes Sweet Shop - "Winter Festival Candy Bulk Order", status: SHIPPED, totalAmount: 3200, currency: GBP
4. Quality Quidditch Supplies - "New Broom Model Stock", status: DRAFT, totalAmount: 18500, currency: GBP
5. Flourish and Blotts - "New Term Textbook Bundle", status: CANCELLED, totalAmount: 7800, currency: GBP
6. The Three Broomsticks - "Butterbeer Barrel Restock", status: DELIVERED, totalAmount: 1950, currency: GBP

**Sample Data - 6 Invoices:**
1. Ollivanders Wand Shop - status: PAID, totalAmount: 8400, currency: GBP, paymentTerms: NET-30
2. Gringotts Wizarding Bank - status: SENT, totalAmount: 52000, currency: GBP, paymentTerms: NET-45
3. Honeydukes Sweet Shop - status: PAID, totalAmount: 3200, currency: GBP, paymentTerms: NET-30
4. Quality Quidditch Supplies - status: DRAFT, totalAmount: 18500, currency: GBP, paymentTerms: NET-60
5. Flourish and Blotts - status: OVERDUE, totalAmount: 7800, currency: GBP, paymentTerms: NET-30
6. The Three Broomsticks - status: PAID, totalAmount: 1950, currency: GBP, paymentTerms: NET-30

**Document Templates (3 custom styles — use createDocumentTemplate for each):**
1. "Diagon Alley Trade Report" — templateType: slide_deck, description: "Parchment-themed executive scroll for alliance presentations", styleJson: {"layout":"corporate","accentColor":"#7B3F00","backgroundColor":"#2C1A0E","h1Color":"#D4A853"}
2. "Merchant Profile Scroll" — templateType: one_pager, description: "Single-page merchant summary for alliance committee review", styleJson: {"layout":"light","accentColor":"#7B3F00","includeCustomFields":true}
3. "Alliance Member Export" — templateType: csv_export, description: "Full merchant registry export with trade and compliance data", styleJson: {"includeFields":["name","status","business_type","trade_specialty","hex_free_certified","annual_revenue"]}

**Notification Templates (3 owl-post templates — use createNotificationTemplate for each):**
1. name: "New Trade Agreement", notificationType: OPPORTUNITY_CREATED, isActive: true
   subject: "New Agreement Opened: {{opportunityName}} with {{customerName}}"
   body: "A new trade agreement has been initiated.\n\nMerchant: {{customerName}}\nAgreement: {{opportunityName}}\nValue: {{amount}}\nStage: {{stage}}\n\nView: {{link}}"
2. name: "Merchant Status Update", notificationType: CUSTOMER_UPDATED, isActive: true
   subject: "Merchant Record Updated: {{customerName}}"
   body: "A merchant profile has been updated.\n\nMerchant: {{customerName}}\nUpdated by: {{assignee}}\n\nReview: {{link}}"
3. name: "Order Dispatched", notificationType: ORDER_CREATED, isActive: true
   subject: "Order Dispatched: {{customerName}} — {{amount}} Galleons"
   body: "A new order has been created.\n\nMerchant: {{customerName}}\nAmount: {{amount}}\nExpected delivery: {{dueDate}}\n\nTrack: {{link}}"

Please execute this complete setup now, creating all fields, policies, merchants, contacts, agreements, activities, customRecords, orders, invoices, document templates, and notification templates. Update the tenant settings with the branding information provided. Let the Diagon Alley Alliance flourish!
```

---

## Prompt 2: Hogwarts School of Witchcraft & Wizardry - Student Services

```
I'm setting up a new CRM workspace for "Hogwarts School of Witchcraft & Wizardry - Student Services Division" - managing student enrollment, academic progress, house point allocation, and magical development across all four houses.

Please help me set up this workspace completely. Here's what I need:

**Company Branding:**
- Logo: https://via.placeholder.com/200x100?text=Hogwarts+School
- Primary Color: #551A8B
- Secondary Color: #CCCCCC
- Website: https://hogwarts-studentservices.magic
- Bio: "Hogwarts School of Witchcraft & Wizardry serves as the premier institution for magical education in the United Kingdom and Ireland. Our Student Services division tracks academic achievement, house standing, disciplinary records, and magical talent development across all seven years of magical education. Draco Dormiens Nunquam Titillandus."

**Custom Fields for Customers (Students):**
1. "Hogwarts House" (select field, required) - options: Gryffindor, Slytherin, Hufflepuff, Ravenclaw
2. "Current Year" (number field, 1-7) - year of study
3. "House Point Total" (number field) - accumulated points
4. "Patronus Form" (text field) - if corporeal patronus achieved
5. "Magical Ability Level" (select field) - options: Beginner, Intermediate, Advanced, Exceptional, Prodigy
6. "OWLS Passed (Count)" (number field, 0-15) - OWL exam passes
7. "NEWTS Passed (Count)" (number field, 0-7) - NEWT exam passes
8. "Disciplinary Record Score" (number field, 0-100) - lower is better
9. "Quidditch Team Member" (boolean field) - on house team
10. "Prefect/Head Status" (text field) - if leadership role
11. "Wand Core Compatibility" (select field) - options: Phoenix Feather, Unicorn Hair, Dragon Heartstring, Other
12. "Magical Creature Interaction Level" (percentage field) - comfort with creatures
13. "Academic Progress Status" (workflow field) - milestones: First Year, Progressing, Excellent Standing, Honors, Outstanding

**Custom Fields for Opportunities (Magical Achievements/Placements):**
1. "Achievement Type" (select field, required) - options: House Cup Victory, Prefect Appointment, Quidditch Achievement, Spell Mastery, Creature Companionship, Apprenticeship Placement
2. "House Cup Points Contributed" (number field) - points earned
3. "Magical Specialty Area" (select field) - options: Transfiguration, Potions, Defense Against Dark Arts, Charms, Herbology, Flying, Care of Creatures
4. "Career Potential" (select field) - options: Auror, Healer, Magizoologist, Potions Master, Researcher, Ministry Official, Entrepreneur
5. "Placement Progress" (workflow field) - milestones: Identified Talent, Developed Skills, Exceptional Performance, Mentor Matched, Career Placement

**Calculated Fields:**
1. For Customer: "Magical Potential Score" = (HousePointTotal / 50) + (MagicalAbilityLevel * 15) + (OWLsPassed * 4) + (if PatronusForm then 50 else 0)
2. For Opportunity: "Achievement Completion %" = if PlacementProgress exists then (PlacementProgress.currentIndex + 1) * 20 else 0

**Business Policies:**
1. DENY policy: Cannot expel students with Exceptional+ ability level (MagicalAbilityLevel >= "Exceptional" AND Expulsion = true)
2. WARN policy: Warn on House Cup risk (HousePointTotal < -50 AND EndOfYear = true)
3. DENY policy: Block Prefect appointment for students with Disciplinary Score > 60 (DisciplinaryScore > 60 AND PrefectAppointment = true)

**Sample Data - 15 Students:**

1. Harry Potter
   - Hogwarts House: Gryffindor
   - Current Year: 7
   - House Point Total: 1250
   - Patronus Form: Phoenix
   - Magical Ability Level: Advanced
   - OWLS Passed: 9
   - NEWTS Passed: 5
   - Disciplinary Record Score: 25
   - Quidditch Team Member: true
   - Prefect/Head Status: Head Boy
   - Wand Core Compatibility: Phoenix Feather
   - Magical Creature Interaction Level: 75%
   - Academic Progress Status: Outstanding (index 4)

2. Hermione Granger
   - Hogwarts House: Gryffindor
   - Current Year: 7
   - House Point Total: 1890
   - Patronus Form: Otter
   - Magical Ability Level: Prodigy
   - OWLS Passed: 15
   - NEWTS Passed: 7
   - Disciplinary Record Score: 10
   - Quidditch Team Member: false
   - Prefect/Head Status: Head Girl
   - Wand Core Compatibility: Dragon Heartstring
   - Magical Creature Interaction Level: 80%
   - Academic Progress Status: Outstanding (index 4)

3. Ron Weasley
   - Hogwarts House: Gryffindor
   - Current Year: 7
   - House Point Total: 1120
   - Patronus Form: Jack Russell Terrier
   - Magical Ability Level: Advanced
   - OWLS Passed: 8
   - NEWTS Passed: 5
   - Disciplinary Record Score: 40
   - Quidditch Team Member: true
   - Prefect/Head Status: Prefect
   - Wand Core Compatibility: Unicorn Hair
   - Magical Creature Interaction Level: 70%
   - Academic Progress Status: Excellent Standing (index 3)

4. Draco Malfoy
   - Hogwarts House: Slytherin
   - Current Year: 7
   - House Point Total: 980
   - Patronus Form: Unformed
   - Magical Ability Level: Advanced
   - OWLS Passed: 10
   - NEWTS Passed: 6
   - Disciplinary Record Score: 65
   - Quidditch Team Member: true
   - Prefect/Head Status: Prefect
   - Wand Core Compatibility: Unicorn Hair
   - Magical Creature Interaction Level: 60%
   - Academic Progress Status: Excellent Standing (index 3)

5. Luna Lovegood
   - Hogwarts House: Ravenclaw
   - Current Year: 6
   - House Point Total: 1450
   - Patronus Form: Hare
   - Magical Ability Level: Exceptional
   - OWLS Passed: 8
   - NEWTS Passed: 0
   - Disciplinary Record Score: 20
   - Quidditch Team Member: false
   - Prefect/Head Status: None
   - Wand Core Compatibility: Phoenix Feather
   - Magical Creature Interaction Level: 95%
   - Academic Progress Status: Excellent Standing (index 3)

6. Neville Longbottom
   - Hogwarts House: Gryffindor
   - Current Year: 7
   - House Point Total: 1310
   - Patronus Form: Frank the Lion
   - Magical Ability Level: Advanced
   - OWLS Passed: 6
   - NEWTS Passed: 4
   - Disciplinary Record Score: 35
   - Quidditch Team Member: false
   - Prefect/Head Status: None
   - Wand Core Compatibility: Unicorn Hair
   - Magical Creature Interaction Level: 85%
   - Academic Progress Status: Excellent Standing (index 3)

7. Ginny Weasley
   - Hogwarts House: Gryffindor
   - Current Year: 6
   - House Point Total: 1520
   - Patronus Form: Horse
   - Magical Ability Level: Exceptional
   - OWLS Passed: 10
   - NEWTS Passed: 0
   - Disciplinary Record Score: 30
   - Quidditch Team Member: true
   - Prefect/Head Status: Prefect
   - Wand Core Compatibility: Phoenix Feather
   - Magical Creature Interaction Level: 75%
   - Academic Progress Status: Outstanding (index 4)

8. Pansy Parkinson
   - Hogwarts House: Slytherin
   - Current Year: 7
   - House Point Total: 890
   - Patronus Form: Cat
   - Magical Ability Level: Intermediate
   - OWLS Passed: 7
   - NEWTS Passed: 4
   - Disciplinary Record Score: 50
   - Quidditch Team Member: false
   - Prefect/Head Status: None
   - Wand Core Compatibility: Dragon Heartstring
   - Magical Creature Interaction Level: 55%
   - Academic Progress Status: Progressing (index 1)

9. Cho Chang
   - Hogwarts House: Ravenclaw
   - Current Year: 6
   - House Point Total: 1280
   - Patronus Form: Swan
   - Magical Ability Level: Advanced
   - OWLS Passed: 9
   - NEWTS Passed: 0
   - Disciplinary Record Score: 25
   - Quidditch Team Member: true
   - Prefect/Head Status: Prefect
   - Wand Core Compatibility: Unicorn Hair
   - Magical Creature Interaction Level: 65%
   - Academic Progress Status: Excellent Standing (index 3)

10. Michael Corner
    - Hogwarts House: Ravenclaw
    - Current Year: 7
    - House Point Total: 1100
    - Patronus Form: Raven
    - Magical Ability Level: Intermediate
    - OWLS Passed: 7
    - NEWTS Passed: 4
    - Disciplinary Record Score: 45
    - Quidditch Team Member: false
    - Prefect/Head Status: Prefect
    - Wand Core Compatibility: Dragon Heartstring
    - Magical Creature Interaction Level: 60%
    - Academic Progress Status: Excellent Standing (index 3)

11. Susan Bones
    - Hogwarts House: Hufflepuff
    - Current Year: 7
    - House Point Total: 1210
    - Patronus Form: Badger
    - Magical Ability Level: Advanced
    - OWLS Passed: 8
    - NEWTS Passed: 5
    - Disciplinary Record Score: 20
    - Quidditch Team Member: true
    - Prefect/Head Status: Prefect
    - Wand Core Compatibility: Phoenix Feather
    - Magical Creature Interaction Level: 70%
    - Academic Progress Status: Excellent Standing (index 3)

12. Justin Finch-Fletchley
    - Hogwarts House: Hufflepuff
    - Current Year: 7
    - House Point Total: 1050
    - Patronus Form: Fox
    - Magical Ability Level: Intermediate
    - OWLS Passed: 6
    - NEWTS Passed: 3
    - Disciplinary Record Score: 40
    - Quidditch Team Member: true
    - Prefect/Head Status: None
    - Wand Core Compatibility: Unicorn Hair
    - Magical Creature Interaction Level: 65%
    - Academic Progress Status: Progressing (index 1)

13. Padma Patil
    - Hogwarts House: Ravenclaw
    - Current Year: 7
    - House Point Total: 1410
    - Patronus Form: Antelope
    - Magical Ability Level: Exceptional
    - OWLS Passed: 11
    - NEWTS Passed: 6
    - Disciplinary Record Score: 15
    - Quidditch Team Member: false
    - Prefect/Head Status: None
    - Wand Core Compatibility: Dragon Heartstring
    - Magical Creature Interaction Level: 75%
    - Academic Progress Status: Outstanding (index 4)

14. Seamus Finnigan
    - Hogwarts House: Gryffindor
    - Current Year: 7
    - House Point Total: 890
    - Patronus Form: Bloodhound
    - Magical Ability Level: Intermediate
    - OWLS Passed: 5
    - NEWTS Passed: 3
    - Disciplinary Record Score: 55
    - Quidditch Team Member: false
    - Prefect/Head Status: None
    - Wand Core Compatibility: Phoenix Feather
    - Magical Creature Interaction Level: 50%
    - Academic Progress Status: Progressing (index 1)

15. Dean Thomas
    - Hogwarts House: Gryffindor
    - Current Year: 7
    - House Point Total: 1040
    - Patronus Form: Doe
    - Magical Ability Level: Intermediate
    - OWLS Passed: 6
    - NEWTS Passed: 3
    - Disciplinary Record Score: 48
    - Quidditch Team Member: false
    - Prefect/Head Status: None
    - Wand Core Compatibility: Unicorn Hair
    - Magical Creature Interaction Level: 62%
    - Academic Progress Status: Progressing (index 1)

**Sample Data - 20 Contacts:**
- Albus Dumbledore (Headmaster) at Hogwarts Castle
- Minerva McGonagall (Transfiguration Professor) at Hogwarts Castle
- Rubeus Hagrid (Care of Creatures Professor) at Hogwarts Castle
- Sybill Trelawney (Divination Professor) at Hogwarts Castle
- Filius Flitwick (Charms Professor) at Hogwarts Castle
- Horace Slughorn (Potions Professor) at Hogwarts Castle
- Alastor Moody/Barty Crouch Jr (Defense Professor) at Hogwarts Castle
- Pomona Sprout (Herbology Professor) at Hogwarts Castle
- Madam Pomfrey (School Matron) at Hogwarts Hospital Wing
- Severus Snape (Former Professor) at Hogwarts Castle
- Gilderoy Lockhart (Former Professor) at St. Mungo's
- Viktor Krum (Durmstrang Champion) at Durmstrang Institute
- Fleur Delacour (Beauxbatons Champion) at Beauxbatons Academy
- Cedric Diggory (Former Champion) at Hogwarts Castle
- Dolores Umbridge (Former High Inquisitor) at Ministry
- Rita Skeeter (Press Liaison) at Daily Prophet
- Lucius Malfoy (Alumni Parent) at Manor
- Molly Weasley (Parent Support) at Headquarters
- James Potter Sr (Alumni/Deceased) at Records
- Lily Potter Sr (Alumni/Deceased) at Records

**Sample Data - 15 Achievements:**

1. Harry Potter - Auror Placement
   - Student: Harry Potter
   - Achievement Type: Spell Mastery
   - House Cup Points Contributed: 125
   - Magical Specialty Area: Defense Against Dark Arts
   - Career Potential: Auror
   - Placement Progress: Mentor Matched (index 3)

2. Hermione Granger - Minister Internship
   - Student: Hermione Granger
   - Achievement Type: Spell Mastery
   - House Cup Points Contributed: 180
   - Magical Specialty Area: Transfiguration
   - Career Potential: Ministry Official
   - Placement Progress: Career Placement (index 4)

3. Ron Weasley - Auror Office
   - Student: Ron Weasley
   - Achievement Type: House Cup Victory
   - House Cup Points Contributed: 100
   - Magical Specialty Area: Defense Against Dark Arts
   - Career Potential: Auror
   - Placement Progress: Mentor Matched (index 3)

4. Draco Malfoy - Potions Apprenticeship
   - Student: Draco Malfoy
   - Achievement Type: Apprenticeship Placement
   - House Cup Points Contributed: 80
   - Magical Specialty Area: Potions
   - Career Potential: Potions Master
   - Placement Progress: Mentor Matched (index 3)

5. Luna Lovegood - Magizoologist Program
   - Student: Luna Lovegood
   - Achievement Type: Spell Mastery
   - House Cup Points Contributed: 90
   - Magical Specialty Area: Care of Creatures
   - Career Potential: Magizoologist
   - Placement Progress: Career Placement (index 4)

6. Neville Longbottom - Herbology Professorship
   - Student: Neville Longbottom
   - Achievement Type: Spell Mastery
   - House Cup Points Contributed: 110
   - Magical Specialty Area: Herbology
   - Career Potential: Potions Master
   - Placement Progress: Career Placement (index 4)

7. Gryffindor House Cup Victory
   - Student: Harry Potter
   - Achievement Type: House Cup Victory
   - House Cup Points Contributed: 250
   - Magical Specialty Area: General Excellence
   - Career Potential: Entrepreneur
   - Placement Progress: Career Placement (index 4)

8. Ginny Weasley - Professional Quidditch
   - Student: Ginny Weasley
   - Achievement Type: Quidditch Achievement
   - House Cup Points Contributed: 150
   - Magical Specialty Area: Flying
   - Career Potential: Entrepreneur
   - Placement Progress: Career Placement (index 4)

9. Cho Chang - Ravenclaw Leadership
   - Student: Cho Chang
   - Achievement Type: Prefect Appointment
   - House Cup Points Contributed: 120
   - Magical Specialty Area: Transfiguration
   - Career Potential: Ministry Official
   - Placement Progress: Career Placement (index 4)

10. Michael Corner - Ravenclaw Prefect
    - Student: Michael Corner
    - Achievement Type: Prefect Appointment
    - House Cup Points Contributed: 85
    - Magical Specialty Area: Charms
    - Career Potential: Ministry Official
    - Placement Progress: Career Placement (index 4)

11. Padma Patil - Academic Excellence
    - Student: Padma Patil
    - Achievement Type: Spell Mastery
    - House Cup Points Contributed: 140
    - Magical Specialty Area: Charms
    - Career Potential: Researcher
    - Placement Progress: Career Placement (index 4)

12. Susan Bones - Auror Academy
    - Student: Susan Bones
    - Achievement Type: Apprenticeship Placement
    - House Cup Points Contributed: 100
    - Magical Specialty Area: Defense Against Dark Arts
    - Career Potential: Auror
    - Placement Progress: Mentor Matched (index 3)

13. Seamus Finnigan - Explosive Specialist
    - Student: Seamus Finnigan
    - Achievement Type: Creature Companionship
    - House Cup Points Contributed: 60
    - Magical Specialty Area: Transfiguration
    - Career Potential: Entrepreneur
    - Placement Progress: Developed Skills (index 1)

14. Dean Thomas - Magical Art Career
    - Student: Dean Thomas
    - Achievement Type: Apprenticeship Placement
    - House Cup Points Contributed: 70
    - Magical Specialty Area: Flying
    - Career Potential: Entrepreneur
    - Placement Progress: Developed Skills (index 1)

15. Justin Finch-Fletchley - Muggle Relations
    - Student: Justin Finch-Fletchley
    - Achievement Type: Apprenticeship Placement
    - House Cup Points Contributed: 50
    - Magical Specialty Area: Charms
    - Career Potential: Ministry Official
    - Placement Progress: Identified Talent (index 0)

**Sample Data - 18 Activities:**

1. Defense Against Dark Arts Lesson - Meeting, 60 minutes, 2024-01-15, Completed, Advanced curse defense
2. Patronus Production Training - Activity, 90 minutes, 2024-01-16, Completed, Corporeal patronus focus
3. House Cup Tournament - Activity, 240 minutes, 2024-01-17, Completed, First-round inter-house match
4. Quidditch Match Event - Activity, 120 minutes, 2024-01-18, Completed, Gryffindor vs Slytherin
5. Prefect Duty Rotation - Activity, 180 minutes, 2024-01-19, Completed, Evening corridor patrol
6. Ministry Career Fair - Meeting, 120 minutes, 2024-01-22, Completed, Auror recruitment presentation
7. Spell Dueling Competition - Activity, 90 minutes, 2024-01-23, Completed, Dueling club tournament
8. Creature Care Field Trip - Activity, 180 minutes, 2024-01-24, Completed, Hippogriff care expedition
9. Transfiguration Practical Exam - Meeting, 120 minutes, 2024-01-25, Completed, Advanced transformation test
10. Herbology Greenhouse Work - Activity, 90 minutes, 2024-01-26, Completed, Dangerous plant handling
11. Potions Master Class - Meeting, 90 minutes, 2024-01-29, Completed, Advanced brewing techniques
12. Quidditch Practice Session - Activity, 120 minutes, 2024-01-30, Scheduled, House team drills
13. House Point Review - Meeting, 45 minutes, 2024-02-01, Completed, Term tally assessment
14. Charms Practical Workshop - Activity, 90 minutes, 2024-02-02, Completed, Protective spell training
15. Prefect Leadership Meeting - Meeting, 60 minutes, 2024-02-05, Scheduled, Discipline protocol review
16. Career Mentorship Session - Activity, 60 minutes, 2024-02-06, Completed, Individual pathway planning
17. End-of-Year House Cup Ceremony - Meeting, 180 minutes, 2024-02-15, Completed, Award presentations
18. House Cup Victory Celebration - Activity, 240 minutes, 2024-02-20, Completed, Gryffindor feast and commemoration

Make these appropriately magical with dates across "Hogwarts School Year 1995-1998".

**Standard Field Mapping (apply when creating all records):**
- Customer `status`: Derive from context — currently enrolled students (Year 1-7) → `active`; prospective/pending admission students → `prospect`; graduated/expelled/transferred → `inactive`. Aim for ~70% active, 20% prospect, 10% inactive.
- Opportunity `stage`: Map from the workflow progress index — 0 = `prospecting`, 1 = `qualification`, 2 = `proposal`, 3 = `negotiation`, 4 = `closed_won`. Use `closed_lost` for failed/withdrawn applications.
- Activity `status`: Extract from each description — "Completed" → `completed`, "Scheduled"/"Upcoming" → `pending`, "Cancelled" → `cancelled`.

**Sample Data - 10 CustomRecords (use bulkCreateCustomRecords):**
1. Nimbus 2000 Broomstick - type: equipment, status: active, serialNumber: NIM-HPOT-001, customer: Harry Potter
2. Hogwarts Standard Book of Spells (Set, Years 1-7) - type: inventory, status: active, serialNumber: BOOK-HERM-001, customer: Hermione Granger
3. Deluminator (Dumbledore's Gift) - type: artifact, status: active, serialNumber: DLMN-RON-001, customer: Ron Weasley
4. Pensieve (Headmaster's Office) - type: artifact, status: active, serialNumber: PENS-HMST-001
5. Sorting Hat (School Property) - type: artifact, status: active, serialNumber: HAT-HOG-001
6. Divination Crystal Ball Set - type: equipment, status: active, serialNumber: CRY-DIV-001
7. Potions Classroom Cauldron Set (Class Supply) - type: inventory, status: maintenance, serialNumber: CAU-POT-CLS
8. Restricted Section Bookcase — Locked - type: furniture, status: active, serialNumber: CASE-LIB-R01
9. Astronomy Tower Telescope Array - type: equipment, status: active, serialNumber: TEL-ASTR-001
10. Mandrake Earmuffs (Class Set, 30 pairs) - type: inventory, status: inactive, serialNumber: EAR-HRB-030

**Sample Data - 6 Orders:**
1. Harry Potter - "Advanced DADA Curriculum Package", status: DELIVERED, totalAmount: 280, currency: GBP
2. Hermione Granger - "Extended Library Access & Research Materials", status: CONFIRMED, totalAmount: 150, currency: GBP
3. Draco Malfoy - "Premium Quidditch Training Subscription", status: SHIPPED, totalAmount: 420, currency: GBP
4. Luna Lovegood - "Specialised Creature Studies Kit", status: DRAFT, totalAmount: 95, currency: GBP
5. Neville Longbottom - "Advanced Herbology Equipment Bundle", status: CANCELLED, totalAmount: 180, currency: GBP
6. Ginny Weasley - "Junior Chaser Development Program", status: DELIVERED, totalAmount: 320, currency: GBP

**Sample Data - 6 Invoices:**
1. Harry Potter - status: PAID, totalAmount: 280, currency: GBP, paymentTerms: NET-30
2. Hermione Granger - status: SENT, totalAmount: 150, currency: GBP, paymentTerms: NET-30
3. Draco Malfoy - status: PAID, totalAmount: 420, currency: GBP, paymentTerms: NET-14
4. Luna Lovegood - status: DRAFT, totalAmount: 95, currency: GBP, paymentTerms: NET-30
5. Neville Longbottom - status: OVERDUE, totalAmount: 180, currency: GBP, paymentTerms: NET-30
6. Ginny Weasley - status: PAID, totalAmount: 320, currency: GBP, paymentTerms: NET-30

**Document Templates (3 custom styles — use createDocumentTemplate for each):**
1. "Student Progress Report" — templateType: slide_deck, description: "Formal academic progress deck for parent-teacher consultations", styleJson: {"layout":"corporate","accentColor":"#740001","backgroundColor":"#1A0A00","h1Color":"#D4AF37"}
2. "Academic Profile Summary" — templateType: one_pager, description: "Single-page student profile for staff review", styleJson: {"layout":"light","accentColor":"#740001","includeCustomFields":true}
3. "Student Registry Export" — templateType: csv_export, description: "Full student data export with house, year, and achievement data", styleJson: {"includeFields":["name","status","house","year","magical_aptitude_score","patronus_form"]}

**Notification Templates (3 owl-post templates — use createNotificationTemplate for each):**
1. name: "Enrollment Confirmation", notificationType: CUSTOMER_CREATED, isActive: true
   subject: "Welcome to Hogwarts: {{customerName}}"
   body: "A new student has been enrolled.\n\nStudent: {{customerName}}\nHouse: assigned at Sorting\nYear: {{status}}\n\nView profile: {{link}}"
2. name: "Achievement Milestone", notificationType: OPPORTUNITY_UPDATED, isActive: true
   subject: "Achievement Update: {{customerName}} — {{opportunityName}}"
   body: "A student achievement has been updated.\n\nStudent: {{customerName}}\nAchievement: {{opportunityName}}\nStage: {{stage}}\n\n{{link}}"
3. name: "Order Dispatched to Student", notificationType: ORDER_CREATED, isActive: true
   subject: "Materials Ordered: {{customerName}}"
   body: "Curriculum materials have been ordered.\n\nStudent: {{customerName}}\nPackage: {{opportunityName}}\nTotal: {{amount}}\nExpected: {{dueDate}}\n\n{{link}}"

Please execute this complete setup now, creating all fields, policies, students, contacts, achievements, activities, customRecords, orders, invoices, document templates, and notification templates. Update the tenant settings with the branding information provided. Welcome to Hogwarts!
```

---

## Prompt 3: Ministry of Magic - Department of Magical Law Enforcement

```
I'm setting up a new CRM workspace for "Ministry of Magic - Department of Magical Law Enforcement" - tracking Aurors, cases, dark wizards, illegal magical activity, and prosecution across the United Kingdom.

Please help me set up this workspace completely. Here's what I need:

**Company Branding:**
- Logo: https://via.placeholder.com/200x100?text=Ministry+of+Magic
- Primary Color: #000080
- Secondary Color: #FF0000
- Website: https://ministry-mleod.magic
- Bio: "The Department of Magical Law Enforcement serves as the premier law enforcement agency of the Ministry of Magic. We coordinate Auror operations, investigate dark magic crimes, apprehend fugitives, and maintain order in the wizarding world. Justice through magical investigation. Security through vigilance."

**Custom Fields for Customers (Aurors/Agents):**
1. "Auror Rank" (select field, required) - options: Junior Auror, Senior Auror, Head Auror, Trainee
2. "Years of Service" (number field) - career length
3. "Cases Solved" (number field) - closed investigations
4. "Dark Wizard Captures" (number field) - arrests of dark practitioners
5. "Curse Mastery Level" (select field) - options: Resistant, Immune, Master Caster, Counter-Curse Expert
6. "Duel Rating" (number field, 0-10) - combat proficiency
7. "Last Physical Examination" (date field) - health clearance
8. "Ministry Commendation Count" (number field) - medals & honors
9. "Hex Resistance Training (%)" (percentage field) - defensive training completion
10. "Dark Arts Knowledge Level" (number field, 1-10) - expertise in dark magic
11. "Field Assignment Status" (select field) - options: Active Field, Desk Duty, Recovery, Training, International
12. "Case Load Current" (number field) - active investigations
13. "Auror Career Progression" (workflow field) - milestones: Trainee, Junior, Promoted, Senior, Distinguished Service

**Custom Fields for Opportunities (Cases/Investigations):**
1. "Case Type" (select field, required) - options: Dark Wizard Apprehension, Illegal Magical Activity, Curse Investigation, Creature Escape, Artifact Theft, Rogue Witch Hunt
2. "Threat Level" (select field) - options: Low, Medium, High, Critical, Dark Lord Level
3. "Primary Suspect" (text field) - name of dark wizard or criminal
4. "Location of Interest" (text field) - where the crime occurred
5. "Assigned Auror" (select field) - lead investigator
6. "Investigation Complexity" (number field, 1-10) - case difficulty
7. "Investigation Progress" (workflow field) - milestones: Opened, Investigation, Tracked, Apprehended, Prosecution, Convicted

**Calculated Fields:**
1. For Customer: "Auror Effectiveness Score" = (CasesSolved * 2) + (DarkWizardCaptures * 5) + (DuelRating * 8) + (MinistryCommendationCount * 10)
2. For Opportunity: "Case Resolution %" = if InvestigationProgress exists then (InvestigationProgress.currentIndex + 1) * 16.6 else 0

**Business Policies:**
1. DENY policy: Cannot close case without Apprehension stage or explicit Ministry approval (InvestigationProgress < 3 AND CaseStatus = "Closed")
2. WARN policy: Warn on assigning cases to Aurors with Case Load > 5 (CurrentCaseLoad > 5 AND NewAssignment)
3. DENY policy: Cannot investigate Dark Lord level cases without Senior Auror rank (ThreatLevel = "Dark Lord Level" AND AurorRank = "Junior Auror")

**Sample Data - 15 Aurors:**

1. Alastor Moody
   - Auror Rank: Head Auror
   - Years of Service: 40
   - Cases Solved: 287
   - Dark Wizard Captures: 52
   - Curse Mastery Level: Master Caster
   - Duel Rating: 9.8
   - Last Physical Examination: 2024-01-10
   - Ministry Commendation Count: 15
   - Hex Resistance Training: 98%
   - Dark Arts Knowledge Level: 10
   - Field Assignment Status: Active Field
   - Case Load Current: 2
   - Auror Career Progression: Distinguished Service (index 4)

2. Neville Longbottom
   - Auror Rank: Senior Auror
   - Years of Service: 7
   - Cases Solved: 156
   - Dark Wizard Captures: 34
   - Curse Mastery Level: Resistant
   - Duel Rating: 8.5
   - Last Physical Examination: 2024-01-12
   - Ministry Commendation Count: 8
   - Hex Resistance Training: 85%
   - Dark Arts Knowledge Level: 7
   - Field Assignment Status: Active Field
   - Case Load Current: 3
   - Auror Career Progression: Senior (index 3)

3. Harry Potter
   - Auror Rank: Senior Auror
   - Years of Service: 5
   - Cases Solved: 189
   - Dark Wizard Captures: 47
   - Curse Mastery Level: Counter-Curse Expert
   - Duel Rating: 9.4
   - Last Physical Examination: 2024-01-15
   - Ministry Commendation Count: 12
   - Hex Resistance Training: 92%
   - Dark Arts Knowledge Level: 9
   - Field Assignment Status: Active Field
   - Case Load Current: 4
   - Auror Career Progression: Distinguished Service (index 4)

4. Gawain Robards
   - Auror Rank: Senior Auror
   - Years of Service: 35
   - Cases Solved: 412
   - Dark Wizard Captures: 68
   - Curse Mastery Level: Master Caster
   - Duel Rating: 9.1
   - Last Physical Examination: 2024-01-08
   - Ministry Commendation Count: 22
   - Hex Resistance Training: 96%
   - Dark Arts Knowledge Level: 9
   - Field Assignment Status: Desk Duty
   - Case Load Current: 1
   - Auror Career Progression: Distinguished Service (index 4)

5. Williamson
   - Auror Rank: Junior Auror
   - Years of Service: 2
   - Cases Solved: 42
   - Dark Wizard Captures: 8
   - Curse Mastery Level: Resistant
   - Duel Rating: 6.2
   - Last Physical Examination: 2024-01-14
   - Ministry Commendation Count: 1
   - Hex Resistance Training: 65%
   - Dark Arts Knowledge Level: 4
   - Field Assignment Status: Active Field
   - Case Load Current: 5
   - Auror Career Progression: Junior (index 1)

6. Dawlish
   - Auror Rank: Junior Auror
   - Years of Service: 3
   - Cases Solved: 68
   - Dark Wizard Captures: 11
   - Curse Mastery Level: Resistant
   - Duel Rating: 6.8
   - Last Physical Examination: 2024-01-11
   - Ministry Commendation Count: 2
   - Hex Resistance Training: 72%
   - Dark Arts Knowledge Level: 5
   - Field Assignment Status: Active Field
   - Case Load Current: 4
   - Auror Career Progression: Junior (index 1)

7. Kinsley Shacklebolt
   - Auror Rank: Senior Auror
   - Years of Service: 30
   - Cases Solved: 378
   - Dark Wizard Captures: 64
   - Curse Mastery Level: Master Caster
   - Duel Rating: 9.0
   - Last Physical Examination: 2024-01-13
   - Ministry Commendation Count: 20
   - Hex Resistance Training: 94%
   - Dark Arts Knowledge Level: 8
   - Field Assignment Status: Active Field
   - Case Load Current: 3
   - Auror Career Progression: Senior (index 3)

8. Ron Weasley
   - Auror Rank: Senior Auror
   - Years of Service: 6
   - Cases Solved: 165
   - Dark Wizard Captures: 38
   - Curse Mastery Level: Immune
   - Duel Rating: 8.3
   - Last Physical Examination: 2024-01-16
   - Ministry Commendation Count: 7
   - Hex Resistance Training: 88%
   - Dark Arts Knowledge Level: 7
   - Field Assignment Status: Active Field
   - Case Load Current: 6
   - Auror Career Progression: Senior (index 3)

9. Hermione Granger-Weasley
   - Auror Rank: Senior Auror
   - Years of Service: 4
   - Cases Solved: 210
   - Dark Wizard Captures: 31
   - Curse Mastery Level: Master Caster
   - Duel Rating: 8.9
   - Last Physical Examination: 2024-01-09
   - Ministry Commendation Count: 6
   - Hex Resistance Training: 91%
   - Dark Arts Knowledge Level: 8
   - Field Assignment Status: Desk Duty
   - Case Load Current: 2
   - Auror Career Progression: Senior (index 3)

10. Tonks (Retired)
    - Auror Rank: Senior Auror
    - Years of Service: 12
    - Cases Solved: 198
    - Dark Wizard Captures: 35
    - Curse Mastery Level: Counter-Curse Expert
    - Duel Rating: 8.1
    - Last Physical Examination: 2023-12-20
    - Ministry Commendation Count: 9
    - Hex Resistance Training: 82%
    - Dark Arts Knowledge Level: 7
    - Field Assignment Status: Recovery
    - Case Load Current: 0
    - Auror Career Progression: Senior (index 3)

11. Kingsley Shacklebolt
    - Auror Rank: Senior Auror
    - Years of Service: 30
    - Cases Solved: 398
    - Dark Wizard Captures: 62
    - Curse Mastery Level: Master Caster
    - Duel Rating: 9.2
    - Last Physical Examination: 2024-01-07
    - Ministry Commendation Count: 21
    - Hex Resistance Training: 95%
    - Dark Arts Knowledge Level: 9
    - Field Assignment Status: Active Field
    - Case Load Current: 2
    - Auror Career Progression: Distinguished Service (index 4)

12. Proudfoot
    - Auror Rank: Trainee
    - Years of Service: 1
    - Cases Solved: 18
    - Dark Wizard Captures: 2
    - Curse Mastery Level: Resistant
    - Duel Rating: 5.5
    - Last Physical Examination: 2024-01-06
    - Ministry Commendation Count: 0
    - Hex Resistance Training: 45%
    - Dark Arts Knowledge Level: 3
    - Field Assignment Status: Training
    - Case Load Current: 1
    - Auror Career Progression: Trainee (index 0)

13. Savage
    - Auror Rank: Senior Auror
    - Years of Service: 8
    - Cases Solved: 122
    - Dark Wizard Captures: 19
    - Curse Mastery Level: Counter-Curse Expert
    - Duel Rating: 7.2
    - Last Physical Examination: 2024-01-05
    - Ministry Commendation Count: 4
    - Hex Resistance Training: 78%
    - Dark Arts Knowledge Level: 6
    - Field Assignment Status: Active Field
    - Case Load Current: 3
    - Auror Career Progression: Promoted (index 2)

14. Scrimgeour
    - Auror Rank: Senior Auror
    - Years of Service: 28
    - Cases Solved: 356
    - Dark Wizard Captures: 61
    - Curse Mastery Level: Master Caster
    - Duel Rating: 9.2
    - Last Physical Examination: 2024-01-04
    - Ministry Commendation Count: 19
    - Hex Resistance Training: 93%
    - Dark Arts Knowledge Level: 8
    - Field Assignment Status: Desk Duty
    - Case Load Current: 1
    - Auror Career Progression: Senior (index 3)

15. Lestrange Hunter
    - Auror Rank: Senior Auror
    - Years of Service: 6
    - Cases Solved: 89
    - Dark Wizard Captures: 23
    - Curse Mastery Level: Master Caster
    - Duel Rating: 8.6
    - Last Physical Examination: 2024-01-17
    - Ministry Commendation Count: 11
    - Hex Resistance Training: 89%
    - Dark Arts Knowledge Level: 8
    - Field Assignment Status: Active Field
    - Case Load Current: 4
    - Auror Career Progression: Senior (index 3)

**Sample Data - 20 Contacts:**
- Barty Crouch Sr (Former Head - Deceased) at Ministry Records
- Barty Crouch Jr (Criminal - Imprisoned) at Azkaban
- Lucius Malfoy (Dark Supporter - Tracked) at Malfoy Manor
- Bellatrix Lestrange (Dark Follower - Deceased) at Ministry Records
- Severus Snape (Double Agent - Complicated) at Ministry Files
- Pettigrew (Escapee - Wanted) at Large
- Igor Karkaroff (Dark Wizard - Tracked) at International Division
- Fenrir Greyback (Beast Master Criminal) at Creature Division
- Dolores Umbridge (Corrupted Official - Tracked) at Ministry Internal
- Voldemort (Dark Lord - Primary Target - Deceased) at Records
- Lord Voldemort (Tom Riddle - Former Student) at Historical Records
- Quirinus Quirrell (Dark Associate - Deceased) at Ministry Files
- Igor Karkaroff (International Fugitive) at Interpol Liaison
- Ministry Official (Political Contact) at Office
- Auror Trainer (Academy Director) at Training Division
- Evidence Officer (Magical Artifacts) at Evidence Storage
- Medical Examiner (Forensic Magic) at Ministry Hospital
- Communication Officer (Press Liaison) at Public Relations
- International Liaison (Foreign Cases) at International Division
- Dark Wizard Database Manager (Intelligence) at Records Division

**Sample Data - 15 Cases:**

1. Dark Wizard Lucius Malfoy Tracking
   - Auror: Harry Potter
   - Case Type: Dark Wizard Apprehension
   - Threat Level: Critical
   - Primary Suspect: Lucius Malfoy
   - Location of Interest: Malfoy Manor
   - Investigation Complexity: 8
   - Investigation Progress: Apprehended (index 3)

2. Bellatrix Lestrange Legacy Investigation
   - Auror: Alastor Moody
   - Case Type: Dark Wizard Apprehension
   - Threat Level: Dark Lord Level
   - Primary Suspect: Bellatrix Lestrange
   - Location of Interest: Azkaban Prison
   - Investigation Complexity: 9
   - Investigation Progress: Convicted (index 5)

3. Illegal Animagus Ring Smuggling
   - Auror: Neville Longbottom
   - Case Type: Illegal Magical Activity
   - Threat Level: High
   - Primary Suspect: Smuggling Ring Operators
   - Location of Interest: Hogsmeade Station
   - Investigation Complexity: 7
   - Investigation Progress: Tracked (index 2)

4. Azkaban Escape Ring Bust
   - Auror: Gawain Robards
   - Case Type: Illegal Magical Activity
   - Threat Level: Critical
   - Primary Suspect: Escape Ring Facilitators
   - Location of Interest: Azkaban Prison
   - Investigation Complexity: 8
   - Investigation Progress: Apprehended (index 3)

5. Department of Mysteries Break-in
   - Auror: Harry Potter
   - Case Type: Artifact Theft
   - Threat Level: High
   - Primary Suspect: Dark Force Activity
   - Location of Interest: Ministry of Magic
   - Investigation Complexity: 8
   - Investigation Progress: Convicted (index 5)

6. Knockturn Alley Dark Potion Ring
   - Auror: Williamson
   - Case Type: Illegal Magical Activity
   - Threat Level: Medium
   - Primary Suspect: Dark Potion Suppliers
   - Location of Interest: Knockturn Alley
   - Investigation Complexity: 6
   - Investigation Progress: Tracked (index 2)

7. Werewolf Attack Pattern Series
   - Auror: Dawlish
   - Case Type: Creature Escape
   - Threat Level: High
   - Primary Suspect: Fenrir Greyback Network
   - Location of Interest: Multiple Rural Areas
   - Investigation Complexity: 7
   - Investigation Progress: Tracked (index 2)

8. Curse Hex Distribution Network
   - Auror: Proudfoot
   - Case Type: Illegal Magical Activity
   - Threat Level: Medium
   - Primary Suspect: Curse Distribution Ring
   - Location of Interest: London Underworld
   - Investigation Complexity: 5
   - Investigation Progress: Investigation (index 1)

9. Unregistered Magical Creature Farm
   - Auror: Savage
   - Case Type: Creature Escape
   - Threat Level: Low
   - Primary Suspect: Illegal Breeding Operations
   - Location of Interest: Rural Yorkshire
   - Investigation Complexity: 4
   - Investigation Progress: Apprehended (index 3)

10. Death Eater Underground Network
    - Auror: Kinsley Shacklebolt
    - Case Type: Dark Wizard Apprehension
    - Threat Level: Dark Lord Level
    - Primary Suspect: Remnant Death Eater Cells
    - Location of Interest: Multiple International
    - Investigation Complexity: 10
    - Investigation Progress: Convicted (index 5)

11. Time Turner Theft Investigation
    - Auror: Ron Weasley
    - Case Type: Artifact Theft
    - Threat Level: High
    - Primary Suspect: Dark Arts Collectors
    - Location of Interest: Ministry Corridors
    - Investigation Complexity: 8
    - Investigation Progress: Tracked (index 2)

12. Forbidden Spell Supplier
    - Auror: Hermione Granger-Weasley
    - Case Type: Illegal Magical Activity
    - Threat Level: High
    - Primary Suspect: Spell Black Market Operators
    - Location of Interest: International Network
    - Investigation Complexity: 7
    - Investigation Progress: Prosecution (index 4)

13. Dementor Escape Security Breach
    - Auror: Alastor Moody
    - Case Type: Creature Escape
    - Threat Level: Critical
    - Primary Suspect: Prison Security Compromise
    - Location of Interest: Azkaban Prison
    - Investigation Complexity: 9
    - Investigation Progress: Apprehended (index 3)

14. Dragon Egg Smuggling Ring
    - Auror: Scrimgeour
    - Case Type: Artifact Theft
    - Threat Level: Medium
    - Primary Suspect: International Smugglers
    - Location of Interest: Global Trade Routes
    - Investigation Complexity: 6
    - Investigation Progress: Tracked (index 2)

15. Prophecy Chamber Vandalism
    - Auror: Lestrange Hunter
    - Case Type: Rogue Witch Hunt
    - Threat Level: Low
    - Primary Suspect: Chamber Vandal
    - Location of Interest: Ministry Department
    - Investigation Complexity: 3
    - Investigation Progress: Convicted (index 5)

**Sample Data - 18 Activities:**

1. Dark Wizard Surveillance Operations - Activity, 480 minutes, 2024-01-15, Completed, Malfoy Manor surveillance
2. Curse Analysis Briefing - Meeting, 60 minutes, 2024-01-16, Completed, Hex classification review
3. Auror Physical Training - Activity, 120 minutes, 2024-01-17, Completed, Combat conditioning
4. Defense Dueling Practice - Activity, 90 minutes, 2024-01-18, Completed, Advanced curse defense
5. Azkaban Prisoner Review - Meeting, 45 minutes, 2024-01-19, Completed, Security update audit
6. International Case Coordination - Call, 30 minutes, 2024-01-22, Scheduled, Interpol liaison
7. Magical Evidence Examination - Activity, 150 minutes, 2024-01-23, Completed, Dark artifact analysis
8. Trial Preparation Session - Meeting, 90 minutes, 2024-01-24, Completed, Prosecution strategy
9. Rogue Wizard Apprehension Operation - Activity, 360 minutes, 2024-01-25, Completed, Field apprehension
10. Dark Arts Prohibition Review - Meeting, 75 minutes, 2024-01-26, Scheduled, Legal standards update
11. Evidence Chain Review - Activity, 120 minutes, 2024-01-29, Completed, Artifact documentation
12. Ministry Internal Investigation - Meeting, 90 minutes, 2024-01-30, Completed, Corruption allegation
13. Creature Division Coordination - Call, 45 minutes, 2024-02-01, Completed, Creature threat briefing
14. Curse Breaker Consultation - Activity, 120 minutes, 2024-02-02, Completed, Complex curse neutralization
15. International Auror Conference - Meeting, 180 minutes, 2024-02-05, Completed, Global security standards
16. Safe House Security Check - Activity, 180 minutes, 2024-02-06, Scheduled, Witness protection validation
17. Dark Wizard Database Update - Activity, 90 minutes, 2024-02-15, Completed, Intelligence compilation
18. Annual Auror Performance Review - Meeting, 120 minutes, 2024-02-20, Completed, Career progression assessment

Make these appropriately dark and serious with dates across "Post-War Reconstruction (1998-2005)".

**Standard Field Mapping (apply when creating all records):**
- Customer `status`: Derive from context — active Aurors on duty → `active`; recruits/trainees → `prospect`; retired/suspended/inactive → `inactive`. Aim for ~65% active, 20% prospect, 15% inactive.
- Opportunity `stage`: Map from the case/investigation workflow index — 0 = `prospecting`, 1 = `qualification`, 2 = `proposal`, 3 = `negotiation`, 4 = `closed_won`. Use `closed_lost` for closed/dismissed cases.
- Activity `status`: Extract from each description — "Completed" → `completed`, "Scheduled"/"Ongoing" → `pending`, "Cancelled" → `cancelled`.

**Sample Data - 10 CustomRecords (use bulkCreateCustomRecords):**
1. Enhanced Probity Probe Mark III - type: equipment, status: active, serialNumber: PP-MKD-001, customer: Kingsley Shacklebolt
2. Invisibility Cloak (Confiscated) - type: artifact, status: active, serialNumber: IC-CONF-034
3. Dark Arts Detection Array - type: hardware, status: active, serialNumber: DA-DET-007, customer: Nymphadora Tonks
4. Azkaban Transport Broomstick Fleet (5 units) - type: equipment, status: active, serialNumber: BRM-AZK-005
5. Floo Powder Emergency Reserve - type: inventory, status: active, serialNumber: FLOO-EMG-001
6. Auror Communication Mirror Set (12 pairs) - type: hardware, status: maintenance, serialNumber: MIR-COMM-012
7. Dark Mark Tracking Compass - type: artifact, status: active, serialNumber: DMT-COMP-001, customer: Harry Potter
8. Evidence Preservation Containers (Set of 50) - type: inventory, status: active, serialNumber: EVD-CONT-050
9. Restricted Dark Artefact Vault (Ministry) - type: storage, status: active, serialNumber: VAULT-MIN-003
10. Memory Extraction Pensieve (Field Unit) - type: equipment, status: inactive, serialNumber: PENS-FLD-002

**Sample Data - 6 Orders:**
1. Kingsley Shacklebolt - "Advanced Combat Spell Module", status: DELIVERED, totalAmount: 4200, currency: GBP
2. Harry Potter - "Specialist Dark Arts Investigation Kit", status: CONFIRMED, totalAmount: 3800, currency: GBP
3. Nymphadora Tonks - "Metamorphmagus Field Equipment Pack", status: SHIPPED, totalAmount: 2100, currency: GBP
4. Ron Weasley - "Tactical Communication Upgrade", status: DRAFT, totalAmount: 1800, currency: GBP
5. Hermione Granger - "Advanced Legal Research Package", status: CANCELLED, totalAmount: 950, currency: GBP
6. Alastor Moody - "Enhanced Defensive Gear Replacement", status: DELIVERED, totalAmount: 5600, currency: GBP

**Sample Data - 6 Invoices:**
1. Kingsley Shacklebolt - status: PAID, totalAmount: 4200, currency: GBP, paymentTerms: NET-30
2. Harry Potter - status: SENT, totalAmount: 3800, currency: GBP, paymentTerms: NET-30
3. Nymphadora Tonks - status: PAID, totalAmount: 2100, currency: GBP, paymentTerms: NET-14
4. Ron Weasley - status: DRAFT, totalAmount: 1800, currency: GBP, paymentTerms: NET-30
5. Hermione Granger - status: OVERDUE, totalAmount: 950, currency: GBP, paymentTerms: NET-30
6. Alastor Moody - status: PAID, totalAmount: 5600, currency: GBP, paymentTerms: NET-14

**Document Templates (3 custom styles — use createDocumentTemplate for each):**
1. "Auror Case Brief" — templateType: slide_deck, description: "Classified dark-theme case briefing for senior Ministry review", styleJson: {"layout":"corporate","accentColor":"#1A237E","backgroundColor":"#0A0E1A","h1Color":"#42A5F5"}
2. "Auror Field Summary" — templateType: one_pager, description: "Concise field operative profile for departmental records", styleJson: {"layout":"light","accentColor":"#1A237E","includeCustomFields":true}
3. "Auror Registry Export" — templateType: csv_export, description: "Full operative registry export with clearance and case data", styleJson: {"includeFields":["name","status","rank","specialization","security_clearance","cases_solved"]}

**Notification Templates (3 Ministry dispatch templates — use createNotificationTemplate for each):**
1. name: "New Case Assignment", notificationType: OPPORTUNITY_CREATED, isActive: true
   subject: "Case Assigned: {{opportunityName}} → {{customerName}}"
   body: "A new investigation has been opened.\n\nAuror: {{customerName}}\nCase: {{opportunityName}}\nPriority: {{stage}}\n\nCase file: {{link}}"
2. name: "Case Status Update", notificationType: OPPORTUNITY_UPDATED, isActive: true
   subject: "Case Update: {{opportunityName}} — Stage {{stage}}"
   body: "Investigation status has changed.\n\nAuror: {{customerName}}\nCase: {{opportunityName}}\nNew Stage: {{stage}}\nAssigned: {{assignee}}\n\n{{link}}"
3. name: "Operative Equipment Issued", notificationType: ORDER_CREATED, isActive: true
   subject: "Equipment Order: {{customerName}} — {{amount}} Galleons"
   body: "Field equipment has been requisitioned.\n\nOperative: {{customerName}}\nItems: {{opportunityName}}\nTotal Cost: {{amount}}\nExpected: {{dueDate}}\n\n{{link}}"

Please execute this complete setup now, creating all fields, policies, aurors, contacts, cases, activities, customRecords, orders, invoices, document templates, and notification templates. Update the tenant settings with the branding information provided. Justice awaits!
```

---

## 📖 How to Use These Harry Potter Prompts

1. **Choose your magical organization**: Diagon Alley Alliance, Hogwarts, or Ministry of Magic
2. **Copy the entire prompt** from this file
3. **Go to chat**: http://localhost:5173/onboarding
4. **Create new tenant** → Skip wizard → Go to Chat
5. **Paste the prompt** and hit Enter
6. **Wait ~4-5 minutes** (executes automatically in wizard mode)
7. **Explore your magical CRM**

**Each prompt creates**:
- ✅ 13 unique custom fields (magically themed)
- ✅ 2 calculated fields with spellcasting SpEL expressions
- ✅ 2-3 business policies with magical constraints
- ✅ 15 sample records (merchants, students, or aurors)
- ✅ 20 themed contacts from the HP universe
- ✅ 15 magical agreements/achievements/cases
- ✅ 18+ activities from the wizarding world
- ✅ Branded company settings with magical locations

**Total setup time**: ~15 minutes for all 3 Harry Potter tenants

---

## ⚡ The Magic Awaits

May these prompts bring the wonder of the wizarding world to your CRM. Accio data!

