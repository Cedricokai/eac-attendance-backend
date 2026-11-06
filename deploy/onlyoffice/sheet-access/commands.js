/* Executed by ONLYOFFICE callCommand, with no DOM or workbook conversion. */
function eacReadActiveSheet() {
  try { return { name: Api.GetActiveSheet().GetName() }; }
  catch (error) { return { error: String(error.message || error) }; }
}
function eacReadSheetAccess() {
  try {
    var active = Api.GetActiveSheet();
    return { sheets: Api.GetSheets().map(function (sheet) {
      var rule = null;
      try { rule = sheet.GetProtectedRange("EAC_SHEET_ACCESS"); }
      catch (missing) { if (!/range.*not found/i.test(String(missing.message || missing))) throw missing; }
      var members = rule ? (rule.GetAllUsers() || []).map(function (user) {
        return { id: user.GetId(), name: user.GetName(), access: user.GetType() };
      }) : [];
      return { name: sheet.GetName(), active: sheet.GetName() === active.GetName(), restricted: !!rule, members: members };
    }) };
  } catch (error) { return { error: String(error.message || error) }; }
}

function eacApplySheetAccess() {
  try {
    var request = Asc.scope.eacSheetAccess;
    if (!request || !request.canManage) throw new Error("Only the workbook owner or an authorized manager can change sheet access.");
    var sheet = Api.GetSheet(request.sheet);
    if (!sheet) throw new Error("The sheet was renamed or removed. Refresh and try again.");
    var members = request.members || [];
    if (request.restricted !== false && !members.some(function (user) { return user.id === request.ownerId && user.access === "CanEdit"; })) throw new Error("The workbook owner must retain editing access.");
    var rule = null;
    try { rule = sheet.GetProtectedRange("EAC_SHEET_ACCESS"); }
    catch (missing) { if (!/range.*not found/i.test(String(missing.message || missing))) throw missing; }
    if (!rule && request.restricted === false) return { applied: true, sheet: sheet.GetName() };
    if (!rule) rule = sheet.AddProtectedRange("EAC_SHEET_ACCESS", "$A$1:$XFD$1048576");
    if (!rule) throw new Error("ONLYOFFICE could not create this sheet's native restriction. Check for conflicting protected ranges.");
    members.forEach(function (user) {
      if (user.access !== "CanEdit" && user.access !== "CanView") throw new Error("Invalid sheet permission.");
      if (!rule.AddUser(user.id, user.name, user.access)) throw new Error("Could not grant access to " + user.name);
    });
    (rule.GetAllUsers() || []).forEach(function (user) {
      if (!members.some(function (member) { return member.id === user.GetId(); }) && !rule.DeleteUser(user.GetId())) throw new Error("Could not remove an earlier employee permission.");
    });
    // ONLYOFFICE also returns false when the requested type is already set.
    // EAC verifies the actual saved XML before accepting the checkpoint.
    rule.SetAnyoneType(request.restricted === false ? "CanEdit" : "NotView");
    return { applied: true, sheet: sheet.GetName(), checkpointVerificationRequired: true };
  } catch (error) { return { error: String(error.message || error) }; }
}

function eacEnsureAdministratorAccess() {
  try {
    var request = Asc.scope.eacAdminAccess;
    if (!request || !request.adminOverride || !request.canManage) throw new Error("ADMIN access is required.");
    var changed = 0;
    Api.GetSheets().forEach(function (sheet) {
      (sheet.GetAllProtectedRanges() || []).forEach(function (rule) {
        var existing = rule.GetAllUsers() || [];
        request.administrators.forEach(function (admin) {
          if (!existing.some(function (user) { return user.GetId() === admin.id && user.GetType() === "CanEdit"; })) {
            if (!rule.AddUser(admin.id, admin.name, "CanEdit")) throw new Error("The native protection on " + sheet.GetName() + " could not be updated. Contact its owner to remove the existing protection first.");
            changed++;
          }
        });
      });
    });
    return { applied: true, changed: changed };
  } catch (error) { return { error: String(error.message || error) }; }
}
