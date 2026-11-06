# EAC sheet access: local use and Synology redeployment

## Assign the six sheets in EAC STAFF BASIC INFO

1. Sign in as the workbook owner or ADMIN and open the existing XLSX in File Management. Do not upload a second copy.
2. Click **Sheet access** in the EAC toolbar above the editor. Choose one of the actual saved workbook sheets.
3. Enable **Limit this sheet to selected employees**. Search the existing employee directory and assign **View only** or **View and edit**. Everyone else gets **No access**. The owner and ADMIN accounts are mandatory and cannot be removed.
4. Leave **Also share the workbook** enabled if the selected employees do not already have access. This uses the existing sharing service and sends no email. Employees still need File Management page access and applicable system operation permissions.
5. Click **Save assignments and apply**. EAC saves the assignment policy and audit record; the editor applies the native sheet rule. Commit any active cell edit with Enter, then click the EAC **Save workbook** button to commit the workbook checkpoint. Pending assignments block other users from reopening until the verified save completes.
6. The workbook remains one file with every sheet name listed. Clicking an unassigned sheet shows a denial panel; other sheets remain selectable. ADMIN retains EAC control and is added to native protected ranges. A pre-existing password/encryption or protection the native engine refuses to modify still requires its owner/password; this connector does not crack encryption or bypass product licensing.

File History lists immutable saved workbook versions and the last employee reported by the signed save callback. Assignment history separately records the employee and policy version. During co-editing, a checkpoint can include multiple contributors; the last-editor label does not attribute every cell or sheet to one employee.

These are editor access controls with full-file export guards. An employee missing access to a sheet cannot download or copy the whole workbook, including older versions. Native print/download/copy are disabled for partial access, and guest links cannot export managed restricted workbooks. Manual replacements/restores require policy reapplication. Drawings and formulas can still reveal related information inside a complete workbook. For confidentiality outside the editor, use separate files with separate file ACLs. No rows, sheets, images, formulas or documents are filtered, split or reconstructed by the backend.

## Local startup

Run `Start-EAC-With-Office.cmd` from the backend checkout, or start Docker/ONLYOFFICE with `Start-OnlyOffice.cmd`, then run the backend in IntelliJ and the frontend with `run-local.ps1`. The scripts preserve existing port listeners; stop the old EAC process before starting a second backend on 8080. URLs are frontend `http://localhost:5173`, backend `http://localhost:8080`, Office `http://localhost:8082`.

Restart the backend after Java changes and reopen the workbook after plugin changes. The local launcher copies the extension into the running Office container without restarting it. The plugin is bundled into the deployment image, so Synology does not depend on a Windows path or a manual container copy.

## Prepare the existing NAS stack

Do not replace MySQL volumes, reset users, copy local demo accounts to production or point at a fresh/empty file directory. Take a database backup and a matching copy/snapshot of the existing canonical file directory first. The additive V15 migration creates policy and audit tables; it does not rewrite existing file data. The current Hibernate update configuration can create these tables, or apply `src/main/resources/db/V15__office_sheet_access.sql` using the existing database migration procedure when automatic schema updates are disabled.

Add the fields in `synology.env.example` to the existing private deployment environment. Use the exact current NAS originals/backup directories and checkout path. Keep the existing separate application JWT and database credentials. Generate a distinct random Office secret locally, for example `openssl rand -base64 48`, and keep it in that private environment file. Do not commit or send it in chat. Do not enable demo accounts in production (`APP_COMMUNICATION_DEMO_ENABLED=false`).

Merge `docker-compose.onlyoffice.synology.override.yml` with the actual compose file already deployed. It overrides only the existing `eac-system` build/environment/storage mounts and adds `eac-onlyoffice` on its existing `eac-network`. It does not define another MySQL service. The explicit file-root variables prevent silently switching to an empty directory. Preserve any existing extra upload/backup mounts in the base compose file.

Example commands, with `YOUR_EXISTING_COMPOSE.yml` replaced by the currently deployed file:

