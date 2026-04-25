# Star Wars Universe - Staged Assistant Setup Prompts

Decomposed versions of each Star Wars tenant prompt, broken into 10 smaller steps for smaller AI models.

---

## Rebel Alliance Supply Chain — 10 Steps

### Step 1 of 10 — Customer Custom Fields

```
I'm setting up a CRM workspace for the "Rebel Alliance Supply Chain". In this step, please create the custom fields for Customers (Rebel Suppliers/Allies).

Create the following custom fields for the Customer entity:
1. "Faction Status" (select field, required) — options: Core Ally, Regional Partner, Independent Supplier, Sympathizer
2. "Hyperdrive Capability" (boolean field) — can they travel across systems
3. "Sector of Operations" (text field) — which sector they operate in
4. "Credits in Reserve" (currency field) — financial capacity
5. "Alliance Trust Rating" (number field, 0-100) — trust score
6. "Last Supply Run Date" (date field) — when they last delivered
7. "Inventory Level" (percentage field) — current stock availability
8. "Smuggler Network Connection" (boolean field) — have contraband connections
9. "Species Represented" (select field) — options: Human, Wookiee, Twi'lek, Mon Calamari, Ewok, Droid, Other
10. "Base Location" (text field) — where they operate from
11. "Contact Frequency" (multiselect) — options: Encrypted Transmission, Hologram, Dead Drop, Secure Relay
12. "Operation Notes" (textarea field) — classified notes
13. "Rebellion Support Type" (workflow field) — milestones: Recruit, Train, Equip, Deploy, Victorious

Confirm when all 13 customer custom fields have been created.
```

---

### Step 2 of 10 — Opportunity Custom Fields

```
I'm continuing setup for the Rebel Alliance Supply Chain. Customer custom fields are done. Now create the custom fields for the Opportunity entity (Supply Missions/Operations).

Create the following custom fields for the Opportunity entity:
1. "Mission Classification" (select field, required) — options: Weapon Procurement, Food/Medical, Tech/Droid, Intelligence, Sabotage, Evacuation
2. "Imperial Threat Level" (select field) — options: Low, Medium, High, Critical - Death Star Proximity
3. "Rebel Operatives Assigned" (multiselect) — options: Luke, Leia, Han, Chewbacca, Lando, Yoda, Obi-Wan
4. "Mission Briefing" (richtext) — full operation details
5. "Rebellion Victory Progress" (workflow field) — milestones: Planning, Infiltration, Execution, Escape, Safe Return

Confirm when all 5 opportunity custom fields have been created.
```

---

### Step 3 of 10 — Calculated Fields & Business Policies

```
I'm continuing setup for the Rebel Alliance Supply Chain. Custom fields are done. Now create the calculated fields and business policies.

**Calculated Fields:**
1. For Customer — "Strategic Value Score":
   Expression: (Alliance_Trust_Rating * 2) + (Credits_In_Reserve / 100000) + (if Hyperdrive_Capability then 25 else 0)

2. For Opportunity — "Mission Completion %":
   Expression: if Rebellion_Victory_Progress exists then (Rebellion_Victory_Progress.currentIndex + 1) * 20 else 0

**Business Policies (Rego syntax — each condition on a separate line):**
1. DENY — "No Sabotage with Imperial Sympathizers":
   input.entity.faction_status == "Sympathizer"
   input.entity.mission_classification == "Sabotage"

2. WARN — "Critical Threat Level Mission":
   input.entity.imperial_threat_level == "Critical - Death Star Proximity"

3. DENY — "Minimum Trust Rating Required":
   input.entity.alliance_trust_rating < 40

Confirm when calculated fields and policies are created.
```

---

### Step 4 of 10 — Tenant Settings & Branding

```
I'm continuing setup for the Rebel Alliance Supply Chain. Now update the tenant settings with the following branding:

- Company Name: Rebel Alliance Supply Chain
- Logo URL: https://static.wikia.nocookie.net/starwars/images/7/71/Redstarbird.svg/revision/latest?cb=20080228205026
- Primary Color: #FF0000
- Secondary Color: #FFD700
- Website: https://rebel-alliance.galaxy
- Bio: "The Rebel Alliance Supply Chain coordinates resources, personnel, and strategic equipment across multiple star systems. We manage procurement from suppliers, track inventory across hidden bases, and ensure operational readiness against the Empire. Our mission: freedom through logistics."

Confirm when tenant settings have been updated.
```

---

### Step 5 of 10 — Customers 1–8

```
I'm continuing setup for the Rebel Alliance Supply Chain. Now create the first 8 suppliers/allies.

1. Name: Mos Eisley Spaceport, status: active
   Custom fields: Faction Status=Independent Supplier, Hyperdrive Capability=false, Sector of Operations=Tatooine, Credits in Reserve=2500000, Alliance Trust Rating=85, Inventory Level=78, Smuggler Network Connection=true, Species Represented=Human, Base Location="Mos Eisley, Tatooine", Contact Frequency=[Encrypted Transmission, Dead Drop], Rebellion Support Type currentIndex=3

2. Name: Bothawui Information Bureau, status: active
   Custom fields: Faction Status=Core Ally, Hyperdrive Capability=true, Sector of Operations="Bothan Space", Credits in Reserve=5200000, Alliance Trust Rating=95, Inventory Level=92, Smuggler Network Connection=false, Species Represented=Other, Base Location=Bothawui, Contact Frequency=[Encrypted Transmission, Hologram], Rebellion Support Type currentIndex=4

3. Name: Wookiee Mining Collective, status: active
   Custom fields: Faction Status=Core Ally, Hyperdrive Capability=false, Sector of Operations=Kashyyyk, Credits in Reserve=1800000, Alliance Trust Rating=90, Inventory Level=65, Smuggler Network Connection=false, Species Represented=Wookiee, Base Location=Kashyyyk, Contact Frequency=[Secure Relay], Rebellion Support Type currentIndex=2

4. Name: Mon Calamari Shipyards, status: active
   Custom fields: Faction Status=Core Ally, Hyperdrive Capability=true, Sector of Operations="Mon Calamari", Credits in Reserve=8500000, Alliance Trust Rating=98, Inventory Level=88, Smuggler Network Connection=false, Species Represented=Mon Calamari, Base Location="Mon Calamari", Contact Frequency=[Hologram, Secure Relay], Rebellion Support Type currentIndex=4

5. Name: Ewok Forest Alliance, status: prospect
   Custom fields: Faction Status=Regional Partner, Hyperdrive Capability=false, Sector of Operations=Endor, Credits in Reserve=450000, Alliance Trust Rating=72, Inventory Level=45, Smuggler Network Connection=false, Species Represented=Ewok, Base Location="Bright Tree Village, Endor", Contact Frequency=[Secure Relay], Rebellion Support Type currentIndex=1

6. Name: Tatooine Desert Traders, status: prospect
   Custom fields: Faction Status=Regional Partner, Hyperdrive Capability=false, Sector of Operations=Tatooine, Credits in Reserve=950000, Alliance Trust Rating=68, Inventory Level=55, Smuggler Network Connection=true, Species Represented=Human, Base Location="Mos Eisley, Tatooine", Contact Frequency=[Encrypted Transmission, Dead Drop], Rebellion Support Type currentIndex=0

7. Name: Dagobah Hidden Repository, status: active
   Custom fields: Faction Status=Core Ally, Hyperdrive Capability=true, Sector of Operations=Dagobah, Credits in Reserve=3200000, Alliance Trust Rating=100, Inventory Level=71, Smuggler Network Connection=false, Species Represented=Other, Base Location=Dagobah, Contact Frequency=[Hologram], Rebellion Support Type currentIndex=4

8. Name: Bespin Cloud City, status: active
   Custom fields: Faction Status=Core Ally, Hyperdrive Capability=true, Sector of Operations=Bespin, Credits in Reserve=6100000, Alliance Trust Rating=87, Inventory Level=82, Smuggler Network Connection=true, Species Represented=Human, Base Location="Cloud City, Bespin", Contact Frequency=[Hologram, Secure Relay], Rebellion Support Type currentIndex=2

Confirm when all 8 customers are created.
```

---

### Step 6 of 10 — Customers 9–15

```
I'm continuing setup for the Rebel Alliance Supply Chain. First 8 customers are done. Now create customers 9–15.

9. Name: Naboo Royal Guard, status: prospect
   Custom fields: Faction Status=Regional Partner, Hyperdrive Capability=true, Sector of Operations=Naboo, Credits in Reserve=2800000, Alliance Trust Rating=75, Inventory Level=79, Smuggler Network Connection=false, Species Represented=Human, Base Location=Naboo, Contact Frequency=[Hologram], Rebellion Support Type currentIndex=1

10. Name: Outer Rim Mercenaries, status: prospect
    Custom fields: Faction Status=Independent Supplier, Hyperdrive Capability=true, Sector of Operations="Outer Rim", Credits in Reserve=1200000, Alliance Trust Rating=55, Inventory Level=48, Smuggler Network Connection=true, Species Represented=Other, Base Location="Mos Eisley, Tatooine", Contact Frequency=[Encrypted Transmission, Dead Drop], Rebellion Support Type currentIndex=0

11. Name: Cantina Black Market, status: inactive
    Custom fields: Faction Status=Sympathizer, Hyperdrive Capability=false, Sector of Operations=Various, Credits in Reserve=300000, Alliance Trust Rating=25, Inventory Level=22, Smuggler Network Connection=true, Species Represented=Other, Base Location="Mos Eisley, Tatooine", Contact Frequency=[Dead Drop], Rebellion Support Type currentIndex=0

12. Name: Kessel Run Spice Traders, status: prospect
    Custom fields: Faction Status=Independent Supplier, Hyperdrive Capability=true, Sector of Operations=Kessel, Credits in Reserve=2100000, Alliance Trust Rating=62, Inventory Level=68, Smuggler Network Connection=true, Species Represented=Human, Base Location=Kessel, Contact Frequency=[Encrypted Transmission, Dead Drop], Rebellion Support Type currentIndex=3

13. Name: Mustafar Weapon Forges, status: active
    Custom fields: Faction Status=Regional Partner, Hyperdrive Capability=false, Sector of Operations=Mustafar, Credits in Reserve=3800000, Alliance Trust Rating=80, Inventory Level=54, Smuggler Network Connection=false, Species Represented=Other, Base Location=Mustafar, Contact Frequency=[Secure Relay], Rebellion Support Type currentIndex=2

14. Name: Scarif Data Vault, status: active
    Custom fields: Faction Status=Core Ally, Hyperdrive Capability=true, Sector of Operations=Scarif, Credits in Reserve=4500000, Alliance Trust Rating=93, Inventory Level=76, Smuggler Network Connection=false, Species Represented=Human, Base Location=Scarif, Contact Frequency=[Encrypted Transmission, Secure Relay], Rebellion Support Type currentIndex=3

15. Name: Yoda's Jedi Temple, status: active
    Custom fields: Faction Status=Core Ally, Hyperdrive Capability=true, Sector of Operations=Dagobah, Credits in Reserve=7200000, Alliance Trust Rating=99, Inventory Level=85, Smuggler Network Connection=false, Species Represented=Other, Base Location=Dagobah, Contact Frequency=[Hologram, Secure Relay], Rebellion Support Type currentIndex=4

Confirm when all 7 customers are created.
```

