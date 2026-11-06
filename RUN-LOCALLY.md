# Run the backend locally

Double-click **Start-Backend.cmd** in this folder. It loads your private `.env.local` configuration and starts the backend at http://localhost:8080 using installed Maven when available, otherwise the Maven wrapper. Keep the window open; press **Ctrl+C** to stop it.

Requirements: JDK 17 and the local MySQL server running with the database configured in `.env.local`. Your database password and encryption keys stay in that ignored local file.

From a PowerShell terminal in the backend folder, run:

```powershell
.\run-local.ps1
```

If port 8080 is already in use, stop the existing backend before starting another instance. The launcher does not terminate other programs.

## IntelliJ green Run button
Select **EAC Backend Local** in the Run configuration dropdown, then press the green Run button (or Shift+F10). This configuration uses the `local` Spring profile, reads `.env.local` from the backend project folder, and disables DevTools restart for a stable local launch. If IntelliJ asks for a JDK, select Java 17. Open the backend folder as the IntelliJ project so its working directory is correct.

Run only one backend instance on port 8080 at a time. The local profile is explicit; production startup remains unchanged.

## Normal Run button
The main application now detects `.env.local` in the backend working directory and uses the local profile by default. You can press Run directly on `EacBackendProjectApplication` without adding environment variables or selecting a special profile. Explicit active profiles take precedence, and deployments without `.env.local` keep their existing profile behavior. Keep IntelliJ's working directory set to the backend project folder.

## Lightweight mode: no Docker on this PC

Run **Start-EAC-Lite.cmd** to start the backend and frontend without Docker Desktop. Native browser editing of DOCX/XLSX/PPTX is disabled in this mode. Existing originals, saved cell locks and file-opening passwords remain in stored/downloaded files; use Microsoft Office locally to edit a downloaded copy. Other EAC modules continue normally. Run only one backend on 8080; if an existing backend is running, the launcher preserves it and its current settings.

ONLYOFFICE Docs is a separate Linux document server. Replacing Docker Desktop with another local Linux VM would still require the document-server image/runtime and would not remove its storage cost. The intended smaller arrangement is to run Docs on the NAS and connect EAC to that server. The source code pushed to GitHub does not contain a Docker image, WSL disk, node_modules, uploaded documents or the local database.

A NAS Office service is not yet configured here. After its HTTPS endpoint and existing storage paths are confirmed, use the existing Synology override and Office guide. For local EAC against remote Docs, use the NAS HTTPS editor/server URLs, a matching separate JWT secret, and a connector address that the NAS can reach on this PC. That LAN address must be explicitly allowed by the document server's request filter; `host.docker.internal` refers to the host of the container and must not be reused for a different machine. Do not publish local database credentials or open public access to the development backend.
