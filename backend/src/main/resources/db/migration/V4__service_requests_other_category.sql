-- US06 : catégories complémentaires, catégorie « Autre » et précision saisie par le client.

-- Métiers manquants dans V2 (idempotent sur slug)
INSERT INTO service_categories (slug, name, icon, sort_order) VALUES
    ('mecanique',     'Mécanique auto',  'mechanic',    11),
    ('serrurerie',    'Serrurerie',      'locksmith',   12),
    ('carrelage',     'Carrelage',       'tiling',      13),
    ('electromenager','Électroménager',  'appliance',   14),
    ('informatique',  'Informatique',    'computer',    15),
    ('demenagement',  'Déménagement',    'moving',      16)
ON CONFLICT (slug) DO NOTHING;

-- « Autre » : catégorie technique pour les demandes hors liste.
-- Inactive pour ne pas apparaître dans GET /api/categories ni dans la recherche (US03/US04) ;
-- le formulaire de demande l'ajoute lui-même avec un champ de précision.
INSERT INTO service_categories (slug, name, icon, sort_order, active) VALUES
    ('autre', 'Autre', 'other', 999, false)
ON CONFLICT (slug) DO NOTHING;

-- Catégorie précisée par le client quand il choisit « Autre »
ALTER TABLE service_requests ADD COLUMN category_other VARCHAR(80);
ALTER TABLE service_requests ADD CONSTRAINT chk_service_requests_category_other
    CHECK (category_other IS NULL OR length(btrim(category_other)) BETWEEN 2 AND 80);
