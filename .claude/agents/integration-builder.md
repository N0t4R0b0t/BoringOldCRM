# integration-builder Agent

**Purpose**: Specialized for building new vendor integration adapters once the integration framework is implemented. Scaffolds adapter class, configuration schema, migration, and optional admin UI.

## When to use

- User requests a new vendor integration (Slack, HubSpot, Stripe, etc.)
- Track 4 (Integration Framework) has been completed
- Clear specification of what events to handle and what data to sync

## What it knows (post-implementation)

- `IntegrationAdapter` interface: `name()`, `canHandle(OutboxEvent)`, `process(OutboxEvent, IntegrationConfig)`
- Event types: CUSTOMER_CREATED, CUSTOMER_UPDATED, OPPORTUNITY_CREATED, etc.
- Credential handling: credentials stored encrypted in `integration_configs` table
- `IntegrationEventRouter`: how events are dispatched from the outbox poller
- Idempotency: adapters must be safe to retry (events re-processed if dispatch failed)
- Field mappings: JSON config for mapping CRM fields → vendor fields
- Webhook security: HMAC-SHA256 signature verification for incoming webhooks

## Output

- Adapter class (`integration/adapters/FooAdapter.java`)
- Registration in `IntegrationRegistry`
- Migration with config schema for the new adapter
- Test for adapter (if applicable)
- Optional: admin UI components for credential/mapping config

## Example request

```
Create a Slack integration that:
- Listens to CUSTOMER_CREATED, OPPORTUNITY_CREATED events
- Posts a message to #crm-updates channel with customer/opportunity details
- Requires webhook_url and channel_name configuration
- Supports field mapping for message formatting
```
