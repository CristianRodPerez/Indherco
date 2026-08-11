import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiGet } from '../../api/httpClient';
import type { AuditLog } from '../../api/types';
import { getStoredUser, getToken } from '../../auth/authStorage';
import { AppShell } from '../../layouts/AppShell';

export function AuditPage() {
  const navigate = useNavigate();
  const [token] = useState(() => getToken());
  const [user] = useState(() => getStoredUser());
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!token || !user) {
      navigate('/login');
      return;
    }
    if (user.baseRole !== 'ADMIN_OFICINA') {
      navigate('/oficina');
      return;
    }
    loadAudit();
  }, [navigate, token, user]);

  async function loadAudit() {
    if (!token) return;
    setLoading(true);
    setError('');
    try {
      setLogs(await apiGet<AuditLog[]>('/audit', token));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cargar la auditoria.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AppShell title="Auditoria" subtitle="Registro de acciones importantes del sistema.">
      <section className="office-section">
        <div className="section-header">
          <h2>Ultimos eventos</h2>
          <button className="action-button action-button--secondary" onClick={loadAudit}>{loading ? 'Cargando...' : 'Actualizar'}</button>
        </div>
        {error && <p className="error-message">{error}</p>}
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Usuario</th>
                <th>Modulo</th>
                <th>Accion</th>
                <th>Entidad</th>
                <th>Detalle</th>
                <th>Motivo</th>
                <th>Correlation ID</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id}>
                  <td>{formatDateTime(log.occurredAt)}</td>
                  <td>{log.username || '-'}</td>
                  <td>{log.module}</td>
                  <td><span className="type-pill type-pill--neutral">{labelAction(log.action)}</span></td>
                  <td>{log.entity ? `${log.entity} #${log.entityId ?? '-'}` : '-'}</td>
                  <td>{log.newDetail || log.previousDetail || '-'}</td>
                  <td>{log.reason || '-'}</td>
                  <td className="mono-cell">{log.correlationId || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {logs.length === 0 && !loading && <p className="muted empty-table">No hay eventos para mostrar.</p>}
        </div>
      </section>
    </AppShell>
  );
}

function labelAction(action: string) {
  return action.replaceAll('_', ' ');
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('es-CL', {
    dateStyle: 'short',
    timeStyle: 'short'
  });
}
