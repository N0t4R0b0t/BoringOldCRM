-- Initial tenant schema creation
-- This migration creates the complete tenant schema with all core CRM tables,
-- custom field management, calculated fields, audit logging, and outbox for async operations.

CREATE TABLE IF NOT EXISTS customers (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'active',
  owner_id BIGINT,
  custom_data JSONB,
  table_data_jsonb JSONB,
  industry VARCHAR(255),
  website VARCHAR(255),
  notes TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS contacts (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  customer_id BIGINT,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255),
  phone VARCHAR(20),
  title VARCHAR(100),
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  notes TEXT,
  status VARCHAR(50) NOT NULL DEFAULT 'active',
  custom_data JSONB,
  table_data_jsonb JSONB,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS opportunities (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  stage VARCHAR(50) NOT NULL DEFAULT 'prospecting',
  status VARCHAR(50),
  value NUMERIC(15,2),
  probability NUMERIC(5,2) DEFAULT 0,
  close_date DATE,
  expected_close_date TIMESTAMP,
  notes TEXT,
  owner_id BIGINT,
  opportunity_type_slug VARCHAR(100),
  custom_data JSONB,
  table_data_jsonb JSONB,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS opportunity_types (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  slug VARCHAR(100) NOT NULL,
  description TEXT,
  display_order INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(tenant_id, slug)
);

CREATE TABLE IF NOT EXISTS activities (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  subject VARCHAR(255) NOT NULL,
  type VARCHAR(50) NOT NULL,
  description TEXT,
  due_at TIMESTAMP,
  owner_id BIGINT,
  related_type VARCHAR(50),
  related_id BIGINT,
  status VARCHAR(50) NOT NULL DEFAULT 'pending',
  custom_data JSONB,
  table_data_jsonb JSONB,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS custom_field_definitions (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entity_type VARCHAR(50) NOT NULL,
  key VARCHAR(100) NOT NULL,
  label VARCHAR(255) NOT NULL,
  type VARCHAR(50) NOT NULL,
  config_jsonb JSONB NOT NULL DEFAULT '{}',
  required BOOLEAN DEFAULT FALSE,
  display_in_table BOOLEAN,
  display_order INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(tenant_id, entity_type, key)
);

CREATE TABLE IF NOT EXISTS entity_custom_fields (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entity_type VARCHAR(50) NOT NULL,
  entity_id BIGINT NOT NULL,
  data_jsonb JSONB NOT NULL DEFAULT '{}',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(tenant_id, entity_type, entity_id)
);

CREATE TABLE IF NOT EXISTS calculated_field_definitions (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entity_type VARCHAR(50) NOT NULL,
  key VARCHAR(100) NOT NULL,
  label VARCHAR(255) NOT NULL,
  expression TEXT NOT NULL,
  return_type VARCHAR(50) NOT NULL,
  config_jsonb JSONB NOT NULL DEFAULT '{}',
  enabled BOOLEAN NOT NULL DEFAULT true,
  display_order INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(tenant_id, entity_type, key)
);

CREATE TABLE IF NOT EXISTS calculated_field_values (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entity_type VARCHAR(50) NOT NULL,
  entity_id BIGINT NOT NULL,
  calculated_field_id BIGINT NOT NULL,
  value_jsonb JSONB,
  computed_at TIMESTAMP,
  UNIQUE(tenant_id, entity_type, entity_id, calculated_field_id)
);

CREATE TABLE IF NOT EXISTS calculated_field_refresh_queue (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entity_type VARCHAR(50) NOT NULL,
  entity_id BIGINT NOT NULL,
  calculated_field_id BIGINT,
  reason VARCHAR(255),
  status VARCHAR(50) NOT NULL DEFAULT 'pending',
  queued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  actor_id BIGINT,
  action VARCHAR(50) NOT NULL,
  entity_type VARCHAR(50),
  entity_id BIGINT,
  payload_jsonb JSONB NOT NULL DEFAULT '{}',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS outbox_events (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  payload_jsonb JSONB NOT NULL DEFAULT '{}',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  published_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_messages (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(50) NOT NULL,
  content TEXT NOT NULL,
  context_entity_type VARCHAR(100),
  context_entity_id BIGINT,
  session_id VARCHAR(100),
  input_tokens INTEGER DEFAULT 0,
  output_tokens INTEGER DEFAULT 0,
  model_used VARCHAR(100),
  tool_calls JSONB,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS token_usage_ledger (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  session_id VARCHAR(100),
  input_tokens INTEGER NOT NULL DEFAULT 0,
  output_tokens INTEGER NOT NULL DEFAULT 0,
  model_used VARCHAR(100),
  operation VARCHAR(100),
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS saved_filters (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  entity_type VARCHAR(100) NOT NULL,
  filter_config JSONB NOT NULL,
  created_by BIGINT NOT NULL,
  is_public BOOLEAN NOT NULL DEFAULT FALSE,
  tenant_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS tenant_backup_jobs (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  type VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  includes_data BOOLEAN DEFAULT false,
  label VARCHAR(255),
  payload TEXT,
  result_payload TEXT,
  progress INTEGER DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  started_at TIMESTAMP,
  completed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tenant_documents (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  mime_type VARCHAR(100),
  size_bytes BIGINT,
  content_base64 TEXT,
  storage_url VARCHAR(1024),
  content_type VARCHAR(50) NOT NULL DEFAULT 'file',
  tags JSONB,
  source VARCHAR(50) NOT NULL DEFAULT 'user_upload',
  linked_entity_type VARCHAR(50),
  linked_entity_id BIGINT,
  linked_field_key VARCHAR(255),
  owner_id BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS assets (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  type VARCHAR(100),
  serial_number VARCHAR(255),
  status VARCHAR(50) NOT NULL DEFAULT 'active',
  customer_id BIGINT,
  notes TEXT,
  custom_data JSONB,
  table_data_jsonb JSONB,
  owner_id BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS opportunity_assets (
  opportunity_id BIGINT NOT NULL,
  asset_id BIGINT NOT NULL,
  PRIMARY KEY (opportunity_id, asset_id)
);

CREATE TABLE IF NOT EXISTS opportunity_contacts (
  opportunity_id BIGINT NOT NULL,
  contact_id BIGINT NOT NULL,
  PRIMARY KEY (opportunity_id, contact_id)
);

CREATE TABLE IF NOT EXISTS user_groups (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS user_group_memberships (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (group_id, user_id)
);

CREATE TABLE IF NOT EXISTS record_access_policies (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entity_type VARCHAR(50) NOT NULL,
  entity_id BIGINT NOT NULL,
  access_mode VARCHAR(20) NOT NULL,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (tenant_id, entity_type, entity_id)
);

CREATE TABLE IF NOT EXISTS record_access_grants (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entity_type VARCHAR(50) NOT NULL,
  entity_id BIGINT NOT NULL,
  grantee_type VARCHAR(10) NOT NULL,
  grantee_id BIGINT NOT NULL,
  permission VARCHAR(10) NOT NULL DEFAULT 'WRITE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (tenant_id, entity_type, entity_id, grantee_type, grantee_id)
);

CREATE TABLE IF NOT EXISTS policy_rules (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  entity_type VARCHAR(50) NOT NULL,
  operations JSONB NOT NULL,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  expression TEXT NOT NULL,
  severity VARCHAR(10) NOT NULL DEFAULT 'DENY',
  enabled BOOLEAN NOT NULL DEFAULT true,
  display_order INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (tenant_id, entity_type, name)
);

CREATE TABLE IF NOT EXISTS document_templates (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  template_type VARCHAR(50) NOT NULL,
  style_json TEXT,
  is_default BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification_inbox (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  notification_type VARCHAR(50) NOT NULL,
  entity_type VARCHAR(50),
  entity_id BIGINT,
  actor_user_id BIGINT,
  actor_display_name VARCHAR(255),
  message TEXT NOT NULL,
  read_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification_templates (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  notification_type VARCHAR(50) NOT NULL,
  name VARCHAR(255),
  subject_template TEXT,
  body_template TEXT,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index creation
CREATE INDEX IF NOT EXISTS idx_customers_tenant_id ON customers(tenant_id, id);
CREATE INDEX IF NOT EXISTS idx_contacts_tenant_id ON contacts(tenant_id, id);
CREATE INDEX IF NOT EXISTS idx_contacts_customer_id ON contacts(tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_opportunities_tenant_id ON opportunities(tenant_id, id);
CREATE INDEX IF NOT EXISTS idx_opportunities_customer_id ON opportunities(tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_activities_tenant_id ON activities(tenant_id, id);
CREATE INDEX IF NOT EXISTS idx_activities_related ON activities(tenant_id, related_type, related_id);
CREATE INDEX IF NOT EXISTS idx_custom_field_definitions_tenant ON custom_field_definitions(tenant_id, entity_type);
CREATE INDEX IF NOT EXISTS idx_entity_custom_fields_tenant ON entity_custom_fields(tenant_id, entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_calculated_field_defs_tenant ON calculated_field_definitions(tenant_id, entity_type);
CREATE INDEX IF NOT EXISTS idx_calculated_field_values_tenant ON calculated_field_values(tenant_id, entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_calculated_field_refresh_queue_status ON calculated_field_refresh_queue(status, queued_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_created ON audit_log(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_events_published ON outbox_events(published_at, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_events_pending ON outbox_events(published_at) WHERE published_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_chat_messages_tenant_id ON chat_messages(tenant_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_user_id ON chat_messages(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_context ON chat_messages(context_entity_type, context_entity_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_created_at ON chat_messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_id ON chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_token_usage_ledger_user_id ON token_usage_ledger(user_id);
CREATE INDEX IF NOT EXISTS idx_saved_filters_tenant_id ON saved_filters(tenant_id);
CREATE INDEX IF NOT EXISTS idx_saved_filters_created_by ON saved_filters(created_by);
CREATE INDEX IF NOT EXISTS idx_saved_filters_is_public ON saved_filters(is_public);
CREATE INDEX IF NOT EXISTS idx_tenant_documents_tenant ON tenant_documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_documents_linked ON tenant_documents(tenant_id, linked_entity_type, linked_entity_id);
CREATE INDEX IF NOT EXISTS idx_tenant_documents_source ON tenant_documents(tenant_id, source);
CREATE INDEX IF NOT EXISTS idx_tenant_documents_field ON tenant_documents(tenant_id, linked_entity_type, linked_entity_id, linked_field_key);
CREATE INDEX IF NOT EXISTS idx_assets_tenant_id ON assets(tenant_id);
CREATE INDEX IF NOT EXISTS idx_assets_customer_id ON assets(tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_opportunity_assets_opp ON opportunity_assets(opportunity_id);
CREATE INDEX IF NOT EXISTS idx_opportunity_assets_asset ON opportunity_assets(asset_id);
CREATE INDEX IF NOT EXISTS idx_opp_contacts_opp ON opportunity_contacts(opportunity_id);
CREATE INDEX IF NOT EXISTS idx_opp_contacts_contact ON opportunity_contacts(contact_id);
CREATE INDEX IF NOT EXISTS idx_user_groups_tenant ON user_groups(tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_group_memberships_group ON user_group_memberships(group_id);
CREATE INDEX IF NOT EXISTS idx_user_group_memberships_user ON user_group_memberships(tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_record_access_policies_lookup ON record_access_policies(tenant_id, entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_record_access_grants_lookup ON record_access_grants(tenant_id, entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_policy_rules_lookup ON policy_rules(tenant_id, entity_type, enabled);
CREATE INDEX IF NOT EXISTS idx_policy_rules_operations ON policy_rules USING GIN(operations);
CREATE INDEX IF NOT EXISTS idx_document_templates_tenant ON document_templates(tenant_id);
CREATE INDEX IF NOT EXISTS idx_document_templates_type ON document_templates(tenant_id, template_type);
CREATE INDEX IF NOT EXISTS idx_notification_inbox_user ON notification_inbox(tenant_id, user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notif_tmpl_tenant ON notification_templates(tenant_id);
CREATE INDEX IF NOT EXISTS idx_notif_tmpl_tenant_type_active ON notification_templates(tenant_id, notification_type) WHERE is_active = true;
