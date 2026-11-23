CREATE TABLE IF NOT EXISTS cart_merge_requests (
    member_id bigint NOT NULL REFERENCES members (id) ON DELETE CASCADE,
    merge_id uuid NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id, merge_id)
);