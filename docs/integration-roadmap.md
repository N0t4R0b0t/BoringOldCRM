# Integration Framework Roadmap & Future Integration Suggestions

**Status**: Community Feedback Welcomed  
**Last Updated**: April 2026  
**Horizon**: 2026-2027

---

## Overview

BOCRM's integration framework (Track 4) provides a solid foundation for connecting external services. This document outlines planned integrations and suggests additional services that would provide significant value to users.

The framework uses an **adapter pattern** with AES-256-GCM credential storage, making it easy to add new integrations without core system changes.

---

## Tier 1: High Priority (6-9 months)

These integrations address the largest user bases and provide immediate ROI.

### 1. Salesforce Integration

**Why**: Salesforce users need bidirectional sync between BOCRM and Salesforce CRM

**Scope**:
- Two-way sync: Accounts ↔ Customers, Contacts ↔ Contacts
- Field mapping config UI (user-defined property mapping)
- Conflict resolution strategy (last-write-wins, manual approval)
- Real-time push OR scheduled batch sync

**Estimated Effort**: 3-4 sprints  
**Complexity**: High (OAuth, batch API, bulk API considerations)  
**Dependencies**:
- Salesforce SDK (salesforce-cli-core or Force.com REST API)
- Field mapping service (generic key-value config)
- Conflict resolution service

**API Reference**: [Salesforce REST API v60](https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/)

**Sample Adapter Structure**:
```java
@Component
public class SalesforceAdapter implements IntegrationAdapter {
  private String getAuthToken(Map<String, String> credentials) {
    // OAuth 2.0 flow with clientId, clientSecret, refreshToken
  }
  
  void syncCustomer(Customer customer, Map<String, String> creds) {
    // PATCH /services/data/v60.0/sobjects/Account/{sfId}
  }
  
  void syncContact(Contact contact, Map<String, String> creds) {
    // PATCH /services/data/v60.0/sobjects/Contact/{sfId}
  }
}
```

---

### 2. Google Workspace Integration

**Why**: 80% of business users have Gmail/Google Drive; heavy integration demand

**Scope A: Gmail**
- Send CRM event summary emails
- BCC mode: auto-file emails in Google Drive by customer/contact
- Email sync: bring email threads into BOCRM Activity timeline

**Scope B: Google Sheets**
- Export opportunities/pipeline to auto-updating Sheet
- Bulk import customers from Sheet (CSV + API)
- Real-time collaboration on shared data

**Scope C: Google Drive**
- Auto-organize documents by customer/contact
- Share folder structure with sales team
- Link Drive documents in BOCRM as customRecords

**Estimated Effort**: 2-3 sprints per scope (recommend starting with Sheets)  
**Complexity**: Medium (OAuth 2.0, quota management)  
**Dependencies**:
- Google API Client Library (google-api-services-sheets, google-api-services-drive, google-api-services-gmail)
- OAuth 2.0 token management service

