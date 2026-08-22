// site.js — scroll-spy and scroll-reveal for the KasiGuru download page.
//
// Both use IntersectionObserver rather than a scroll listener, which would run
// on every frame for no gain. This file used to be an inline <script> at the
// foot of index.html, but the page's own CSP has no 'unsafe-inline' in
// script-src, so it was silently blocked in production. Moving it here is the
// fix, not a refactor for its own sake.
//
// Reveal is purely additive (adds a class that fades content in): under
// prefers-reduced-motion it is skipped entirely and every element is already
// in its final, readable state from CSS alone, so nothing depends on this
// script having run.
(function () {
  var links = Array.prototype.slice.call(document.querySelectorAll('#site-nav a[href^="#"]:not(.clay)'));
  var byId = {};
  links.forEach(function (a) { byId[a.getAttribute('href').slice(1)] = a; });
  var targets = Object.keys(byId)
    .map(function (id) { return document.getElementById(id); })
    .filter(Boolean);

  if (targets.length && 'IntersectionObserver' in window) {
    var visible = {};
    var spy = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) { visible[e.target.id] = e.isIntersecting ? e.intersectionRatio : 0; });
      var best = null, bestRatio = 0;
      Object.keys(visible).forEach(function (id) {
        if (visible[id] > bestRatio) { bestRatio = visible[id]; best = id; }
      });
      links.forEach(function (a) { a.removeAttribute('aria-current'); });
      if (best && byId[best]) byId[best].setAttribute('aria-current', 'true');
    }, { rootMargin: '-20% 0px -60% 0px', threshold: [0, 0.25, 0.5, 1] });
    targets.forEach(function (t) { spy.observe(t); });
  }

  var revealables = Array.prototype.slice.call(document.querySelectorAll('.reveal'));
  if (!revealables.length) return;
  if (!('IntersectionObserver' in window) || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    revealables.forEach(function (el) { el.classList.add('in'); });
    return;
  }
  var io = new IntersectionObserver(function (entries, obs) {
    var i = 0;
    entries.forEach(function (e) {
      if (!e.isIntersecting) return;
      // Stagger 35ms per item, capped at 8, matching the app's Motion.kt.
      e.target.style.transitionDelay = Math.min(i, 7) * 35 + 'ms';
      e.target.classList.add('in');
      obs.unobserve(e.target);
      i++;
    });
  }, { rootMargin: '0px 0px -12% 0px', threshold: 0.15 });
  revealables.forEach(function (el) { io.observe(el); });
})();
