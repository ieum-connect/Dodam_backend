CREATE TABLE push_devices (
 id VARCHAR(255) PRIMARY KEY,
 user_id UUID NOT NULL REFERENCES users(id),
 token VARCHAR(4096) NOT NULL,
 active BOOLEAN NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX push_devices_user_idx ON push_devices(user_id,active);
CREATE TABLE push_deliveries (
 id UUID PRIMARY KEY,
 notification_id UUID NOT NULL REFERENCES system_notifications(id),
 user_id UUID NOT NULL REFERENCES users(id),
 device_id VARCHAR(255) NOT NULL REFERENCES push_devices(id),
 schedule_id UUID NOT NULL,
 due_at TIMESTAMPTZ NOT NULL,
 next_attempt_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,
 attempts INTEGER NOT NULL,
 status VARCHAR(255) NOT NULL,
 UNIQUE(notification_id,device_id)
);
CREATE INDEX push_delivery_due_idx ON push_deliveries(status,next_attempt_at);
