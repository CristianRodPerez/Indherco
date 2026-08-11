import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiGet, apiPatch, apiPost, apiPut } from '../../api/httpClient';
import type { User } from '../../api/types';
import { getStoredUser, getToken } from '../../auth/authStorage';
import { AppShell } from '../../layouts/AppShell';

const emptyForm = {
  name: '',
  username: '',
  password: '',
  baseRole: 'OPERADOR',
  canRegisterProduction: false,
  canRegisterDispatch: false,
  canRegisterConsumption: false
};

export function UsersPage() {
  const navigate = useNavigate();
  const [token] = useState(() => getToken());
  const [currentUser] = useState(() => getStoredUser());
  const [users, setUsers] = useState<User[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [editingUserId, setEditingUserId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState({
    name: '',
    baseRole: 'OPERADOR',
    canRegisterProduction: false,
    canRegisterDispatch: false,
    canRegisterConsumption: false
  });

  useEffect(() => {
    if (!token || !currentUser) {
      navigate('/login');
      return;
    }
    if (currentUser.baseRole !== 'ADMIN_OFICINA') {
      navigate('/operador');
      return;
    }
    loadUsers();
  }, [currentUser, navigate, token]);

  async function loadUsers() {
    if (!token) return;
    try {
      setUsers(await apiGet<User[]>('/users', token));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudieron cargar los usuarios.');
    }
  }

  async function createUser(event: FormEvent) {
    event.preventDefault();
    if (!token) return;
    setMessage('');
    setError('');

    try {
      await apiPost<User>('/users', form, token);
      setMessage('Usuario creado correctamente.');
      setForm(emptyForm);
      await loadUsers();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo crear el usuario.');
    }
  }

  function updateField(name: string, value: string | boolean) {
    setForm((current) => ({ ...current, [name]: value }));
  }

  function startEdit(user: User) {
    setEditingUserId(user.id);
    setEditForm({
      name: user.name,
      baseRole: user.baseRole,
      canRegisterProduction: user.canRegisterProduction,
      canRegisterDispatch: user.canRegisterDispatch,
      canRegisterConsumption: user.canRegisterConsumption
    });
  }

  async function saveEdit(userId: number) {
    if (!token) return;
    setMessage('');
    setError('');
    try {
      await apiPut<User>(`/users/${userId}`, editForm, token);
      setEditingUserId(null);
      setMessage('Usuario actualizado correctamente.');
      await loadUsers();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo actualizar el usuario.');
    }
  }

  async function toggleUserStatus(user: User) {
    if (!token) return;
    setMessage('');
    setError('');
    try {
      await apiPatch<User>(`/users/${user.id}/status`, { active: !user.active }, token);
      setMessage(user.active ? 'Usuario desactivado.' : 'Usuario activado.');
      await loadUsers();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cambiar el estado.');
    }
  }

  return (
    <AppShell title="Usuarios" subtitle="Crea personas y define que puede registrar cada una.">
      {(message || error) && (
        <section className="feedback-strip">
          {message && <p className="success-message">{message}</p>}
          {error && <p className="error-message">{error}</p>}
        </section>
      )}

      <section className="split-grid">
        <div className="office-section">
          <h2>Crear usuario</h2>
          <form className="form compact-form" onSubmit={createUser}>
            <label>
              Nombre
              <input value={form.name} onChange={(event) => updateField('name', event.target.value)} required />
            </label>
            <label>
              Usuario
              <input value={form.username} onChange={(event) => updateField('username', event.target.value)} required />
            </label>
            <label>
              Contrasena
              <input value={form.password} onChange={(event) => updateField('password', event.target.value)} type="password" required />
            </label>
            <label>
              Tipo
              <select value={form.baseRole} onChange={(event) => updateField('baseRole', event.target.value)}>
                <option value="OPERADOR">Operador</option>
                <option value="OFICINA">Oficina</option>
                <option value="ADMIN_OFICINA">Admin</option>
              </select>
            </label>

            {form.baseRole === 'OPERADOR' && (
              <div className="checkbox-grid">
                <label>
                  <input type="checkbox" checked={form.canRegisterProduction} onChange={(event) => updateField('canRegisterProduction', event.target.checked)} />
                  Produccion
                </label>
                <label>
                  <input type="checkbox" checked={form.canRegisterDispatch} onChange={(event) => updateField('canRegisterDispatch', event.target.checked)} />
                  Despacho
                </label>
                <label>
                  <input type="checkbox" checked={form.canRegisterConsumption} onChange={(event) => updateField('canRegisterConsumption', event.target.checked)} />
                  Consumo
                </label>
              </div>
            )}

            <button className="primary-button" type="submit">Crear usuario</button>
          </form>
        </div>

        <div className="office-section">
          <h2>Personas creadas</h2>
          <div className="simple-list">
            {users.map((user) => (
              <article key={user.id}>
                {editingUserId === user.id ? (
                  <div className="edit-panel">
                    <label>
                      Nombre
                      <input value={editForm.name} onChange={(event) => setEditForm({ ...editForm, name: event.target.value })} />
                    </label>
                    <label>
                      Tipo
                      <select value={editForm.baseRole} onChange={(event) => setEditForm({ ...editForm, baseRole: event.target.value })}>
                        <option value="OPERADOR">Operador</option>
                        <option value="OFICINA">Oficina</option>
                        <option value="ADMIN_OFICINA">Admin</option>
                      </select>
                    </label>
                    {editForm.baseRole === 'OPERADOR' && (
                      <div className="checkbox-grid">
                        <label><input type="checkbox" checked={editForm.canRegisterProduction} onChange={(event) => setEditForm({ ...editForm, canRegisterProduction: event.target.checked })} /> Produccion</label>
                        <label><input type="checkbox" checked={editForm.canRegisterDispatch} onChange={(event) => setEditForm({ ...editForm, canRegisterDispatch: event.target.checked })} /> Despacho</label>
                        <label><input type="checkbox" checked={editForm.canRegisterConsumption} onChange={(event) => setEditForm({ ...editForm, canRegisterConsumption: event.target.checked })} /> Consumo</label>
                      </div>
                    )}
                    <div className="row-actions">
                      <button onClick={() => saveEdit(user.id)}>Guardar</button>
                      <button onClick={() => setEditingUserId(null)}>Cancelar</button>
                    </div>
                  </div>
                ) : (
                  <>
                    <strong>{user.name}</strong>
                    <span>{user.username} - {roleText(user)} - {user.active ? 'Activo' : 'Inactivo'}</span>
                    <div className="row-actions">
                      <button onClick={() => startEdit(user)}>Editar</button>
                      <button onClick={() => toggleUserStatus(user)}>{user.active ? 'Desactivar' : 'Activar'}</button>
                    </div>
                  </>
                )}
              </article>
            ))}
            {users.length === 0 && <p className="muted">Aun no hay usuarios.</p>}
          </div>
        </div>
      </section>
    </AppShell>
  );
}

function permissionsText(user: User) {
  const permissions = [
    user.canRegisterProduction ? 'Produccion' : '',
    user.canRegisterDispatch ? 'Despacho' : '',
    user.canRegisterConsumption ? 'Consumo' : ''
  ].filter(Boolean);

  return permissions.length > 0 ? permissions.join(', ') : 'Sin permisos';
}

function roleText(user: User) {
  if (user.baseRole === 'ADMIN_OFICINA') return 'Admin';
  if (user.baseRole === 'OFICINA') return 'Oficina';
  return permissionsText(user);
}
