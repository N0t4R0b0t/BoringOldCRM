-- V19: Add Orders and Invoices tables for financial module

-- Orders table: represents a customer purchase order, optionally linked to a winning opportunity
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    opportunity_id BIGINT,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    subtotal DECIMAL(15,2),
    tax_amount DECIMAL(15,2),
    order_date DATE,
    expected_delivery_date DATE,
    notes TEXT,
    owner_id BIGINT,
    line_items JSONB DEFAULT '[]'::jsonb,
    custom_data JSONB DEFAULT '{}'::jsonb,
    table_data_jsonb JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Invoices table: represents a billing document, optionally linked to an order
CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    order_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    subtotal DECIMAL(15,2),
    tax_amount DECIMAL(15,2),
    total_amount DECIMAL(15,2),
    due_date DATE,
    payment_terms VARCHAR(100),
    notes TEXT,
    owner_id BIGINT,
    line_items JSONB DEFAULT '[]'::jsonb,
    custom_data JSONB DEFAULT '{}'::jsonb,
    table_data_jsonb JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for orders
CREATE INDEX IF NOT EXISTS idx_orders_tenant_id ON orders(tenant_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_opportunity_id ON orders(opportunity_id);
CREATE INDEX IF NOT EXISTS idx_orders_owner_id ON orders(owner_id);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);

-- Indexes for invoices
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_id ON invoices(tenant_id);
CREATE INDEX IF NOT EXISTS idx_invoices_customer_id ON invoices(customer_id);
CREATE INDEX IF NOT EXISTS idx_invoices_order_id ON invoices(order_id);
CREATE INDEX IF NOT EXISTS idx_invoices_owner_id ON invoices(owner_id);
CREATE INDEX IF NOT EXISTS idx_invoices_created_at ON invoices(created_at);

-- JSONB indexes for custom field search (GIN index for faster lookups)
CREATE INDEX IF NOT EXISTS idx_orders_custom_data_gin ON orders USING GIN (custom_data);
CREATE INDEX IF NOT EXISTS idx_invoices_custom_data_gin ON invoices USING GIN (custom_data);
