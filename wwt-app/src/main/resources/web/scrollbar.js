(function () {
  if (window.__webkujiequScrollbarInstalled) return;
  window.__webkujiequScrollbarInstalled = true;

  var CSS =
    "" +
    "/* WebKujiequ thin dark scrollbar */" +
    "html {" +
    "  scrollbar-width: thin;" +
    "  scrollbar-color: rgba(255,255,255,0.28) transparent;" +
    "}" +
    "body {" +
    "  scrollbar-width: thin;" +
    "  scrollbar-color: rgba(255,255,255,0.28) transparent;" +
    "}" +
    "::-webkit-scrollbar {" +
    "  width: 6px;" +
    "  height: 6px;" +
    "}" +
    "::-webkit-scrollbar-track {" +
    "  background: transparent;" +
    "}" +
    "::-webkit-scrollbar-thumb {" +
    "  background-color: rgba(255,255,255,0.22);" +
    "  border-radius: 999px;" +
    "  border: 1px solid transparent;" +
    "  background-clip: content-box;" +
    "}" +
    "::-webkit-scrollbar-thumb:hover {" +
    "  background-color: rgba(255,255,255,0.38);" +
    "}" +
    "::-webkit-scrollbar-thumb:active {" +
    "  background-color: rgba(255,255,255,0.48);" +
    "}" +
    "::-webkit-scrollbar-corner {" +
    "  background: transparent;" +
    "}";

  function inject() {
    try {
      var doc = document;
      if (!doc || !doc.documentElement) return;
      var id = "webkujiequ-scrollbar-style";
      var el = doc.getElementById(id);
      if (!el) {
        el = doc.createElement("style");
        el.id = id;
        el.type = "text/css";
        (doc.head || doc.documentElement).appendChild(el);
      }
      if (el.textContent !== CSS) el.textContent = CSS;
    } catch (e) {
      console.warn("[webkujiequ] scrollbar inject failed", e);
    }
  }

  inject();

  // Re-inject after DOM is ready / SPA may replace head contents.
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", inject, { once: true });
  }
  setTimeout(inject, 0);
  setTimeout(inject, 300);
  setTimeout(inject, 1000);

  // Observe head removals (some SPAs rebuild head).
  try {
    var obs = new MutationObserver(function () {
      if (!document.getElementById("webkujiequ-scrollbar-style")) inject();
    });
    obs.observe(document.documentElement, { childList: true, subtree: true });
  } catch (e2) {}
})();
