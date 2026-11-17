-- US09 (décision du client sur le devis) et US10 (suivi des demandes).

-- 1. Statuts de la demande après la décision du client (backlog : « Devis accepté » / « Devis refusé »)
ALTER TABLE service_requests DROP CONSTRAINT service_requests_status_check;
ALTER TABLE service_requests ADD CONSTRAINT service_requests_status_check
    CHECK (status IN ('SUBMITTED', 'ACCEPTED', 'REJECTED', 'QUOTED', 'QUOTE_ACCEPTED', 'QUOTE_REJECTED',
                      'SCHEDULED', 'COMPLETED', 'CANCELLED', 'EXPIRED'));

-- 2. La contrainte « valid_until > now() » était réévaluée à chaque UPDATE : une fois la date passée,
--    plus aucune décision ne pouvait être enregistrée sur le devis. L'expiration est désormais
--    vérifiée par l'application au moment de la décision.
ALTER TABLE quotes DROP CONSTRAINT quotes_valid_until_check;

-- 3. Description de l'intervention proposée (backlog US08 : « un montant et une description »)
ALTER TABLE quotes ADD COLUMN description TEXT;

-- 4. Date de la décision du client
ALTER TABLE quotes ADD COLUMN decided_at TIMESTAMPTZ;

-- 5. Un seul devis actif (en attente ou accepté) par demande (US08, critère 3)
CREATE UNIQUE INDEX uq_quotes_active_per_request ON quotes (request_id)
    WHERE status IN ('PENDING', 'ACCEPTED');
