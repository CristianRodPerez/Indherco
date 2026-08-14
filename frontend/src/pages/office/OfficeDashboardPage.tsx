import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiGet, apiPost } from '../../api/httpClient';
import type { Dashboard, Movement } from '../../api/types';
import { getStoredUser, getToken } from '../../auth/authStorage';
import { AppShell } from '../../layouts/AppShell';

export function OfficeDashboardPage() {
  const navigate = useNavigate();
  const [token] = useState(() => getToken());
  const [user] = useState(() => getStoredUser());
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [receiptSupplyId, setReceiptSupplyId] = useState('');
  const [receiptQuantity, setReceiptQuantity] = useState('');
  const [receiptObservation, setReceiptObservation] = useState('');

  useEffect(() => {
    if (!token || !user) {
      navigate('/login');
      return;
    }
    if (user.baseRole === 'OPERADOR') {
      navigate('/operador');
      return;
    }
    apiGet<Dashboard>('/dashboard/today', token).then(setDashboard).catch((err) => setError(err.message));
  }, [navigate, token, user]);

  const metrics = [
    { label: 'Produccion de hoy', value: `${dashboard?.productionToday ?? 0}`, tone: 'green' },
    { label: 'Despachos de hoy', value: `${dashboard?.dispatchToday ?? 0}`, tone: 'blue' },
    { label: 'Consumos de hoy', value: `${dashboard?.consumptionToday ?? 0}`, tone: 'amber' },
    { label: 'Alertas activas', value: `${dashboard?.activeAlerts.length ?? 0}`, tone: 'red' }
  ];

  async function closeDay() {
    if (!token) return;
    setError('');
    setMessage('');
    try {
      await apiPost('/daily-closing', { observation: 'Cierre generado desde dashboard.' }, token);
      setMessage('Dia cerrado correctamente.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cerrar el dia.');
    }
  }

  async function dismissDailyAlert(type: string) {
    if (!token) return;
    setError('');
    setMessage('');
    try {
      setDashboard(await apiPost<Dashboard>('/dashboard/daily-alerts/dismiss', { type }, token));
      setMessage('Alerta ocultada para el dia de hoy.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo ocultar la alerta.');
    }
  }

  async function registerSupplyReceipt(event: FormEvent) {
    event.preventDefault();
    if (!token) return;
    setError('');
    setMessage('');
    try {
      await apiPost<Movement>('/movements/supply-receipt', {
        supplyId: Number(receiptSupplyId),
        quantity: Number(receiptQuantity),
        observation: receiptObservation
      }, token);
      setReceiptQuantity('');
      setReceiptObservation('');
      setDashboard(await apiGet<Dashboard>('/dashboard/today', token));
      setMessage('Entrada de insumos registrada correctamente.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo registrar la entrada de insumos.');
    }
  }

  return (
    <AppShell title="Inicio oficina" subtitle="Resumen para control de produccion y stock.">
      {message && <p className="success-message">{message}</p>}
      {error && <p className="error-message">{error}</p>}

      <section className="alert-panel">
        <div className="section-header">
          <div>
            <h2>Alertas operativas</h2>
            <p className="muted">Prioridades del dia para que oficina pueda coordinar con planta.</p>
          </div>
          <button className="close-day-button" onClick={closeDay}>Cerrar dia</button>
        </div>
        <div className="alert-grid">
          {dashboard?.activeAlerts.map((alert) => (
            <article className={`alert-card alert-card--${alert.level.toLowerCase()}`} key={`${alert.type}-${alert.id}`}>
              <strong>{labelAlertType(alert.type)}</strong>
              <span>{alert.message}</span>
              <div className="alert-card-footer">
                <small>{labelAlertLevel(alert.level)}</small>
                {isDismissibleDailyAlert(alert.type) && (
                  <button type="button" onClick={() => dismissDailyAlert(alert.type)}>Ocultar hoy</button>
                )}
              </div>
            </article>
          ))}
          {dashboard?.activeAlerts.length === 0 && <p className="muted">No hay alertas activas.</p>}
        </div>
      </section>

      <section className="metric-grid">
        {metrics.map((metric) => (
          <article className={`metric-card metric-card--${metric.tone}`} key={metric.label}>
            <p>{metric.label}</p>
            <strong>{metric.value}</strong>
          </article>
        ))}
      </section>

      <section className="office-section operator-form operator-form--supply-receipt">
        <h2>Ingresar stock recibido</h2>
        <p className="muted">Registra los insumos que llegan para aumentar el stock y conservar la trazabilidad.</p>
        <form className="form compact-form" onSubmit={registerSupplyReceipt}>
          <label>
            Insumo
            <select value={receiptSupplyId} onChange={(event) => setReceiptSupplyId(event.target.value)} required>
              <option value="">Seleccione</option>
              {dashboard?.suppliesStock.map((supply) => <option key={supply.id} value={supply.id}>{supply.name} ({supply.currentStock} {supply.unitOfMeasure})</option>)}
            </select>
          </label>
          <label>Cantidad recibida<input type="number" min="1" step="1" value={receiptQuantity} onChange={(event) => setReceiptQuantity(event.target.value)} required /></label>
          <label>Observacion<input value={receiptObservation} onChange={(event) => setReceiptObservation(event.target.value)} placeholder="Proveedor, guia u otro detalle" /></label>
          <button className="primary-button" type="submit">Registrar entrada</button>
        </form>
      </section>

      <section className="split-grid">
        <div className="office-section">
          <h2>Stock productos</h2>
          <div className="compact-table-wrap">
            <table className="compact-table">
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Tipo</th>
                  <th>Stock</th>
                </tr>
              </thead>
              <tbody>
                {dashboard?.productsStock.map((product) => (
                  <tr key={product.id}>
                    <td>{product.name}</td>
                    <td>{product.type || '-'}</td>
                    <td>
                      <span className={`stock-amount stock-amount--${stockState(product.currentStock, product.minimumStock)}`}>
                        {product.currentStock} {product.unitOfMeasure}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {dashboard?.productsStock.length === 0 && <p className="muted empty-table">Sin productos registrados.</p>}
          </div>
        </div>

        <div className="office-section">
          <h2>Stock insumos</h2>
          <div className="compact-table-wrap">
            <table className="compact-table">
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Categoria</th>
                  <th>Stock</th>
                </tr>
              </thead>
              <tbody>
                {dashboard?.suppliesStock.map((supply) => (
                  <tr key={supply.id}>
                    <td>{supply.name}</td>
                    <td>{supply.category || '-'}</td>
                    <td>
                      <span className={`stock-amount stock-amount--${stockState(supply.currentStock, supply.minimumStock)}`}>
                        {supply.currentStock} {supply.unitOfMeasure}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {dashboard?.suppliesStock.length === 0 && <p className="muted empty-table">Sin insumos registrados.</p>}
          </div>
        </div>
      </section>

      <section className="office-section">
        <h2>Ultimos movimientos</h2>
        <div className="simple-list">
          {dashboard?.latestMovements.map((movement) => (
            <article key={movement.id}>
              <strong>{movement.movementType}</strong>
              <span>{movement.itemName} - {movement.quantity} {movement.unitOfMeasure}</span>
            </article>
          ))}
          {dashboard?.latestMovements.length === 0 && <p className="muted">Sin movimientos registrados.</p>}
        </div>
      </section>

    </AppShell>
  );
}

function isDismissibleDailyAlert(type: string) {
  return type === 'REGISTRO_PRODUCCION_PENDIENTE'
    || type === 'REGISTRO_DESPACHO_PENDIENTE'
    || type === 'REGISTRO_CONSUMO_PENDIENTE';
}

function labelAlertLevel(level: string) {
  if (level === 'CRITICA') return 'Critica';
  if (level === 'ADVERTENCIA') return 'Advertencia';
  return 'Informacion';
}

function labelAlertType(type: string) {
  if (type === 'REGISTRO_PRODUCCION_PENDIENTE') return 'Falta produccion';
  if (type === 'REGISTRO_DESPACHO_PENDIENTE') return 'Falta despacho';
  if (type === 'REGISTRO_CONSUMO_PENDIENTE') return 'Falta consumo';
  if (type === 'STOCK_BAJO_PRODUCTO') return 'Stock bajo producto';
  if (type === 'STOCK_BAJO_INSUMO') return 'Stock bajo insumo';
  if (type === 'DESPACHO_SIN_STOCK') return 'Despacho sin stock';
  if (type === 'CONSUMO_SIN_STOCK') return 'Consumo sin stock';
  if (type === 'DIA_SIN_CIERRE') return 'Dia sin cierre';
  return 'Alerta';
}

function stockState(currentStock: number, minimumStock?: number) {
  if (minimumStock == null) return 'optimo';
  if (currentStock <= 0) return 'critico';
  if (currentStock <= minimumStock) return 'bajo';
  return 'optimo';
}