---

### Step 7 of 10 — Contacts

```
I'm continuing setup for the Rebel Alliance Supply Chain. 15 suppliers are created. Now create 20 contacts.

1. General Dodonna — Supreme Commander (company: Rebel Base)
2. Admiral Ackbar — Fleet Commander at Mon Calamari Shipyards
3. General Madine — Ground Operations (company: Rebel Base)
4. Carrie Fisherman — Intelligence Director at Bothawui Information Bureau
5. Wedge Antilles — Fighter Wing Lead (company: Rebel Base)
6. Jyn Erso — Sabotage Chief at Scarif Data Vault
7. Cassian Andor — Field Operations (company: Rebel Base)
8. Poe Dameron — Pilot Commander (company: Rebel Base)
9. Rey Skywalker — Force Sensitive at Dagobah Hidden Repository
10. Finn — Infantry Commander (company: Rebel Base)
11. Chewbacca — Engineering at Mos Eisley Spaceport
12. Maz Kanata — Ancient Knowledge (company: Takodana Castle)
13. Lando Calrissian — Cloud City at Bespin Cloud City
14. Amilyn Holdo — Logistics (company: Rebel Base)
15. Paige Tico — Bomber Pilot (company: Rebel Base)
16. Rose Tico — Engineering Support (company: Rebel Base)
17. Thane Kyrell — Commando Lead (company: Rebel Base)
18. Enfys Nest — Pirate Alliance at Kessel Run Spice Traders
19. Bail Organa — Political Liaison at Naboo Royal Guard
20. Galen Erso — Weapons Development at Scarif Data Vault

Confirm when all 20 contacts are created.
```

---

### Step 8 of 10 — Opportunities (Missions)

```
I'm continuing setup for the Rebel Alliance Supply Chain. Now create 15 missions as Opportunities. Map Rebellion Victory Progress index: 0=prospecting, 1=qualification, 2=proposal, 3=negotiation, 4=closed_won.

1. Name: Steal Death Star Plans, customer: Scarif Data Vault, stage: qualification, amount: 15000000
   Custom fields: Mission Classification=Weapon Procurement, Imperial Threat Level=Critical - Death Star Proximity, Rebel Operatives Assigned=[Luke, Leia, Obi-Wan], Mission Briefing="Infiltrate Scarif facility and extract Death Star technical specifications", Rebellion Victory Progress currentIndex=1

2. Name: Medical Supply Run to Hoth, customer: Mon Calamari Shipyards, stage: prospecting, amount: 2300000
   Custom fields: Mission Classification=Food/Medical, Imperial Threat Level=Medium, Rebel Operatives Assigned=[Han, Chewbacca], Mission Briefing="Secure medical supplies and freeze-resistant rations for Echo Base", Rebellion Victory Progress currentIndex=0

3. Name: Procure X-Wing Fighters, customer: Mon Calamari Shipyards, stage: proposal, amount: 12000000
   Custom fields: Mission Classification=Tech/Droid, Imperial Threat Level=High, Rebel Operatives Assigned=[Luke, Lando], Mission Briefing="Acquire squadron of T-65 X-Wing fighter craft", Rebellion Victory Progress currentIndex=2

4. Name: Rescue Echo Base Personnel, customer: Outer Rim Mercenaries, stage: negotiation, amount: 8500000
   Custom fields: Mission Classification=Evacuation, Imperial Threat Level=Critical - Death Star Proximity, Rebel Operatives Assigned=[Leia, Han, Chewbacca], Mission Briefing="Extract key personnel from compromised Echo Base facility", Rebellion Victory Progress currentIndex=3

5. Name: Recover Ancient Jedi Knowledge, customer: Yoda's Jedi Temple, stage: prospecting, amount: 5200000
   Custom fields: Mission Classification=Intelligence, Imperial Threat Level=Low, Rebel Operatives Assigned=[Luke, Yoda, Obi-Wan], Mission Briefing="Retrieve archived Jedi texts and training records", Rebellion Victory Progress currentIndex=0

6. Name: Infiltrate Imperial Communications, customer: Kessel Run Spice Traders, stage: qualification, amount: 7800000
   Custom fields: Mission Classification=Sabotage, Imperial Threat Level=High, Rebel Operatives Assigned=[Leia], Mission Briefing="Compromise Imperial communication arrays and relay encoded transmissions", Rebellion Victory Progress currentIndex=1

7. Name: Acquire Hyperdrive Cores, customer: Mustafar Weapon Forges, stage: prospecting, amount: 6100000
   Custom fields: Mission Classification=Tech/Droid, Imperial Threat Level=Medium, Rebel Operatives Assigned=[Han, Lando], Mission Briefing="Obtain hyperdrive components for starship upgrades", Rebellion Victory Progress currentIndex=0

8. Name: Sabotage TIE Fighter Production, customer: Bothawui Information Bureau, stage: prospecting, amount: 9300000
   Custom fields: Mission Classification=Sabotage, Imperial Threat Level=High, Rebel Operatives Assigned=[Leia], Mission Briefing="Introduce defects into Imperial TIE fighter manufacturing", Rebellion Victory Progress currentIndex=0

9. Name: Transport Thermal Detonators, customer: Mos Eisley Spaceport, stage: proposal, amount: 3400000
   Custom fields: Mission Classification=Weapon Procurement, Imperial Threat Level=Medium, Rebel Operatives Assigned=[Han, Chewbacca, Lando], Mission Briefing="Move shipment of thermal detonators to forward base", Rebellion Victory Progress currentIndex=2

10. Name: Diplomatic Mission to Ewoks, customer: Ewok Forest Alliance, stage: prospecting, amount: 2100000
    Custom fields: Mission Classification=Intelligence, Imperial Threat Level=Low, Rebel Operatives Assigned=[Leia], Mission Briefing="Negotiate alliance with Endor native populations", Rebellion Victory Progress currentIndex=0

11. Name: Recover Lost Rebel Cells, customer: Outer Rim Mercenaries, stage: qualification, amount: 4800000
    Custom fields: Mission Classification=Evacuation, Imperial Threat Level=High, Rebel Operatives Assigned=[Leia], Mission Briefing="Locate and extract isolated rebel cells from Outer Rim", Rebellion Victory Progress currentIndex=1

12. Name: Capture Imperial Officer, customer: Bothawui Information Bureau, stage: prospecting, amount: 6500000
    Custom fields: Mission Classification=Intelligence, Imperial Threat Level=High, Rebel Operatives Assigned=[Luke, Leia, Han], Mission Briefing="Capture high-ranking Imperial officer for interrogation", Rebellion Victory Progress currentIndex=0

13. Name: Defend Echo Base, customer: Bespin Cloud City, stage: proposal, amount: 11000000
    Custom fields: Mission Classification=Weapon Procurement, Imperial Threat Level=Critical - Death Star Proximity, Rebel Operatives Assigned=[Leia, Han], Mission Briefing="Fortify Echo Base against Imperial assault", Rebellion Victory Progress currentIndex=2

14. Name: Create Distraction at Mos Eisley, customer: Tatooine Desert Traders, stage: qualification, amount: 2800000
    Custom fields: Mission Classification=Sabotage, Imperial Threat Level=Medium, Rebel Operatives Assigned=[Han, Lando], Mission Briefing="Stage diversion to mask rebel operations elsewhere", Rebellion Victory Progress currentIndex=1

15. Name: Final Strike on Death Star, customer: Dagobah Hidden Repository, stage: closed_won, amount: 25000000
    Custom fields: Mission Classification=Weapon Procurement, Imperial Threat Level=Critical - Death Star Proximity, Rebel Operatives Assigned=[Luke, Leia, Han], Mission Briefing="Execute final assault on Death Star battle station", Rebellion Victory Progress currentIndex=4

Confirm when all 15 opportunities are created.
```

---

### Step 9 of 10 — Activities

