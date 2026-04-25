# /new-integration

**Status**: Available after Track 4 (Integration Framework) is implemented.

Scaffold a new vendor integration adapter following BOCRM's integration framework pattern.

## Ask me for:
1. Vendor name (e.g., `Slack`, `HubSpot`, `Stripe`)
2. Integration type: `OUTGOING` (CRM → vendor) or `INCOMING` (vendor → CRM) or `BIDIRECTIONAL`
3. Events to listen for (e.g., `CUSTOMER_CREATED`, `OPPORTUNITY_UPDATED`)
4. Configuration required (API key, webhook URL, field mappings)
5. Optional: credential fields to encrypt

## What I'll do:
1. **Create adapter class** — `integration/adapters/FooAdapter.java`
   - Implements `IntegrationAdapter` interface
   - Handles `canHandle(OutboxEvent)` logic
   - Implements `process(event, config)` with vendor API calls

2. **Register adapter** in `integration/IntegrationRegistry.java`
   - Add to the list of available adapters
   - Make discoverable via `/integrations/available` endpoint

3. **Create migration** with config schema
   - `integration_definitions` entry for the new adapter
   - JSON schema for credentials and field mappings

4. **Update admin UI** to support new config form
   - Auto-render credential input fields
   - Optional: field mapping UI if needed

## Example adapter structure:
```java
@Component
public class SlackAdapter implements IntegrationAdapter {
  
  @Override
  public String name() {
    return "slack";
  }
  
  @Override
  public boolean canHandle(OutboxEvent event) {
    return "CUSTOMER_CREATED".equals(event.getEventType());
  }
  
  @Override
  public void process(OutboxEvent event, IntegrationConfig config) {
    String webhookUrl = config.getCredentials().path("webhook_url").asText();
    String message = formatCustomerMessage((Customer) event.getPayload());
    httpClient.postJson(webhookUrl, message);
  }
}
```

## Notes:
- Credentials are stored encrypted; decrypt inside `process()`
- Use `IntegrationEventRouter` to test routing works
- Adapter must be idempotent (safe to retry)
