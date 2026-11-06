# Cedric handoff — 6 November 2026

Both repositories are prepared on codex/eac-nas-incognito-2026-11-04 for the owner's authorized GitHub push. This branch contains the coordinated Files/ONLYOFFICE, Communication, Timesheet UI and Transport updates, including employee return-work review and driver workspaces using existing EAC accounts.

Local cleanup: the two generated communication demo sign-ins were disabled, their password hashes replaced with random credentials and their reset tokens cleared. Their linked demo employees were made inactive. One enabled main administrator was preserved, as were real employee accounts and historical audit/document records. The local demo-account creation controller and credential manifest were removed. No deployment database was modified.

Automated test sources, fixtures, scripts and test-only Maven dependencies were removed at the owner's explicit request after prior regression checks. Operational SMTP testing remains a supported application feature. Future developers should restore/add automated coverage when changing behavior. Prior passing validation includes 77 file/Office integration tests and 26 transport tests. Final packaging/build status is recorded in the publishing summary.

Private .env files, service credentials, the stable document-password encryption key, databases, uploads, backups, generated output and local diagnostics must never be committed. Keep the encryption key private and stable; deleting it would prevent the server from unlocking remembered document passwords. Eight pre-existing employee uploads are removed from Git tracking only; local document files remain. Old Git history was not rewritten.

NAS deployment is prepared, not executed. Use deploy/onlyoffice/DS1525-HANDOFF.md and SYNOLOGY.md, preserving the existing database and exact originals/backup mounts. Apply additive migrations V14–V19 using the deployment's schema procedure. Configure private production secrets, DNS/TLS and the document-server reverse proxy. Do not copy the local database to the NAS. Local and NAS server secrets are separate.