```
I'm continuing setup for the Rebel Alliance Supply Chain. Now create 18 activities. Map: "Completed" → completed, "Scheduled" → pending.

1. Subject: Scarif Intelligence Briefing, type: meeting, duration: 90 min, date: 2024-01-15, status: pending, notes: "Death Star plans discussion"
2. Subject: Hyperspace Jump to Hoth, type: call, duration: 120 min, date: 2024-01-16, status: completed, notes: "Medical supply transport"
3. Subject: X-Wing Fighter Inspection, type: meeting, duration: 60 min, date: 2024-01-18, status: pending, notes: "Technical acceptance review"
4. Subject: Echo Base Personnel Evacuation, type: email, duration: 45 min, date: 2024-01-19, status: completed, notes: "Extraction coordination"
5. Subject: Jedi Temple Strategy Session, type: meeting, duration: 90 min, date: 2024-01-22, status: pending, notes: "Knowledge recovery planning"
6. Subject: Imperial Communications Infiltration, type: meeting, duration: 120 min, date: 2024-01-23, status: completed, notes: "Sabotage briefing"
7. Subject: Hyperdrive Core Testing, type: meeting, duration: 60 min, date: 2024-01-24, status: pending, notes: "Equipment verification"
8. Subject: TIE Fighter Production Sabotage, type: call, duration: 75 min, date: 2024-01-25, status: completed, notes: "Operation coordination"
9. Subject: Thermal Detonator Transport, type: email, duration: 30 min, date: 2024-01-29, status: completed, notes: "Shipment tracking"
10. Subject: Ewok Diplomatic Prep, type: meeting, duration: 90 min, date: 2024-01-30, status: pending, notes: "Alliance negotiation"
11. Subject: Lost Cells Recovery Operation, type: meeting, duration: 120 min, date: 2024-02-01, status: pending, notes: "Rescue planning"
12. Subject: Imperial Officer Capture, type: call, duration: 60 min, date: 2024-02-02, status: completed, notes: "Interrogation prep"
13. Subject: Echo Base Defense Fortification, type: meeting, duration: 90 min, date: 2024-02-05, status: pending, notes: "Defensive strategy"
14. Subject: Mos Eisley Distraction Planning, type: meeting, duration: 75 min, date: 2024-02-06, status: completed, notes: "Diversion tactics"
15. Subject: Death Star Final Assault, type: meeting, duration: 120 min, date: 2024-02-08, status: pending, notes: "Mission briefing"
16. Subject: Alliance Coordination Conference, type: call, duration: 90 min, date: 2024-02-09, status: completed, notes: "Multi-faction alignment"
17. Subject: Supply Chain Verification, type: email, duration: 30 min, date: 2024-02-12, status: completed, notes: "Inventory confirmation"
18. Subject: Victory Celebration Planning, type: meeting, duration: 60 min, date: 2024-02-15, status: pending, notes: "Post-mission protocol"

Confirm when all 18 activities are created.
```

---

### Step 10 of 10 — CustomRecords, Orders, Invoices & Templates

```
I'm continuing setup for the Rebel Alliance Supply Chain. This is the final step.

**CustomRecords (use bulkCreateCustomRecords):**
1. name: "X-Wing Starfighter Red Five", type: spacecraft, status: active, serialNumber: XW-R5-001
2. name: "Millennium Falcon Transponder", type: hardware, status: maintenance, serialNumber: MF-TRNSP-001
3. name: "Mon Calamari Cruiser Targeting Array", type: hardware, status: active, serialNumber: MC-TGT-001, customer: Mon Calamari Shipyards
4. name: "Rebel Base Encryption Terminal", type: hardware, status: active, serialNumber: ENC-ECHO-001
5. name: "Astromech Droid R2-D2 Unit", type: droid, status: active, serialNumber: R2D2-REG-001
6. name: "Bothan Intelligence Data Archive", type: software, status: active, serialNumber: BTH-ARCH-001, customer: Bothawui Information Bureau
7. name: "Speeder Bike Patrol Unit (x8)", type: vehicle, status: active, serialNumber: SPD-END-008
8. name: "Proton Torpedo Stock — 200 units", type: inventory, status: active, serialNumber: PRT-STOCK-200
9. name: "Medical Frigate Life Support Array", type: hardware, status: active, serialNumber: MED-NEA-001
10. name: "Holo-Comm Array — Field Portable", type: hardware, status: inactive, serialNumber: HOL-FLD-003

**Orders:**
1. customer: Mon Calamari Shipyards, name: "X-Wing Maintenance & Upgrade Package", status: DELIVERED, totalAmount: 85000, currency: USD
2. customer: Mon Calamari Shipyards, name: "Capital Ship Weapons Array Refit", status: CONFIRMED, totalAmount: 620000, currency: USD
3. customer: Bothawui Information Bureau, name: "Cold Weather Survival Equipment", status: SHIPPED, totalAmount: 42000, currency: USD
4. customer: Bothawui Information Bureau, name: "Encrypted Communication Suite", status: DRAFT, totalAmount: 38000, currency: USD
5. customer: Ewok Forest Alliance, name: "Speeder Bike Replacement Parts", status: CANCELLED, totalAmount: 15000, currency: USD
6. customer: Mos Eisley Spaceport, name: "Smuggling Compartment Refit Kit", status: DELIVERED, totalAmount: 28000, currency: USD

**Invoices:**
1. customer: Mon Calamari Shipyards, status: PAID, totalAmount: 85000, currency: USD, paymentTerms: NET-30
2. customer: Mon Calamari Shipyards, status: SENT, totalAmount: 620000, currency: USD, paymentTerms: NET-60
3. customer: Bothawui Information Bureau, status: PAID, totalAmount: 42000, currency: USD, paymentTerms: NET-30
4. customer: Bothawui Information Bureau, status: DRAFT, totalAmount: 38000, currency: USD, paymentTerms: NET-30
5. customer: Ewok Forest Alliance, status: OVERDUE, totalAmount: 15000, currency: USD, paymentTerms: NET-14
6. customer: Mos Eisley Spaceport, status: PAID, totalAmount: 28000, currency: USD, paymentTerms: NET-30

**Document Templates (use createDocumentTemplate for each):**
1. name: "Rebel Mission Briefing Deck", templateType: slide_deck, description: "Dark tactical briefing deck for high-command mission planning", styleJson: {"layout":"corporate","accentColor":"#E8A000","backgroundColor":"#0A0A14","h1Color":"#FF6B35"}
2. name: "Alliance Faction Profile", templateType: one_pager, description: "Single-page faction dossier for high command review", styleJson: {"layout":"light","accentColor":"#E8A000","includeCustomFields":true}
3. name: "Alliance Roster Export", templateType: csv_export, description: "Full alliance member export with trust rating and support type data", styleJson: {"includeFields":["name","status","faction","trust_rating","rebellion_support_type","fleet_strength"]}

**Notification Templates (use createNotificationTemplate for each):**
1. name: "New Mission Opened", notificationType: OPPORTUNITY_CREATED, isActive: true
   subject: "Mission Alert: {{opportunityName}} ({{customerName}})"
   body: "A new Rebel mission has been initiated.\n\nFaction: {{customerName}}\nMission: {{opportunityName}}\nPriority: {{stage}}\nResources: {{amount}}\n\nView mission file: {{link}}"

2. name: "Alliance Status Change", notificationType: CUSTOMER_UPDATED, isActive: true
   subject: "Faction Update: {{customerName}}"
   body: "Alliance member status has changed.\n\nFaction: {{customerName}}\nUpdated by: {{assignee}}\n\nReview: {{link}}"

3. name: "Supply Order Dispatched", notificationType: ORDER_CREATED, isActive: true
   subject: "Supply Order: {{customerName}} — {{amount}} Credits"
   body: "Rebel supply order dispatched.\n\nFaction: {{customerName}}\nOrder: {{opportunityName}}\nTotal: {{amount}} Credits\nExpected: {{dueDate}}\n\nTrack: {{link}}"

Confirm when all customRecords, orders, invoices, document templates, and notification templates are created. Rebel Alliance setup complete!
```

---

## Galactic Empire Corporate Division — 10 Steps

### Step 1 of 10 — Customer Custom Fields

```
I'm setting up a CRM workspace for "Galactic Empire Corporate Division". In this step, please create the custom fields for Customers (Imperial Territories/Contractors).

Create the following custom fields for the Customer entity:
1. "Territory Classification" (select field, required) — options: Core World, Outer Rim Occupied, Mining Colony, Military Installation, Research Facility
2. "Stormtrooper Garrison Size" (number field) — troop count
3. "Imperial Compliance Rating" (number field, 0-100) — loyalty to Empire
4. "Tarkin Doctrine Adherence" (percentage field) — fear-based control percentage
5. "TIE Fighter Squadron Count" (number field) — military customRecords
6. "Monthly Tribute (Credits)" (currency field) — taxation revenue
7. "Last Imperial Inspection" (date field) — compliance audit date
8. "Planet Destruction Threat Level" (select field) — options: None, Moderate, High, Death Star Target
9. "Sith Lord Assignment" (text field) — which Sith oversees them
10. "Detention Center Capacity" (number field) — prisoner holding
11. "Propaganda Effectiveness" (percentage field) — brainwashing success rate
12. "Rebellion Activity" (select field) — options: None Detected, Minor Pockets, Organized Cells, Active Threat
13. "Imperial Conquest Stage" (workflow field) — milestones: Initial Contact, Negotiation, Military Occupation, Full Integration, Subjugation

Confirm when all 13 customer custom fields have been created.
```

---

### Step 2 of 10 — Opportunity Custom Fields

```
I'm continuing setup for Galactic Empire Corporate Division. Customer custom fields are done. Now create the custom fields for the Opportunity entity (Imperial Operations/Conquests).

Create the following custom fields for the Opportunity entity:
1. "Operation Type" (select field, required) — options: Territory Conquest, Resource Acquisition, Military Build-up, Detention Operations, Propaganda Campaign
2. "Destruction Method" (select field) — options: Ground Assault, Orbital Bombardment, Death Star Strike, Blockade, Espionage
3. "Assigned Sith Lord" (select field) — options: Vader, Palpatine, Inquisitors, Unknown
4. "Imperial Efficiency Score" (number field, 0-100) — operational precision
5. "Conquest Progress" (workflow field) — milestones: Planning, Deployment, Siege, Surrender, Dominion Established

Confirm when all 5 opportunity custom fields have been created.
```

---

### Step 3 of 10 — Calculated Fields & Business Policies

```
I'm continuing setup for Galactic Empire Corporate Division. Custom fields are done. Now create the calculated fields and business policies.

**Calculated Fields:**
1. For Customer — "Imperial Threat Score":
   Expression: (Imperial_Compliance_Rating * 0.6) + (if TIE_Fighter_Squadron_Count > 10 then 30 else 15) + (Tarkin_Doctrine_Adherence * 0.4)

2. For Opportunity — "Conquest Completion %":
   Expression: if Conquest_Progress exists then (Conquest_Progress.currentIndex + 1) * 20 else 0

**Business Policies (Rego syntax — each condition on a separate line):**
1. DENY — "Cannot Reduce Garrison During Active Rebellion":
   input.entity.rebellion_activity == "Active Threat"
   input.entity.troop_reduction == true

2. WARN — "Death Star Target Planet":
   input.entity.planet_destruction_threat_level == "Death Star Target"

3. DENY — "Territory Conquest Requires Sith Assignment":
   input.entity.sith_lord_assignment == ""
   input.entity.operation_type == "Territory Conquest"

Confirm when calculated fields and policies are created.
```

