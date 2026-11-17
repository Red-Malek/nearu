-- DEV ONLY (pas une migration Flyway) : en attendant les écrans prestataire US07 / US08,
-- simule « le prestataire accepte la demande et envoie un devis » sur la demande la plus récente
-- encore en attente, pour tester US09 (décision du client) et US10 (suivi).
--
-- Usage : Get-Content backend\dev\make-test-quote.sql | docker exec -i nearu-db psql -U nearu -d nearu

WITH cible AS (
    SELECT id, professional_id FROM service_requests
    WHERE status IN ('SUBMITTED', 'ACCEPTED')
    ORDER BY created_at DESC
    LIMIT 1
), devis AS (
    INSERT INTO quotes (request_id, professional_id, amount, description, proposed_start,
                        estimated_duration_minutes, valid_until)
    SELECT id, professional_id, 120.500, 'Diagnostic et réparation, pièces comprises.',
           now() + interval '2 days', 90, now() + interval '7 days'
    FROM cible
    RETURNING request_id
)
UPDATE service_requests r SET status = 'QUOTED', updated_at = now()
FROM devis WHERE r.id = devis.request_id
RETURNING r.title AS demande, r.status;
