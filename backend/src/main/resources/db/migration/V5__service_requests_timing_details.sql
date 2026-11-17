-- US06 (écran « Envoyer une demande ») : délai souhaité, heure, repère d'adresse.
-- lat / lng (position du client) et urgency existent déjà depuis V1.

ALTER TABLE service_requests
    ADD COLUMN timing VARCHAR NOT NULL DEFAULT 'FLEXIBLE'
        CHECK (timing IN ('URGENT', 'TODAY', 'WITHIN_48H', 'SCHEDULED', 'FLEXIBLE')),
    ADD COLUMN preferred_time TIME,
    ADD COLUMN address_note VARCHAR(200);

-- Une date choisie est obligatoire quand le client choisit « Choisir date et heure »
ALTER TABLE service_requests ADD CONSTRAINT chk_service_requests_scheduled_date
    CHECK (timing <> 'SCHEDULED' OR preferred_date IS NOT NULL);

-- La position est complète (latitude et longitude) ou absente
ALTER TABLE service_requests ADD CONSTRAINT chk_service_requests_position
    CHECK ((lat IS NULL) = (lng IS NULL)
           AND (lat IS NULL OR (lat BETWEEN -90 AND 90 AND lng BETWEEN -180 AND 180)));
