CREATE INDEX IF NOT EXISTS idx_work_orders_status ON work_orders(status);
CREATE INDEX IF NOT EXISTS idx_work_orders_expected_completion_status ON work_orders(expected_completion_at, status);
CREATE INDEX IF NOT EXISTS idx_deliveries_status ON deliveries(status);
CREATE INDEX IF NOT EXISTS idx_deliveries_expected_delivery_status ON deliveries(expected_delivery_at, status);
CREATE INDEX IF NOT EXISTS idx_assets_type ON assets(type);
CREATE INDEX IF NOT EXISTS idx_work_updates_work_order_created ON work_updates(work_order_id, created_at);
CREATE INDEX IF NOT EXISTS idx_location_updates_delivery_recorded ON location_updates(delivery_id, recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_reference_created ON audit_events(reference_type, reference_id, created_at DESC);
