(function () {
  // __KJQ_AUTH_JSON__ is replaced by Rust with a JSON object literal.
  var defaults = __KJQ_AUTH_JSON__;
  window.__kjqAuth = Object.assign({}, defaults, window.__kjqAuth || {});

  function AUTH() {
    return window.__kjqAuth || defaults;
  }

  function isMc() {
    return String(AUTH().gameId || "3") === "3";
  }

  function currentRoleId() {
    var a = AUTH();
    return (
      a.roleId ||
      localStorage.getItem(isMc() ? "mc_roleId" : "roleId") ||
      localStorage.getItem("roleId") ||
      localStorage.getItem("mc_roleId") ||
      ""
    );
  }

  function currentServerId() {
    var a = AUTH();
    return (
      a.serverId ||
      localStorage.getItem(isMc() ? "mc_serverId" : "serverId") ||
      localStorage.getItem("serverId") ||
      localStorage.getItem("mc_serverId") ||
      ""
    );
  }

  function rolePayload(override) {
    var a = AUTH();
    var roleId = (override && override.roleId) || currentRoleId();
    var serverId = (override && override.serverId) || currentServerId();
    return {
      code: 0,
      result: true,
      roleId: String(roleId || ""),
      serverId: String(serverId || ""),
      roleName: (override && override.roleName) || "",
      userId: a.userId || localStorage.getItem("userId") || "",
      gameId: String(a.gameId || "3"),
      serverName: (override && override.serverName) || "",
      channelId: a.channelId || "19",
    };
  }

  function clearUserCaches() {
    // Wipe previous account data so SPA cannot keep reading user A after switch.
    var keys = [
      "token",
      "userId",
      "roleId",
      "serverId",
      "mc_roleId",
      "mc_serverId",
      "initUserInfo",
      "userInfo",
      "mc_userInfo",
      "mc-growth-simulator-user-info",
      "mc-growth-simulator-role-info",
      // resource-briefing (PROJECT_KEY=mcResMonReport)
      "mcResMonReport_ROLE_INFO",
      "mcResMonReport_APP_USER_INFO",
      // mccalendar (namespaced storage prefix aki-calendar:)
      "aki-calendar:user-info",
      "aki-calendar:role-info",
      "mc_roleName",
      "roleName",
      "REQUEST_IP",
    ];
    for (var i = 0; i < keys.length; i++) {
      try {
        localStorage.removeItem(keys[i]);
      } catch (e0) {}
    }
    try {
      sessionStorage.removeItem("mc-base-params");
    } catch (e1) {}
  }

  function seedStorage(opts) {
    try {
      opts = opts || {};
      if (opts.clearFirst) {
        clearUserCaches();
      }

      var a = AUTH();
      // Prefer explicit AUTH fields over leftover localStorage.
      var roleId = a.roleId || currentRoleId();
      var serverId = a.serverId || currentServerId();

      if (a.token) localStorage.setItem("token", a.token);
      else localStorage.removeItem("token");
      if (a.userId) localStorage.setItem("userId", a.userId);
      if (a.requestIp) localStorage.setItem("REQUEST_IP", a.requestIp);

      if (roleId) {
        localStorage.setItem("roleId", roleId);
        localStorage.setItem("mc_roleId", roleId);
      }
      if (serverId) {
        localStorage.setItem("serverId", serverId);
        localStorage.setItem("mc_serverId", serverId);
      }

      var userInfo = {
        token: a.token || "",
        userId: a.userId || "",
        did: a.did || "",
        roleId: roleId,
        serverId: serverId,
        channelId: a.channelId || "19",
        gameId: String(a.gameId || "3"),
        appVersion: "3.0.1",
        enterSource: "3",
        os: "android",
        ua: navigator.userAgent || "",
      };
      localStorage.setItem("initUserInfo", JSON.stringify(userInfo));
      localStorage.setItem("userInfo", JSON.stringify(userInfo));
      localStorage.setItem("mc_userInfo", JSON.stringify(rolePayload()));

      // growth-calculator uses dedicated pinia localStorage keys.
      // Without overwriting them, WebView keeps the previous user's role
      // and it looks like user data is hardcoded.
      localStorage.setItem(
        "mc-growth-simulator-user-info",
        JSON.stringify({
          token: a.token || "",
          userId: a.userId || "",
          did: a.did || "",
          os: "android",
          appVersion: "3.0.1",
          ua: navigator.userAgent || "",
          channelId: a.channelId || "19",
          enterSource: "3",
        })
      );
      localStorage.setItem(
        "mc-growth-simulator-role-info",
        JSON.stringify({
          userId: a.userId || "",
          gameId: Number(a.gameId || 3) || 3,
          serverId: String(serverId || ""),
          serverName: "",
          roleId: String(roleId || ""),
          roleName: "",
        })
      );

      // resource-briefing keys (PROJECT_KEY = mcResMonReport)
      // APP_INFO / ROLE_INFO are read on boot; if stale, user looks "hardcoded".
      var rbAppInfo = {
        token: a.token || "",
        userId: a.userId || "",
        did: a.did || "",
        roleId: String(roleId || ""),
        serverId: String(serverId || ""),
        channelId: a.channelId || "19",
        appVersion: "3.0.1",
        enterSource: "3",
        os: "android",
        ua: navigator.userAgent || "",
      };
      var rbRoleInfo = {
        userId: a.userId || "",
        roleId: String(roleId || ""),
        serverId: String(serverId || ""),
        serverName: "",
        roleName: "",
        gameId: String(a.gameId || "3"),
      };
      localStorage.setItem(
        "mcResMonReport_APP_USER_INFO",
        JSON.stringify(rbAppInfo)
      );
      localStorage.setItem(
        "mcResMonReport_ROLE_INFO",
        JSON.stringify(rbRoleInfo)
      );

      // mccalendar uses namespaced keys: aki-calendar:user-info / role-info
      var calUser = {
        token: a.token || "",
        userId: a.userId || "",
        did: a.did || "",
        os: "android",
        appVersion: "3.0.1",
        ua: navigator.userAgent || "",
        channelId: a.channelId || "19",
        enterSource: "3",
        headUrl: "",
      };
      var calRole = {
        userId: a.userId || "",
        gameId: Number(a.gameId || 3) || 3,
        serverId: String(serverId || ""),
        serverName: "",
        roleId: String(roleId || ""),
        roleName: "",
        headPhotoUrl: "",
        gameHeadUrl: "",
      };
      localStorage.setItem("aki-calendar:user-info", JSON.stringify(calUser));
      localStorage.setItem("aki-calendar:role-info", JSON.stringify(calRole));

      try {
        sessionStorage.setItem(
          "mc-base-params",
          JSON.stringify({
            roleId: String(roleId || ""),
            serverId: String(serverId || ""),
            isMaster: true,
            gameId: String(a.gameId || "3"),
            eventId: "",
          })
        );
      } catch (e2) {}

      console.log("[webkujiequ] storage seeded", {
        hasToken: !!a.token,
        hasDid: !!a.did,
        userId: a.userId || null,
        roleId: roleId || null,
        serverId: serverId || null,
        gameId: a.gameId || "3",
      });
    } catch (e) {
      console.error("[webkujiequ] seedStorage failed", e);
    }
  }

  // Called from Rust after daemon OPEN/AUTH updates window.__kjqAuth.
  // opts.clearFirst=true wipes previous user caches before writing.
  window.__kjqApplyAuth = function (next, opts) {
    if (next && typeof next === "object") {
      // Full replace of known fields so empty values from a new user can win.
      window.__kjqAuth = Object.assign({}, defaults, next);
    }
    seedStorage(Object.assign({ clearFirst: true }, opts || {}));
    return true;
  };

  window.__kjqClearUserCaches = clearUserCaches;

  function userInfoPayload() {
    var a = AUTH();
    return JSON.stringify({
      token: a.token || localStorage.getItem("token") || "",
      userId: a.userId || localStorage.getItem("userId") || "",
      did: a.did || "",
      roleId: currentRoleId(),
      serverId: currentServerId(),
      channelId: a.channelId || "19",
      appVersion: "3.0.1",
      enterSource: "3",
    });
  }

  function refreshPayload() {
    var a = AUTH();
    return JSON.stringify({
      code: 0,
      token: a.token || localStorage.getItem("token") || "",
      userId: a.userId || localStorage.getItem("userId") || "",
      did: a.did || "",
      appVersion: "3.0.1",
    });
  }

  function okJson(extra) {
    var base = { result: true, code: 0, data: {} };
    if (extra && typeof extra === "object") {
      for (var k in extra) base[k] = extra[k];
    }
    return JSON.stringify(base);
  }

  var handlers = {};

  function handleCall(name, data, callback) {
    var cb = typeof callback === "function" ? callback : function () {};
    var payload = data || {};
    console.log("[webkujiequ] callHandler", name, payload);

    switch (name) {
      case "getUserInfo":
        seedStorage();
        cb(userInfoPayload());
        return;
      case "refreshToken":
      case "refreshTokenV2":
        seedStorage();
        cb(refreshPayload());
        return;
      case "selectRole":
      case "chooseRole":
        try {
          var selected = rolePayload({
            roleId: payload && payload.roleId,
            serverId: payload && payload.serverId,
          });
          if (selected.roleId) {
            localStorage.setItem("roleId", selected.roleId);
            localStorage.setItem("mc_roleId", selected.roleId);
          }
          if (selected.serverId) {
            localStorage.setItem("serverId", selected.serverId);
            localStorage.setItem("mc_serverId", selected.serverId);
          }
          localStorage.setItem("mc_userInfo", JSON.stringify(selected));
          localStorage.setItem("userInfo", JSON.stringify(selected));
          try {
            sessionStorage.setItem(
              "mc-base-params",
              JSON.stringify({
                roleId: selected.roleId,
                serverId: selected.serverId,
                isMaster: true,
                gameId: String(
                  (payload && payload.gameId) || AUTH().gameId || "3"
                ),
                eventId: (payload && payload.eventId) || "",
              })
            );
          } catch (e3) {}
          console.log("[webkujiequ] selectRole ->", selected);
          cb(JSON.stringify(selected));
        } catch (e) {
          console.error("[webkujiequ] selectRole failed", e);
          cb(JSON.stringify(rolePayload()));
        }
        return;
      case "finishPage":
      case "close_webview":
      case "appLogout":
      case "setNavigationBarHidden":
      case "setAppNavbarText":
      case "setToolInfo":
      case "setAppShareData":
      case "appShare":
      case "appShareSuccess":
      case "share":
      case "showWidgetGuide":
      case "activityChangeTitle":
      case "toSkip":
      case "openGame":
      case "new_openPage":
      case "download_image":
      case "chooseActivityPhoto":
      case "useSystemSetting":
        cb(okJson());
        return;
      case "getSystemStatus":
        cb(
          JSON.stringify({
            result: true,
            data: { network: true, battery: 100, brightness: 0.5 },
          })
        );
        return;
      case "startDeviceMotionUpdates":
        cb(JSON.stringify({ result: true }));
        return;
      case "getDeviceMotion":
        cb(
          JSON.stringify({
            result: true,
            data: { gravity: { x: 0, y: -1, z: 0 } },
          })
        );
        return;
      default:
        if (handlers[name]) {
          try {
            handlers[name](payload, cb);
          } catch (e) {
            console.error("[webkujiequ] handler error", name, e);
            cb(okJson());
          }
          return;
        }
        console.warn("[webkujiequ] unhandled bridge call", name, payload);
        cb(okJson());
    }
  }

  var bridge = {
    init: function (cb) {
      if (typeof cb === "function") {
        try {
          cb(null, null, null);
        } catch (e) {}
      }
    },
    callHandler: function (name, data, callback) {
      if (typeof data === "function" && callback === undefined) {
        callback = data;
        data = {};
      }
      setTimeout(function () {
        handleCall(name, data, callback);
      }, 0);
    },
    registerHandler: function (name, callback) {
      handlers[name] = callback;
      console.log("[webkujiequ] registerHandler", name);
    },
  };

  window.WebViewJavascriptBridge = bridge;
  seedStorage();

  try {
    document.dispatchEvent(new Event("WebViewJavascriptBridgeReady"));
  } catch (e) {
    var ev = document.createEvent("Event");
    ev.initEvent("WebViewJavascriptBridgeReady", true, true);
    document.dispatchEvent(ev);
  }

  console.log("[webkujiequ] WebViewJavascriptBridge mock installed");
})();
