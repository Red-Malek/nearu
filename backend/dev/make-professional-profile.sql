-- DEV ONLY (pas une migration Flyway) : en attendant l'écran de profil prestataire (US02),
-- crée le profil public d'un compte « Commerçant / Prestataire » inscrit via /inscription,
-- pour qu'il apparaisse dans la recherche et puisse recevoir des demandes.
--
-- Usage (remplacer le téléphone saisi à l'inscription, format +216XXXXXXXX) :
--   Get-Content backend\dev\make-professional-profile.sql | docker exec -i nearu-db psql -U nearu -d nearu -v phone="'+21650000000'"
-- Sans effet si le profil existe déjà.

INSERT INTO professional_profiles (user_id, display_name, bio, governorate_id)
SELECT u.id, u.full_name, 'Profil de test créé en développement.', (SELECT id FROM governorates WHERE code = 'tunis')
FROM users u
WHERE u.phone = :phone AND u.role = 'PROFESSIONAL'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO professional_services (professional_id, category_id)
SELECT u.id, c.id
FROM users u
JOIN service_categories c ON c.slug IN ('plomberie', 'electricite')
WHERE u.phone = :phone AND u.role = 'PROFESSIONAL'
ON CONFLICT (professional_id, category_id) DO NOTHING;

SELECT p.user_id AS id_prestataire, p.display_name,
       'http://localhost:4200/prestataires/' || p.user_id AS profil_public
FROM professional_profiles p JOIN users u ON u.id = p.user_id
WHERE u.phone = :phone;
