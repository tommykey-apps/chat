CREATE TABLE room_members (
    room_id   UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id   VARCHAR(255) NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    role      VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (room_id, user_id)
);
