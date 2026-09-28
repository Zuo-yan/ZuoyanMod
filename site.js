const menuButton = document.querySelector('.menu-toggle');
const navigation = document.querySelector('nav');
menuButton?.addEventListener('click', () => {
  const open = navigation.classList.toggle('open');
  menuButton.setAttribute('aria-expanded', String(open));
});
navigation?.querySelectorAll('a').forEach(link => link.addEventListener('click', () => {
  navigation.classList.remove('open');
  menuButton?.setAttribute('aria-expanded', 'false');
}));

const revealTargets = document.querySelectorAll('.feature-card, .relic-row, .install-card, .quote-band');
revealTargets.forEach(el => el.classList.add('reveal'));
if ('IntersectionObserver' in window) {
  const revealObserver = new IntersectionObserver(entries => entries.forEach(entry => {
    if (entry.isIntersecting) {
      entry.target.classList.add('is-visible');
      revealObserver.unobserve(entry.target);
    }
  }), { threshold: 0.12 });
  revealTargets.forEach(el => revealObserver.observe(el));
} else revealTargets.forEach(el => el.classList.add('is-visible'));
