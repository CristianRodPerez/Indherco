import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiGet, apiPatch, apiPost, apiPut } from '../../api/httpClient';
import type { OfficeInventoryItem, OfficeInventoryMovement } from '../../api/types';
import { getStoredUser, getToken } from '../../auth/authStorage';
import { AppShell } from '../../layouts/AppShell';

const emptyItem = {
  name: '',
  category: '',
  unitOfMeasure: 'unidad',
  currentStock: 0,
  minimumStock: 0
};

export function OfficeInventoryPage() {
  const navigate = useNavigate();
  const [token] = useState(() => getToken());
  const [user] = useState(() => getStoredUser());
  const isAdmin = user?.baseRole === 'ADMIN_OFICINA';
  const [items, setItems] = useState<OfficeInventoryItem[]>([]);
  const [movements, setMovements] = useState<OfficeInventoryMovement[]>([]);
  const [itemForm, setItemForm] = useState(emptyItem);
  const [selectedItemId, setSelectedItemId] = useState('');
  const [movementType, setMovementType] = useState<'ENTRADA' | 'CONSUMO'>('ENTRADA');
  const [quantity, setQuantity] = useState('');
  const [observation, setObservation] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [editingItemId, setEditingItemId] = useState<number | null>(null);
  const [itemEdit, setItemEdit] = useState(emptyItem);

  useEffect(() => {
    if (!token || !user) {
      navigate('/login');
      return;
    }
    if (user.baseRole === 'OPERADOR') {
      navigate('/operador');
      return;
    }
    loadInventory();
  }, [navigate, token, user]);

  async function loadInventory() {
    if (!token) return;
    try {
      const [loadedItems, loadedMovements] = await Promise.all([
        apiGet<OfficeInventoryItem[]>('/office-inventory/items?activeOnly=true', token),
        apiGet<OfficeInventoryMovement[]>('/office-inventory/movements', token)
      ]);
      setItems(loadedItems);
      setMovements(loadedMovements);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cargar el inventario de oficina.');
    }
  }

  async function createItem(event: FormEvent) {
    event.preventDefault();
    if (!token) return;
    setMessage('');
    setError('');
    try {
      await apiPost<OfficeInventoryItem>('/office-inventory/items', itemForm, token);
      setItemForm(emptyItem);
      setMessage('Item creado correctamente.');
      await loadInventory();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo crear el item.');
    }
  }

  async function registerMovement(event: FormEvent) {
    event.preventDefault();
    if (!token) return;
    setMessage('');
    setError('');
    try {
      await apiPost<OfficeInventoryMovement>('/office-inventory/movements', {
        itemId: Number(selectedItemId),
        movementType,
        quantity: parseInt(quantity, 10),
        observation
      }, token);
      setQuantity('');
      setObservation('');
      setMessage('Movimiento registrado correctamente.');
      await loadInventory();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo registrar el movimiento.');
    }
  }

  function startItemEdit(item: OfficeInventoryItem) {
    setEditingItemId(item.id);
    setItemEdit({
      name: item.name,
      category: item.category ?? '',
      unitOfMeasure: item.unitOfMeasure,
      currentStock: item.currentStock,
      minimumStock: item.minimumStock ?? 0
    });
  }

  async function saveItem(itemId: number) {
    if (!token) return;
    setMessage('');
    setError('');
    try {
      await apiPut<OfficeInventoryItem>(`/office-inventory/items/${itemId}`, itemEdit, token);
      setEditingItemId(null);
      setMessage('Item actualizado correctamente.');
      await loadInventory();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo actualizar el item.');
    }
  }

  async function toggleItem(item: OfficeInventoryItem) {
    if (!token) return;
    setMessage('');
    setError('');
    try {
      await apiPatch<OfficeInventoryItem>(`/office-inventory/items/${item.id}/status`, { active: !item.active }, token);
      setMessage(item.active ? 'Item desactivado.' : 'Item activado.');
      await loadInventory();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cambiar el estado.');
    }
  }

  return (
    <AppShell title="Inventario oficina" subtitle="Implementos de seguridad, herramientas y materiales internos.">
      <section className="split-grid">
        {isAdmin && (
          <div className="office-section">
            <h2>Crear item</h2>
            <form className="form compact-form" onSubmit={createItem}>
              <label>Nombre<input value={itemForm.name} onChange={(event) => setItemForm({ ...itemForm, name: event.target.value })} required /></label>
              <label>Categoria<input value={itemForm.category} onChange={(event) => setItemForm({ ...itemForm, category: event.target.value })} placeholder="Seguridad, herramienta, oficina" /></label>
              <label>Unidad<input value={itemForm.unitOfMeasure} onChange={(event) => setItemForm({ ...itemForm, unitOfMeasure: event.target.value })} required /></label>
              <label>Stock inicial<input type="number" min="0" step="1" value={itemForm.currentStock} onChange={(event) => setItemForm({ ...itemForm, currentStock: parseInt(event.target.value || '0', 10) })} /></label>
              <label>Stock minimo<input type="number" min="0" step="1" value={itemForm.minimumStock} onChange={(event) => setItemForm({ ...itemForm, minimumStock: parseInt(event.target.value || '0', 10) })} /></label>
              <button className="primary-button" type="submit">Crear item</button>
            </form>
          </div>
        )}

        <div className="office-section">
          <h2>Registrar movimiento</h2>
          <form className="form compact-form" onSubmit={registerMovement}>
            <label>
              Item
              <select value={selectedItemId} onChange={(event) => setSelectedItemId(event.target.value)} required>
                <option value="">Seleccione</option>
                {items.map((item) => <option key={item.id} value={item.id}>{item.name} ({item.currentStock} {item.unitOfMeasure})</option>)}
              </select>
            </label>
            <label>
              Tipo
              <select value={movementType} onChange={(event) => setMovementType(event.target.value as 'ENTRADA' | 'CONSUMO')}>
                <option value="ENTRADA">Agregar stock</option>
                <option value="CONSUMO">Consumir/entregar</option>
              </select>
            </label>
            <label>Cantidad<input type="number" min="1" step="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} required /></label>
            <label>Observacion<input value={observation} onChange={(event) => setObservation(event.target.value)} placeholder="Opcional" /></label>
            <button className="primary-button" type="submit">Guardar movimiento</button>
          </form>
        </div>
      </section>

      {message && <p className="success-message">{message}</p>}
      {error && <p className="error-message">{error}</p>}

      <section className="split-grid">
        <div className="office-section">
          <h2>Stock oficina</h2>
          <div className="simple-list stock-list">
            {items.map((item) => (
              <article key={item.id}>
                {editingItemId === item.id ? (
                  <div className="edit-panel">
                    <label>Nombre<input value={itemEdit.name} onChange={(event) => setItemEdit({ ...itemEdit, name: event.target.value })} /></label>
                    <label>Categoria<input value={itemEdit.category} onChange={(event) => setItemEdit({ ...itemEdit, category: event.target.value })} /></label>
                    <label>Unidad<input value={itemEdit.unitOfMeasure} onChange={(event) => setItemEdit({ ...itemEdit, unitOfMeasure: event.target.value })} /></label>
                    <label>Stock minimo<input type="number" min="0" step="1" value={itemEdit.minimumStock} onChange={(event) => setItemEdit({ ...itemEdit, minimumStock: parseInt(event.target.value || '0', 10) })} /></label>
                    <div className="row-actions">
                      <button onClick={() => saveItem(item.id)}>Guardar</button>
                      <button onClick={() => setEditingItemId(null)}>Cancelar</button>
                    </div>
                  </div>
                ) : (
                  <>
                    <strong>{item.name}</strong>
                    <span>{item.currentStock} {item.unitOfMeasure}</span>
                    {isAdmin && (
                      <div className="row-actions">
                        <button onClick={() => startItemEdit(item)}>Editar</button>
                        <button onClick={() => toggleItem(item)}>{item.active ? 'Desactivar' : 'Activar'}</button>
                      </div>
                    )}
                  </>
                )}
              </article>
            ))}
            {items.length === 0 && <p className="muted">Aun no hay items.</p>}
          </div>
        </div>

        <div className="office-section">
          <h2>Ultimos movimientos</h2>
          <div className="simple-list">
            {movements.slice(0, 10).map((movement) => (
              <article key={movement.id}>
                <strong>{movement.movementType}</strong>
                <span>{movement.itemName} - {movement.quantity} {movement.unitOfMeasure} - {movement.registeredBy}</span>
              </article>
            ))}
            {movements.length === 0 && <p className="muted">Sin movimientos.</p>}
          </div>
        </div>
      </section>
    </AppShell>
  );
}
