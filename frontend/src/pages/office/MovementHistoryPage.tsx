import { FormEvent, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiGet, apiPost } from '../../api/httpClient';
import type { Movement } from '../../api/types';
import { getStoredUser, getToken } from '../../auth/authStorage';
import { AppShell } from '../../layouts/AppShell';
import { downloadMovementMonthlyPdf } from '../../reports/movementMonthlyPdf';

const today = toLocalDateInputValue(new Date());
const currentMonth = today.slice(0, 7);

export function MovementHistoryPage() {
  const navigate = useNavigate();
  const [token] = useState(() => getToken());
  const [currentUser] = useState(() => getStoredUser());
  const [movements, setMovements] = useState<Movement[]>([]);
  const [reportMonth, setReportMonth] = useState(currentMonth);
  const [selectedDate, setSelectedDate] = useState('');
  const [type, setType] = useState('TODOS');
  const [cancelTarget, setCancelTarget] = useState<Movement | null>(null);
  const [cancelReason, setCancelReason] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [downloadingReport, setDownloadingReport] = useState(false);
  const isAdmin = currentUser?.baseRole === 'ADMIN_OFICINA';

  useEffect(() => {
    if (!token || !currentUser) {
      navigate('/login');
      return;
    }
    if (currentUser.baseRole === 'OPERADOR') {
      navigate('/operador');
      return;
    }
    loadMonthMovements(reportMonth);
  }, [currentUser, navigate, token]);

  const filteredMovements = useMemo(() => {
    return movements.filter((movement) => {
      const matchesType = type === 'TODOS' || movement.movementType === type;
      const matchesDate = !selectedDate || movement.movementDate === selectedDate;
      return matchesType && matchesDate;
    });
  }, [movements, selectedDate, type]);

  const summary = useMemo(() => {
    return movements.reduce(
      (totals, movement) => {
        if (movement.movementType === 'PRODUCCION') totals.production += Number(movement.quantity);
        if (movement.movementType === 'DESPACHO') totals.dispatch += Number(movement.quantity);
        if (movement.movementType === 'CONSUMO') totals.consumption += Number(movement.quantity);
        return totals;
      },
      { production: 0, dispatch: 0, consumption: 0 }
    );
  }, [movements]);

  async function loadMovements(startDate: string, endDate: string) {
    if (!token) return;
    setLoading(true);
    setError('');
    try {
      const query = new URLSearchParams();
      if (startDate) query.set('from', startDate);
      if (endDate) query.set('to', endDate);
      setMovements(await apiGet<Movement[]>(`/movements?${query.toString()}`, token));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cargar el historial.');
    } finally {
      setLoading(false);
    }
  }

  async function loadMonthMovements(month: string) {
    if (!month) return;
    const [startDate, endDate] = monthRange(month);
    await loadMovements(startDate, endDate);
  }

  function changeMonth(month: string) {
    setReportMonth(month);
    setSelectedDate('');
    loadMonthMovements(month);
  }

  function clearDaySearch() {
    setSelectedDate('');
  }

  async function cancelMovement(event: FormEvent) {
    event.preventDefault();
    if (!token || !cancelTarget || !cancelReason.trim()) return;
    setError('');
    setMessage('');
    try {
      await apiPost(`/movements/${cancelTarget.id}/cancel`, { reason: cancelReason }, token);
      setMessage('Movimiento anulado correctamente.');
      setCancelTarget(null);
      setCancelReason('');
      await loadMonthMovements(reportMonth);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo anular el movimiento.');
    }
  }

  async function downloadMonthlyReport() {
    if (!token || !reportMonth) return;
    setError('');
    setMessage('');
    setDownloadingReport(true);
    try {
      const [startDate, endDate] = monthRange(reportMonth);
      const query = new URLSearchParams({ from: startDate, to: endDate });
      const monthlyMovements = await apiGet<Movement[]>(`/movements?${query.toString()}`, token);
      downloadMovementMonthlyPdf(reportMonth, monthlyMovements);
      setMessage('PDF mensual generado correctamente.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo generar el PDF mensual.');
    } finally {
      setDownloadingReport(false);
    }
  }

  return (
    <AppShell title="Historial" subtitle="Trazabilidad de produccion, despacho y consumo.">
      <section className="office-section">
        <div className="section-header">
          <div>
            <h2>Mes del historial</h2>
            <p className="muted">La tabla muestra por defecto todos los movimientos del mes seleccionado.</p>
          </div>
        </div>
        <div className="history-toolbar history-toolbar--compact">
          <label>
            Mes
            <input type="month" value={reportMonth} onChange={(event) => changeMonth(event.target.value)} />
          </label>
        </div>
        {message && <p className="success-message">{message}</p>}
        {error && <p className="error-message">{error}</p>}
      </section>

      <section className="metric-grid">
        <article className="metric-card metric-card--green">
          <p>Produccion del mes</p>
          <strong>{summary.production}</strong>
        </article>
        <article className="metric-card metric-card--blue">
          <p>Despacho del mes</p>
          <strong>{summary.dispatch}</strong>
        </article>
        <article className="metric-card metric-card--amber">
          <p>Consumo del mes</p>
          <strong>{summary.consumption}</strong>
        </article>
        <article className="metric-card metric-card--red">
          <p>Movimientos visibles</p>
          <strong>{filteredMovements.length}</strong>
        </article>
      </section>

      <section className="office-section">
        <div className="section-header">
          <div>
            <h2>Reporte mensual PDF</h2>
            <p className="muted">Descarga el historial del mes con resumen y detalle de movimientos.</p>
          </div>
          <button className="small-report-button" type="button" onClick={downloadMonthlyReport}>
            {downloadingReport ? 'Generando...' : 'Descargar PDF'}
          </button>
        </div>
      </section>

      <section className="office-section">
        <div className="section-header">
          <div>
            <h2>Busqueda rapida</h2>
            <p className="muted">Opcional: revisa un dia puntual o filtra por tipo dentro del mes.</p>
          </div>
        </div>
        <div className="history-toolbar">
          <label>
            Fecha puntual
            <input type="date" value={selectedDate} onChange={(event) => setSelectedDate(event.target.value)} />
          </label>
          <label>
            Tipo
            <select value={type} onChange={(event) => setType(event.target.value)}>
              <option value="TODOS">Todos</option>
              <option value="PRODUCCION">Produccion</option>
              <option value="DESPACHO">Despacho</option>
              <option value="CONSUMO">Consumo</option>
              <option value="ANULACION">Anulacion</option>
            </select>
          </label>
          <button className="action-button action-button--secondary" type="button" onClick={clearDaySearch}>Ver mes completo</button>
        </div>
        {loading && <p className="muted empty-table">Cargando movimientos...</p>}
      </section>

      {cancelTarget && (
        <section className="office-section danger-zone">
          <h2>Anular movimiento #{cancelTarget.id}</h2>
          <p className="muted">Se creara un movimiento inverso y el registro original quedara marcado como anulado.</p>
          <form className="filter-form" onSubmit={cancelMovement}>
            <label className="wide-field">
              Motivo
              <input value={cancelReason} onChange={(event) => setCancelReason(event.target.value)} placeholder="Ej: registro duplicado" />
            </label>
            <button className="danger-button" type="submit">Confirmar anulacion</button>
            <button className="action-button action-button--secondary" type="button" onClick={() => { setCancelTarget(null); setCancelReason(''); }}>Cancelar</button>
          </form>
        </section>
      )}

      <section className="office-section">
        <h2>Movimientos registrados</h2>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Hora registro</th>
                <th>Usuario</th>
                <th>Tipo</th>
                <th>Producto/Insumo</th>
                <th>Cantidad</th>
                <th>Stock anterior</th>
                <th>Stock nuevo</th>
                <th>Estado</th>
                <th>Observacion</th>
                {isAdmin && <th>Acciones</th>}
              </tr>
            </thead>
            <tbody>
              {filteredMovements.map((movement) => (
                <tr key={movement.id}>
                  <td>{formatDate(movement.movementDate)}</td>
                  <td>{formatDateTime(movement.registeredAt)}</td>
                  <td>{movement.registeredBy}</td>
                  <td><span className={`type-pill type-pill--${movement.movementType.toLowerCase()}`}>{labelType(movement.movementType)}</span></td>
                  <td>{movement.itemName}</td>
                  <td>{movement.quantity} {movement.unitOfMeasure}</td>
                  <td>{movement.previousStock}</td>
                  <td>{movement.newStock}</td>
                  <td><span className={`status-pill status-pill--${movement.status.toLowerCase()}`}>{labelStatus(movement.status)}</span></td>
                  <td>{movement.observation || '-'}</td>
                  {isAdmin && (
                    <td>
                      {movement.status === 'ACTIVO' && movement.movementType !== 'ANULACION' ? (
                        <button className="small-danger-button" onClick={() => setCancelTarget(movement)}>Anular</button>
                      ) : (
                        <span className="muted">-</span>
                      )}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
          {filteredMovements.length === 0 && <p className="muted empty-table">No hay movimientos para mostrar.</p>}
        </div>
      </section>
    </AppShell>
  );
}

function labelType(type: Movement['movementType']) {
  if (type === 'PRODUCCION') return 'Produccion';
  if (type === 'DESPACHO') return 'Despacho';
  if (type === 'CONSUMO') return 'Consumo';
  return 'Anulacion';
}

function labelStatus(status: Movement['status']) {
  return status === 'ACTIVO' ? 'Activo' : 'Anulado';
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

function monthRange(month: string) {
  const [year, monthNumber] = month.split('-').map(Number);
  const startDate = `${month}-01`;
  const endDate = toLocalDateInputValue(new Date(year, monthNumber, 0));
  return [startDate, endDate];
}

function toLocalDateInputValue(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
