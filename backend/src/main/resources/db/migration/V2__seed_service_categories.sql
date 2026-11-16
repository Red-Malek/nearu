-- US03: initial service categories (idempotent on slug)
INSERT INTO service_categories (slug, name, icon, sort_order) VALUES
    ('plomberie',     'Plomberie',     'plumbing',    1),
    ('electricite',   'Électricité',   'electricity', 2),
    ('menuiserie',    'Menuiserie',    'carpentry',   3),
    ('chauffeurs',    'Chauffeurs',    'driver',      4),
    ('peinture',      'Peinture',      'painting',    5),
    ('climatisation', 'Climatisation', 'hvac',        6),
    ('reparation',    'Réparation',    'repair',      7),
    ('jardinage',     'Jardinage',     'gardening',   8),
    ('nettoyage',     'Nettoyage',     'cleaning',    9),
    ('maconnerie',    'Maçonnerie',    'masonry',    10)
ON CONFLICT (slug) DO NOTHING;