**API Reference**: [Google Workspace APIs](https://developers.google.com/workspace/products)

**Sample Adapter Structure**:
```java
@Component
public class GoogleSheetsAdapter implements IntegrationAdapter {
  void exportOpportunities(List<Opportunity> opps, Map<String, String> creds) {
    // Add rows to Sheet: spreadsheetId, range="Sheet1!A1"
    // POST /v4/spreadsheets/{spreadsheetId}/values/Sheet1!A1:append
  }
  
  void updateSheet(JsonNode data, Map<String, String> creds) {
    // Append or update based on entityId
  }
}
```

---

### 3. Microsoft Teams Integration

**Why**: Teams is growing enterprise standard; alternative to Slack for many orgs

**Scope**:
- Send events as adaptive cards to Teams channels
- Richer UI than Slack (buttons, colors, fields)
- Thread management (group related events)
- Mention support (@user for important events)

**Estimated Effort**: 1-2 sprints  
**Complexity**: Low (similar to Slack, but adaptive cards are more sophisticated)  
**Dependencies**:
- Incoming Webhook or Bot integration
- Teams API SDK (teams-java-sdk or native REST)

**API Reference**: [Microsoft Teams Webhooks](https://learn.microsoft.com/en-us/microsoftteams/platform/webhooks-and-connectors/how-to/connectors-using)

**Sample Payload**:
```json
{
  "@type": "MessageCard",
  "@context": "https://schema.org/extensions",
  "summary": "Customer Created",
  "themeColor": "0078D4",
  "sections": [
    {
      "activityTitle": "New Customer: Acme Corp",
      "text": "Status: active, Revenue: $50k/year",
      "potentialAction": [
        {
          "@type": "OpenUri",
          "name": "View in BOCRM",
          "targets": [{"os": "default", "uri": "https://bocrm.app/customers/42"}]
        }
      ]
    }
  ]
}
```

---

### 4. GitHub / GitLab Integration

**Why**: Tech-heavy orgs need to link sales deals with engineering milestones

**Scope**:
- Create GitHub issues from high-value opportunities
- Auto-close issue when opportunity closes
- Ping engineering Slack when sales activity (product interest)
- Link GitHub releases to customer communications

**Estimated Effort**: 2-3 sprints  
**Complexity**: Medium (webhook verification, issue state management)  
**Dependencies**:
- GitHub API v3 or GitLab API
- Webhook signature verification (HMAC)

**API Reference**: [GitHub REST API](https://docs.github.com/en/rest)

---

### 5. Jira Integration

**Why**: Operations teams use Jira; want to ticket high-value customer issues

**Scope**:
- Create Jira issue from BOCRM Activity (support request)
- Link issue to Customer
- Sync status (BOCRM activity completed → Jira issue closed)
- Auto-assign to team based on issue type

**Estimated Effort**: 2-3 sprints  
**Complexity**: Medium (Jira OAuth, custom fields, transitions)  
**Dependencies**:
- Jira REST API 3
- Jira automation rules (for reciprocal updates)

**API Reference**: [Atlassian Cloud REST API v3](https://developer.atlassian.com/cloud/jira/rest/v3/)

---

## Tier 2: Medium Priority (9-18 months)

These address mid-market needs or specific use cases.

### 6. Twilio (SMS / WhatsApp)

**Why**: Sales teams send SMS follow-ups; auto-log in CRM

**Scope**:
- Send SMS when opportunity created (auto-alert)
- Receive SMS as Activity (webhook)
- WhatsApp integration (business account)
- Two-way conversation threading

**Estimated Effort**: 2 sprints  
**Complexity**: Low  
**Dependencies**: Twilio SDK (twilio-java)

---

### 7. Stripe / Payment Gateway Integration

**Why**: BOCRM handles orders/invoices; need payment tracking

**Scope**:
- Create Stripe customer from BOCRM Customer
- Auto-create invoice on Stripe when Order placed
- Receive payment webhook → auto-mark Invoice paid
- Sync subscription status

**Estimated Effort**: 2-3 sprints  
**Complexity**: Medium (PCI compliance, webhook security)  
**Dependencies**: Stripe Java SDK

---

### 8. Mailchimp / Email Marketing Integration

**Why**: Sales teams manage mailing lists; want CRM-driven campaigns

**Scope**:
- Sync Contacts to Mailchimp list
- Create list segment by Opportunity stage
- Log email opens/clicks as Activities
- Bi-directional unsubscribe sync

**Estimated Effort**: 2 sprints  
**Complexity**: Low-Medium  
**Dependencies**: Mailchimp SDK or REST API

---

### 9. Asana / Monday.com (Project Management)

**Why**: Sales ops teams track deal progress in project boards

**Scope**:
- Create Asana task from Opportunity
- Update task status when opportunity stage changes
- Assign to sales rep
- Attach documents/files

**Estimated Effort**: 2 sprints  
**Complexity**: Low-Medium  
**Dependencies**: Asana API or Monday.com API

---

### 10. Calendly / Scheduling Integration

**Why**: Sales reps spend time booking calls; auto-log to CRM

**Scope**:
- Sync BOCRM Contacts to Calendly
- Log scheduled call as Activity with Calendly link
- Add Calendly availability to contact profile
- Timezone-aware scheduling

**Estimated Effort**: 1-2 sprints  
**Complexity**: Low  
**Dependencies**: Calendly API (OAuth)

---

### 11. Airtable Integration

**Why**: Small/mid-market teams use Airtable for operations; sync CRM

**Scope**:
- Bi-directional Customer sync
- Opportunity pipeline visualization
- Custom field mapping
- Webhook-based real-time updates

**Estimated Effort**: 2 sprints  
**Complexity**: Low-Medium  
**Dependencies**: Airtable API

---

### 12. Notion Integration

**Why**: Team wikis store customer intel; want to organize by BOCRM data

**Scope**:
- Create Notion page per Customer (linked database)
- Embed BOCRM customer profile in Notion
- Sync custom fields as Notion properties
- Two-way notes sync

**Estimated Effort**: 2 sprints  
**Complexity**: Low (Notion API is simple)  
**Dependencies**: Notion API SDK

---

## Tier 3: Nice-to-Have (18+ months)

### 13. Zoom Integration
- Auto-create Zoom meeting link in Activity
- Log meeting recording link

### 14. Freshdesk / Help Scout (Support Ticketing)
- Link support ticket to Customer
- Auto-create ticket from BOCRM comment
- Sync resolution status

### 15. Intercom / Drift (Chat / Customer Communication)
- Embed BOCRM customer profile in chat
- Log conversation as Activity
- Two-way message sync

### 16. LinkedIn / Hunter (Prospecting)
- Enrich contact via Hunter email finder
- Import LinkedIn profile data
- Match BOCRM contacts to LinkedIn profiles

### 17. AWS / Cloud Storage (S3, GCP Cloud Storage)
- Backup BOCRM data to cloud
- Store document customRecords
- Implement S3 pre-signed URLs for file access

### 18. Segment / Analytics (Data Integration)
- Send CRM events to Segment
- Route to downstream tools (GA, Amplitude, Mixpanel)
- Unified customer data platform

### 19. Auth0 / Okta (Identity Management)
- Sync BOCRM users with corporate directory
- JIT provisioning
- Single sign-on improvements

### 20. Redis / Cache Integration
- Cache integration credentials
- Faster credential decryption
- Session management for long-lived OAuth tokens

---

## Technical Guidelines for New Adapters

### 1. Credential Storage

```java
// Always use AES-256-GCM via EncryptionService
@Autowired private EncryptionService encryptionService;

// In adapter process method
Map<String, String> credentials = decryptCredentials(config.getCredentialsEncrypted());
String apiKey = credentials.get("apiKey");  // Now in memory
```

### 2. Error Handling

```java
try {
  adapter.process(event, credentials);
  // Mark success: event.setPublishedAt(now()) happens in poller
} catch (Exception e) {
  log.error("Integration delivery failed: {}", e.getMessage());
  // Poller will increment retry_count automatically
  throw e;  // Let poller handle retry
}
```

### 3. Idempotency

```java
// Always use entity ID + timestamp as idempotency key
String idempotencyKey = String.format("%s_%d_%d", 
  event.getEntityType(), 
  event.getEntityId(), 
  event.getCreatedAt().toInstant().toEpochMilli());

// External system should reject duplicates with same key
httpClient.post(url, body, Map.of("Idempotency-Key", idempotencyKey));
```

### 4. Logging

```java
// Log non-sensitive data only
log.info("Adapter {} processing event {} for entity {}", 
  adapter.name(), event.getId(), event.getEntityType());

// DO NOT log credentials, tokens, or sensitive data
// log.debug("API Key: " + apiKey);  // WRONG
```

### 5. Testing

```java
@Test
void testAdapterProcessing() {
  Map<String, String> creds = Map.of("apiKey", "test-key");
  OutboxEvent event = createTestEvent();
  
  // Mock external service
  when(mockHttpClient.post(any(), any(), any()))
    .thenReturn(200);
  
  adapter.process(event, creds);
  
  // Verify correct endpoint called
  verify(mockHttpClient).post(
    eq("https://api.example.com/events"),
    any(),
    any()
  );
}
```

---

## Evaluation Criteria for New Integrations

Before proposing a new integration, evaluate:

| Criterion | Weight | Notes |
|-----------|--------|-------|
| **Market Demand** | 25% | User requests, competitive offerings |
| **Ease of Implementation** | 20% | API quality, SDK availability, complexity |
| **ROI / Adoption** | 20% | Estimated user adoption %, revenue impact |
| **Maintenance Cost** | 15% | API stability, rate limits, version updates |
| **Differentiation** | 10% | Does it set BOCRM apart from competitors? |
| **Security Posture** | 10% | OAuth support, data residency, compliance |

**Minimum Score for Tier 1**: 18/25  
**Minimum Score for Tier 2**: 15/25

---

## Implementation Checklist for New Adapter

- [ ] Design API client (mock external service first)
- [ ] Implement IntegrationAdapter interface
- [ ] Create integration tests with mocked external API
- [ ] Add credential fields to integration form
- [ ] Update apiClient.ts if UI form changes needed
- [ ] Write adapter documentation (what syncs, field mapping, limitations)
- [ ] Add integration to ADAPTERS array in IntegrationsAdminPage.tsx
- [ ] Test end-to-end (create config, trigger event, verify delivery)
- [ ] Load test (100+ events/second throughput)
- [ ] Security audit (no credential logging, HTTPS only)
- [ ] Add to this roadmap document
- [ ] Create GitHub issue for future maintenance

---

## FAQ: Adding Integrations

### Q: Do I need to modify IntegrationController?
**A**: No. Controller is generic (CRUD for any adapter type). Just create new Adapter implementation.

### Q: How do I test without real credentials?
**A**: Use Webhook.site or RequestBin for webhooks. Mock HTTP client for others.

### Q: What if external API has rate limits?
**A**: Adapter should respect limits and return appropriate error. Poller will retry. Consider batch operations for high-volume adapters.

### Q: Can adapters call other adapters?
**A**: Not recommended. Keep adapters focused on one external system.

### Q: How do I handle OAuth token refresh?
**A**: Store refreshToken in credentials. When access token expires, use refresh token to get new one, update credentials in DB.

---

## Contribution Guidelines

Want to propose a new integration?

1. **Research the API**: Evaluate ease of implementation
2. **File a GitHub Issue** with:
   - Integration name
   - Use case / why BOCRM needs it
   - API documentation link
   - Estimated effort (sprints)
   - Scoring against evaluation criteria
3. **Discuss with team**: Community feedback shapes roadmap
4. **Implement** (if approved): Follow technical guidelines above
5. **Submit PR** with tests, docs, and adapter code

---

## Long-Term Vision

By end of 2027, BOCRM should support 20+ integrations, making it the **most connected CRM for small/mid-market businesses**:

- **Productivity**: Sync with Slack, Teams, Email, Calendly
- **Operations**: Connect Jira, Asana, GitHub, project boards
- **Growth**: Mailchimp, Zapier, Google Sheets, Airtable
- **Sales Stack**: Salesforce, HubSpot (bidirectional), Stripe, payment gateways
- **Analytics**: Segment, Google Analytics, Mixpanel
- **Infrastructure**: AWS, GCP, Azure backup; Auth0, Okta SSO

---

## Monitoring Integration Health

### Recommended Dashboards

```sql
-- Integration adoption by adapter type
SELECT adapter_type, COUNT(*) as config_count 
FROM integration_configs 
GROUP BY adapter_type;

-- Delivery success rate (past 24h)
SELECT 
  adapter_type,
  COUNT(*) as total_events,
  SUM(CASE WHEN published_at IS NOT NULL THEN 1 ELSE 0 END) as delivered,
  ROUND(SUM(CASE WHEN published_at IS NOT NULL THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as success_rate
FROM outbox_events
WHERE event_type = 'CRM_EVENT' AND created_at > NOW() - INTERVAL 24 HOUR
GROUP BY adapter_type;

-- High-retry events (potential failures)
SELECT id, adapter_type, retry_count, created_at
FROM outbox_events
WHERE event_type = 'CRM_EVENT' AND published_at IS NULL AND retry_count > 3
ORDER BY retry_count DESC;
```

### Alerting

- [ ] Alert when integration success rate < 95% for 1 hour
- [ ] Alert when OutboxEvent.retry_count > 10 (stuck event)
- [ ] Monitor external API rate limit errors
- [ ] Track EncryptionService decryption failures

---

## References

- [Track 4 Implementation Details](/integration-framework.md)
- [GitHub Issues: Integration Requests](https://github.com/your-org/bocrm/labels/integration)
- [Slack App Directory](https://api.slack.com/apps)
- [Salesforce Developer Docs](https://developer.salesforce.com/)
- [Google Workspace APIs](https://developers.google.com/workspace)

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Apr 2026 | Initial roadmap with 20 integration suggestions |

