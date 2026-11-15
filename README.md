# NearU

Marketplace de services locaux en Tunisie : trouver un prestataire,
envoyer une demande et obtenir un devis.

## Stack
Angular · Spring Boot · PostgreSQL/PostGIS · Supabase Auth · Docker

## Structure
- `backend/`  API Spring Boot
- `frontend/` application Angular
- `docs/`     backlog, contrat d'API, décisions

## Lancer le projet
```bash
cp .env.example .env     # puis remplir les valeurs
docker compose up
```
(À compléter une fois le backend et le frontend ajoutés.)

## Workflow Git
- `main` est toujours stable, aucun push direct
- une branche par US : `feature/us06-send-request`
- petites pull requests, relues par un autre membre
- commits : `feat(us06): ...`, `fix(...)`, `chore: ...`, `docs: ...`
