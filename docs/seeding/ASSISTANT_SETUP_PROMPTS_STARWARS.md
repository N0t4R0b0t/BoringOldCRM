# Star Wars Universe - Assistant Setup Prompts

Three fully-configured demo tenants themed around the Star Wars universe with complete feature coverage.

---

## Prompt 1: Rebel Alliance Supply Chain

```
I'm setting up a new CRM workspace for the "Rebel Alliance Supply Chain" - the central logistics and procurement operation managing supplies, weapons, and equipment across all Rebel bases throughout the galaxy.

Please help me set up this workspace completely. Here's what I need:

**Company Branding:**
- Logo: https://static.wikia.nocookie.net/starwars/images/7/71/Redstarbird.svg/revision/latest?cb=20080228205026
- Primary Color: #FF0000
- Secondary Color: #FFD700
- Website: https://rebel-alliance.galaxy
- Bio: "The Rebel Alliance Supply Chain coordinates resources, personnel, and strategic equipment across multiple star systems. We manage procurement from suppliers, track inventory across hidden bases, and ensure operational readiness against the Empire. Our mission: freedom through logistics."

**Custom Fields for Customers (Rebel Suppliers/Allies):**
1. "Faction Status" (select field, required) - options: Core Ally, Regional Partner, Independent Supplier, Sympathizer
2. "Hyperdrive Capability" (boolean field) - can they travel across systems
3. "Sector of Operations" (text field) - which sector they operate in
4. "Credits in Reserve" (currency field) - financial capacity
5. "Alliance Trust Rating" (number field, 0-100) - trust score
6. "Last Supply Run Date" (date field) - when they last delivered
7. "Inventory Level" (percentage field) - current stock availability
8. "Smuggler Network Connection" (boolean field) - have contraband connections
9. "Species Represented" (select field) - options: Human, Wookiee, Twi'lek, Mon Calamari, Ewok, Droid, Other
10. "Base Location" (text field) - where they operate from
11. "Contact Frequency" (multiselect) - options: Encrypted Transmission, Hologram, Dead Drop, Secure Relay
12. "Operation Notes" (textarea field) - classified notes
13. "Rebellion Support Type" (workflow field) - milestones: Recruit, Train, Equip, Deploy, Victorious

**Custom Fields for Opportunities (Supply Missions/Operations):**
1. "Mission Classification" (select field, required) - options: Weapon Procurement, Food/Medical, Tech/Droid, Intelligence, Sabotage, Evacuation
2. "Imperial Threat Level" (select field) - options: Low, Medium, High, Critical - Death Star Proximity
3. "Rebel Operatives Assigned" (multiselect) - options: Luke, Leia, Han, Chewbacca, Lando, Yoda, Obi-Wan
4. "Mission Briefing" (richtext) - full operation details
5. "Rebellion Victory Progress" (workflow field) - milestones: Planning, Infiltration, Execution, Escape, Safe Return

**Calculated Fields:**
1. For Customer: "Strategic Value Score" = (AllianceTrustRating * 2) + (CreditsInReserve / 100000) + (if HyperdrivCapability then 25 else 0)
2. For Opportunity: "Mission Completion %" = if RebelVictoryProgress exists then (RebelVictoryProgress.currentIndex + 1) * 20 else 0

**Business Policies:**
1. DENY policy: Block operations with Imperial Sympathizers (FactionStatus = "Sympathizer" AND MissionClassification = "Sabotage")
2. WARN policy: Warn on Critical threat level missions (ImperialThreatLevel = "Critical - Death Star Proximity")
3. DENY policy: Block supply runs without trust rating above 40 (AllianceTrustRating < 40)

**Sample Data - 15 Suppliers/Allies (Customers):**

1. Mos Eisley Spaceport
   - Faction Status: Independent Supplier
   - Alliance Trust Rating: 85
   - Credits in Reserve: $2.5M
   - Sector of Operations: Tatooine
   - Hyperdrive Capability: false
   - Inventory Level: 78%
   - Smuggler Network Connection: true
   - Species Represented: Human
   - Base Location: Mos Eisley, Tatooine
   - Contact Frequency: Encrypted Transmission, Dead Drop
   - Rebellion Support Type: Deploy (index 3)

2. Bothawui Information Bureau
   - Faction Status: Core Ally
   - Alliance Trust Rating: 95
   - Credits in Reserve: $5.2M
   - Sector of Operations: Bothan Space
   - Hyperdrive Capability: true
   - Inventory Level: 92%
   - Smuggler Network Connection: false
   - Species Represented: Other
   - Base Location: Bothawui
   - Contact Frequency: Encrypted Transmission, Hologram
   - Rebellion Support Type: Victorious (index 4)

3. Wookiee Mining Collective
   - Faction Status: Core Ally
   - Alliance Trust Rating: 90
   - Credits in Reserve: $1.8M
   - Sector of Operations: Kashyyyk
   - Hyperdrive Capability: false
   - Inventory Level: 65%
   - Smuggler Network Connection: false
   - Species Represented: Wookiee
   - Base Location: Kashyyyk
   - Contact Frequency: Secure Relay
   - Rebellion Support Type: Equip (index 2)

4. Mon Calamari Shipyards
   - Faction Status: Core Ally
   - Alliance Trust Rating: 98
   - Credits in Reserve: $8.5M
   - Sector of Operations: Mon Calamari
   - Hyperdrive Capability: true
   - Inventory Level: 88%
   - Smuggler Network Connection: false
   - Species Represented: Mon Calamari
   - Base Location: Mon Calamari
   - Contact Frequency: Hologram, Secure Relay
   - Rebellion Support Type: Victorious (index 4)

5. Ewok Forest Alliance
   - Faction Status: Regional Partner
   - Alliance Trust Rating: 72
   - Credits in Reserve: $450K
   - Sector of Operations: Endor
   - Hyperdrive Capability: false
   - Inventory Level: 45%
   - Smuggler Network Connection: false
   - Species Represented: Ewok
   - Base Location: Bright Tree Village, Endor
   - Contact Frequency: Secure Relay
   - Rebellion Support Type: Train (index 1)

6. Tatooine Desert Traders
   - Faction Status: Regional Partner
   - Alliance Trust Rating: 68
   - Credits in Reserve: $950K
   - Sector of Operations: Tatooine
   - Hyperdrive Capability: false
   - Inventory Level: 55%
   - Smuggler Network Connection: true
   - Species Represented: Human
   - Base Location: Mos Eisley, Tatooine
   - Contact Frequency: Encrypted Transmission, Dead Drop
   - Rebellion Support Type: Recruit (index 0)

7. Dagobah Hidden Repository
   - Faction Status: Core Ally
   - Alliance Trust Rating: 100
   - Credits in Reserve: $3.2M
   - Sector of Operations: Dagobah
   - Hyperdrive Capability: true
   - Inventory Level: 71%
   - Smuggler Network Connection: false
   - Species Represented: Other
   - Base Location: Dagobah
   - Contact Frequency: Hologram
   - Rebellion Support Type: Victorious (index 4)

8. Bespin Cloud City
   - Faction Status: Core Ally
   - Alliance Trust Rating: 87
   - Credits in Reserve: $6.1M
   - Sector of Operations: Bespin
   - Hyperdrive Capability: true
   - Inventory Level: 82%
   - Smuggler Network Connection: true
   - Species Represented: Human
   - Base Location: Cloud City, Bespin
   - Contact Frequency: Hologram, Secure Relay
   - Rebellion Support Type: Equip (index 2)

9. Naboo Royal Guard
   - Faction Status: Regional Partner
   - Alliance Trust Rating: 75
   - Credits in Reserve: $2.8M
   - Sector of Operations: Naboo
   - Hyperdrive Capability: true
   - Inventory Level: 79%
   - Smuggler Network Connection: false
   - Species Represented: Human
   - Base Location: Naboo
   - Contact Frequency: Hologram
   - Rebellion Support Type: Train (index 1)

10. Outer Rim Mercenaries
    - Faction Status: Independent Supplier
    - Alliance Trust Rating: 55
    - Credits in Reserve: $1.2M
    - Sector of Operations: Outer Rim
    - Hyperdrive Capability: true
    - Inventory Level: 48%
    - Smuggler Network Connection: true
    - Species Represented: Other
    - Base Location: Mos Eisley, Tatooine
    - Contact Frequency: Encrypted Transmission, Dead Drop
    - Rebellion Support Type: Recruit (index 0)

11. Cantina Black Market
    - Faction Status: Sympathizer
    - Alliance Trust Rating: 25
    - Credits in Reserve: $300K
    - Sector of Operations: Various
    - Hyperdrive Capability: false
    - Inventory Level: 22%
    - Smuggler Network Connection: true
    - Species Represented: Other
    - Base Location: Mos Eisley, Tatooine
    - Contact Frequency: Dead Drop
    - Rebellion Support Type: Recruit (index 0)

12. Kessel Run Spice Traders
    - Faction Status: Independent Supplier
    - Alliance Trust Rating: 62
    - Credits in Reserve: $2.1M
    - Sector of Operations: Kessel
    - Hyperdrive Capability: true
    - Inventory Level: 68%
    - Smuggler Network Connection: true
    - Species Represented: Human
    - Base Location: Kessel
    - Contact Frequency: Encrypted Transmission, Dead Drop
    - Rebellion Support Type: Deploy (index 3)

13. Mustafar Weapon Forges
    - Faction Status: Regional Partner
    - Alliance Trust Rating: 80
    - Credits in Reserve: $3.8M
    - Sector of Operations: Mustafar
    - Hyperdrive Capability: false
    - Inventory Level: 54%
    - Smuggler Network Connection: false
    - Species Represented: Other
    - Base Location: Mustafar
    - Contact Frequency: Secure Relay
    - Rebellion Support Type: Equip (index 2)

14. Scarif Data Vault
    - Faction Status: Core Ally
    - Alliance Trust Rating: 93
    - Credits in Reserve: $4.5M
    - Sector of Operations: Scarif
    - Hyperdrive Capability: true
    - Inventory Level: 76%
    - Smuggler Network Connection: false
    - Species Represented: Human
    - Base Location: Scarif
    - Contact Frequency: Encrypted Transmission, Secure Relay
    - Rebellion Support Type: Deploy (index 3)

15. Yoda's Jedi Temple
    - Faction Status: Core Ally
    - Alliance Trust Rating: 99
    - Credits in Reserve: $7.2M
    - Sector of Operations: Dagobah
    - Hyperdrive Capability: true
    - Inventory Level: 85%
    - Smuggler Network Connection: false
    - Species Represented: Other
    - Base Location: Dagobah
    - Contact Frequency: Hologram, Secure Relay
    - Rebellion Support Type: Victorious (index 4)

**Sample Data - 20 Contacts:**
- General Dodonna (Supreme Commander) at Rebel Base
- Admiral Ackbar (Fleet Commander) at Mon Calamari Shipyards
- General Madine (Ground Operations) at Rebel Base
- Carrie Fisherman (Intelligence Director) at Bothawui
- Wedge Antilles (Fighter Wing Lead) at Rebel Base
- Jyn Erso (Sabotage Chief) at Scarif
- Cassian Andor (Field Operations) at Rebel Base
- Poe Dameron (Pilot Commander) at Rebel Base
- Rey Skywalker (Force Sensitive) at Dagobah
- Finn (Infantry Commander) at Rebel Base
- Chewbacca (Engineering) at Mos Eisley
- Maz Kanata (Ancient Knowledge) at Takodana Castle
- Lando Calrissian (Cloud City) at Bespin
- Amilyn Holdo (Logistics) at Rebel Base
- Paige Tico (Bomber Pilot) at Rebel Base
- Rose Tico (Engineering Support) at Rebel Base
- Thane Kyrell (Commando Lead) at Rebel Base
- Enfys Nest (Pirate Alliance) at Kessel
- Bail Organa (Political Liaison) at Naboo
- Galen Erso (Weapons Development) at Scarif

**Sample Data - 15 Missions (Opportunities Linked to Suppliers):**

1. Steal Death Star Plans
   - Supplier: Scarif Data Vault
   - Mission Classification: Weapon Procurement
   - Amount: $15M
   - Probability: 85%
   - Imperial Threat Level: Critical
   - Rebel Operatives Assigned: Luke, Leia, Obi-Wan
   - Mission Briefing: Infiltrate Scarif facility and extract Death Star technical specifications
   - Rebellion Victory Progress: Infiltration (index 1)

2. Medical Supply Run to Hoth
   - Supplier: Mon Calamari Shipyards
   - Mission Classification: Food/Medical
   - Amount: $2.3M
   - Probability: 75%
   - Imperial Threat Level: Medium
   - Rebel Operatives Assigned: Han, Chewbacca
   - Mission Briefing: Secure medical supplies and freeze-resistant rations for Echo Base
   - Rebellion Victory Progress: Planning (index 0)

3. Procure X-Wing Fighters
   - Supplier: Mon Calamari Shipyards
   - Mission Classification: Tech/Droid
   - Amount: $12M
   - Probability: 90%
   - Imperial Threat Level: High
   - Rebel Operatives Assigned: Luke, Wedge, Lando
   - Mission Briefing: Acquire squadron of T-65 X-Wing fighter craft
   - Rebellion Victory Progress: Execution (index 2)

4. Rescue Echo Base Personnel
   - Supplier: Outer Rim Mercenaries
   - Mission Classification: Evacuation
   - Amount: $8.5M
   - Probability: 60%
   - Imperial Threat Level: Critical
   - Rebel Operatives Assigned: Leia, Han, Chewbacca
   - Mission Briefing: Extract key personnel from compromised Echo Base facility
   - Rebellion Victory Progress: Escape (index 3)

5. Recover Ancient Jedi Knowledge
   - Supplier: Yoda's Jedi Temple
   - Mission Classification: Intelligence
   - Amount: $5.2M
   - Probability: 80%
   - Imperial Threat Level: Low
   - Rebel Operatives Assigned: Luke, Yoda, Obi-Wan
   - Mission Briefing: Retrieve archived Jedi texts and training records
   - Rebellion Victory Progress: Planning (index 0)

6. Infiltrate Imperial Communications
   - Supplier: Kessel Run Spice Traders
   - Mission Classification: Sabotage
   - Amount: $7.8M
   - Probability: 45%
   - Imperial Threat Level: High
   - Rebel Operatives Assigned: Leia, Jyn, Cassian
   - Mission Briefing: Compromise Imperial communication arrays and relay encoded transmissions
   - Rebellion Victory Progress: Infiltration (index 1)

7. Acquire Hyperdrive Cores
   - Supplier: Mustafar Weapon Forges
   - Mission Classification: Tech/Droid
   - Amount: $6.1M
   - Probability: 70%
   - Imperial Threat Level: Medium
   - Rebel Operatives Assigned: Han, Lando
   - Mission Briefing: Obtain hyperdrive components for starship upgrades
   - Rebellion Victory Progress: Planning (index 0)

8. Sabotage TIE Fighter Production
   - Supplier: Bothawui Information Bureau
   - Mission Classification: Sabotage
   - Amount: $9.3M
   - Probability: 55%
   - Imperial Threat Level: High
   - Rebel Operatives Assigned: Jyn, Cassian, Poe
   - Mission Briefing: Introduce defects into Imperial TIE fighter manufacturing
   - Rebellion Victory Progress: Planning (index 0)

9. Transport Thermal Detonators
   - Supplier: Mos Eisley Spaceport
   - Mission Classification: Weapon Procurement
   - Amount: $3.4M
   - Probability: 65%
   - Imperial Threat Level: Medium
   - Rebel Operatives Assigned: Han, Chewbacca, Lando
   - Mission Briefing: Move shipment of thermal detonators to forward base
   - Rebellion Victory Progress: Execution (index 2)

10. Diplomatic Mission to Ewoks
    - Supplier: Ewok Forest Alliance
    - Mission Classification: Intelligence
    - Amount: $2.1M
    - Probability: 75%
    - Imperial Threat Level: Low
    - Rebel Operatives Assigned: Leia, C-3PO, R2-D2
    - Mission Briefing: Negotiate alliance with Endor native populations
    - Rebellion Victory Progress: Planning (index 0)

11. Recover Lost Rebel Cells
    - Supplier: Outer Rim Mercenaries
    - Mission Classification: Evacuation
    - Amount: $4.8M
    - Probability: 50%
    - Imperial Threat Level: High
    - Rebel Operatives Assigned: Leia, Cassian, Jyn
    - Mission Briefing: Locate and extract isolated rebel cells from Outer Rim
    - Rebellion Victory Progress: Infiltration (index 1)

12. Capture Imperial Officer
    - Supplier: Bothawui Information Bureau
    - Mission Classification: Intelligence
    - Amount: $6.5M
    - Probability: 40%
    - Imperial Threat Level: High
    - Rebel Operatives Assigned: Luke, Leia, Han
    - Mission Briefing: Capture high-ranking Imperial officer for interrogation
    - Rebellion Victory Progress: Planning (index 0)

13. Defend Echo Base
    - Supplier: Bespin Cloud City
    - Mission Classification: Weapon Procurement
    - Amount: $11M
    - Probability: 55%
    - Imperial Threat Level: Critical
    - Rebel Operatives Assigned: Leia, Han, Wedge
    - Mission Briefing: Fortify Echo Base against Imperial assault
    - Rebellion Victory Progress: Execution (index 2)

14. Create Distraction at Mos Eisley
    - Supplier: Tatooine Desert Traders
    - Mission Classification: Sabotage
    - Amount: $2.8M
    - Probability: 60%
    - Imperial Threat Level: Medium
    - Rebel Operatives Assigned: Han, Lando
    - Mission Briefing: Stage diversion to mask rebel operations elsewhere
    - Rebellion Victory Progress: Infiltration (index 1)

15. Final Strike on Death Star
    - Supplier: Dagobah Hidden Repository
    - Mission Classification: Weapon Procurement
    - Amount: $25M
    - Probability: 90%
    - Imperial Threat Level: Critical
    - Rebel Operatives Assigned: Luke, Leia, Han, Wedge
    - Mission Briefing: Execute final assault on Death Star battle station
    - Rebellion Victory Progress: Safe Return (index 4)

**Sample Data - 18 Activities:**

1. Scarif Intelligence Briefing - Meeting, 90 minutes, 2024-01-15, Scheduled, Death Star plans discussion
2. Hyperspace Jump to Hoth - Call, 120 minutes, 2024-01-16, Completed, Medical supply transport
3. X-Wing Fighter Inspection - Meeting, 60 minutes, 2024-01-18, Scheduled, Technical acceptance review
4. Echo Base Personnel Evacuation - Email, 45 minutes, 2024-01-19, Completed, Extraction coordination
5. Jedi Temple Strategy Session - Meeting, 90 minutes, 2024-01-22, Scheduled, Knowledge recovery planning
6. Imperial Communications Infiltration - Meeting, 120 minutes, 2024-01-23, Completed, Sabotage briefing
7. Hyperdrive Core Testing - Meeting, 60 minutes, 2024-01-24, Scheduled, Equipment verification
8. TIE Fighter Production Sabotage - Call, 75 minutes, 2024-01-25, Completed, Operation coordination
9. Thermal Detonator Transport - Email, 30 minutes, 2024-01-29, Completed, Shipment tracking
10. Ewok Diplomatic Prep - Meeting, 90 minutes, 2024-01-30, Scheduled, Alliance negotiation
11. Lost Cells Recovery Operation - Meeting, 120 minutes, 2024-02-01, Scheduled, Rescue planning
12. Imperial Officer Capture - Call, 60 minutes, 2024-02-02, Completed, Interrogation prep
13. Echo Base Defense Fortification - Meeting, 90 minutes, 2024-02-05, Scheduled, Defensive strategy
14. Mos Eisley Distraction Planning - Meeting, 75 minutes, 2024-02-06, Completed, Diversion tactics
15. Death Star Final Assault - Meeting, 120 minutes, 2024-02-08, Scheduled, Mission briefing
16. Alliance Coordination Conference - Call, 90 minutes, 2024-02-09, Completed, Multi-faction alignment
17. Supply Chain Verification - Email, 30 minutes, 2024-02-12, Completed, Inventory confirmation
18. Victory Celebration Planning - Meeting, 60 minutes, 2024-02-15, Scheduled, Post-mission protocol

**Standard Field Mapping (apply when creating all records):**
- Customer `status`: Derive from context — Rebellion Support Type index 2-4 (Equip/Deploy/Victorious) or Alliance Trust Rating ≥ 70 → `active`; Trust Rating 40-69 or index 0-1 → `prospect`; Trust Rating < 40 or Sympathizer faction → `inactive`. Aim for ~60% active, 25% prospect, 15% inactive.
- Opportunity `stage`: Map from Rebellion Victory Progress index — 0 = `prospecting`, 1 = `qualification`, 2 = `proposal`, 3 = `negotiation`, 4 = `closed_won`. Use `closed_lost` for failed/aborted missions.
- Activity `status`: Extract from each description — "Completed" → `completed`, "Scheduled"/"Active" → `pending`, "Cancelled"/"Aborted" → `cancelled`.

**Sample Data - 10 CustomRecords (use bulkCreateCustomRecords):**
1. X-Wing Starfighter Red Five - type: spacecraft, status: active, serialNumber: XW-R5-001, customer: Red Squadron
2. Millennium Falcon Transponder - type: hardware, status: maintenance, serialNumber: MF-TRNSP-001, customer: Smugglers Alliance
3. Mon Calamari Cruiser Targeting Array - type: hardware, status: active, serialNumber: MC-TGT-001, customer: Mon Calamari Fleet
4. Rebel Base Encryption Terminal - type: hardware, status: active, serialNumber: ENC-ECHO-001, customer: Echo Base Command
5. Astromech Droid R2-D2 Unit - type: droid, status: active, serialNumber: R2D2-REG-001, customer: Red Squadron
6. Bothan Intelligence Data Archive - type: software, status: active, serialNumber: BTH-ARCH-001, customer: Bothan Spy Network
7. Speeder Bike Patrol Unit (x8) - type: vehicle, status: active, serialNumber: SPD-END-008, customer: Endor Strike Team
8. Proton Torpedo Stock — 200 units - type: inventory, status: active, serialNumber: PRT-STOCK-200
9. Medical Frigate Life Support Array - type: hardware, status: active, serialNumber: MED-NEA-001, customer: Medical Frigate Nebulon-B
10. Holo-Comm Array — Field Portable - type: hardware, status: inactive, serialNumber: HOL-FLD-003, customer: Echo Base Command

**Sample Data - 6 Orders:**
1. Red Squadron - "X-Wing Maintenance & Upgrade Package", status: DELIVERED, totalAmount: 85000, currency: USD
2. Mon Calamari Fleet - "Capital Ship Weapons Array Refit", status: CONFIRMED, totalAmount: 620000, currency: USD
3. Echo Base Command - "Cold Weather Survival Equipment", status: SHIPPED, totalAmount: 42000, currency: USD
4. Bothan Spy Network - "Encrypted Communication Suite", status: DRAFT, totalAmount: 38000, currency: USD
5. Endor Strike Team - "Speeder Bike Replacement Parts", status: CANCELLED, totalAmount: 15000, currency: USD
6. Smugglers Alliance - "Smuggling Compartment Refit Kit", status: DELIVERED, totalAmount: 28000, currency: USD

**Sample Data - 6 Invoices:**
1. Red Squadron - status: PAID, totalAmount: 85000, currency: USD, paymentTerms: NET-30
2. Mon Calamari Fleet - status: SENT, totalAmount: 620000, currency: USD, paymentTerms: NET-60
3. Echo Base Command - status: PAID, totalAmount: 42000, currency: USD, paymentTerms: NET-30
4. Bothan Spy Network - status: DRAFT, totalAmount: 38000, currency: USD, paymentTerms: NET-30
5. Endor Strike Team - status: OVERDUE, totalAmount: 15000, currency: USD, paymentTerms: NET-14
6. Smugglers Alliance - status: PAID, totalAmount: 28000, currency: USD, paymentTerms: NET-30

**Document Templates (3 custom styles — use createDocumentTemplate for each):**
1. "Rebel Mission Briefing Deck" — templateType: slide_deck, description: "Dark tactical briefing deck for high-command mission planning", styleJson: {"layout":"corporate","accentColor":"#E8A000","backgroundColor":"#0A0A14","h1Color":"#FF6B35"}
2. "Alliance Faction Profile" — templateType: one_pager, description: "Single-page faction dossier for high command review", styleJson: {"layout":"light","accentColor":"#E8A000","includeCustomFields":true}
3. "Alliance Roster Export" — templateType: csv_export, description: "Full alliance member export with trust rating and support type data", styleJson: {"includeFields":["name","status","faction","trust_rating","rebellion_support_type","fleet_strength"]}

**Notification Templates (3 holocomm transmission templates — use createNotificationTemplate for each):**
1. name: "New Mission Opened", notificationType: OPPORTUNITY_CREATED, isActive: true
   subject: "Mission Alert: {{opportunityName}} ({{customerName}})"
   body: "A new Rebel mission has been initiated.\n\nFaction: {{customerName}}\nMission: {{opportunityName}}\nPriority: {{stage}}\nResources: {{amount}}\n\nView mission file: {{link}}"
2. name: "Alliance Status Change", notificationType: CUSTOMER_UPDATED, isActive: true
   subject: "Faction Update: {{customerName}}"
   body: "Alliance member status has changed.\n\nFaction: {{customerName}}\nUpdated by: {{assignee}}\n\nReview: {{link}}"
3. name: "Supply Order Dispatched", notificationType: ORDER_CREATED, isActive: true
   subject: "Supply Order: {{customerName}} — {{amount}} Credits"
   body: "Rebel supply order dispatched.\n\nFaction: {{customerName}}\nOrder: {{opportunityName}}\nTotal: {{amount}} Credits\nExpected: {{dueDate}}\n\nTrack: {{link}}"

Please execute this complete setup now, creating all fields, policies, customers, contacts, opportunities, activities, custom records, orders, invoices, document templates, and notification templates. Update the tenant settings with the branding information provided. May the Force be with us!
```

