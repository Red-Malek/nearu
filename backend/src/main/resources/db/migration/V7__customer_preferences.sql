-- US « Paramètres utilisateur » : préférences de localisation du client (page Localisation).
-- Nom, téléphone : déjà dans users. E-mail et mot de passe : gérés par Supabase Auth.

CREATE TABLE customer_preferences (
    user_id            UUID PRIMARY KEY REFERENCES users(id),
    address_line       VARCHAR(255),
    postal_code        VARCHAR(10),
    city               VARCHAR(100),
    search_radius_km   INTEGER NOT NULL DEFAULT 3 CHECK (search_radius_km BETWEEN 1 AND 100),
    use_gps            BOOLEAN NOT NULL DEFAULT true,
    hide_exact_address BOOLEAN NOT NULL DEFAULT true,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
