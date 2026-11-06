# EAC Sheet Access — plugin v6

Use the EAC **Sheet access** dialog above the workbook. Select a saved sheet, enable its restriction, and assign employees No access, View only or View and edit. The employee directory and policy/audit records belong to EAC. Optional workbook sharing reuses existing File Management access. Owner and ADMIN retain full control.

The background plugin bridges signed user identities and a nonce to the EAC page using exact origins. It applies the official native `EAC_SHEET_ACCESS` full-sheet range without rebuilding or truncating cell contents. Save the workbook to finish: the backend checks the actual saved native rules against the pending policy before accepting the checkpoint. Unassigned employees wait during policy changes. Sheet tabs remain visible; denied sheets show an EAC overlay and native warning.

Employees lacking access to any sheet cannot export current or historical whole-workbook files. Full-file replacement and restore require edit permission on every restricted sheet. These controls are not cryptographic sheet isolation: the trusted editor receives the complete workbook, and cross-sheet formulas or drawings can disclose related data. Use separate permissioned files for strict confidentiality. Existing encryption/passwords are not bypassed.

Local startup copies this folder into the Document Server plugin directory; the Synology Dockerfile embeds it. Reopen editors after an asset update. Java changes require a backend restart after saving and closing documents.

`plugins.js` is the unmodified official SDK bootstrap, downloaded 2026-10-30 from https://onlyoffice.github.io/sdkjs-plugins/v1/plugins.js. SHA-256: `1BC0BD71EEBCB477CEAAEE3AB00ED1A10CE7EAE117EC68AE8024645C435F524E`. Copyright and AGPL notices are retained.

Nine command-boundary tests, 27 targeted Java tests and live synthetic six-sheet owner/employee/ADMIN sessions passed. Native view-only edit denial, No-access sheet denial, employee-attributed checkpoints and preservation of both users' saved edits were checked. See the root ONLYOFFICE.md for evidence, limitations and deployment requirements. The broader protected-range readiness flag remains false pending deployment acceptance testing.

Plugin v6 also adds owner/manager right-click Lock selected cells and Unlock selected cells, using a strictly version-gated Docs 10.0.0 adapter for standard worksheet protection. Existing or newly chosen sheet passwords are transient. First-lock initialization is explicit in the EAC dialog. Standard cell styles and sheetProtection are written into XLSX for offline editors; user-based EAC sheet restrictions remain online. See root ONLYOFFICE.md for verification and limitations.