---

## Prompt 2: Galactic Empire Corporate Division

```
I'm setting up a new CRM workspace for "Galactic Empire Corporate Division" - the administrative and logistics command structure managing Imperial operations, procurement, and personnel across occupied territories.

Please help me set up this workspace completely. Here's what I need:

**Company Branding:**
- Logo: https://via.placeholder.com/200x100?text=Galactic+Empire
- Primary Color: #1a1a1a
- Secondary Color: #CCCCCC
- Website: https://imperial-order.galaxy
- Bio: "The Galactic Empire maintains order through superior organization and overwhelming force. Our Corporate Division manages procurement, facility operations, and resource allocation across thousands of systems. We ensure the Emperor's vision is executed with absolute efficiency and precision."

**Custom Fields for Customers (Imperial Territories/Contractors):**
1. "Territory Classification" (select field, required) - options: Core World, Outer Rim Occupied, Mining Colony, Military Installation, Research Facility
2. "Stormtrooper Garrison Size" (number field) - troop count
3. "Imperial Compliance Rating" (number field, 0-100) - loyalty to Empire
4. "Tarkin Doctrine Adherence" (percentage field) - fear-based control percentage
5. "TIE Fighter Squadron Count" (number field) - military customRecords
6. "Monthly Tribute (Credits)" (currency field) - taxation revenue
7. "Last Imperial Inspection" (date field) - compliance audit date
8. "Planet Destruction Threat Level" (select field) - options: None, Moderate, High, Death Star Target
9. "Sith Lord Assignment" (text field) - which Sith oversees them
10. "Detention Center Capacity" (number field) - prisoner holding
11. "Propaganda Effectiveness" (percentage field) - brainwashing success rate
12. "Rebellion Activity" (select field) - options: None Detected, Minor Pockets, Organized Cells, Active Threat
13. "Imperial Conquest Stage" (workflow field) - milestones: Initial Contact, Negotiation, Military Occupation, Full Integration, Subjugation

**Custom Fields for Opportunities (Imperial Operations/Conquests):**
1. "Operation Type" (select field, required) - options: Territory Conquest, Resource Acquisition, Military Build-up, Detention Operations, Propaganda Campaign
2. "Destruction Method" (select field) - options: Ground Assault, Orbital Bombardment, Death Star Strike, Blockade, Espionage
3. "Assigned Sith Lord" (select field) - options: Vader, Palpatine, Inquisitors, Unknown
4. "Imperial Efficiency Score" (number field, 0-100) - operational precision
5. "Conquest Progress" (workflow field) - milestones: Planning, Deployment, Siege, Surrender, Dominion Established

**Calculated Fields:**
1. For Customer: "Imperial Threat Score" = (ImperialComplianceRating * 0.6) + (if TIEFighterSquadronCount > 10 then 30 else 15) + (TarkinDoctrineAdherence * 0.4)
2. For Opportunity: "Conquest Completion %" = if ConquestProgress exists then (ConquestProgress.currentIndex + 1) * 20 else 0

**Business Policies:**
1. DENY policy: Cannot reduce garrison if rebellion activity is "Active Threat" (RebellionActivity = "Active Threat" AND TroopReduction)
2. WARN policy: Warn on attempting to spare planets in Death Star range (PlanetDestructionThreatLevel = "Death Star Target")
3. DENY policy: Block operations without Sith Lord assignment (SithLordAssignment is empty for "Territory Conquest")

**Sample Data - 15 Imperial Territories (Customers):**

1. Coruscant Imperial Palace
   - Territory Classification: Core World
   - Stormtrooper Garrison Size: 50000
   - Imperial Compliance Rating: 100
   - Tarkin Doctrine Adherence: 95%
   - TIE Fighter Squadron Count: 500
   - Monthly Tribute: $50M
   - Last Imperial Inspection: 2024-01-01
   - Planet Destruction Threat Level: None
   - Sith Lord Assignment: Emperor Palpatine
   - Detention Center Capacity: 10000
   - Propaganda Effectiveness: 99%
   - Rebellion Activity: None Detected
   - Imperial Conquest Stage: Subjugation (index 4)

2. Scarif Military Installation
   - Territory Classification: Military Installation
   - Stormtrooper Garrison Size: 25000
   - Imperial Compliance Rating: 98
   - Tarkin Doctrine Adherence: 90%
   - TIE Fighter Squadron Count: 120
   - Monthly Tribute: $15M
   - Last Imperial Inspection: 2024-01-05
   - Planet Destruction Threat Level: None
   - Sith Lord Assignment: Director Krennic
   - Detention Center Capacity: 5000
   - Propaganda Effectiveness: 88%
   - Rebellion Activity: Minor Pockets
   - Imperial Conquest Stage: Full Integration (index 3)

3. Cloud City Bespin
   - Territory Classification: Outer Rim Occupied
   - Stormtrooper Garrison Size: 8000
   - Imperial Compliance Rating: 85
   - Tarkin Doctrine Adherence: 75%
   - TIE Fighter Squadron Count: 45
   - Monthly Tribute: $8.5M
   - Last Imperial Inspection: 2024-01-10
   - Planet Destruction Threat Level: Moderate
   - Sith Lord Assignment: Darth Vader
   - Detention Center Capacity: 2000
   - Propaganda Effectiveness: 78%
   - Rebellion Activity: Minor Pockets
   - Imperial Conquest Stage: Military Occupation (index 2)

4. Tatooine Spaceport
   - Territory Classification: Outer Rim Occupied
   - Stormtrooper Garrison Size: 3000
   - Imperial Compliance Rating: 62
   - Tarkin Doctrine Adherence: 50%
   - TIE Fighter Squadron Count: 20
   - Monthly Tribute: $3.2M
   - Last Imperial Inspection: 2023-12-20
   - Planet Destruction Threat Level: Low
   - Sith Lord Assignment: Local Commander
   - Detention Center Capacity: 500
   - Propaganda Effectiveness: 42%
   - Rebellion Activity: Organized Cells
   - Imperial Conquest Stage: Negotiation (index 1)

5. Kessel Mining Colony
   - Territory Classification: Mining Colony
   - Stormtrooper Garrison Size: 15000
   - Imperial Compliance Rating: 88
   - Tarkin Doctrine Adherence: 85%
   - TIE Fighter Squadron Count: 60
   - Monthly Tribute: $12M
   - Last Imperial Inspection: 2024-01-08
   - Planet Destruction Threat Level: Low
   - Sith Lord Assignment: Tarkin Overseer
   - Detention Center Capacity: 3000
   - Propaganda Effectiveness: 82%
   - Rebellion Activity: None Detected
   - Imperial Conquest Stage: Full Integration (index 3)

6. Mustafar Research Facility
   - Territory Classification: Research Facility
   - Stormtrooper Garrison Size: 5000
   - Imperial Compliance Rating: 92
   - Tarkin Doctrine Adherence: 88%
   - TIE Fighter Squadron Count: 25
   - Monthly Tribute: $7.8M
   - Last Imperial Inspection: 2024-01-02
   - Planet Destruction Threat Level: None
   - Sith Lord Assignment: Darth Vader
   - Detention Center Capacity: 1500
   - Propaganda Effectiveness: 95%
   - Rebellion Activity: None Detected
   - Imperial Conquest Stage: Subjugation (index 4)

7. Death Star Space Station
   - Territory Classification: Military Installation
   - Stormtrooper Garrison Size: 1000000
   - Imperial Compliance Rating: 100
   - Tarkin Doctrine Adherence: 100%
   - TIE Fighter Squadron Count: 1000
   - Monthly Tribute: $100M
   - Last Imperial Inspection: 2024-01-01
   - Planet Destruction Threat Level: Death Star Target
   - Sith Lord Assignment: Grand Moff Tarkin
   - Detention Center Capacity: 50000
   - Propaganda Effectiveness: 100%
   - Rebellion Activity: None Detected
   - Imperial Conquest Stage: Subjugation (index 4)

8. Geonosis War Factory
   - Territory Classification: Manufacturing
   - Stormtrooper Garrison Size: 12000
   - Imperial Compliance Rating: 90
   - Tarkin Doctrine Adherence: 92%
   - TIE Fighter Squadron Count: 80
   - Monthly Tribute: $11M
   - Last Imperial Inspection: 2024-01-06
   - Planet Destruction Threat Level: None
   - Sith Lord Assignment: Industrial Commander
   - Detention Center Capacity: 2500
   - Propaganda Effectiveness: 85%
   - Rebellion Activity: None Detected
   - Imperial Conquest Stage: Full Integration (index 3)

9. Ord Mantell Garrison
   - Territory Classification: Military Installation
   - Stormtrooper Garrison Size: 6000
   - Imperial Compliance Rating: 91
   - Tarkin Doctrine Adherence: 89%
   - TIE Fighter Squadron Count: 35
   - Monthly Tribute: $6.5M
   - Last Imperial Inspection: 2024-01-12
   - Planet Destruction Threat Level: Low
   - Sith Lord Assignment: Commander Needa
   - Detention Center Capacity: 1500
   - Propaganda Effectiveness: 84%
   - Rebellion Activity: Minor Pockets
   - Imperial Conquest Stage: Full Integration (index 3)

10. Naboo Royal Palace
    - Territory Classification: Core World
    - Stormtrooper Garrison Size: 4000
    - Imperial Compliance Rating: 88
    - Tarkin Doctrine Adherence: 82%
    - TIE Fighter Squadron Count: 30
    - Monthly Tribute: $9.2M
    - Last Imperial Inspection: 2024-01-09
    - Planet Destruction Threat Level: None
    - Sith Lord Assignment: Emperor Representative
    - Detention Center Capacity: 1000
    - Propaganda Effectiveness: 80%
    - Rebellion Activity: Minor Pockets
    - Imperial Conquest Stage: Military Occupation (index 2)

11. Bothawui Occupied Sector
    - Territory Classification: Outer Rim Occupied
    - Stormtrooper Garrison Size: 10000
    - Imperial Compliance Rating: 72
    - Tarkin Doctrine Adherence: 65%
    - TIE Fighter Squadron Count: 50
    - Monthly Tribute: $5.1M
    - Last Imperial Inspection: 2024-01-11
    - Planet Destruction Threat Level: Moderate
    - Sith Lord Assignment: Regional Governor
    - Detention Center Capacity: 2000
    - Propaganda Effectiveness: 65%
    - Rebellion Activity: Active Threat
    - Imperial Conquest Stage: Military Occupation (index 2)

12. Neimodian Patrol Station
    - Territory Classification: Outer Rim Occupied
    - Stormtrooper Garrison Size: 7000
    - Imperial Compliance Rating: 95
    - Tarkin Doctrine Adherence: 93%
    - TIE Fighter Squadron Count: 40
    - Monthly Tribute: $7.9M
    - Last Imperial Inspection: 2024-01-03
    - Planet Destruction Threat Level: Low
    - Sith Lord Assignment: Trade Liaison
    - Detention Center Capacity: 1500
    - Propaganda Effectiveness: 90%
    - Rebellion Activity: None Detected
    - Imperial Conquest Stage: Full Integration (index 3)

13. Endor Forest Base
    - Territory Classification: Outer Rim Occupied
    - Stormtrooper Garrison Size: 2000
    - Imperial Compliance Rating: 40
    - Tarkin Doctrine Adherence: 30%
    - TIE Fighter Squadron Count: 15
    - Monthly Tribute: $1.8M
    - Last Imperial Inspection: 2023-12-15
    - Planet Destruction Threat Level: High
    - Sith Lord Assignment: Field Commander
    - Detention Center Capacity: 500
    - Propaganda Effectiveness: 25%
    - Rebellion Activity: Active Threat
    - Imperial Conquest Stage: Initial Contact (index 0)

14. Kamino Cloning Facility
    - Territory Classification: Research Facility
    - Stormtrooper Garrison Size: 8000
    - Imperial Compliance Rating: 100
    - Tarkin Doctrine Adherence: 99%
    - TIE Fighter Squadron Count: 55
    - Monthly Tribute: $14M
    - Last Imperial Inspection: 2024-01-04
    - Planet Destruction Threat Level: None
    - Sith Lord Assignment: Emperor Palpatine
    - Detention Center Capacity: 3000
    - Propaganda Effectiveness: 98%
    - Rebellion Activity: None Detected
    - Imperial Conquest Stage: Subjugation (index 4)

15. Alderaan Sector Command
    - Territory Classification: Core World
    - Stormtrooper Garrison Size: 20000
    - Imperial Compliance Rating: 85
    - Tarkin Doctrine Adherence: 80%
    - TIE Fighter Squadron Count: 100
    - Monthly Tribute: $25M
    - Last Imperial Inspection: 2023-12-18
    - Planet Destruction Threat Level: Death Star Target
    - Sith Lord Assignment: Grand Moff Tarkin
    - Detention Center Capacity: 5000
    - Propaganda Effectiveness: 92%
    - Rebellion Activity: Organized Cells
    - Imperial Conquest Stage: Military Occupation (index 2)

**Sample Data - 20 Contacts:**
- Grand Moff Tarkin (Commander) at Imperial Palace
- Darth Vader (Dark Lord of the Sith) at Imperial Command
- Emperor Palpatine (Supreme Leader) at Imperial Palace
- Moff Jerjerrod (Death Star Commander) at Death Star
- Admiral Piett (Executor Captain) at Imperial Fleet
- General Veers (Ground Operations) at Imperial Army
- Director Krennic (Death Star Project) at Scarif
- Captain Needa (Star Destroyer Commander) at Imperial Navy
- Leia Organa's Double (Infiltrator) at Empire
- Count Dooku (Military Strategist) at Empire
- General Grievous (Military Offensive) at Empire Command
- Jango Fett (Bounty Operations) at Empire
- Boba Fett (Enforcement) at Outer Rim
- Thrawn (Imperial Navy) at Unknown Base
- Starkiller (Dark Force) at Unknown
- Ysanne Isard (Intelligence) at Intelligence HQ
- Praji (Storm Commander) at Imperial Palace
- Veers Clone (Field Commander) at Ground Operations
- Tion Medon (Occupation Governor) at Pau City
- Lott Dod (Trade Federation) at Corporate Offices

**Sample Data - 15 Operations (Linked to Territories):**

1. Destroy Alderaan
   - Territory: Alderaan Sector Command
   - Operation Type: Territory Conquest
   - Destruction Method: Orbital Bombardment
   - Assigned Sith Lord: Palpatine
   - Imperial Efficiency Score: 95
   - Conquest Progress: Siege (index 2)

2. Establish Bespin Control
   - Territory: Cloud City Bespin
   - Operation Type: Territory Conquest
   - Destruction Method: Military Occupation
   - Assigned Sith Lord: Vader
   - Imperial Efficiency Score: 88
   - Conquest Progress: Planning (index 0)

3. Build Death Star II
   - Territory: Death Star Space Station
   - Operation Type: Military Build-up
   - Destruction Method: Resource Acquisition
   - Assigned Sith Lord: Palpatine
   - Imperial Efficiency Score: 92
   - Conquest Progress: Deployment (index 1)

4. Suppress Endor Rebellion
   - Territory: Endor Forest Base
   - Operation Type: Detention Operations
   - Destruction Method: Ground Assault
   - Assigned Sith Lord: Inquisitors
   - Imperial Efficiency Score: 45
   - Conquest Progress: Deployment (index 1)

5. Capture Luke Skywalker
   - Territory: Tatooine Spaceport
   - Operation Type: Territory Conquest
   - Destruction Method: Espionage
   - Assigned Sith Lord: Vader
   - Imperial Efficiency Score: 60
   - Conquest Progress: Planning (index 0)

6. Construct Imperial City
   - Territory: Coruscant Imperial Palace
   - Operation Type: Territory Conquest
   - Destruction Method: Military Occupation
   - Assigned Sith Lord: Palpatine
   - Imperial Efficiency Score: 78
   - Conquest Progress: Deployment (index 1)

7. Mass Production TIE Fighters
   - Territory: Geonosis War Factory
   - Operation Type: Military Build-up
   - Destruction Method: Resource Acquisition
   - Assigned Sith Lord: Tarkin Overseer
   - Imperial Efficiency Score: 85
   - Conquest Progress: Deployment (index 1)

8. Subjugate Outer Rim
   - Territory: Bothawui Occupied Sector
   - Operation Type: Territory Conquest
   - Destruction Method: Blockade
   - Assigned Sith Lord: Vader
   - Imperial Efficiency Score: 72
   - Conquest Progress: Planning (index 0)

9. Establish Order 66 Protocol
   - Territory: Kamino Cloning Facility
   - Operation Type: Detention Operations
   - Destruction Method: Espionage
   - Assigned Sith Lord: Palpatine
   - Imperial Efficiency Score: 99
   - Conquest Progress: Dominion Established (index 4)

10. Purge Jedi Order
    - Territory: Mustafar Research Facility
    - Operation Type: Territory Conquest
    - Destruction Method: Termination
    - Assigned Sith Lord: Vader
    - Imperial Efficiency Score: 98
    - Conquest Progress: Dominion Established (index 4)

11. Control Coruscant Population
    - Territory: Coruscant Imperial Palace
    - Operation Type: Propaganda Campaign
    - Destruction Method: Blockade
    - Assigned Sith Lord: Palpatine
    - Imperial Efficiency Score: 91
    - Conquest Progress: Surrender (index 3)

12. Obtain Ancient Sith Artifacts
    - Territory: Kessel Mining Colony
    - Operation Type: Military Build-up
    - Destruction Method: Espionage
    - Assigned Sith Lord: Vader
    - Imperial Efficiency Score: 55
    - Conquest Progress: Deployment (index 1)

13. Establish Rule on Tatooine
    - Territory: Tatooine Spaceport
    - Operation Type: Territory Conquest
    - Destruction Method: Military Occupation
    - Assigned Sith Lord: Local Commander
    - Imperial Efficiency Score: 68
    - Conquest Progress: Siege (index 2)

14. Capture Rebellion Leadership
    - Territory: Naboo Royal Palace
    - Operation Type: Territory Conquest
    - Destruction Method: Ground Assault
    - Assigned Sith Lord: Vader
    - Imperial Efficiency Score: 50
    - Conquest Progress: Planning (index 0)

15. Galactic Supremacy Consolidation
    - Territory: Ord Mantell Garrison
    - Operation Type: Territory Conquest
    - Destruction Method: Military Occupation
    - Assigned Sith Lord: Palpatine
    - Imperial Efficiency Score: 89
    - Conquest Progress: Siege (index 2)

**Sample Data - 18+ Activities:**

1. Tarkin Doctrine Briefing - Meeting, 90 minutes, 2024-01-15, Scheduled, Discipline enforcement training
2. Vader's Force Interrogation - Meeting, 60 minutes, 2024-01-16, Completed, Prisoner interrogation
3. TIE Fighter Operations - Call, 120 minutes, 2024-01-18, Scheduled, Fighter deployment coordination
4. Alderaan Bombardment Coordination - Email, 240 minutes, 2024-01-19, Completed, Weapons targeting
5. Stormtrooper Deployment - Meeting, 60 minutes, 2024-01-22, Scheduled, Garrison assignment
6. Death Star Status Update - Email, 20 minutes, 2024-01-23, Completed, Technical assessment
7. Sith Training Session - Meeting, 120 minutes, 2024-01-24, Scheduled, Force user instruction
8. Propaganda Broadcast - Call, 30 minutes, 2024-01-25, Completed, Media coordination
9. Bespin Control Establishment - Meeting, 90 minutes, 2024-01-29, Scheduled, Territory takeover
10. Death Star II Construction - Email, 45 minutes, 2024-01-30, Completed, Project status
11. Endor Rebellion Suppression - Meeting, 120 minutes, 2024-02-01, Scheduled, Military strategy
12. Jedi Order Purge Planning - Meeting, 90 minutes, 2024-02-02, Completed, Eradication protocol
13. Imperial City Construction - Call, 60 minutes, 2024-02-05, Scheduled, Infrastructure development
14. Outer Rim Subjugation - Meeting, 90 minutes, 2024-02-06, Completed, Conquest strategy
15. Order 66 Implementation - Email, 30 minutes, 2024-02-08, Completed, Protocol activation
16. Coruscant Population Control - Meeting, 120 minutes, 2024-02-09, Scheduled, Social engineering
17. Tatooine Rule Establishment - Call, 75 minutes, 2024-02-12, Completed, Regional governance
18. Galactic Supremacy Planning - Meeting, 90 minutes, 2024-02-15, Scheduled, Final conquest

**Standard Field Mapping (apply when creating all records):**
- Customer `status`: Derive from context — territories under Imperial control with high compliance → `active`; newly conquered or partially compliant → `prospect`; rebel-held or lost territories → `inactive`. Aim for ~60% active, 25% prospect, 15% inactive.
- Opportunity `stage`: Map from the operation workflow index — 0 = `prospecting`, 1 = `qualification`, 2 = `proposal`, 3 = `negotiation`, 4 = `closed_won`. Use `closed_lost` for failed/repelled operations.
- Activity `status`: Extract from each description — "Completed" → `completed`, "Scheduled"/"In Progress" → `pending`, "Cancelled"/"Aborted" → `cancelled`.

**Sample Data - 10 CustomRecords (use bulkCreateCustomRecords):**
1. Star Destroyer Command Bridge Console - type: hardware, status: active, serialNumber: SD-ISD-CMD-001, customer: Coruscant Industrial Sector
2. Death Star Superlaser Control System - type: hardware, status: active, serialNumber: DS-SLS-001
3. AT-AT Walker Fleet (12 units) - type: vehicle, status: active, serialNumber: ATAT-HBH-012, customer: Hoth Outer Rim Territories
4. TIE Fighter Squadron (24 units) - type: spacecraft, status: active, serialNumber: TIE-SQ-024, customer: Tatooine Desert Territories
5. Imperial Encryption Relay Station - type: hardware, status: active, serialNumber: ENC-COR-007, customer: Coruscant Industrial Sector
6. Stormtrooper Armor Stockpile (500 sets) - type: inventory, status: active, serialNumber: ST-ARM-500
7. Planetary Shield Generator — Hoth Grade - type: hardware, status: maintenance, serialNumber: SHD-PLN-001, customer: Hoth Outer Rim Territories
8. Imperial Census Data Terminal - type: hardware, status: active, serialNumber: CDT-NAB-001, customer: Naboo Colonial Administration
9. Thermal Detonator Reserve (Classified) - type: inventory, status: inactive, serialNumber: TD-RES-CLF
10. Probe Droid Deployment Pod (x20) - type: equipment, status: active, serialNumber: PD-POD-020

**Sample Data - 6 Orders:**
1. Coruscant Industrial Sector - "Imperial Infrastructure Expansion Pack", status: DELIVERED, totalAmount: 8500000, currency: USD
2. Hoth Outer Rim Territories - "Cold Climate Military Equipment Bundle", status: CONFIRMED, totalAmount: 3200000, currency: USD
3. Tatooine Desert Territories - "Desert Patrol Vehicle Fleet", status: SHIPPED, totalAmount: 1800000, currency: USD
4. Naboo Colonial Administration - "Administrative Control System Install", status: DRAFT, totalAmount: 950000, currency: USD
5. Mustafar Mining Colony - "Volcanic Heat Shield Equipment", status: CANCELLED, totalAmount: 420000, currency: USD
6. Bespin Cloud City - "Carbon Freeze Chamber Upgrade", status: DELIVERED, totalAmount: 2700000, currency: USD

**Sample Data - 6 Invoices:**
1. Coruscant Industrial Sector - status: PAID, totalAmount: 8500000, currency: USD, paymentTerms: NET-30
2. Hoth Outer Rim Territories - status: SENT, totalAmount: 3200000, currency: USD, paymentTerms: NET-60
3. Tatooine Desert Territories - status: PAID, totalAmount: 1800000, currency: USD, paymentTerms: NET-30
4. Naboo Colonial Administration - status: DRAFT, totalAmount: 950000, currency: USD, paymentTerms: NET-60
5. Mustafar Mining Colony - status: OVERDUE, totalAmount: 420000, currency: USD, paymentTerms: NET-30
6. Bespin Cloud City - status: PAID, totalAmount: 2700000, currency: USD, paymentTerms: NET-30

**Document Templates (3 custom styles — use createDocumentTemplate for each):**
1. "Imperial Sector Report" — templateType: slide_deck, description: "Authoritarian dark-theme conquest briefing for Grand Moff review", styleJson: {"layout":"corporate","accentColor":"#B71C1C","backgroundColor":"#0A0A0A","h1Color":"#E0E0E0"}
2. "Territory Control Profile" — templateType: one_pager, description: "Imperial sector administrative summary for sector governors", styleJson: {"layout":"light","accentColor":"#B71C1C","includeCustomFields":true}
3. "Imperial Territory Export" — templateType: csv_export, description: "Galactic territory control export with compliance and subjugation metrics", styleJson: {"includeFields":["name","status","planet_class","population","compliance_level","strategic_value"]}

**Notification Templates (3 Imperial dispatch templates — use createNotificationTemplate for each):**
1. name: "New Operation Initiated", notificationType: OPPORTUNITY_CREATED, isActive: true
   subject: "Operation Order: {{opportunityName}} — {{customerName}}"
   body: "A new Imperial operation has been authorized.\n\nSector: {{customerName}}\nOperation: {{opportunityName}}\nPhase: {{stage}}\nForce Deployed: {{amount}}\n\nView briefing: {{link}}"
2. name: "Territory Status Change", notificationType: CUSTOMER_UPDATED, isActive: true
   subject: "Sector Update: {{customerName}}"
   body: "Imperial sector status has changed.\n\nSector: {{customerName}}\nUpdated by: {{assignee}}\n\nReview: {{link}}"
3. name: "Supply Requisition", notificationType: ORDER_CREATED, isActive: true
   subject: "Imperial Supply Order: {{customerName}} — {{amount}} Credits"
   body: "Supply requisition processed.\n\nSector: {{customerName}}\nSupplies: {{opportunityName}}\nTotal: {{amount}} Credits\nArrival: {{dueDate}}\n\n{{link}}"

Please execute this complete setup now, creating all fields, policies, territories, contacts, operations, activities, customRecords, orders, invoices, document templates, and notification templates. Update the tenant settings with the branding information provided. The Empire will rise!
```