---

### Step 4 of 10 — Tenant Settings & Branding

```
I'm continuing setup for Galactic Empire Corporate Division. Now update the tenant settings with the following branding:

- Company Name: Galactic Empire Corporate Division
- Logo URL: https://via.placeholder.com/200x100?text=Galactic+Empire
- Primary Color: #1a1a1a
- Secondary Color: #CCCCCC
- Website: https://imperial-order.galaxy
- Bio: "The Galactic Empire maintains order through superior organization and overwhelming force. Our Corporate Division manages procurement, facility operations, and resource allocation across thousands of systems. We ensure the Emperor's vision is executed with absolute efficiency and precision."

Confirm when tenant settings have been updated.
```

---

### Step 5 of 10 — Customers 1–8

```
I'm continuing setup for Galactic Empire Corporate Division. Now create the first 8 Imperial territories.

1. Name: Coruscant Imperial Palace, status: active
   Custom fields: Territory Classification=Core World, Stormtrooper Garrison Size=50000, Imperial Compliance Rating=100, Tarkin Doctrine Adherence=95, TIE Fighter Squadron Count=500, Monthly Tribute=50000000, Last Imperial Inspection=2024-01-01, Planet Destruction Threat Level=None, Sith Lord Assignment="Emperor Palpatine", Detention Center Capacity=10000, Propaganda Effectiveness=99, Rebellion Activity=None Detected, Imperial Conquest Stage currentIndex=4

2. Name: Scarif Military Installation, status: active
   Custom fields: Territory Classification=Military Installation, Stormtrooper Garrison Size=25000, Imperial Compliance Rating=98, Tarkin Doctrine Adherence=90, TIE Fighter Squadron Count=120, Monthly Tribute=15000000, Last Imperial Inspection=2024-01-05, Planet Destruction Threat Level=None, Sith Lord Assignment="Director Krennic", Detention Center Capacity=5000, Propaganda Effectiveness=88, Rebellion Activity=Minor Pockets, Imperial Conquest Stage currentIndex=3

3. Name: Cloud City Bespin, status: active
   Custom fields: Territory Classification=Outer Rim Occupied, Stormtrooper Garrison Size=8000, Imperial Compliance Rating=85, Tarkin Doctrine Adherence=75, TIE Fighter Squadron Count=45, Monthly Tribute=8500000, Last Imperial Inspection=2024-01-10, Planet Destruction Threat Level=Moderate, Sith Lord Assignment="Darth Vader", Detention Center Capacity=2000, Propaganda Effectiveness=78, Rebellion Activity=Minor Pockets, Imperial Conquest Stage currentIndex=2

4. Name: Tatooine Spaceport, status: prospect
   Custom fields: Territory Classification=Outer Rim Occupied, Stormtrooper Garrison Size=3000, Imperial Compliance Rating=62, Tarkin Doctrine Adherence=50, TIE Fighter Squadron Count=20, Monthly Tribute=3200000, Last Imperial Inspection=2023-12-20, Planet Destruction Threat Level=None, Sith Lord Assignment="Local Commander", Detention Center Capacity=500, Propaganda Effectiveness=42, Rebellion Activity=Organized Cells, Imperial Conquest Stage currentIndex=1

5. Name: Kessel Mining Colony, status: active
   Custom fields: Territory Classification=Mining Colony, Stormtrooper Garrison Size=15000, Imperial Compliance Rating=88, Tarkin Doctrine Adherence=85, TIE Fighter Squadron Count=60, Monthly Tribute=12000000, Last Imperial Inspection=2024-01-08, Planet Destruction Threat Level=None, Sith Lord Assignment="Tarkin Overseer", Detention Center Capacity=3000, Propaganda Effectiveness=82, Rebellion Activity=None Detected, Imperial Conquest Stage currentIndex=3

6. Name: Mustafar Research Facility, status: active
   Custom fields: Territory Classification=Research Facility, Stormtrooper Garrison Size=5000, Imperial Compliance Rating=92, Tarkin Doctrine Adherence=88, TIE Fighter Squadron Count=25, Monthly Tribute=7800000, Last Imperial Inspection=2024-01-02, Planet Destruction Threat Level=None, Sith Lord Assignment="Darth Vader", Detention Center Capacity=1500, Propaganda Effectiveness=95, Rebellion Activity=None Detected, Imperial Conquest Stage currentIndex=4

7. Name: Death Star Space Station, status: active
   Custom fields: Territory Classification=Military Installation, Stormtrooper Garrison Size=1000000, Imperial Compliance Rating=100, Tarkin Doctrine Adherence=100, TIE Fighter Squadron Count=1000, Monthly Tribute=100000000, Last Imperial Inspection=2024-01-01, Planet Destruction Threat Level=Death Star Target, Sith Lord Assignment="Grand Moff Tarkin", Detention Center Capacity=50000, Propaganda Effectiveness=100, Rebellion Activity=None Detected, Imperial Conquest Stage currentIndex=4

8. Name: Geonosis War Factory, status: active
   Custom fields: Territory Classification=Military Installation, Stormtrooper Garrison Size=12000, Imperial Compliance Rating=90, Tarkin Doctrine Adherence=92, TIE Fighter Squadron Count=80, Monthly Tribute=11000000, Last Imperial Inspection=2024-01-06, Planet Destruction Threat Level=None, Sith Lord Assignment="Industrial Commander", Detention Center Capacity=2500, Propaganda Effectiveness=85, Rebellion Activity=None Detected, Imperial Conquest Stage currentIndex=3

Confirm when all 8 territories are created.
```

---

### Step 6 of 10 — Customers 9–15

```
I'm continuing setup for Galactic Empire Corporate Division. First 8 territories done. Now create territories 9–15.

9. Name: Ord Mantell Garrison, status: active
   Custom fields: Territory Classification=Military Installation, Stormtrooper Garrison Size=6000, Imperial Compliance Rating=91, Tarkin Doctrine Adherence=89, TIE Fighter Squadron Count=35, Monthly Tribute=6500000, Last Imperial Inspection=2024-01-12, Planet Destruction Threat Level=None, Sith Lord Assignment="Commander Needa", Detention Center Capacity=1500, Propaganda Effectiveness=84, Rebellion Activity=Minor Pockets, Imperial Conquest Stage currentIndex=3

10. Name: Naboo Royal Palace, status: active
    Custom fields: Territory Classification=Core World, Stormtrooper Garrison Size=4000, Imperial Compliance Rating=88, Tarkin Doctrine Adherence=82, TIE Fighter Squadron Count=30, Monthly Tribute=9200000, Last Imperial Inspection=2024-01-09, Planet Destruction Threat Level=None, Sith Lord Assignment="Emperor Representative", Detention Center Capacity=1000, Propaganda Effectiveness=80, Rebellion Activity=Minor Pockets, Imperial Conquest Stage currentIndex=2

11. Name: Bothawui Occupied Sector, status: prospect
    Custom fields: Territory Classification=Outer Rim Occupied, Stormtrooper Garrison Size=10000, Imperial Compliance Rating=72, Tarkin Doctrine Adherence=65, TIE Fighter Squadron Count=50, Monthly Tribute=5100000, Last Imperial Inspection=2024-01-11, Planet Destruction Threat Level=Moderate, Sith Lord Assignment="Regional Governor", Detention Center Capacity=2000, Propaganda Effectiveness=65, Rebellion Activity=Active Threat, Imperial Conquest Stage currentIndex=2

12. Name: Neimodian Patrol Station, status: active
    Custom fields: Territory Classification=Outer Rim Occupied, Stormtrooper Garrison Size=7000, Imperial Compliance Rating=95, Tarkin Doctrine Adherence=93, TIE Fighter Squadron Count=40, Monthly Tribute=7900000, Last Imperial Inspection=2024-01-03, Planet Destruction Threat Level=None, Sith Lord Assignment="Trade Liaison", Detention Center Capacity=1500, Propaganda Effectiveness=90, Rebellion Activity=None Detected, Imperial Conquest Stage currentIndex=3

13. Name: Endor Forest Base, status: inactive
    Custom fields: Territory Classification=Outer Rim Occupied, Stormtrooper Garrison Size=2000, Imperial Compliance Rating=40, Tarkin Doctrine Adherence=30, TIE Fighter Squadron Count=15, Monthly Tribute=1800000, Last Imperial Inspection=2023-12-15, Planet Destruction Threat Level=High, Sith Lord Assignment="Field Commander", Detention Center Capacity=500, Propaganda Effectiveness=25, Rebellion Activity=Active Threat, Imperial Conquest Stage currentIndex=0

14. Name: Kamino Cloning Facility, status: active
    Custom fields: Territory Classification=Research Facility, Stormtrooper Garrison Size=8000, Imperial Compliance Rating=100, Tarkin Doctrine Adherence=99, TIE Fighter Squadron Count=55, Monthly Tribute=14000000, Last Imperial Inspection=2024-01-04, Planet Destruction Threat Level=None, Sith Lord Assignment="Emperor Palpatine", Detention Center Capacity=3000, Propaganda Effectiveness=98, Rebellion Activity=None Detected, Imperial Conquest Stage currentIndex=4

15. Name: Alderaan Sector Command, status: active
    Custom fields: Territory Classification=Core World, Stormtrooper Garrison Size=20000, Imperial Compliance Rating=85, Tarkin Doctrine Adherence=80, TIE Fighter Squadron Count=100, Monthly Tribute=25000000, Last Imperial Inspection=2023-12-18, Planet Destruction Threat Level=Death Star Target, Sith Lord Assignment="Grand Moff Tarkin", Detention Center Capacity=5000, Propaganda Effectiveness=92, Rebellion Activity=Organized Cells, Imperial Conquest Stage currentIndex=2

Confirm when all 7 territories are created.
```

---

### Step 7 of 10 — Contacts

