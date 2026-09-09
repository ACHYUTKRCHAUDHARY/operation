const form = document.getElementById('login-form');
const errorEl = document.getElementById('login-error');
const successEl = document.getElementById('login-success');
const params = new URLSearchParams(window.location.search);

if (params.get('registered') === '1') {
  successEl.textContent = 'Account created successfully. Sign in to continue.';
  const email = params.get('email');
  if (email) form.elements.email.value = email;
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  errorEl.textContent = '';
  successEl.textContent = '';

  const data = Object.fromEntries(new FormData(form).entries());
  const button = form.querySelector('button[type="submit"]');
  const original = button.textContent;
  button.disabled = true;
  button.textContent = 'Signing in…';

  try {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    if (!response.ok) {
      let message = 'Invalid email or password';
      try {
        const body = await response.json();
        message = body.detail || body.message || body.error || message;
      } catch (_) {}
      throw new Error(message);
    }

    await response.json();
    window.location.replace('/');
  } catch (error) {
    errorEl.textContent = error.message;
  } finally {
    button.disabled = false;
    button.textContent = original;
  }
});
