(function () {
  if (window.__webkujiequHideUiChromeInstalled) return;
  window.__webkujiequHideUiChromeInstalled = true;

  var STYLE_ID = "webkujiequ-hide-ui-chrome";
  // Kuro H5 chrome to strip in embedded WebView
  var SELECTORS = [
    ".action-area",
    "div.action-area",
    "[class~='action-area']",
    ".navbar-unit",
    "div.navbar-unit",
    "[class~='navbar-unit']",
  ];
  var CSS =
    "" +
    "/* Hide Kuro H5 chrome (action bar / navbar) in embedded WebView */" +
    SELECTORS.join(",") +
    " {" +
    "  display: none !important;" +
    "  visibility: hidden !important;" +
    "  pointer-events: none !important;" +
    "  height: 0 !important;" +
    "  max-height: 0 !important;" +
    "  overflow: hidden !important;" +
    "  margin: 0 !important;" +
    "  padding: 0 !important;" +
    "  opacity: 0 !important;" +
    "}";

  function injectCss() {
    try {
      var doc = document;
      if (!doc || !doc.documentElement) return;
      var el = doc.getElementById(STYLE_ID);
      if (!el) {
        el = doc.createElement("style");
        el.id = STYLE_ID;
        el.type = "text/css";
        (doc.head || doc.documentElement).appendChild(el);
      }
      if (el.textContent !== CSS) el.textContent = CSS;
    } catch (e) {
      console.warn("[webkujiequ] hide-ui-chrome css failed", e);
    }
  }

  function removeNodes() {
    try {
      var list = document.querySelectorAll(".action-area, .navbar-unit, .van-sticky, .header-area, .extra-area");
      for (var i = 0; i < list.length; i++) {
        var n = list[i];
        if (n && n.parentNode) {
          n.parentNode.removeChild(n);
        }
      }
    } catch (e2) {
      console.warn("[webkujiequ] hide-ui-chrome remove failed", e2);
    }
  }

  function apply() {
    injectCss();
    removeNodes();
  }

  apply();

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", apply, { once: true });
  }
  setTimeout(apply, 0);
  setTimeout(apply, 300);
  setTimeout(apply, 1000);
  setTimeout(apply, 2500);

  try {
    var obs = new MutationObserver(function () {
      if (!document.getElementById(STYLE_ID)) injectCss();
      if (document.querySelector(".action-area, .navbar-unit")) removeNodes();
    });
    obs.observe(document.documentElement, { childList: true, subtree: true });
  } catch (e3) {}
})();
