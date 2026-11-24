(() => {
  document.querySelectorAll('[data-password-toggle]').forEach(button => {
    button.addEventListener('click', () => {
      const input = document.getElementById(button.dataset.passwordToggle);
      const visible = input.type === 'password';
      input.type = visible ? 'text' : 'password';
      button.textContent = visible ? 'Hide' : 'Show';
      button.setAttribute('aria-label', `${visible ? 'Hide' : 'Show'} password`);
      button.setAttribute('aria-pressed', String(visible));
    });
  });
  const form = document.querySelector('.register-form');
  if (form) form.addEventListener('submit', event => {
    const password = form.elements.password.value;
    const confirmation = form.elements.confirmPassword.value;
    if (password !== confirmation) {
      event.preventDefault();
      form.elements.confirmPassword.setCustomValidity('Passwords do not match');
      form.reportValidity();
      form.elements.confirmPassword.addEventListener('input', () => form.elements.confirmPassword.setCustomValidity(''), { once: true });
    }
  });
})();
