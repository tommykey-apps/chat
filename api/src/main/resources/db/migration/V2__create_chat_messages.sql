CREATE TABLE chat_messages (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id      UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    sender_id    VARCHAR(255) NOT NULL,
    sender_name  VARCHAR(100) NOT NULL,
    content      TEXT,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_room_id ON chat_messages(room_id, created_at);