```
I'm continuing setup for Galactic Empire Corporate Division. 15 territories are created. Now create 20 contacts.

1. Grand Moff Tarkin — Commander at Coruscant Imperial Palace
2. Darth Vader — Dark Lord of the Sith (company: Imperial Command)
3. Emperor Palpatine — Supreme Leader at Coruscant Imperial Palace
4. Moff Jerjerrod — Death Star Commander at Death Star Space Station
5. Admiral Piett — Executor Captain (company: Imperial Fleet)
6. General Veers — Ground Operations (company: Imperial Army)
7. Director Krennic — Death Star Project at Scarif Military Installation
8. Captain Needa — Star Destroyer Commander (company: Imperial Navy)
9. Leia Organa's Double — Infiltrator (company: Empire)
10. Count Dooku — Military Strategist (company: Empire)
11. General Grievous — Military Offensive (company: Empire Command)
12. Jango Fett — Bounty Operations (company: Empire)
13. Boba Fett — Enforcement (company: Outer Rim)
14. Thrawn — Imperial Navy (company: Unknown Base)
15. Starkiller — Dark Force (company: Unknown)
16. Ysanne Isard — Intelligence (company: Intelligence HQ)
17. Praji — Storm Commander at Coruscant Imperial Palace
18. Veers Clone — Field Commander (company: Ground Operations)
19. Tion Medon — Occupation Governor (company: Pau City)
20. Lott Dod — Trade Federation (company: Corporate Offices)

Confirm when all 20 contacts are created.
```

---

### Step 8 of 10 — Opportunities (Imperial Operations)

```
I'm continuing setup for Galactic Empire Corporate Division. Now create 15 Imperial operations as Opportunities. Map Conquest Progress index: 0=prospecting, 1=qualification, 2=proposal, 3=negotiation, 4=closed_won.

1. Name: Destroy Alderaan, customer: Alderaan Sector Command, stage: proposal
   Custom fields: Operation Type=Territory Conquest, Destruction Method=Orbital Bombardment, Assigned Sith Lord=Palpatine, Imperial Efficiency Score=95, Conquest Progress currentIndex=2

2. Name: Establish Bespin Control, customer: Cloud City Bespin, stage: prospecting
   Custom fields: Operation Type=Territory Conquest, Destruction Method=Ground Assault, Assigned Sith Lord=Vader, Imperial Efficiency Score=88, Conquest Progress currentIndex=0

3. Name: Build Death Star II, customer: Death Star Space Station, stage: qualification
   Custom fields: Operation Type=Military Build-up, Destruction Method=Ground Assault, Assigned Sith Lord=Palpatine, Imperial Efficiency Score=92, Conquest Progress currentIndex=1

4. Name: Suppress Endor Rebellion, customer: Endor Forest Base, stage: qualification
   Custom fields: Operation Type=Detention Operations, Destruction Method=Ground Assault, Assigned Sith Lord=Inquisitors, Imperial Efficiency Score=45, Conquest Progress currentIndex=1

5. Name: Capture Luke Skywalker, customer: Tatooine Spaceport, stage: prospecting
   Custom fields: Operation Type=Territory Conquest, Destruction Method=Espionage, Assigned Sith Lord=Vader, Imperial Efficiency Score=60, Conquest Progress currentIndex=0

6. Name: Construct Imperial City, customer: Coruscant Imperial Palace, stage: qualification
   Custom fields: Operation Type=Territory Conquest, Destruction Method=Ground Assault, Assigned Sith Lord=Palpatine, Imperial Efficiency Score=78, Conquest Progress currentIndex=1

7. Name: Mass Production TIE Fighters, customer: Geonosis War Factory, stage: qualification
   Custom fields: Operation Type=Military Build-up, Destruction Method=Ground Assault, Assigned Sith Lord=Unknown, Imperial Efficiency Score=85, Conquest Progress currentIndex=1

8. Name: Subjugate Outer Rim, customer: Bothawui Occupied Sector, stage: prospecting
   Custom fields: Operation Type=Territory Conquest, Destruction Method=Blockade, Assigned Sith Lord=Vader, Imperial Efficiency Score=72, Conquest Progress currentIndex=0

9. Name: Establish Order 66 Protocol, customer: Kamino Cloning Facility, stage: closed_won
   Custom fields: Operation Type=Detention Operations, Destruction Method=Espionage, Assigned Sith Lord=Palpatine, Imperial Efficiency Score=99, Conquest Progress currentIndex=4

10. Name: Purge Jedi Order, customer: Mustafar Research Facility, stage: closed_won
    Custom fields: Operation Type=Territory Conquest, Destruction Method=Ground Assault, Assigned Sith Lord=Vader, Imperial Efficiency Score=98, Conquest Progress currentIndex=4

11. Name: Control Coruscant Population, customer: Coruscant Imperial Palace, stage: negotiation
    Custom fields: Operation Type=Propaganda Campaign, Destruction Method=Blockade, Assigned Sith Lord=Palpatine, Imperial Efficiency Score=91, Conquest Progress currentIndex=3

12. Name: Obtain Ancient Sith Artifacts, customer: Kessel Mining Colony, stage: qualification
    Custom fields: Operation Type=Military Build-up, Destruction Method=Espionage, Assigned Sith Lord=Vader, Imperial Efficiency Score=55, Conquest Progress currentIndex=1

13. Name: Establish Rule on Tatooine, customer: Tatooine Spaceport, stage: proposal
    Custom fields: Operation Type=Territory Conquest, Destruction Method=Ground Assault, Assigned Sith Lord=Unknown, Imperial Efficiency Score=68, Conquest Progress currentIndex=2

14. Name: Capture Rebellion Leadership, customer: Naboo Royal Palace, stage: prospecting
    Custom fields: Operation Type=Territory Conquest, Destruction Method=Ground Assault, Assigned Sith Lord=Vader, Imperial Efficiency Score=50, Conquest Progress currentIndex=0

15. Name: Galactic Supremacy Consolidation, customer: Ord Mantell Garrison, stage: proposal
    Custom fields: Operation Type=Territory Conquest, Destruction Method=Ground Assault, Assigned Sith Lord=Palpatine, Imperial Efficiency Score=89, Conquest Progress currentIndex=2

Confirm when all 15 opportunities are created.
```

---

### Step 9 of 10 — Activities

```
I'm continuing setup for Galactic Empire Corporate Division. Now create 18 activities. Map: "Completed" → completed, "Scheduled" → pending.

1. Subject: Tarkin Doctrine Briefing, type: meeting, duration: 90 min, date: 2024-01-15, status: pending, notes: "Discipline enforcement training"
2. Subject: Vader's Force Interrogation, type: meeting, duration: 60 min, date: 2024-01-16, status: completed, notes: "Prisoner interrogation"
3. Subject: TIE Fighter Operations, type: call, duration: 120 min, date: 2024-01-18, status: pending, notes: "Fighter deployment coordination"
4. Subject: Alderaan Bombardment Coordination, type: email, duration: 240 min, date: 2024-01-19, status: completed, notes: "Weapons targeting"
5. Subject: Stormtrooper Deployment, type: meeting, duration: 60 min, date: 2024-01-22, status: pending, notes: "Garrison assignment"
6. Subject: Death Star Status Update, type: email, duration: 20 min, date: 2024-01-23, status: completed, notes: "Technical assessment"
7. Subject: Sith Training Session, type: meeting, duration: 120 min, date: 2024-01-24, status: pending, notes: "Force user instruction"
8. Subject: Propaganda Broadcast, type: call, duration: 30 min, date: 2024-01-25, status: completed, notes: "Media coordination"
9. Subject: Bespin Control Establishment, type: meeting, duration: 90 min, date: 2024-01-29, status: pending, notes: "Territory takeover"
10. Subject: Death Star II Construction, type: email, duration: 45 min, date: 2024-01-30, status: completed, notes: "Project status"
11. Subject: Endor Rebellion Suppression, type: meeting, duration: 120 min, date: 2024-02-01, status: pending, notes: "Military strategy"
12. Subject: Jedi Order Purge Planning, type: meeting, duration: 90 min, date: 2024-02-02, status: completed, notes: "Eradication protocol"
13. Subject: Imperial City Construction, type: call, duration: 60 min, date: 2024-02-05, status: pending, notes: "Infrastructure development"
14. Subject: Outer Rim Subjugation, type: meeting, duration: 90 min, date: 2024-02-06, status: completed, notes: "Conquest strategy"
15. Subject: Order 66 Implementation, type: email, duration: 30 min, date: 2024-02-08, status: completed, notes: "Protocol activation"
16. Subject: Coruscant Population Control, type: meeting, duration: 120 min, date: 2024-02-09, status: pending, notes: "Social engineering"
17. Subject: Tatooine Rule Establishment, type: call, duration: 75 min, date: 2024-02-12, status: completed, notes: "Regional governance"
18. Subject: Galactic Supremacy Planning, type: meeting, duration: 90 min, date: 2024-02-15, status: pending, notes: "Final conquest"

Confirm when all 18 activities are created.
```

---

### Step 10 of 10 — CustomRecords, Orders, Invoices & Templates

