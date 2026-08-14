import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiPost } from '../../api/httpClient';
import { saveSession } from '../../auth/authStorage';
import type { AuthResponse } from '../../auth/authTypes';

export function LoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [keepSignedIn, setKeepSignedIn] = useState(false);
  const [fieldsLocked, setFieldsLocked] = useState(true);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError('');
    try {
      const response = await apiPost<AuthResponse>('/auth/login', { username, password });
      saveSession(response.token, response.user, keepSignedIn);
      navigate(response.user.baseRole === 'OPERADOR' ? '/operador' : '/oficina');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo iniciar sesion.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-page">
      <section className="login-panel">
        <div className="login-logo">
          <img src="/brand/indherco-logo-horizontal.png" alt="Indherco" />
        </div>
        <h1>Iniciar sesión</h1>
        <form className="form" onSubmit={handleSubmit} autoComplete="off">
          <label>
            Usuario
            <input name="indherco-login-user" value={username} onChange={(event) => setUsername(event.target.value)} onFocus={() => setFieldsLocked(false)} readOnly={fieldsLocked} type="text" autoComplete="off" />
          </label>
          <label>
            Contraseña
            <input name="indherco-login-password" value={password} onChange={(event) => setPassword(event.target.value)} onFocus={() => setFieldsLocked(false)} readOnly={fieldsLocked} type="password" autoComplete="new-password" />
          </label>
          <label className="remember-session">
            <input
              type="checkbox"
              checked={keepSignedIn}
              onChange={(event) => setKeepSignedIn(event.target.checked)}
            />
            Mantener sesión iniciada
          </label>
          {error && <p className="error-message">{error}</p>}
          <button type="submit" className="primary-button" disabled={loading}>
            {loading ? 'Entrando...' : 'Entrar'}
          </button>
        </form>
      </section>
    </main>
  );
}
