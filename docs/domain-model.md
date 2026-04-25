# Domain model

## Core entities
- Customer
- Contact
- Opportunity
- Activity

## Example relational tables
- customers(id, tenant_id, name, status, created_at, updated_at)
- contacts(id, tenant_id, customer_id, name, email, phone, title)
- opportunities(id, tenant_id, customer_id, stage, value, close_date)
- activities(id, tenant_id, subject, type, due_at, owner_id)

## Custom fields
- custom_field_definitions(id, tenant_id, entity_type, key, label, type, config_jsonb)
- entity_custom_fields(id, tenant_id, entity_type, entity_id, data_jsonb)

## Indexing notes
- Composite indexes on (tenant_id, id)
- GIN index on JSONB columns where needed
- Partial indexes on active entities or current stage
