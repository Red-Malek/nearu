-- US01: sign-up only asks for email, password, role and name.
-- The email lives in Supabase Auth, so the phone number becomes optional.
ALTER TABLE users ALTER COLUMN phone DROP NOT NULL;
