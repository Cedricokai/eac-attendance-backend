# DS1525+ handoff: PC-independent deployment

Prepared for the authorized 6 November 2026 GitHub handoff on branch codex/eac-nas-incognito-2026-11-04. Local demo sign-ins and credential manifests have been retired; automated test sources were removed at the owner’s request. Preserve the admin and real employee data. Private environment files, MySQL data, uploads and backups must not be committed. NAS deployment has not been executed.

## How it runs

Local native Office editing currently uses ONLYOFFICE in Docker Desktop on the Windows PC. The EAC Java backend and frontend do not themselves require Docker Desktop; the existing lite launcher runs them without Office. After deployment, the NAS runs eac-system, the existing MySQL service, and eac-onlyoffice in Synology Container Manager. Employees only use the HTTPS EAC website in a browser. Their PCs do not run Docker, and Albert's PC can be switched off. Container restart policies are already included.

Synology lists DS1525+ as AMD Ryzen V1500B / x86_64 with 8 GB installed RAM, and lists it as compatible with Container Manager. The prepared Office image is pinned for linux/amd64. ONLYOFFICE's published Docker minimum is 4 GB RAM and 40 GB free disk; the current compose limits Office to 5 GB. Check actual available memory alongside DSM, EAC and MySQL. More memory may be needed under load; these figures are not a load test of this NAS.

Sources: https://www.synology.com/en-us/products/DS1525%2B ; https://www.synology.com/en-in/dsm/packages/ContainerManager ; https://helpcenter.onlyoffice.com/docs/installation/docs-community-sys-reqs-docker.aspx .

## Existing deployment inputs

Use the currently deployed compose file and its existing database/volumes. Merge docker-compose.onlyoffice.synology.override.yml; do not replace the database. Set EAC_BACKEND_SOURCE, EAC_FILES_ROOT and EAC_FILE_BACKUPS_ROOT to the actual existing NAS folders, identified through Container Manager volume mappings. Those paths are still unconfirmed. No sample directory may replace a populated originals directory. Take matching database and original-file backups before applying additive migrations V14 through V19.

EAC public address: https://eac-system.eaconcept.org . Suggested Office origin: https://office.eaconcept.org . The latter still requires its DNS, HTTPS certificate and reverse proxy with WebSockets. Internal service addresses are http://eac-onlyoffice and http://eac-system:8080; Windows localhost addresses are not used by the NAS stack. Keep the separate Office JWT secret private and consistent on both services. The override disables local demo seeding in production. Build the frontend with the existing production API address.

Run the read-only preflight from the backend checkout on the NAS:

~~~sh
sh deploy/onlyoffice/preflight-synology.sh YOUR_EXISTING_COMPOSE.yml .env
~~~

It checks architecture, resources, Docker access, merged configuration and current mount mappings; it does not start, stop, rebuild or delete anything. Then follow SYNOLOGY.md for controlled build/redeploy. The Office extension is baked into deploy/onlyoffice/Dockerfile, including its versioned assets and gzip sidecars, so no Windows path or local Docker copy is needed.

## Acceptance before publication

Albert will test the current localhost frontend in Incognito using the existing local accounts. Check password typing in Verify file password, both cell-lock dialogs and offline password download. Confirm that editing/pasting does not move the caret or drop characters, full passwords reach verification, and password confirmation still matches. Check sharing and online-only download restrictions with the second local employee. Use synthetic documents.

After acceptance, Albert will request local credential/demo cleanup before publication. Rerun the candidate-source secret scan, review coordinated diffs, commit both local branches and push only when explicitly requested. No push or deployment is part of this preparation.

Verified document opening passwords persist encrypted in the existing EAC database. Generate APP_FILES_PASSWORD_KEY once in the NAS private environment and preserve it across container rebuilds/restarts. Use a separate key for production. The Windows local environment already has its own private stable key; neither key belongs in Git. Test a backend restart after verifying a synthetic encrypted workbook and confirm that another verification prompt is unnecessary.
