// Applies the saved theme before the first paint, so a Light page never flashes dark first.
// The choice itself lives in Settings (src/lib/theme.ts), which keeps this copy in localStorage.
(function () {
  var choice = 'system';
  try { choice = localStorage.getItem('kg-theme') || 'system'; } catch (e) {}
  var light = choice === 'light' ||
    (choice === 'system' && !!window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches);
  document.documentElement.setAttribute('data-theme', light ? 'light' : 'dark');
})();
