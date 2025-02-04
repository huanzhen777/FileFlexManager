-- =====================================================
-- 表结构创建
-- =====================================================
CREATE TABLE IF NOT EXISTS tag_file_hash
(
    id          SERIAL PRIMARY KEY,
    tag_id      INTEGER   NOT NULL,
    file_hash   VARCHAR(128) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 索引创建
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_tag_file_hash_tag_id ON tag_file_hash (tag_id);
CREATE INDEX IF NOT EXISTS idx_tag_file_hash_file_hash ON tag_file_hash (file_hash);
CREATE UNIQUE INDEX IF NOT EXISTS idx_tag_file_hash_unique ON tag_file_hash (tag_id, file_hash);

ALTER TABLE tag ADD COLUMN IF NOT EXISTS bind_file BOOLEAN NOT NULL DEFAULT FALSE;