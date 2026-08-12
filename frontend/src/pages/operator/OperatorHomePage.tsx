import { FormEvent, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiGet, apiPost } from '../../api/httpClient';
import type { Movement, Product, Supply } from '../../api/types';
import { clearSession, getStoredUser, getToken } from '../../auth/authStorage';
import { AppShell } from '../../layouts/AppShell';

export function OperatorHomePage() {
  const navigate = useNavigate();
  const [token] = useState(() => getToken());
  const [user] = useState(() => getStoredUser());
  const [products, setProducts] = useState<Product[]>([]);
  const [supplies, setSupplies] = useState<Supply[]>([]);
  const [movements, setMovements] = useState<Movement[]>([]);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [activeForm, setActiveForm] = useState<'production' | 'dispatch' | 'consumption' | null>(null);
  const [productId, setProductId] = useState('');
  const [supplyId, setSupplyId] = useState('');
  const [quantity, setQuantity] = useState('');
  const [observation, setObservation] = useState('');

  const canUseProducts = user?.canRegisterProduction || user?.canRegisterDispatch;
  const canUseSupplies = user?.canRegisterConsumption;

  useEffect(() => {
    if (!token || !user) {
      navigate('/login');
      return;
    }
    if (canUseProducts) {
      apiGet<Product[]>('/products?activeOnly=true', token).then(setProducts).catch(() => undefined);
    }
    if (canUseSupplies) {
      apiGet<Supply[]>('/supplies?activeOnly=true', token).then(setSupplies).catch(() => undefined);
    }
    apiGet<Movement[]>('/movements/my', token).then(setMovements).catch(() => undefined);
  }, [canUseProducts, canUseSupplies, navigate, token, user]);

  const formTitle = useMemo(() => {
    if (activeForm === 'production') return 'Registrar produccion';
    if (activeForm === 'dispatch') return 'Registrar despacho';
    if (activeForm === 'consumption') return 'Registrar consumo';
    return '';
  }, [activeForm]);

  async function submitMovement(event: FormEvent) {
    event.preventDefault();
    if (!activeForm || !token) return;
    setMessage('');
    setError('');

    const path = activeForm === 'production'
      ? '/movements/production'
      : activeForm === 'dispatch'
        ? '/movements/dispatch'
        : '/movements/consumption';

    const parsedQuantity = parseInt(quantity, 10);
    const body = activeForm === 'consumption'
      ? { supplyId: Number(supplyId), quantity: parsedQuantity, observation }
      : { productId: Number(productId), quantity: parsedQuantity, observation };

    try {
      await apiPost<Movement>(path, body, token);
      setMessage('Registro guardado correctamente.');
      setQuantity('');
      setObservation('');

      try {
        const updatedMovements = await apiGet<Movement[]>('/movements/my', token);
        setMovements(updatedMovements);
      } catch {
        setMessage('Registro guardado correctamente. Actualiza la pagina para ver Mis registros.');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo guardar el registro.');
    }
  }

  function logout() {
    clearSession();
    navigate('/login');
  }

  return (
    <AppShell title="Inicio operador" subtitle={user ? `Hola, ${user.name}` : 'Acciones disponibles'}>
      <section className="action-grid">
        {user?.canRegisterProduction && <button className="action-button operator-action operator-action--production" onClick={() => setActiveForm('production')}><span>+</span> Registrar produccion</button>}
        {user?.canRegisterDispatch && <button className="action-button operator-action operator-action--dispatch" onClick={() => setActiveForm('dispatch')}><span>-</span> Registrar despacho</button>}
        {user?.canRegisterConsumption && <button className="action-button operator-action operator-action--consumption" onClick={() => setActiveForm('consumption')}><span>-</span> Registrar consumo</button>}
      </section>

      {activeForm && (
        <section className={`office-section operator-form operator-form--${activeForm}`}>
          <h2>{formTitle}</h2>
          <form className="form compact-form" onSubmit={submitMovement}>
            {activeForm === 'consumption' ? (
              <label>
                Insumo
                <select value={supplyId} onChange={(event) => setSupplyId(event.target.value)} required>
                  <option value="">Seleccione</option>
                  {supplies.map((supply) => <option key={supply.id} value={supply.id}>{supply.name} ({supply.currentStock} {supply.unitOfMeasure})</option>)}
                </select>
              </label>
            ) : (
              <label>
                Producto
                <select value={productId} onChange={(event) => setProductId(event.target.value)} required>
                  <option value="">Seleccione</option>
                  {products.map((product) => <option key={product.id} value={product.id}>{product.name} ({product.currentStock} {product.unitOfMeasure})</option>)}
                </select>
              </label>
            )}
            <label>
              Cantidad
              <input value={quantity} onChange={(event) => setQuantity(event.target.value)} type="number" min="1" step="1" required />
            </label>
            <label>
              Observacion
              <input value={observation} onChange={(event) => setObservation(event.target.value)} type="text" placeholder="Opcional" />
            </label>
            {message && <p className="success-message">{message}</p>}
            {error && <p className="error-message">{error}</p>}
            <button className="primary-button" type="submit">Guardar</button>
          </form>
        </section>
      )}

      <section className="office-section">
        <h2>Mis registros</h2>
        <div className="simple-list">
          {movements.slice(0, 8).map((movement) => (
            <article key={movement.id}>
              <strong>{movement.movementType}</strong>
              <span>{movement.itemName} - {movement.quantity} {movement.unitOfMeasure}</span>
            </article>
          ))}
          {movements.length === 0 && <p className="muted">Aun no hay registros.</p>}
        </div>
      </section>

      <section className="operator-logout">
        <button type="button" onClick={logout}>Cerrar sesion</button>
      </section>
    </AppShell>
  );
}