---

## Prompt 3: Bounty Hunters Guild

```
I'm setting up a new CRM workspace for the "Bounty Hunters Guild" - the independent organization managing contracts, targets, and payments for freelance hunters across the galaxy.

Please help me set up this workspace completely. Here's what I need:

**Company Branding:**
- Logo: https://static.wikia.nocookie.net/swrp/images/9/91/BHG.svg/revision/latest?cb=20130720230746
- Primary Color: #8B0000
- Secondary Color: #DAA520
- Website: https://bountyhunters.underworld
- Bio: "The Bounty Hunters Guild provides the galaxy's most dangerous freelance operatives with high-value contracts. We connect elite hunters with clients seeking recovery, elimination, or capture of customRecords. Reputation and credits are everything in this business."

**Custom Fields for Customers (Bounty Hunters):**
1. "Hunter Codename" (text field, required) - display name
2. "Bounty Success Rate (%)" (percentage field) - contract completion rate
3. "Specialization" (multiselect field) - options: Capture Alive, Termination, Recovery, Espionage, Droid Hunting, Force Sensitivity
4. "Favorite Weapon" (select field) - options: Blaster Rifle, Mandalorian Armor, Lightsaber, Carbonite Freezer, Poison, Explosives
5. "Reputation Score" (number field, 0-100) - underworld standing
6. "Kills on Record" (number field) - confirmed terminations
7. "Current Bounty Value" (currency field) - on their own head
8. "Mandalorian Clan Member" (boolean field) - Mandalorian connection
9. "Jedi Hunter" (boolean field) - Force user specialist
10. "Owns Starship" (boolean field) - has transportation
11. "Sabacc Debt" (currency field) - gambling obligations
12. "Previous Employers" (textarea field) - job history
13. "Bounty Taking Progress" (workflow field) - milestones: Contact, Accept, Hunt, Apprehend, Payment Received

**Custom Fields for Opportunities (Bounty Contracts):**
1. "Target Type" (select field, required) - options: Jedi Fugitive, Rebel, Criminal, Corporate Executive, Droid, Wanted Beast
2. "Target Threat Level" (select field) - options: Low, Medium, High, Legendary, Force Sensitive
3. "Bounty Amount (Credits)" (currency field) - contract reward
4. "Capture vs Termination" (select field) - options: Capture Alive, Termination Preferred, Either
5. "Hunter Assignment" (select field) - which hunter to assign
6. "Contract Progress" (workflow field) - milestones: Posted, Assigned, In Pursuit, Target Acquired, Payment Verified

**Calculated Fields:**
1. For Customer: "Hunter Rating" = (BountySuccessRate * 0.7) + (ReputationScore * 0.3) + (if JediHunter then 15 else 0)
2. For Opportunity: "Contract Progress %" = if ContractProgress exists then (ContractProgress.currentIndex + 1) * 20 else 0

**Business Policies:**
1. DENY policy: Cannot assign hunter with less than 60% success rate to "Legendary" threat (BountySuccessRate < 60 AND TargetThreatLevel = "Legendary")
2. WARN policy: Warn if assigning Force-sensitive hunter to Jedi (JediHunter = true AND TargetType = "Jedi Fugitive")
3. DENY policy: No payment without confirmation of target acquisition (ContractProgress < 3 AND PaymentProcessed)

**Sample Data - 15 Bounty Hunters (Customers):**

1. Boba Fett
   - Hunter Codename: The Mandalorian
   - Bounty Success Rate: 95%
   - Specialization: Capture Alive, Termination, Force Sensitivity
   - Favorite Weapon: Mandalorian Armor
   - Reputation Score: 89
   - Kills on Record: 156
   - Current Bounty Value: $500K
   - Mandalorian Clan Member: true
   - Jedi Hunter: false
   - Owns Starship: true
   - Sabacc Debt: $25K
   - Previous Employers: Jabba, Empire, Others
   - Bounty Taking Progress: Payment Received (index 4)

2. Jango Fett
   - Hunter Codename: The Clone Master
   - Bounty Success Rate: 97%
   - Specialization: Termination, Recovery
   - Favorite Weapon: Blaster Rifle
   - Reputation Score: 92
   - Kills on Record: 203
   - Current Bounty Value: $250K
   - Mandalorian Clan Member: true
   - Jedi Hunter: false
   - Owns Starship: true
   - Sabacc Debt: $0
   - Previous Employers: Tyrannos, Empire, Viceroy
   - Bounty Taking Progress: Payment Received (index 4)

3. Cad Bane
   - Hunter Codename: The Collector
   - Bounty Success Rate: 88%
   - Specialization: Capture Alive, Espionage, Droid Hunting
   - Favorite Weapon: Blaster Rifle
   - Reputation Score: 85
   - Kills on Record: 67
   - Current Bounty Value: $120K
   - Mandalorian Clan Member: false
   - Jedi Hunter: true
   - Owns Starship: true
   - Sabacc Debt: $85K
   - Previous Employers: Darth Maul, Sidious, Corporate
   - Bounty Taking Progress: Target Acquired (index 3)

4. Aurra Sing
   - Hunter Codename: The Force Seeker
   - Bounty Success Rate: 82%
   - Specialization: Termination, Force Sensitivity, Recovery
   - Favorite Weapon: Lightsaber
   - Reputation Score: 78
   - Kills on Record: 43
   - Current Bounty Value: $80K
   - Mandalorian Clan Member: false
   - Jedi Hunter: true
   - Owns Starship: true
   - Sabacc Debt: $45K
   - Previous Employers: Grievous, Bounty Collective
   - Bounty Taking Progress: In Pursuit (index 2)

5. Bossk
   - Hunter Codename: The Trandoshan
   - Bounty Success Rate: 85%
   - Specialization: Capture Alive, Termination, Droid Hunting
   - Favorite Weapon: Blaster Rifle
   - Reputation Score: 81
   - Kills on Record: 104
   - Current Bounty Value: $150K
   - Mandalorian Clan Member: false
   - Jedi Hunter: false
   - Owns Starship: true
   - Sabacc Debt: $55K
   - Previous Employers: Bounty Collective, Various Crimelords
   - Bounty Taking Progress: In Pursuit (index 2)

6. Durge
   - Hunter Codename: The Immortal
   - Bounty Success Rate: 90%
   - Specialization: Termination, Droid Hunting
   - Favorite Weapon: Explosives
   - Reputation Score: 87
   - Kills on Record: 178
   - Current Bounty Value: $200K
   - Mandalorian Clan Member: false
   - Jedi Hunter: false
   - Owns Starship: true
   - Sabacc Debt: $0
   - Previous Employers: Dooku, Grievous
   - Bounty Taking Progress: Payment Received (index 4)

7. IG-88
   - Hunter Codename: Assassin Droid
   - Bounty Success Rate: 91%
   - Specialization: Termination, Espionage
   - Favorite Weapon: Blaster Rifle
   - Reputation Score: 84
   - Kills on Record: 256
   - Current Bounty Value: $90K
   - Mandalorian Clan Member: false
   - Jedi Hunter: false
   - Owns Starship: true
   - Sabacc Debt: $0
   - Previous Employers: Jabba, Bounty Collective
   - Bounty Taking Progress: Target Acquired (index 3)

8. Zam Wesell
   - Hunter Codename: The Shapeshifter
   - Bounty Success Rate: 79%
   - Specialization: Espionage, Recovery, Capture Alive
   - Favorite Weapon: Poison
   - Reputation Score: 76
   - Kills on Record: 38
   - Current Bounty Value: $60K
   - Mandalorian Clan Member: false
   - Jedi Hunter: false
   - Owns Starship: true
   - Sabacc Debt: $30K
   - Previous Employers: Bounty Collective, Private Clients
   - Bounty Taking Progress: In Pursuit (index 2)

9. Embo
   - Hunter Codename: The Tracker
   - Bounty Success Rate: 84%
   - Specialization: Capture Alive, Droid Hunting
   - Favorite Weapon: Blaster Rifle
   - Reputation Score: 80
   - Kills on Record: 71
   - Current Bounty Value: $110K
   - Mandalorian Clan Member: false
   - Jedi Hunter: false
   - Owns Starship: true
   - Sabacc Debt: $20K
   - Previous Employers: Bounty Collective, Various Clients
   - Bounty Taking Progress: Assigned (index 1)

10. Latts Razzi
    - Hunter Codename: The Tech Specialist
    - Bounty Success Rate: 81%
    - Specialization: Espionage, Droid Hunting, Termination
    - Favorite Weapon: Blaster Rifle
    - Reputation Score: 77
    - Kills on Record: 52
    - Current Bounty Value: $95K
    - Mandalorian Clan Member: true
    - Jedi Hunter: false
    - Owns Starship: true
    - Sabacc Debt: $40K
    - Previous Employers: Bounty Collective, Crimson Corsairs
    - Bounty Taking Progress: In Pursuit (index 2)

11. Oked Goofta
    - Hunter Codename: The Novice
    - Bounty Success Rate: 73%
    - Specialization: Recovery, Capture Alive
    - Favorite Weapon: Blaster Rifle
    - Reputation Score: 68
    - Kills on Record: 29
    - Current Bounty Value: $40K
    - Mandalorian Clan Member: false
    - Jedi Hunter: false
    - Owns Starship: true
    - Sabacc Debt: $65K
    - Previous Employers: Bounty Collective, Independent Work
    - Bounty Taking Progress: Contact (index 0)

12. Sugi
    - Hunter Codename: The Mandalorian Ally
    - Bounty Success Rate: 83%
    - Specialization: Capture Alive, Termination
    - Favorite Weapon: Mandalorian Armor
    - Reputation Score: 79
    - Kills on Record: 55
    - Current Bounty Value: $75K
    - Mandalorian Clan Member: true
    - Jedi Hunter: false
    - Owns Starship: true
    - Sabacc Debt: $15K
    - Previous Employers: Bounty Collective, Mandalorian Clan
    - Bounty Taking Progress: Assigned (index 1)

13. The Mandalorian (Din Djarin)
    - Hunter Codename: The Lone Wolf
    - Bounty Success Rate: 94%
    - Specialization: Capture Alive, Recovery
    - Favorite Weapon: Mandalorian Armor
    - Reputation Score: 91
    - Kills on Record: 122
    - Current Bounty Value: $180K
    - Mandalorian Clan Member: true
    - Jedi Hunter: false
    - Owns Starship: true
    - Sabacc Debt: $0
    - Previous Employers: Bounty Collective, Guild, Independent
    - Bounty Taking Progress: Payment Received (index 4)

14. Fennec Shand
    - Hunter Codename: The Spymaster
    - Bounty Success Rate: 92%
    - Specialization: Espionage, Recovery, Termination
    - Favorite Weapon: Blaster Rifle
    - Reputation Score: 88
    - Kills on Record: 98
    - Current Bounty Value: $160K
    - Mandalorian Clan Member: false
    - Jedi Hunter: false
    - Owns Starship: true
    - Sabacc Debt: $10K
    - Previous Employers: Cad Bane, Bounty Collective, Empire
    - Bounty Taking Progress: Target Acquired (index 3)

15. Black Krrsantan
    - Hunter Codename: The Wookiee Enforcer
    - Bounty Success Rate: 86%
    - Specialization: Termination, Capture Alive
    - Favorite Weapon: Explosives
    - Reputation Score: 83
    - Kills on Record: 87
    - Current Bounty Value: $140K
    - Mandalorian Clan Member: false
    - Jedi Hunter: false
    - Owns Starship: true
    - Sabacc Debt: $35K
    - Previous Employers: Leia, Bounty Collective, Various Clients
    - Bounty Taking Progress: In Pursuit (index 2)

**Sample Data - 20 Contacts:**
- Jabba the Hutt (Crime Lord) at Tatooine Palace
- Darth Vader (Imperial Representative) at Imperial Command
- Leia Organa (Rebel Contact) at Rebellion Base
- Han Solo (Smuggler Contact) at Cantina
- Maz Kanata (Information Broker) at Takodana Castle
- Peli Motto (Guild Administrator) at Mos Pelgo
- Cobb Vanth (Tatooine Sheriff) at Tatooine
- The Armorer (Mandalorian Representative) at Forge
- Cara Dune (Rebel Connection) at New Republic
- Greef Karga (Guild Officer) at Guild Hall
- Kuiil (Salvage Expert) at Compound
- IG-11 (Protocol Droid) at Guild
- Porg (Mysterious Contact) at Unknown
- Yaddle (Force Council) at Unknown
- Lando Calrissian (Fence/Handler) at Cloud City
- Qi'ra (Spice Runner) at Kessel
- Rio Durant (Militia Leader) at Tatooine
- Cad Bane (Rival Hunter) at Outer Rim
- Grogu (CustomRecord) at Unknown
- Vanth Cargo Master (Logistics) at Guild Hall

**Sample Data - 15 Contracts (Opportunities Linked to Hunters):**

1. Capture Luke Skywalker
   - Hunter: Boba Fett
   - Target Type: Jedi Fugitive
   - Target Threat Level: Force Sensitive
   - Bounty Amount: $50K
   - Capture vs Termination: Capture Alive
   - Contract Progress: In Pursuit (index 2)

2. Eliminate Rebel Cell Commander
   - Hunter: Cad Bane
   - Target Type: Rebel
   - Target Threat Level: High
   - Bounty Amount: $25K
   - Capture vs Termination: Termination Preferred
   - Contract Progress: Assigned (index 1)

3. Retrieve Stolen Artifact
   - Hunter: Bossk
   - Target Type: Corporate Executive
   - Target Threat Level: Medium
   - Bounty Amount: $15K
   - Capture vs Termination: Either
   - Contract Progress: In Pursuit (index 2)

4. Hunt Escaped Wookiee
   - Hunter: Embo
   - Target Type: Wanted Beast
   - Target Threat Level: High
   - Bounty Amount: $12K
   - Capture vs Termination: Capture Alive
   - Contract Progress: Assigned (index 1)

5. Find Padme Amidala's Hidden Child
   - Hunter: Aurra Sing
   - Target Type: Jedi Fugitive
   - Target Threat Level: High
   - Bounty Amount: $75K
   - Capture vs Termination: Capture Alive
   - Contract Progress: Posted (index 0)

6. Capture Obi-Wan Kenobi
   - Hunter: Boba Fett
   - Target Type: Jedi Fugitive
   - Target Threat Level: Legendary
   - Bounty Amount: $100K
   - Capture vs Termination: Capture Alive
   - Contract Progress: In Pursuit (index 2)

7. Eliminate Crime Syndicate Lieutenant
   - Hunter: Fennec Shand
   - Target Type: Criminal
   - Target Threat Level: Medium
   - Bounty Amount: $20K
   - Capture vs Termination: Termination Preferred
   - Contract Progress: Target Acquired (index 3)

8. Retrieve Carbonite Han Solo
   - Hunter: The Mandalorian (Din Djarin)
   - Target Type: Criminal
   - Target Threat Level: High
   - Bounty Amount: $30K
   - Capture vs Termination: Either
   - Contract Progress: In Pursuit (index 2)

9. Hunt Force-Sensitive Child
   - Hunter: Aurra Sing
   - Target Type: Force Sensitive
   - Target Threat Level: High
   - Bounty Amount: $40K
   - Capture vs Termination: Capture Alive
   - Contract Progress: In Pursuit (index 2)

10. Retrieve Imperial Data Vault
    - Hunter: IG-88
    - Target Type: Corporate Executive
    - Target Threat Level: High
    - Bounty Amount: $35K
    - Capture vs Termination: Either
    - Contract Progress: Assigned (index 1)

11. Eliminate Resistance Leader
    - Hunter: Cad Bane
    - Target Type: Rebel
    - Target Threat Level: Medium
    - Bounty Amount: $18K
    - Capture vs Termination: Termination Preferred
    - Contract Progress: Target Acquired (index 3)

12. Capture Leia Organa
    - Hunter: Boba Fett
    - Target Type: Rebel
    - Target Threat Level: High
    - Bounty Amount: $60K
    - Capture vs Termination: Capture Alive
    - Contract Progress: Posted (index 0)

13. Hunt Rogue Droid Army
    - Hunter: IG-88
    - Target Type: Droid
    - Target Threat Level: High
    - Bounty Amount: $22K
    - Capture vs Termination: Termination Preferred
    - Contract Progress: Assigned (index 1)

14. Retrieve Jedi Temple Vault Contents
    - Hunter: The Mandalorian (Din Djarin)
    - Target Type: Jedi Fugitive
    - Target Threat Level: Legendary
    - Bounty Amount: $150K
    - Capture vs Termination: Either
    - Contract Progress: In Pursuit (index 2)

15. Eliminate Death Star Director
    - Hunter: Boba Fett
    - Target Type: Corporate Executive
    - Target Threat Level: Legendary
    - Bounty Amount: $85K
    - Capture vs Termination: Termination Preferred
    - Contract Progress: Posted (index 0)

**Sample Data - 18 Activities:**

1. Contract Posting - Luke Skywalker - Email, 15 minutes, 2024-01-15, Completed, Guild announcement
2. Boba Fett Briefing - Meeting, 60 minutes, 2024-01-16, Scheduled, Contract assignment
3. Rebel Cell Pursuit - Call, 480 minutes, 2024-01-18, Completed, Hunting operation
4. Payment Processing - Fennec Shand - Email, 20 minutes, 2024-01-19, Completed, Contract payout
5. Reputation Update - Boba Fett - Call, 10 minutes, 2024-01-22, Completed, Success confirmation
6. Target Acquisition - Han Solo - Meeting, 30 minutes, 2024-01-23, Scheduled, Confirmation meeting
7. Sabacc Debt Settlement - Cad Bane - Meeting, 90 minutes, 2024-01-24, Completed, Gambling settlement
8. Guild Disciplinary Hearing - Call, 60 minutes, 2024-01-25, Scheduled, Conduct review
9. Contract Posting - Obi-Wan Kenobi - Email, 15 minutes, 2024-01-29, Completed, High-value contract
10. Hunter Briefing - Din Djarin - Meeting, 60 minutes, 2024-01-30, Scheduled, Mission briefing
11. Jedi Pursuit Operation - Call, 720 minutes, 2024-02-01, Completed, Active hunt
12. Payment Processing - IG-88 - Email, 20 minutes, 2024-02-02, Completed, Successful payout
13. Reputation Update - Fennec Shand - Call, 10 minutes, 2024-02-05, Completed, Elite status
14. Force-Sensitive Target Acquisition - Meeting, 30 minutes, 2024-02-06, Scheduled, Complex case
15. Sabacc Debt Settlement - Bossk - Meeting, 90 minutes, 2024-02-08, Completed, High stakes
16. Guild Membership Review - Call, 60 minutes, 2024-02-09, Scheduled, Annual review
17. Contract Posting - Leia Organa - Email, 15 minutes, 2024-02-12, Completed, Major contract
18. Final Payment Verification - Meeting, 30 minutes, 2024-02-15, Scheduled, Contract closure

**Standard Field Mapping (apply when creating all records):**
- Customer `status`: Derive from context — active bounty hunters with recent successful contracts → `active`; newer hunters or those with pending jobs → `prospect`; retired/deceased/blacklisted → `inactive`. Aim for ~60% active, 25% prospect, 15% inactive.
- Opportunity `stage`: Map from the contract workflow index — 0 = `prospecting`, 1 = `qualification`, 2 = `proposal`, 3 = `negotiation`, 4 = `closed_won`. Use `closed_lost` for failed/target-escaped contracts.
- Activity `status`: Extract from each description — "Completed" → `completed`, "Scheduled"/"In Progress" → `pending`, "Cancelled" → `cancelled`.

**Sample Data - 10 CustomRecords (use bulkCreateCustomRecords):**
1. Slave I — Firespray-31 Patrol Craft - type: spacecraft, status: active, serialNumber: SLV1-BF-001, customer: Boba Fett
2. Beskar Armor Set — Mando Grade - type: armor, status: active, serialNumber: BSK-MANDO-001, customer: The Mandalorian
3. IG-88 Combat Chassis Upgrade Module - type: hardware, status: active, serialNumber: IG88-UPGD-003, customer: IG-88
4. Infrared Targeting Goggles (Wookiee-Proof) - type: equipment, status: active, serialNumber: TGT-BOSSK-001, customer: Bossk
5. EE-3 Blaster Carbine — Modified - type: weapon, status: active, serialNumber: EE3-BF-MOD, customer: Boba Fett
6. Whistling Birds Wrist Launcher - type: weapon, status: active, serialNumber: WB-MANDO-001, customer: The Mandalorian
7. Encrypted Bounty Datapad Network - type: software, status: active, serialNumber: DPD-GUILD-001
8. Carbonite Freezing Chamber (Portable) - type: equipment, status: maintenance, serialNumber: CARB-PORT-001
9. Fennec Shand's Sniper Rifle Array - type: weapon, status: active, serialNumber: SNP-FEN-001, customer: Fennec Shand
10. Zam Wesell's Changeling Equipment - type: equipment, status: inactive, serialNumber: CHG-ZAM-001, customer: Zam Wesell

**Sample Data - 6 Orders:**
1. Boba Fett - "Mandalorian Armor Repair & Upgrade Package", status: DELIVERED, totalAmount: 45000, currency: USD
2. The Mandalorian - "Beskar Forge Commission — Full Set", status: CONFIRMED, totalAmount: 82000, currency: USD
3. IG-88 - "Combat Chassis Refurbishment Bundle", status: SHIPPED, totalAmount: 28000, currency: USD
4. Bossk - "Trandoshan Hunting Gear Upgrade", status: DRAFT, totalAmount: 18500, currency: USD
5. Fennec Shand - "Sniper Optics & Stabilizer Set", status: CANCELLED, totalAmount: 12000, currency: USD
6. Dengar - "Medical Reconstruction Parts", status: DELIVERED, totalAmount: 35000, currency: USD

**Sample Data - 6 Invoices:**
1. Boba Fett - status: PAID, totalAmount: 45000, currency: USD, paymentTerms: NET-14
2. The Mandalorian - status: SENT, totalAmount: 82000, currency: USD, paymentTerms: NET-30
3. IG-88 - status: PAID, totalAmount: 28000, currency: USD, paymentTerms: NET-14
4. Bossk - status: DRAFT, totalAmount: 18500, currency: USD, paymentTerms: NET-30
5. Fennec Shand - status: OVERDUE, totalAmount: 12000, currency: USD, paymentTerms: NET-14
6. Dengar - status: PAID, totalAmount: 35000, currency: USD, paymentTerms: NET-30

**Document Templates (3 custom styles — use createDocumentTemplate for each):**
1. "Bounty Contract Dossier" — templateType: slide_deck, description: "Encrypted dark-theme target briefing for Guild dispatch", styleJson: {"layout":"corporate","accentColor":"#8D6E63","backgroundColor":"#12100E","h1Color":"#FFD54F"}
2. "Hunter Profile" — templateType: one_pager, description: "Single-page hunter capability summary for client review", styleJson: {"layout":"light","accentColor":"#8D6E63","includeCustomFields":true}
3. "Guild Registry Export" — templateType: csv_export, description: "Full hunter registry export with success rate and specialization data", styleJson: {"includeFields":["name","status","species","signature_weapon","success_rate","active_contracts"]}

**Notification Templates (3 Guild transmission templates — use createNotificationTemplate for each):**
1. name: "New Bounty Posted", notificationType: OPPORTUNITY_CREATED, isActive: true
   subject: "New Bounty: {{opportunityName}} — {{amount}} Credits"
   body: "A new bounty contract is available.\n\nPosted by: {{customerName}}\nTarget: {{opportunityName}}\nReward: {{amount}} Credits\nPriority: {{stage}}\n\nAccept contract: {{link}}"
2. name: "Contract Status Update", notificationType: OPPORTUNITY_UPDATED, isActive: true
   subject: "Contract Update: {{opportunityName}} — Stage {{stage}}"
   body: "Bounty contract status updated.\n\nHunter: {{customerName}}\nContract: {{opportunityName}}\nStatus: {{stage}}\nAssigned: {{assignee}}\n\n{{link}}"
3. name: "Payment Processed", notificationType: INVOICE_CREATED, isActive: true
   subject: "Payment Processed: {{customerName}} — {{amount}} Credits"
   body: "Guild payment has been processed.\n\nHunter: {{customerName}}\nContract: {{opportunityName}}\nAmount: {{amount}} Credits\nDue: {{dueDate}}\n\nConfirm receipt: {{link}}"

Please execute this complete setup now, creating all fields, policies, hunters, contacts, contracts, activities, customRecords, orders, invoices, document templates, and notification templates. Update the tenant settings with the branding information provided. The credits await!
```

---

## 📖 How to Use These Star Wars Prompts

1. **Open this file**: ASSISTANT_SETUP_PROMPTS_STARWARS.md
2. **Copy one entire prompt** (the complete text block from the code fence)
3. **Go to chat**: http://localhost:5173/onboarding
4. **Create new tenant** → Skip wizard → Go to Chat
5. **Paste the prompt** and hit Enter
6. **Wait ~4-5 minutes** (executes automatically)
7. **Repeat for other two prompts**

**Each prompt creates**:
- ✅ 13 unique custom fields
- ✅ 2 calculated fields with themed SpEL expressions
- ✅ 2-3 business policies
- ✅ 15 sample records (suppliers, territories, or hunters)
- ✅ 20 themed contacts
- ✅ 15 contracts/missions/operations
- ✅ 18+ activities
- ✅ Branded company settings

**Total setup time**: ~15 minutes for all 3 Star Wars tenants

---

## 🌟 The Force Awaits

May these prompts bring chaos and order to your galaxy. Choose your side wisely!

