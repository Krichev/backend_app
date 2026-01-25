ALTER TABLE media_files ADD COLUMN IF NOT EXISTS bucket_name VARCHAR(100);

-- Backfill existing records (assuming all current media is in challenger-app bucket)
UPDATE media_files SET bucket_name = 'challenger-app' WHERE bucket_name IS NULL;

-- Add comment
COMMENT ON COLUMN media_files.bucket_name IS 'MinIO bucket name where file is stored';