```sh
docker compose --env-file .env -f YOUR_EXISTING_COMPOSE.yml -f docker-compose.onlyoffice.synology.override.yml config --quiet
docker compose --env-file .env -f YOUR_EXISTING_COMPOSE.yml -f docker-compose.onlyoffice.synology.override.yml build eac-system eac-onlyoffice
# Let open documents save and close before restarting the editor/backend.
docker compose --env-file .env -f YOUR_EXISTING_COMPOSE.yml -f docker-compose.onlyoffice.synology.override.yml up -d eac-system eac-onlyoffice
docker compose --env-file .env -f YOUR_EXISTING_COMPOSE.yml -f docker-compose.onlyoffice.synology.override.yml restart eac-onlyoffice
```

The restart resolves the current backend Docker IP and explicitly allowlists that IP for Office downloads; private/metadata request filtering stays enabled. Repeat it after a backend container IP changes. Office persistent volumes retain editor state, but canonical originals and immutable versions stay in EAC storage. Do not delete these volumes during active editing. The pinned image was tested on x86-64 Docker; verify the NAS architecture and available memory before building. An unsupported CPU needs a separately verified official image digest.

Configure the NAS HTTPS reverse proxy for the selected Office hostname, forwarding to loopback port 8082 with WebSocket support and preserved Host / X-Forwarded-Proto headers. See `nginx-office.example.conf` as a reference; do not overwrite existing NAS proxy configuration. Backend/Office communication uses internal service DNS; browser scripts use the public HTTPS Office origin. Save-cache links from either explicitly configured origin are fetched through the trusted internal Office origin with redirects disabled. Do not expose MySQL or Office's internal port publicly.

Build the frontend with its existing production API URL, or explicitly set `VITE_API_BASE_URL` to the deployed HTTPS backend base URL before `npm run build`. Redeploy the resulting `dist` using the existing frontend process. Do not deploy a local-development build pointing at localhost. Keep the configured EAC frontend origin consistent with the browser hostname so the editor bridge can validate message origins.

## Deployment acceptance

Verify Office `/healthcheck`, authenticated workbook open, all six sheet tabs, owner assignment, denied employee sheet selection, allowed-sheet editing, ADMIN recovery, native Save, policy pending-state clearance, employee version attribution and close/reopen using synthetic files first. Confirm the real employee directory is used. Test actual canonical storage/backup paths and reverse-proxy WebSockets on the NAS. The NAS has not been accessed by this local implementation; local tests alone do not prove production deployment.

Known addresses supplied by the user: EAC frontend https://eac-system.eaconcept.org and NAS management https://192.168.1.97:3451. The proposed Office address is https://office.eaconcept.org; its DNS/certificate/reverse-proxy entry has not been created. Storage paths are still unknown. In Synology Container Manager, inspect the existing eac-backend container's volume mappings and APP_FILES_STORAGE_ROOT / APP_FILES_BACKUP_ROOT settings before filling the environment example. Do not paste the full environment file, which contains secrets. A read-only command to list current host mounts is:

```sh
docker inspect eac-backend --format '{{range .Mounts}}{{println .Source "->" .Destination}}{{end}}'
```

### Permanent document passwords
Apply V18__remember_office_passwords.sql with the additive migration procedure (local Hibernate update can also create the table). Configure APP_FILES_PASSWORD_KEY once in the NAS private environment, using a separate random secret of at least 32 characters. Keep it stable across restarts and preserve it securely with the deployment settings. The compose override requires it. Remembered opening passwords are encrypted in the EAC database and survive backend restarts, while their plaintext is never returned to browsers. An owner/manager verifies a new password only when a document password changes or after losing/changing the server key.
Employee return-work review uses the additive `src/main/resources/db/V19__file_work_review.sql` migration (or the existing Hibernate schema-update procedure). It stores version receipts and notes in `file_work_returns`, referencing the existing document rather than copying its bytes. Include this table in normal database backups. It does not alter file encryption or employee sheet permissions.
