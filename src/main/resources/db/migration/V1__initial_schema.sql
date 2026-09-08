CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    phone VARCHAR(255),
    email VARCHAR(255),
    billing_address VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE assets (
    id BIGSERIAL PRIMARY KEY,
    asset_code VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    size_description VARCHAR(255),
    serial_number VARCHAR(255),
    current_yard_location VARCHAR(255),
    customer_id BIGINT REFERENCES customers(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_asset_code ON assets(asset_code);

CREATE TABLE work_orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(255) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    asset_id BIGINT NOT NULL REFERENCES assets(id),
    work_type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    priority VARCHAR(255) NOT NULL,
    scope_of_work VARCHAR(2000),
    assigned_team VARCHAR(255),
    blocked_reason VARCHAR(255),
    progress_percent INTEGER,
    estimated_cost NUMERIC(38,2),
    approved_cost NUMERIC(38,2),
    expected_completion_at TIMESTAMPTZ,
    actual_completion_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_work_order_number ON work_orders(order_number);

CREATE TABLE work_updates (
    id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT NOT NULL REFERENCES work_orders(id),
    stage VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    assigned_to VARCHAR(255),
    photo_url VARCHAR(255),
    note VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE inventory_items (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(255),
    quantity_on_hand NUMERIC(38,2),
    reorder_level NUMERIC(38,2),
    unit_cost NUMERIC(38,2),
    preferred_supplier VARCHAR(255),
    updated_at TIMESTAMPTZ
);

CREATE TABLE deliveries (
    id BIGSERIAL PRIMARY KEY,
    delivery_number VARCHAR(255) NOT NULL UNIQUE,
    work_order_id BIGINT NOT NULL UNIQUE REFERENCES work_orders(id),
    asset_id BIGINT NOT NULL REFERENCES assets(id),
    status VARCHAR(255) NOT NULL,
    driver_name VARCHAR(255),
    driver_phone VARCHAR(255),
    vehicle_number VARCHAR(255),
    vehicle_type VARCHAR(255),
    destination_latitude DOUBLE PRECISION,
    destination_longitude DOUBLE PRECISION,
    destination_address VARCHAR(1200),
    expected_delivery_at TIMESTAMPTZ,
    dispatched_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    proof_of_delivery_url VARCHAR(255),
    received_by VARCHAR(255),
    public_tracking_token VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_delivery_public_token ON deliveries(public_tracking_token);

CREATE TABLE location_updates (
    id BIGSERIAL PRIMARY KEY,
    delivery_id BIGINT NOT NULL REFERENCES deliveries(id),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy_meters DOUBLE PRECISION,
    speed_kph DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ
);
CREATE INDEX idx_location_delivery_time ON location_updates(delivery_id, recorded_at);

CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    reference_type VARCHAR(255) NOT NULL,
    reference_id BIGINT NOT NULL,
    action VARCHAR(255) NOT NULL,
    actor VARCHAR(255),
    details VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_audit_reference ON audit_events(reference_type, reference_id, created_at);

CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL
);
CREATE UNIQUE INDEX idx_app_users_email ON app_users(email);

CREATE TABLE drivers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL UNIQUE,
    license_number VARCHAR(255) NOT NULL UNIQUE,
    license_expiry DATE,
    status VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX idx_driver_phone ON drivers(phone);

CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    registration_number VARCHAR(255) NOT NULL UNIQUE,
    vehicle_type VARCHAR(255) NOT NULL,
    capacity_description VARCHAR(255),
    insurance_expiry DATE,
    permit_expiry DATE,
    service_due_at DATE,
    status VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX idx_vehicle_registration ON vehicles(registration_number);

CREATE TABLE quotations (
    id BIGSERIAL PRIMARY KEY,
    quotation_number VARCHAR(255) NOT NULL UNIQUE,
    work_order_id BIGINT NOT NULL REFERENCES work_orders(id),
    base_amount NUMERIC(14,2) NOT NULL,
    transport_charge NUMERIC(14,2) NOT NULL,
    tax_amount NUMERIC(14,2) NOT NULL,
    discount NUMERIC(14,2) NOT NULL,
    total_amount NUMERIC(14,2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    valid_until DATE,
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ
);

CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(255) NOT NULL UNIQUE,
    work_order_id BIGINT NOT NULL REFERENCES work_orders(id),
    amount_due NUMERIC(14,2) NOT NULL,
    amount_paid NUMERIC(14,2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    issue_date DATE,
    due_date DATE,
    created_at TIMESTAMPTZ
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    amount NUMERIC(14,2) NOT NULL,
    method VARCHAR(255) NOT NULL,
    reference_number VARCHAR(255),
    note VARCHAR(255),
    paid_at TIMESTAMPTZ
);

CREATE TABLE suppliers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    contact_person VARCHAR(255),
    phone VARCHAR(255),
    email VARCHAR(255),
    address VARCHAR(255),
    active BOOLEAN NOT NULL
);

CREATE TABLE purchase_requests (
    id BIGSERIAL PRIMARY KEY,
    request_number VARCHAR(255) NOT NULL UNIQUE,
    inventory_item_id BIGINT NOT NULL REFERENCES inventory_items(id),
    supplier_id BIGINT REFERENCES suppliers(id),
    quantity NUMERIC(14,3) NOT NULL,
    expected_unit_cost NUMERIC(14,2),
    status VARCHAR(255) NOT NULL,
    requested_by VARCHAR(255),
    approved_by VARCHAR(255),
    note VARCHAR(255),
    requested_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ
);

CREATE TABLE warranties (
    id BIGSERIAL PRIMARY KEY,
    asset_id BIGINT NOT NULL REFERENCES assets(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    coverage_details VARCHAR(1500),
    status VARCHAR(255) NOT NULL
);

CREATE TABLE complaints (
    id BIGSERIAL PRIMARY KEY,
    complaint_number VARCHAR(255) NOT NULL UNIQUE,
    asset_id BIGINT NOT NULL REFERENCES assets(id),
    description VARCHAR(2000) NOT NULL,
    photo_url VARCHAR(255),
    warranty_covered BOOLEAN NOT NULL,
    status VARCHAR(255) NOT NULL,
    assigned_to VARCHAR(255),
    resolution_note VARCHAR(255),
    created_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ
);