```
I'm continuing setup for Galactic Empire Corporate Division. This is the final step.

**CustomRecords (use bulkCreateCustomRecords):**
1. name: "Star Destroyer Command Bridge Console", type: hardware, status: active, serialNumber: SD-ISD-CMD-001, customer: Coruscant Imperial Palace
2. name: "Death Star Superlaser Control System", type: hardware, status: active, serialNumber: DS-SLS-001, customer: Death Star Space Station
3. name: "AT-AT Walker Fleet (12 units)", type: vehicle, status: active, serialNumber: ATAT-HBH-012
4. name: "TIE Fighter Squadron (24 units)", type: spacecraft, status: active, serialNumber: TIE-SQ-024, customer: Tatooine Spaceport
5. name: "Imperial Encryption Relay Station", type: hardware, status: active, serialNumber: ENC-COR-007, customer: Coruscant Imperial Palace
6. name: "Stormtrooper Armor Stockpile (500 sets)", type: inventory, status: active, serialNumber: ST-ARM-500
7. name: "Planetary Shield Generator — Hoth Grade", type: hardware, status: maintenance, serialNumber: SHD-PLN-001
8. name: "Imperial Census Data Terminal", type: hardware, status: active, serialNumber: CDT-NAB-001, customer: Naboo Royal Palace
9. name: "Thermal Detonator Reserve (Classified)", type: inventory, status: inactive, serialNumber: TD-RES-CLF
10. name: "Probe Droid Deployment Pod (x20)", type: equipment, status: active, serialNumber: PD-POD-020

**Orders:**
1. customer: Coruscant Imperial Palace, name: "Imperial Infrastructure Expansion Pack", status: DELIVERED, totalAmount: 8500000, currency: USD
2. customer: Geonosis War Factory, name: "Cold Climate Military Equipment Bundle", status: CONFIRMED, totalAmount: 3200000, currency: USD
3. customer: Tatooine Spaceport, name: "Desert Patrol Vehicle Fleet", status: SHIPPED, totalAmount: 1800000, currency: USD
4. customer: Naboo Royal Palace, name: "Administrative Control System Install", status: DRAFT, totalAmount: 950000, currency: USD
5. customer: Mustafar Research Facility, name: "Volcanic Heat Shield Equipment", status: CANCELLED, totalAmount: 420000, currency: USD
6. customer: Cloud City Bespin, name: "Carbon Freeze Chamber Upgrade", status: DELIVERED, totalAmount: 2700000, currency: USD

**Invoices:**
1. customer: Coruscant Imperial Palace, status: PAID, totalAmount: 8500000, currency: USD, paymentTerms: NET-30
2. customer: Geonosis War Factory, status: SENT, totalAmount: 3200000, currency: USD, paymentTerms: NET-60
3. customer: Tatooine Spaceport, status: PAID, totalAmount: 1800000, currency: USD, paymentTerms: NET-30
4. customer: Naboo Royal Palace, status: DRAFT, totalAmount: 950000, currency: USD, paymentTerms: NET-60
5. customer: Mustafar Research Facility, status: OVERDUE, totalAmount: 420000, currency: USD, paymentTerms: NET-30
6. customer: Cloud City Bespin, status: PAID, totalAmount: 2700000, currency: USD, paymentTerms: NET-30

**Document Templates (use createDocumentTemplate for each):**
1. name: "Imperial Sector Report", templateType: slide_deck, description: "Authoritarian dark-theme conquest briefing for Grand Moff review", styleJson: {"layout":"corporate","accentColor":"#B71C1C","backgroundColor":"#0A0A0A","h1Color":"#E0E0E0"}
2. name: "Territory Control Profile", templateType: one_pager, description: "Imperial sector administrative summary for sector governors", styleJson: {"layout":"light","accentColor":"#B71C1C","includeCustomFields":true}
3. name: "Imperial Territory Export", templateType: csv_export, description: "Galactic territory control export with compliance and subjugation metrics", styleJson: {"includeFields":["name","status","planet_class","population","compliance_level","strategic_value"]}

**Notification Templates (use createNotificationTemplate for each):**
1. name: "New Operation Initiated", notificationType: OPPORTUNITY_CREATED, isActive: true
   subject: "Operation Order: {{opportunityName}} — {{customerName}}"
   body: "A new Imperial operation has been authorized.\n\nSector: {{customerName}}\nOperation: {{opportunityName}}\nPhase: {{stage}}\nForce Deployed: {{amount}}\n\nView briefing: {{link}}"

2. name: "Territory Status Change", notificationType: CUSTOMER_UPDATED, isActive: true
   subject: "Sector Update: {{customerName}}"
   body: "Imperial sector status has changed.\n\nSector: {{customerName}}\nUpdated by: {{assignee}}\n\nReview: {{link}}"

3. name: "Supply Requisition", notificationType: ORDER_CREATED, isActive: true
   subject: "Imperial Supply Order: {{customerName}} — {{amount}} Credits"
   body: "Supply requisition processed.\n\nSector: {{customerName}}\nSupplies: {{opportunityName}}\nTotal: {{amount}} Credits\nArrival: {{dueDate}}\n\n{{link}}"

Confirm when all customRecords, orders, invoices, document templates, and notification templates are created. Galactic Empire setup complete!
```

---

## Bounty Hunters Guild — 10 Steps

### Step 1 of 10 — Customer Custom Fields

```
I'm setting up a CRM workspace for the "Bounty Hunters Guild". In this step, please create the custom fields for Customers (Bounty Hunters).

Create the following custom fields for the Customer entity:
1. "Hunter Codename" (text field, required) — display name
2. "Bounty Success Rate (%)" (percentage field) — contract completion rate
3. "Specialization" (multiselect field) — options: Capture Alive, Termination, Recovery, Espionage, Droid Hunting, Force Sensitivity
4. "Favorite Weapon" (select field) — options: Blaster Rifle, Mandalorian Armor, Lightsaber, Carbonite Freezer, Poison, Explosives
5. "Reputation Score" (number field, 0-100) — underworld standing
6. "Kills on Record" (number field) — confirmed terminations
7. "Current Bounty Value" (currency field) — on their own head
8. "Mandalorian Clan Member" (boolean field) — Mandalorian connection
9. "Jedi Hunter" (boolean field) — Force user specialist
10. "Owns Starship" (boolean field) — has transportation
11. "Sabacc Debt" (currency field) — gambling obligations
12. "Previous Employers" (textarea field) — job history
13. "Bounty Taking Progress" (workflow field) — milestones: Contact, Accept, Hunt, Apprehend, Payment Received

Confirm when all 13 customer custom fields have been created.
```

---

### Step 2 of 10 — Opportunity Custom Fields

```
I'm continuing setup for the Bounty Hunters Guild. Customer custom fields are done. Now create the custom fields for the Opportunity entity (Bounty Contracts).

Create the following custom fields for the Opportunity entity:
1. "Target Type" (select field, required) — options: Jedi Fugitive, Rebel, Criminal, Corporate Executive, Droid, Wanted Beast
2. "Target Threat Level" (select field) — options: Low, Medium, High, Legendary, Force Sensitive
3. "Bounty Amount (Credits)" (currency field) — contract reward
4. "Capture vs Termination" (select field) — options: Capture Alive, Termination Preferred, Either
5. "Hunter Assignment" (select field) — which hunter to assign
6. "Contract Progress" (workflow field) — milestones: Posted, Assigned, In Pursuit, Target Acquired, Payment Verified

Confirm when all 6 opportunity custom fields have been created.
```

---

### Step 3 of 10 — Calculated Fields & Business Policies

```
I'm continuing setup for the Bounty Hunters Guild. Custom fields are done. Now create the calculated fields and business policies.

**Calculated Fields:**
1. For Customer — "Hunter Rating":
   Expression: (Bounty_Success_Rate * 0.7) + (Reputation_Score * 0.3) + (if Jedi_Hunter then 15 else 0)

2. For Opportunity — "Contract Progress %":
   Expression: if Contract_Progress exists then (Contract_Progress.currentIndex + 1) * 20 else 0

**Business Policies (Rego syntax — each condition on a separate line):**
1. DENY — "Legendary Target Requires Experienced Hunter":
   input.entity.bounty_success_rate < 60
   input.entity.target_threat_level == "Legendary"

2. WARN — "Force-Sensitive Hunter Assigned to Jedi Contract":
   input.entity.jedi_hunter == true
   input.entity.target_type == "Jedi Fugitive"

3. DENY — "No Payment Before Target Acquisition":
   input.entity.contract_progress < 3
   input.entity.payment_processed == true

Confirm when calculated fields and policies are created.
```

---

### Step 4 of 10 — Tenant Settings & Branding

```
I'm continuing setup for the Bounty Hunters Guild. Now update the tenant settings with the following branding:

- Company Name: Bounty Hunters Guild
- Logo URL: https://static.wikia.nocookie.net/swrp/images/9/91/BHG.svg/revision/latest?cb=20130720230746
- Primary Color: #8B0000
- Secondary Color: #DAA520
- Website: https://bountyhunters.underworld
- Bio: "The Bounty Hunters Guild provides the galaxy's most dangerous freelance operatives with high-value contracts. We connect elite hunters with clients seeking recovery, elimination, or capture of customRecords. Reputation and credits are everything in this business."

Confirm when tenant settings have been updated.
```

---

### Step 5 of 10 — Customers (Hunters) 1–8

