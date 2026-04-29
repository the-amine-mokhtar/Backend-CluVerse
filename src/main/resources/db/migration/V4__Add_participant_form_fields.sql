-- ════════════════════════════════════════════════════════════════════════════
-- V4: Add participant form fields for data persistence
-- ════════════════════════════════════════════════════════════════════════════
-- Purpose: Store participant form data (fullName, participantPhone) directly in 
-- the event_participant table to persist participant information at the time of 
-- registration, independent of user profile changes.

ALTER TABLE event_participant
ADD COLUMN IF NOT EXISTS full_name VARCHAR(255) AFTER registration_date,
ADD COLUMN IF NOT EXISTS participant_phone VARCHAR(20) AFTER full_name;

-- Backfill existing participants with data from the user table (optional)
-- This preserves current user data for existing participants
UPDATE event_participant ep
SET 
  full_name = CONCAT(u.first_name, ' ', u.last_name),
  participant_phone = u.phone
FROM user u
WHERE ep.user_id = u.id 
  AND ep.full_name IS NULL 
  AND u.first_name IS NOT NULL;

-- Add indexes for performance
ALTER TABLE event_participant
ADD INDEX IF NOT EXISTS idx_ep_full_name (full_name),
ADD INDEX IF NOT EXISTS idx_ep_participant_phone (participant_phone);
