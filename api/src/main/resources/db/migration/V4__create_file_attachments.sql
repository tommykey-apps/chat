CREATE TABLE file_attachments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id   UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    file_name    VARCHAR(255) NOT NULL,
    file_size    BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    s3_key       VARCHAR(500) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
