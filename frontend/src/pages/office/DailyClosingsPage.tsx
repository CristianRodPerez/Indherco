import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiGet, apiPost } from '../../api/httpClient';
import type { DailyClosing } from '../../api/types';
import { getStoredUser, getToken } from '../../auth/authStorage';
import { AppShell } from '../../layouts/AppShell';

const today = new Date().toISOString().slice(0, 10);

export function DailyClosingsPage() {
  const navigate = useNavigate();
  const [token] = useState(() => getToken());
  const [user] = useState(() => getStoredUser());
  const [closings, setClosings] = useState<DailyClosing[]>([]);
  const [closeDate, setCloseDate] = useState(today);
  const [closeObservation, setCloseObservation] = useState('');
  const [selectedDate, setSelectedDate] = useState(today);
  const [reason, setReason] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const isAdmin = user?.baseRole === 'ADMIN_OFICINA';

  useEffect(() => {
    if (!token || !user) {
      navigate('/login');
      return;
    }
    if (user.baseRole === 'OPERADOR') {
      navigate('/operador');
      return;
    }
    loadClosings();
  }, [navigate, token, user]);

  async function loadClosings() {
    if (!token) return;
    setLoading(true);
    setError('');
    try {
      setClosings(await apiGet<DailyClosing[]>('/daily-closing', token));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudieron cargar los cierres.');
    } finally {
      setLoading(false);
    }
  }

  async function closeDay(event: FormEvent) {
    event.preventDefault();
    if (!token || !closeDate) return;
    setError('');
    setMessage('');
    try {
      await apiPost('/daily-closing', { closingDate: closeDate, observation: closeObservation }, token);
      setMessage('Dia cerrado correctamente.');
      setCloseObservation('');
      await loadClosings();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cerrar el dia.');
    }
  }

  async function reopen(event: FormEvent) {
    event.preventDefault();
    if (!token || !selectedDate || !reason.trim()) return;
    setError('');
    setMessage('');
    try {
      await apiPost(`/daily-closing/${selectedDate}/reopen`, { reason }, token);
      setMessage('Dia reabierto correctamente.');
      setReason('');
      await loadClosings();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo reabrir el dia.');
    }
  }

  return (
    <AppShell title="Cierres diarios" subtitle="Control de dias cerrados y reapertura autorizada.">
      <section className="office-section">
        <h2>Cerrar dia</h2>
        <form className="filter-form" onSubmit={closeDay}>
          <label>
            Fecha
            <input type="date" value={closeDate} onChange={(event) => setCloseDate(event.target.value)} />
          </label>
          <label className="wide-field">
            Observacion
            <input value={closeObservation} onChange={(event) => setCloseObservation(event.target.value)} placeholder="Ej: cierre revisado por oficina" />
          </label>
          <button className="primary-button" type="submit">Cerrar dia</button>
        </form>
        {message && <p className="success-message">{message}</p>}
        {error && <p className="error-message">{error}</p>}
      </section>

      {isAdmin && (
        <section className="office-section">
          <h2>Reabrir dia</h2>
          <form className="filter-form" onSubmit={reopen}>
            <label>
              Fecha
              <input type="date" value={selectedDate} onChange={(event) => setSelectedDate(event.target.value)} />
            </label>
            <label className="wide-field">
              Motivo
              <input value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Ej: correccion de movimiento" />
            </label>
            <button className="danger-button" type="submit">Reabrir dia</button>
          </form>
        </section>
      )}

      {!isAdmin && (
        <section className="office-section">
          <p className="muted">La oficina puede cerrar dias y revisar cierres. Solo admin puede reabrir un dia cerrado.</p>
        </section>
      )}

      <section className="office-section">
        <div className="section-header">
          <h2>Ultimos cierres</h2>
          <button className="action-button action-button--secondary" onClick={loadClosings}>{loading ? 'Cargando...' : 'Actualizar'}</button>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Estado</th>
                <th>Cerro</th>
                <th>Reabrio</th>
                <th>Produccion</th>
                <th>Despacho</th>
                <th>Consumo</th>
                <th>Observacion</th>
                <th>Motivo reapertura</th>
              </tr>
            </thead>
            <tbody>
              {closings.map((closing) => (
                <tr key={closing.id}>
                  <td>{formatDate(closing.closingDate)}</td>
                  <td><span className={`status-pill status-pill--${closing.status.toLowerCase()}`}>{labelStatus(closing.status)}</span></td>
                  <td>{closing.closedBy}<br /><span className="muted">{formatDateTime(closing.closedAt)}</span></td>
                  <td>{closing.reopenedBy || '-'}{closing.reopenedAt && <><br /><span className="muted">{formatDateTime(closing.reopenedAt)}</span></>}</td>
                  <td>{closing.totalProduction}</td>
                  <td>{closing.totalDispatch}</td>
                  <td>{closing.totalConsumption}</td>
                  <td>{closing.observation || '-'}</td>
                  <td>{closing.reopenReason || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {closings.length === 0 && !loading && <p className="muted empty-table">No hay cierres registrados.</p>}
        </div>
      </section>
    </AppShell>
  );
}

function labelStatus(status: DailyClosing['status']) {
  return status === 'CERRADO' ? 'Cerrado' : 'Reabierto';
}

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString('es-CL');
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('es-CL', {
    dateStyle: 'short',
    timeStyle: 'short'
  });
}
