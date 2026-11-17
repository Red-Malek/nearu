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
cp .env.example .env     # puis remplir les valeurs (voir docs/supabase/README.md)
docker compose up -d     # demarre la base de donnees
cd backend && ./mvnw spring-boot:run      # lit automatiquement ../.env (port 8080)
cd frontend && npm install && npm start   # http://localhost:4200, /api redirigé vers le backend
```
Authentification et e-mails (Supabase, SMTP, URL de redirection) : [docs/supabase/README.md](docs/supabase/README.md).

## Workflow Git
- `main` est toujours stable, aucun push direct
- une branche par US : `feature/us06-send-request`
- petites pull requests, relues par un autre membre
- commits : `feat(us06): ...`, `fix(...)`, `chore: ...`, `docs: ...`
