-- Step 1: Add new storage_key column
ALTER TABLE media_files 
ADD COLUMN IF NOT EXISTS storage_key UUID;

-- Step 2: Add content_hash for deduplication
ALTER TABLE media_files 
ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64);

-- Step 3: Backfill storage_key for existing records
UPDATE media_files 
SET storage_key = gen_random_uuid() 
WHERE storage_key IS NULL;

-- Step 4: Make storage_key NOT NULL after backfill
ALTER TABLE media_files 
ALTER COLUMN storage_key SET NOT NULL;

-- Step 5: Add unique constraint on storage_key (the new unique identifier)
ALTER TABLE media_files 
ADD CONSTRAINT uq_media_files_storage_key UNIQUE (storage_key);

-- Step 6: Remove unique constraint from filename
ALTER TABLE media_files 
DROP CONSTRAINT IF EXISTS media_files_filename_key;

-- Step 7: Create indexes for enterprise-scale queries
CREATE INDEX IF NOT EXISTS idx_media_files_uploaded_by_category 
ON media_files(uploaded_by, media_category);

CREATE INDEX IF NOT EXISTS idx_media_files_entity_id_type 
ON media_files(entity_id, media_type);

CREATE INDEX IF NOT EXISTS idx_media_files_content_hash 
ON media_files(content_hash) WHERE content_hash IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_media_files_created_at 
ON media_files(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_media_files_s3_key 
ON media_files(s3_key);

-- Step 8: Add comments
COMMENT ON COLUMN media_files.storage_key IS 'UUID-based unique storage identifier (system-generated)';
COMMENT ON COLUMN media_files.original_filename IS 'Original filename from user (not unique, for display only)';
COMMENT ON COLUMN media_files.content_hash IS 'SHA-256 hash of file content for deduplication';