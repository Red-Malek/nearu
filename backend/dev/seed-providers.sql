-- DEV ONLY (not a Flyway migration): 4 sample providers to try the US04 search before US02 exists.
-- Run: docker exec -i nearu-db psql -U nearu -d nearu < backend/dev/seed-providers.sql
-- Safe to re-run.
INSERT INTO users (id, phone, full_name, role, status) VALUES
    ('00000000-0000-0000-0000-000000000001', '+21620000001', 'Ahmed Trabelsi', 'PROFESSIONAL', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000002', '+21620000002', 'Sami Ben Ali',   'PROFESSIONAL', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000003', '+21620000003', 'Karim Jlassi',   'PROFESSIONAL', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000004', '+21620000004', 'Suspendu Test',  'PROFESSIONAL', 'SUSPENDED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO professional_profiles (user_id, display_name, governorate_id) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Ahmed Trabelsi', (SELECT id FROM governorates WHERE code = 'ariana')),
    ('00000000-0000-0000-0000-000000000002', 'Sami Ben Ali',   (SELECT id FROM governorates WHERE code = 'ariana')),
    ('00000000-0000-0000-0000-000000000003', 'Karim Jlassi',   (SELECT id FROM governorates WHERE code = 'tunis')),
    ('00000000-0000-0000-0000-000000000004', 'Suspendu Test',  (SELECT id FROM governorates WHERE code = 'ariana'))
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO professional_services (professional_id, category_id)
SELECT v.uid::uuid, c.id
FROM (VALUES
    ('00000000-0000-0000-0000-000000000001', 'electricite'),
    ('00000000-0000-0000-0000-000000000001', 'plomberie'),
    ('00000000-0000-0000-0000-000000000002', 'plomberie'),
    ('00000000-0000-0000-0000-000000000003', 'electricite'),
    ('00000000-0000-0000-0000-000000000004', 'electricite')) AS v(uid, slug)
JOIN service_categories c ON c.slug = v.slug
ON CONFLICT (professional_id, category_id) DO NOTHING;