```
I'm continuing setup for the Bounty Hunters Guild. Now create the first 8 bounty hunters.

1. Name: Boba Fett, status: active
   Custom fields: Hunter Codename="The Mandalorian", Bounty Success Rate=95, Specialization=[Capture Alive, Termination], Favorite Weapon=Mandalorian Armor, Reputation Score=89, Kills on Record=156, Current Bounty Value=500000, Mandalorian Clan Member=true, Jedi Hunter=false, Owns Starship=true, Sabacc Debt=25000, Previous Employers="Jabba, Empire, Others", Bounty Taking Progress currentIndex=4

2. Name: Jango Fett, status: active
   Custom fields: Hunter Codename="The Clone Master", Bounty Success Rate=97, Specialization=[Termination, Recovery], Favorite Weapon=Blaster Rifle, Reputation Score=92, Kills on Record=203, Current Bounty Value=250000, Mandalorian Clan Member=true, Jedi Hunter=false, Owns Starship=true, Sabacc Debt=0, Previous Employers="Tyrannos, Empire, Viceroy", Bounty Taking Progress currentIndex=4

3. Name: Cad Bane, status: active
   Custom fields: Hunter Codename="The Collector", Bounty Success Rate=88, Specialization=[Capture Alive, Espionage, Droid Hunting], Favorite Weapon=Blaster Rifle, Reputation Score=85, Kills on Record=67, Current Bounty Value=120000, Mandalorian Clan Member=false, Jedi Hunter=true, Owns Starship=true, Sabacc Debt=85000, Previous Employers="Darth Maul, Sidious, Corporate", Bounty Taking Progress currentIndex=3

4. Name: Aurra Sing, status: active
   Custom fields: Hunter Codename="The Force Seeker", Bounty Success Rate=82, Specialization=[Termination], Favorite Weapon=Lightsaber, Reputation Score=78, Kills on Record=43, Current Bounty Value=80000, Mandalorian Clan Member=false, Jedi Hunter=true, Owns Starship=true, Sabacc Debt=45000, Previous Employers="Grievous, Bounty Collective", Bounty Taking Progress currentIndex=2

5. Name: Bossk, status: active
   Custom fields: Hunter Codename="The Trandoshan", Bounty Success Rate=85, Specialization=[Capture Alive, Termination], Favorite Weapon=Blaster Rifle, Reputation Score=81, Kills on Record=104, Current Bounty Value=150000, Mandalorian Clan Member=false, Jedi Hunter=false, Owns Starship=true, Sabacc Debt=55000, Previous Employers="Bounty Collective, Various Crimelords", Bounty Taking Progress currentIndex=2

6. Name: Durge, status: active
   Custom fields: Hunter Codename="The Immortal", Bounty Success Rate=90, Specialization=[Termination, Droid Hunting], Favorite Weapon=Explosives, Reputation Score=87, Kills on Record=178, Current Bounty Value=200000, Mandalorian Clan Member=false, Jedi Hunter=false, Owns Starship=true, Sabacc Debt=0, Previous Employers="Dooku, Grievous", Bounty Taking Progress currentIndex=4

7. Name: IG-88, status: active
   Custom fields: Hunter Codename="Assassin Droid", Bounty Success Rate=91, Specialization=[Termination, Espionage], Favorite Weapon=Blaster Rifle, Reputation Score=84, Kills on Record=256, Current Bounty Value=90000, Mandalorian Clan Member=false, Jedi Hunter=false, Owns Starship=true, Sabacc Debt=0, Previous Employers="Jabba, Bounty Collective", Bounty Taking Progress currentIndex=3

8. Name: Zam Wesell, status: prospect
   Custom fields: Hunter Codename="The Shapeshifter", Bounty Success Rate=79, Specialization=[Espionage, Recovery, Capture Alive], Favorite Weapon=Poison, Reputation Score=76, Kills on Record=38, Current Bounty Value=60000, Mandalorian Clan Member=false, Jedi Hunter=false, Owns Starship=true, Sabacc Debt=30000, Previous Employers="Bounty Collective, Private Clients", Bounty Taking Progress currentIndex=2

Confirm when all 8 hunters are created.
```

---

### Step 6 of 10 — Customers (Hunters) 9–15

```
I'm continuing setup for the Bounty Hunters Guild. First 8 hunters done. Now create hunters 9–15.

9. Name: Embo, status: active
   Custom fields: Hunter Codename="The Tracker", Bounty Success Rate=84, Specialization=[Capture Alive, Droid Hunting], Favorite Weapon=Blaster Rifle, Reputation Score=80, Kills on Record=71, Current Bounty Value=110000, Mandalorian Clan Member=false, Jedi Hunter=false, Owns Starship=true, Sabacc Debt=20000, Previous Employers="Bounty Collective, Various Clients", Bounty Taking Progress currentIndex=1

10. Name: Latts Razzi, status: active
    Custom fields: Hunter Codename="The Tech Specialist", Bounty Success Rate=81, Specialization=[Espionage, Droid Hunting, Termination], Favorite Weapon=Blaster Rifle, Reputation Score=77, Kills on Record=52, Current Bounty Value=95000, Mandalorian Clan Member=true, Jedi Hunter=false, Owns Starship=true, Sabacc Debt=40000, Previous Employers="Bounty Collective, Crimson Corsairs", Bounty Taking Progress currentIndex=2

11. Name: Oked Goofta, status: prospect
    Custom fields: Hunter Codename="The Novice", Bounty Success Rate=73, Specialization=[Recovery, Capture Alive], Favorite Weapon=Blaster Rifle, Reputation Score=68, Kills on Record=29, Current Bounty Value=40000, Mandalorian Clan Member=false, Jedi Hunter=false, Owns Starship=true, Sabacc Debt=65000, Previous Employers="Bounty Collective, Independent Work", Bounty Taking Progress currentIndex=0

12. Name: Sugi, status: active
    Custom fields: Hunter Codename="The Mandalorian Ally", Bounty Success Rate=83, Specialization=[Capture Alive, Termination], Favorite Weapon=Mandalorian Armor, Reputation Score=79, Kills on Record=55, Current Bounty Value=75000, Mandalorian Clan Member=true, Jedi Hunter=false, Owns Starship=true, Sabacc Debt=15000, Previous Employers="Bounty Collective, Mandalorian Clan", Bounty Taking Progress currentIndex=1

13. Name: The Mandalorian (Din Djarin), status: active
    Custom fields: Hunter Codename="The Lone Wolf", Bounty Success Rate=94, Specialization=[Capture Alive, Recovery], Favorite Weapon=Mandalorian Armor, Reputation Score=91, Kills on Record=122, Current Bounty Value=180000, Mandalorian Clan Member=true, Jedi Hunter=false, Owns Starship=true, Sabacc Debt=0, Previous Employers="Bounty Collective, Guild, Independent", Bounty Taking Progress currentIndex=4

14. Name: Fennec Shand, status: active
    Custom fields: Hunter Codename="The Spymaster", Bounty Success Rate=92, Specialization=[Espionage, Recovery, Termination], Favorite Weapon=Blaster Rifle, Reputation Score=88, Kills on Record=98, Current Bounty Value=160000, Mandalorian Clan Member=false, Jedi Hunter=false, Owns Starship=true, Sabacc Debt=10000, Previous Employers="Cad Bane, Bounty Collective, Empire", Bounty Taking Progress currentIndex=3

15. Name: Black Krrsantan, status: active
    Custom fields: Hunter Codename="The Wookiee Enforcer", Bounty Success Rate=86, Specialization=[Termination, Capture Alive], Favorite Weapon=Explosives, Reputation Score=83, Kills on Record=87, Current Bounty Value=140000, Mandalorian Clan Member=false, Jedi Hunter=false, Owns Starship=true, Sabacc Debt=35000, Previous Employers="Leia, Bounty Collective, Various Clients", Bounty Taking Progress currentIndex=2

Confirm when all 7 hunters are created.
```

---

### Step 7 of 10 — Contacts

```
I'm continuing setup for the Bounty Hunters Guild. 15 hunters are created. Now create 20 contacts (clients, guild officials, and notable figures).

1. Jabba the Hutt — Crime Lord (company: Tatooine Palace)
2. Darth Vader — Imperial Representative (company: Imperial Command)
3. Leia Organa — Rebel Contact (company: Rebellion Base)
4. Han Solo — Smuggler Contact (company: Cantina)
5. Maz Kanata — Information Broker (company: Takodana Castle)
6. Peli Motto — Guild Administrator (company: Mos Pelgo)
7. Cobb Vanth — Tatooine Sheriff (company: Tatooine)
8. The Armorer — Mandalorian Representative (company: Forge)
9. Cara Dune — Rebel Connection (company: New Republic)
10. Greef Karga — Guild Officer (company: Guild Hall)
11. Kuiil — Salvage Expert (company: Compound)
12. IG-11 — Protocol Droid (company: Guild)
13. Porg — Mysterious Contact (company: Unknown)
14. Yaddle — Force Council (company: Unknown)
15. Lando Calrissian — Fence/Handler at Bespin Cloud City
16. Qi'ra — Spice Runner (company: Kessel)
17. Rio Durant — Militia Leader (company: Tatooine)
18. Cad Bane — Rival Hunter at Cad Bane (company: Outer Rim)
19. Grogu — CustomRecord (company: Unknown)
20. Vanth Cargo Master — Logistics (company: Guild Hall)

Confirm when all 20 contacts are created.
```

---

### Step 8 of 10 — Opportunities (Bounty Contracts)

```
I'm continuing setup for the Bounty Hunters Guild. Now create 15 bounty contracts as Opportunities. Map Contract Progress index: 0=prospecting, 1=qualification, 2=proposal, 3=negotiation, 4=closed_won.

1. Name: Capture Luke Skywalker, customer: Boba Fett, stage: proposal, amount: 50000
   Custom fields: Target Type=Jedi Fugitive, Target Threat Level=Force Sensitive, Bounty Amount=50000, Capture vs Termination=Capture Alive, Contract Progress currentIndex=2

2. Name: Eliminate Rebel Cell Commander, customer: Cad Bane, stage: qualification, amount: 25000
   Custom fields: Target Type=Rebel, Target Threat Level=High, Bounty Amount=25000, Capture vs Termination=Termination Preferred, Contract Progress currentIndex=1

3. Name: Retrieve Stolen Artifact, customer: Bossk, stage: proposal, amount: 15000
   Custom fields: Target Type=Corporate Executive, Target Threat Level=Medium, Bounty Amount=15000, Capture vs Termination=Either, Contract Progress currentIndex=2

4. Name: Hunt Escaped Wookiee, customer: Embo, stage: qualification, amount: 12000
   Custom fields: Target Type=Wanted Beast, Target Threat Level=High, Bounty Amount=12000, Capture vs Termination=Capture Alive, Contract Progress currentIndex=1

5. Name: Find Padme Amidala's Hidden Child, customer: Aurra Sing, stage: prospecting, amount: 75000
   Custom fields: Target Type=Jedi Fugitive, Target Threat Level=High, Bounty Amount=75000, Capture vs Termination=Capture Alive, Contract Progress currentIndex=0

6. Name: Capture Obi-Wan Kenobi, customer: Boba Fett, stage: proposal, amount: 100000
   Custom fields: Target Type=Jedi Fugitive, Target Threat Level=Legendary, Bounty Amount=100000, Capture vs Termination=Capture Alive, Contract Progress currentIndex=2

7. Name: Eliminate Crime Syndicate Lieutenant, customer: Fennec Shand, stage: negotiation, amount: 20000
   Custom fields: Target Type=Criminal, Target Threat Level=Medium, Bounty Amount=20000, Capture vs Termination=Termination Preferred, Contract Progress currentIndex=3

8. Name: Retrieve Carbonite Han Solo, customer: The Mandalorian (Din Djarin), stage: proposal, amount: 30000
   Custom fields: Target Type=Criminal, Target Threat Level=High, Bounty Amount=30000, Capture vs Termination=Either, Contract Progress currentIndex=2

9. Name: Hunt Force-Sensitive Child, customer: Aurra Sing, stage: proposal, amount: 40000
   Custom fields: Target Type=Jedi Fugitive, Target Threat Level=High, Bounty Amount=40000, Capture vs Termination=Capture Alive, Contract Progress currentIndex=2

10. Name: Retrieve Imperial Data Vault, customer: IG-88, stage: qualification, amount: 35000
    Custom fields: Target Type=Corporate Executive, Target Threat Level=High, Bounty Amount=35000, Capture vs Termination=Either, Contract Progress currentIndex=1

11. Name: Eliminate Resistance Leader, customer: Cad Bane, stage: negotiation, amount: 18000
    Custom fields: Target Type=Rebel, Target Threat Level=Medium, Bounty Amount=18000, Capture vs Termination=Termination Preferred, Contract Progress currentIndex=3

12. Name: Capture Leia Organa, customer: Boba Fett, stage: prospecting, amount: 60000
    Custom fields: Target Type=Rebel, Target Threat Level=High, Bounty Amount=60000, Capture vs Termination=Capture Alive, Contract Progress currentIndex=0

13. Name: Hunt Rogue Droid Army, customer: IG-88, stage: qualification, amount: 22000
    Custom fields: Target Type=Droid, Target Threat Level=High, Bounty Amount=22000, Capture vs Termination=Termination Preferred, Contract Progress currentIndex=1

14. Name: Retrieve Jedi Temple Vault Contents, customer: The Mandalorian (Din Djarin), stage: proposal, amount: 150000
    Custom fields: Target Type=Jedi Fugitive, Target Threat Level=Legendary, Bounty Amount=150000, Capture vs Termination=Either, Contract Progress currentIndex=2

15. Name: Eliminate Death Star Director, customer: Boba Fett, stage: prospecting, amount: 85000
    Custom fields: Target Type=Corporate Executive, Target Threat Level=Legendary, Bounty Amount=85000, Capture vs Termination=Termination Preferred, Contract Progress currentIndex=0

Confirm when all 15 contracts are created.
```

