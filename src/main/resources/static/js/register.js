const form = document.getElementById('register-form');
const errorEl = document.getElementById('register-error');
const successEl = document.getElementById('register-success');

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  errorEl.textContent = '';
  successEl.textContent = '';

  const data = new FormData(form);
  const fullName = String(data.get('fullName') || '').trim();
  const email = String(data.get('email') || '').trim();
  const password = String(data.get('password') || '');
  const confirmPassword = String(data.get('confirmPassword') || '');

  if (password !== confirmPassword) {
    errorEl.textContent = 'Passwords do not match.';
    return;
  }

  const button = form.querySelector('button[type="submit"]');
  button.disabled = true;
  button.textContent = 'Creating account…';

  try {
    const response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fullName, email, password })
    });

    if (!response.ok) {
      let message = 'Unable to create account.';
      try {
        const payload = await response.json();
        message = payload.detail || payload.message || payload.error || message;
      } catch (_) {}
      throw new Error(message);
    }

    successEl.textContent = 'Account created. Redirecting to sign in…';
    form.reset();
    setTimeout(() => {
      window.location.href = `/login.html?registered=1&email=${encodeURIComponent(email)}`;
    }, 900);
  } catch (error) {
    errorEl.textContent = error.message || 'Unable to create account.';
  } finally {
    button.disabled = false;
    button.textContent = 'Create YardFlow account';
  }
});
