(function () {
  "use strict";
  var options = {}, state = null, activeSheet = "", warnedSheet = "", busy = false, appliedRevision = -1, queuedSelection = "", pendingCellLock=null;
  function send(type, data) {
    window.top.postMessage({ source: "eac-office-sheets", nonce: options.bridgeNonce, type: type, data: data }, options.frontendOrigin);
  }
  function check() {
    if (!state || !activeSheet) return;
    var sheet = state.sheets.find(function (entry) { return entry.name === activeSheet; });
    var allowed = state.adminOverride || options.userId === state.ownerId || (sheet && (!sheet.restricted || ["CanView", "CanEdit"].indexOf(sheet.members[options.userId]) !== -1));
    send("active", { name: activeSheet, denied: !allowed });
    if (!allowed && warnedSheet !== activeSheet) {
      warnedSheet = activeSheet;
      Asc.plugin.executeMethod("ShowError", ["You’re not allowed to view this sheet. Please contact the workbook owner for access.", 0]);
    } else if (allowed) warnedSheet = "";
  }
  function read() {
    if (busy) return;
    busy = true;
    // A viewer must not inspect protected ranges belonging to other employees.
    // Sheet membership and assignments come from EAC's authenticated API.
    Asc.plugin.callCommand(eacReadActiveSheet, false, false, function (result) {
      busy = false;
      if (!result) return;
      if (result.error) { send("error", result.error); return; }
      activeSheet = result.name;
      send("ready", {});
      check();
      if(pendingCellLock!==null){var pending=pendingCellLock;pendingCellLock=null;startCellLock(pending);}
      if (queuedSelection) { var next = queuedSelection; queuedSelection = ""; selectSheet(next); }
    });
  }
  function selectSheet(name) {
    if (busy) { queuedSelection = name; return; }
    busy = true;
    Asc.scope.eacSelectedSheet = name;
    Asc.plugin.callCommand(function () { var sheet = Api.GetSheet(Asc.scope.eacSelectedSheet); if (sheet) sheet.SetActive(); }, false, false, function () { busy = false; read(); });
  }
  function applyPolicies() {
    if (busy || !state || !options.canManage || appliedRevision === state.policyVersion) return;
    busy = true;
    var queue = state.sheets.slice(), version = state.policyVersion;
    function next() {
      var sheet = queue.shift();
      if (!sheet) { busy = false; appliedRevision = version; send("applied", { policyVersion: version }); read(); return; }
      var members = Object.keys(sheet.members).map(function (id) {
        var person = state.employees.find(function (user) { return user.id === id; }) || options.administrators.find(function (user) { return user.id === id; });
        return { id: id, name: person ? person.name : id, access: sheet.members[id] };
      }).filter(function (user) { return user.access === "CanEdit" || user.access === "CanView"; });
      Asc.scope.eacSheetAccess = { canManage: true, ownerId: state.ownerId, sheet: sheet.name, restricted: sheet.restricted, members: members };
      Asc.plugin.callCommand(eacApplySheetAccess, false, true, function (result) {
        if (!result || !result.applied) { busy = false; send("error", result && result.error || "Could not apply the sheet policy."); return; }
        next();
      });
    }
    next();
  }
  function cellLockCommand(command, request, done) {
    Asc.scope.eacCellLock=request;
    Asc.plugin.callCommand(command,false,false,function(result){
      if(!result||result.error){busy=false;send("error",result&&result.error||"Cell protection failed. Keep the workbook open.");return;}
      done(result);
    });
  }
  function startCellLock(locked) {
    if(!options.canManage){send("error","Only the owner, ADMIN or an authorized manager can lock cells.");return;}
    if(busy){pendingCellLock=locked;return;}
    busy=true;
    cellLockCommand(eacCellLockStatus,{canManage:true},function(result){busy=false;result.locked=locked;send("cell-lock-request",result);});
  }
  Asc.plugin.init = function () {
    options = Asc.plugin.info.options || {};
    window.addEventListener("message", function (event) {
      var message = event.data;
      if (event.origin !== options.frontendOrigin || event.source !== window.top || !message || message.source !== "eac-sheet-manager" || message.nonce !== options.bridgeNonce) return;
      if (message.type === "policy") { state = message.data; check(); if (state.pending) applyPolicies(); }
      if (message.type === "apply") { state = message.data; appliedRevision = -1; applyPolicies(); }
      if (message.type === "select") selectSheet(message.data.name);
      if (message.type === "cell-lock-start") startCellLock(message.data.locked!==false);
    });
    // The installed SDK routes plugin lifecycle events through attachEvent/onEvent.
    Asc.plugin.attachEvent("onContextMenuShow",function(context){
      var eligible=options.canManage&&(!context||["Image","Shape","OleObject"].indexOf(context.type)===-1);
      Asc.plugin.executeMethod("AddContextMenuItem",[{guid:Asc.plugin.guid,items:eligible?[{id:"eacLockCell",text:"Lock cell / selected cells…"},{id:"eacUnlockCell",text:"Unlock selected cells…"}]:[]}]);
    });
    Asc.plugin.attachContextMenuClickEvent("eacLockCell",function(){startCellLock(true);});
    Asc.plugin.attachContextMenuClickEvent("eacUnlockCell",function(){startCellLock(false);});
    Asc.plugin.attachEditorEvent("onChangeCurrentSheet", read);
    Asc.plugin.attachEditorEvent("onDocumentContentReady", read);
    if (options.adminOverride) {
      busy = true;
      Asc.scope.eacAdminAccess = options;
      Asc.plugin.callCommand(eacEnsureAdministratorAccess, false, true, function (result) { busy = false; if (!result || !result.applied) send("error", result && result.error || "Native administrator access could not be established."); read(); });
    } else read();
    setInterval(read, 5000);
  };
  Asc.plugin.button = function () {};
}());