---

### Step 9 of 10 — Activities

```
I'm continuing setup for the Bounty Hunters Guild. Now create 18 activities. Map: "Completed" → completed, "Scheduled" → pending.

1. Subject: Contract Posting - Luke Skywalker, type: email, duration: 15 min, date: 2024-01-15, status: completed, notes: "Guild announcement"
2. Subject: Boba Fett Briefing, type: meeting, duration: 60 min, date: 2024-01-16, status: pending, notes: "Contract assignment"
3. Subject: Rebel Cell Pursuit, type: call, duration: 480 min, date: 2024-01-18, status: completed, notes: "Hunting operation"
4. Subject: Payment Processing - Fennec Shand, type: email, duration: 20 min, date: 2024-01-19, status: completed, notes: "Contract payout"
5. Subject: Reputation Update - Boba Fett, type: call, duration: 10 min, date: 2024-01-22, status: completed, notes: "Success confirmation"
6. Subject: Target Acquisition - Han Solo, type: meeting, duration: 30 min, date: 2024-01-23, status: pending, notes: "Confirmation meeting"
7. Subject: Sabacc Debt Settlement - Cad Bane, type: meeting, duration: 90 min, date: 2024-01-24, status: completed, notes: "Gambling settlement"
8. Subject: Guild Disciplinary Hearing, type: call, duration: 60 min, date: 2024-01-25, status: pending, notes: "Conduct review"
9. Subject: Contract Posting - Obi-Wan Kenobi, type: email, duration: 15 min, date: 2024-01-29, status: completed, notes: "High-value contract"
10. Subject: Hunter Briefing - Din Djarin, type: meeting, duration: 60 min, date: 2024-01-30, status: pending, notes: "Mission briefing"
11. Subject: Jedi Pursuit Operation, type: call, duration: 720 min, date: 2024-02-01, status: completed, notes: "Active hunt"
12. Subject: Payment Processing - IG-88, type: email, duration: 20 min, date: 2024-02-02, status: completed, notes: "Successful payout"
13. Subject: Reputation Update - Fennec Shand, type: call, duration: 10 min, date: 2024-02-05, status: completed, notes: "Elite status"
14. Subject: Force-Sensitive Target Acquisition, type: meeting, duration: 30 min, date: 2024-02-06, status: pending, notes: "Complex case"
15. Subject: Sabacc Debt Settlement - Bossk, type: meeting, duration: 90 min, date: 2024-02-08, status: completed, notes: "High stakes"
16. Subject: Guild Membership Review, type: call, duration: 60 min, date: 2024-02-09, status: pending, notes: "Annual review"
17. Subject: Contract Posting - Leia Organa, type: email, duration: 15 min, date: 2024-02-12, status: completed, notes: "Major contract"
18. Subject: Final Payment Verification, type: meeting, duration: 30 min, date: 2024-02-15, status: pending, notes: "Contract closure"

Confirm when all 18 activities are created.
```

---

### Step 10 of 10 — CustomRecords, Orders, Invoices & Templates

```
I'm continuing setup for the Bounty Hunters Guild. This is the final step.

**CustomRecords (use bulkCreateCustomRecords):**
1. name: "Slave I — Firespray-31 Patrol Craft", type: spacecraft, status: active, serialNumber: SLV1-BF-001, customer: Boba Fett
2. name: "Beskar Armor Set — Mando Grade", type: armor, status: active, serialNumber: BSK-MANDO-001, customer: The Mandalorian (Din Djarin)
3. name: "IG-88 Combat Chassis Upgrade Module", type: hardware, status: active, serialNumber: IG88-UPGD-003, customer: IG-88
4. name: "Infrared Targeting Goggles (Wookiee-Proof)", type: equipment, status: active, serialNumber: TGT-BOSSK-001, customer: Bossk
5. name: "EE-3 Blaster Carbine — Modified", type: weapon, status: active, serialNumber: EE3-BF-MOD, customer: Boba Fett
6. name: "Whistling Birds Wrist Launcher", type: weapon, status: active, serialNumber: WB-MANDO-001, customer: The Mandalorian (Din Djarin)
7. name: "Encrypted Bounty Datapad Network", type: software, status: active, serialNumber: DPD-GUILD-001
8. name: "Carbonite Freezing Chamber (Portable)", type: equipment, status: maintenance, serialNumber: CARB-PORT-001
9. name: "Fennec Shand's Sniper Rifle Array", type: weapon, status: active, serialNumber: SNP-FEN-001, customer: Fennec Shand
10. name: "Zam Wesell's Changeling Equipment", type: equipment, status: inactive, serialNumber: CHG-ZAM-001, customer: Zam Wesell

**Orders:**
1. customer: Boba Fett, name: "Mandalorian Armor Repair & Upgrade Package", status: DELIVERED, totalAmount: 45000, currency: USD
2. customer: The Mandalorian (Din Djarin), name: "Beskar Forge Commission — Full Set", status: CONFIRMED, totalAmount: 82000, currency: USD
3. customer: IG-88, name: "Combat Chassis Refurbishment Bundle", status: SHIPPED, totalAmount: 28000, currency: USD
4. customer: Bossk, name: "Trandoshan Hunting Gear Upgrade", status: DRAFT, totalAmount: 18500, currency: USD
5. customer: Fennec Shand, name: "Sniper Optics & Stabilizer Set", status: CANCELLED, totalAmount: 12000, currency: USD
6. customer: Black Krrsantan, name: "Medical Reconstruction Parts", status: DELIVERED, totalAmount: 35000, currency: USD

**Invoices:**
1. customer: Boba Fett, status: PAID, totalAmount: 45000, currency: USD, paymentTerms: NET-14
2. customer: The Mandalorian (Din Djarin), status: SENT, totalAmount: 82000, currency: USD, paymentTerms: NET-30
3. customer: IG-88, status: PAID, totalAmount: 28000, currency: USD, paymentTerms: NET-14
4. customer: Bossk, status: DRAFT, totalAmount: 18500, currency: USD, paymentTerms: NET-30
5. customer: Fennec Shand, status: OVERDUE, totalAmount: 12000, currency: USD, paymentTerms: NET-14
6. customer: Black Krrsantan, status: PAID, totalAmount: 35000, currency: USD, paymentTerms: NET-30

**Document Templates (use createDocumentTemplate for each):**
1. name: "Bounty Contract Dossier", templateType: slide_deck, description: "Encrypted dark-theme target briefing for Guild dispatch", styleJson: {"layout":"corporate","accentColor":"#8D6E63","backgroundColor":"#12100E","h1Color":"#FFD54F"}
2. name: "Hunter Profile", templateType: one_pager, description: "Single-page hunter capability summary for client review", styleJson: {"layout":"light","accentColor":"#8D6E63","includeCustomFields":true}
3. name: "Guild Registry Export", templateType: csv_export, description: "Full hunter registry export with success rate and specialization data", styleJson: {"includeFields":["name","status","species","signature_weapon","success_rate","active_contracts"]}

**Notification Templates (use createNotificationTemplate for each):**
1. name: "New Bounty Posted", notificationType: OPPORTUNITY_CREATED, isActive: true
   subject: "New Bounty: {{opportunityName}} — {{amount}} Credits"
   body: "A new bounty contract is available.\n\nPosted by: {{customerName}}\nTarget: {{opportunityName}}\nReward: {{amount}} Credits\nPriority: {{stage}}\n\nAccept contract: {{link}}"

2. name: "Contract Status Update", notificationType: OPPORTUNITY_UPDATED, isActive: true
   subject: "Contract Update: {{opportunityName}} — Stage {{stage}}"
   body: "Bounty contract status updated.\n\nHunter: {{customerName}}\nContract: {{opportunityName}}\nStatus: {{stage}}\nAssigned: {{assignee}}\n\n{{link}}"

3. name: "Payment Processed", notificationType: INVOICE_CREATED, isActive: true
   subject: "Payment Processed: {{customerName}} — {{amount}} Credits"
   body: "Guild payment has been processed.\n\nHunter: {{customerName}}\nContract: {{opportunityName}}\nAmount: {{amount}} Credits\nDue: {{dueDate}}\n\nConfirm receipt: {{link}}"

Confirm when all customRecords, orders, invoices, document templates, and notification templates are created. Bounty Hunters Guild setup complete!
```
