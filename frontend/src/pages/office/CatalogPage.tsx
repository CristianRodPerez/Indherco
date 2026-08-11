import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiGet, apiPatch, apiPost, apiPut } from '../../api/httpClient';
import type { Product, Supply } from '../../api/types';
import { getStoredUser, getToken } from '../../auth/authStorage';
import { AppShell } from '../../layouts/AppShell';

const emptyProduct = {
  name: '',
  type: '',
  unitOfMeasure: 'unidad',
  currentStock: 0,
  minimumStock: 0
};

const emptySupply = {
  name: '',
  category: '',
  unitOfMeasure: 'unidad',
  currentStock: 0,
  minimumStock: 0
};

export function CatalogPage() {
  const navigate = useNavigate();
  const [token] = useState(() => getToken());
  const [currentUser] = useState(() => getStoredUser());
  const [products, setProducts] = useState<Product[]>([]);
  const [supplies, setSupplies] = useState<Supply[]>([]);
  const [productForm, setProductForm] = useState(emptyProduct);
  const [supplyForm, setSupplyForm] = useState(emptySupply);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [editingProductId, setEditingProductId] = useState<number | null>(null);
  const [editingSupplyId, setEditingSupplyId] = useState<number | null>(null);
  const [productEdit, setProductEdit] = useState(emptyProduct);
  const [supplyEdit, setSupplyEdit] = useState(emptySupply);

  useEffect(() => {
    if (!token || !currentUser) {
      navigate('/login');
      return;
    }
    if (currentUser.baseRole !== 'ADMIN_OFICINA') {
      navigate('/operador');
      return;
    }
    loadCatalog();
  }, [currentUser, navigate, token]);

  async function loadCatalog() {
    if (!token) return;
    try {
      const [loadedProducts, loadedSupplies] = await Promise.all([
        apiGet<Product[]>('/products', token),
        apiGet<Supply[]>('/supplies', token)
      ]);
      setProducts(loadedProducts);
      setSupplies(loadedSupplies);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cargar el catalogo.');
    }
  }

  async function createProduct(event: FormEvent) {
    event.preventDefault();
    if (!token) return;
    setMessage('');
    setError('');

    try {
      await apiPost<Product>('/products', productForm, token);
      setProductForm(emptyProduct);
      setMessage('Producto creado correctamente.');
      await loadCatalog();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo crear el producto.');
    }
  }

  async function createSupply(event: FormEvent) {
    event.preventDefault();
    if (!token) return;
    setMessage('');
    setError('');

    try {
      await apiPost<Supply>('/supplies', supplyForm, token);
      setSupplyForm(emptySupply);
      setMessage('Insumo creado correctamente.');
      await loadCatalog();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo crear el insumo.');
    }
  }

  function startProductEdit(product: Product) {
    setEditingProductId(product.id);
    setProductEdit({
      name: product.name,
      type: product.type ?? '',
      unitOfMeasure: product.unitOfMeasure,
      currentStock: product.currentStock,
      minimumStock: product.minimumStock ?? 0
    });
  }

  function startSupplyEdit(supply: Supply) {
    setEditingSupplyId(supply.id);
    setSupplyEdit({
      name: supply.name,
      category: supply.category ?? '',
      unitOfMeasure: supply.unitOfMeasure,
      currentStock: supply.currentStock,
      minimumStock: supply.minimumStock ?? 0
    });
  }

  async function saveProduct(productId: number) {
    if (!token) return;
    setMessage('');
    setError('');
    try {
      await apiPut<Product>(`/products/${productId}`, productEdit, token);
      setEditingProductId(null);
      setMessage('Producto actualizado correctamente.');
      await loadCatalog();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo actualizar el producto.');
    }
  }

  async function saveSupply(supplyId: number) {
    if (!token) return;
    setMessage('');
    setError('');
    try {
      await apiPut<Supply>(`/supplies/${supplyId}`, supplyEdit, token);
      setEditingSupplyId(null);
      setMessage('Insumo actualizado correctamente.');
      await loadCatalog();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo actualizar el insumo.');
    }
  }

  async function toggleProduct(product: Product) {
    if (!token) return;
    await apiPatch<Product>(`/products/${product.id}/status`, { active: !product.active }, token);
    await loadCatalog();
  }

  async function toggleSupply(supply: Supply) {
    if (!token) return;
    await apiPatch<Supply>(`/supplies/${supply.id}/status`, { active: !supply.active }, token);
    await loadCatalog();
  }

  return (
    <AppShell title="Productos e insumos" subtitle="Carga los postes e insumos antes de registrar movimientos.">
      {(message || error) && (
        <section className="feedback-strip">
          {message && <p className="success-message">{message}</p>}
          {error && <p className="error-message">{error}</p>}
        </section>
      )}

      <section className="split-grid">
        <div className="office-section">
          <h2>Crear producto</h2>
          <form className="form compact-form" onSubmit={createProduct}>
            <label>
              Nombre
              <input value={productForm.name} placeholder="Poste 8 metros" onChange={(event) => setProductForm({ ...productForm, name: event.target.value })} required />
            </label>
            <label>
              Tipo
              <input value={productForm.type} placeholder="Poste" onChange={(event) => setProductForm({ ...productForm, type: event.target.value })} />
            </label>
            <label>
              Unidad
              <input value={productForm.unitOfMeasure} onChange={(event) => setProductForm({ ...productForm, unitOfMeasure: event.target.value })} required />
            </label>
            <label>
              Stock inicial
              <input value={productForm.currentStock} type="number" min="0" step="1" onChange={(event) => setProductForm({ ...productForm, currentStock: parseInt(event.target.value || '0', 10) })} />
            </label>
            <label>
              Stock minimo
              <input value={productForm.minimumStock} type="number" min="0" step="1" onChange={(event) => setProductForm({ ...productForm, minimumStock: parseInt(event.target.value || '0', 10) })} />
            </label>
            <button className="primary-button" type="submit">Crear producto</button>
          </form>
        </div>

        <div className="office-section">
          <h2>Crear insumo</h2>
          <form className="form compact-form" onSubmit={createSupply}>
            <label>
              Nombre
              <input value={supplyForm.name} placeholder="Rollo de alambre" onChange={(event) => setSupplyForm({ ...supplyForm, name: event.target.value })} required />
            </label>
            <label>
              Categoria
              <input value={supplyForm.category} placeholder="Alambre" onChange={(event) => setSupplyForm({ ...supplyForm, category: event.target.value })} />
            </label>
            <label>
              Unidad
              <input value={supplyForm.unitOfMeasure} onChange={(event) => setSupplyForm({ ...supplyForm, unitOfMeasure: event.target.value })} required />
            </label>
            <label>
              Stock inicial
              <input value={supplyForm.currentStock} type="number" min="0" step="1" onChange={(event) => setSupplyForm({ ...supplyForm, currentStock: parseInt(event.target.value || '0', 10) })} />
            </label>
            <label>
              Stock minimo
              <input value={supplyForm.minimumStock} type="number" min="0" step="1" onChange={(event) => setSupplyForm({ ...supplyForm, minimumStock: parseInt(event.target.value || '0', 10) })} />
            </label>
            <button className="primary-button" type="submit">Crear insumo</button>
          </form>
        </div>
      </section>

      <section className="split-grid">
        <div className="office-section">
          <h2>Productos creados</h2>
          <div className="simple-list">
            {products.map((product) => (
              <article key={product.id}>
                {editingProductId === product.id ? (
                  <div className="edit-panel">
                    <label>Nombre<input value={productEdit.name} onChange={(event) => setProductEdit({ ...productEdit, name: event.target.value })} /></label>
                    <label>Tipo<input value={productEdit.type} onChange={(event) => setProductEdit({ ...productEdit, type: event.target.value })} /></label>
                    <label>Unidad<input value={productEdit.unitOfMeasure} onChange={(event) => setProductEdit({ ...productEdit, unitOfMeasure: event.target.value })} /></label>
                    <label>Stock minimo<input type="number" min="0" step="1" value={productEdit.minimumStock} onChange={(event) => setProductEdit({ ...productEdit, minimumStock: parseInt(event.target.value || '0', 10) })} /></label>
                    <div className="row-actions">
                      <button onClick={() => saveProduct(product.id)}>Guardar</button>
                      <button onClick={() => setEditingProductId(null)}>Cancelar</button>
                    </div>
                  </div>
                ) : (
                  <>
                    <strong>{product.name}</strong>
                    <span>{product.currentStock} {product.unitOfMeasure} - {product.active ? 'Activo' : 'Inactivo'}</span>
                    <div className="row-actions">
                      <button onClick={() => startProductEdit(product)}>Editar</button>
                      <button onClick={() => toggleProduct(product)}>{product.active ? 'Desactivar' : 'Activar'}</button>
                    </div>
                  </>
                )}
              </article>
            ))}
            {products.length === 0 && <p className="muted">Aun no hay productos.</p>}
          </div>
        </div>

        <div className="office-section">
          <h2>Insumos creados</h2>
          <div className="simple-list">
            {supplies.map((supply) => (
              <article key={supply.id}>
                {editingSupplyId === supply.id ? (
                  <div className="edit-panel">
                    <label>Nombre<input value={supplyEdit.name} onChange={(event) => setSupplyEdit({ ...supplyEdit, name: event.target.value })} /></label>
                    <label>Categoria<input value={supplyEdit.category} onChange={(event) => setSupplyEdit({ ...supplyEdit, category: event.target.value })} /></label>
                    <label>Unidad<input value={supplyEdit.unitOfMeasure} onChange={(event) => setSupplyEdit({ ...supplyEdit, unitOfMeasure: event.target.value })} /></label>
                    <label>Stock minimo<input type="number" min="0" step="1" value={supplyEdit.minimumStock} onChange={(event) => setSupplyEdit({ ...supplyEdit, minimumStock: parseInt(event.target.value || '0', 10) })} /></label>
                    <div className="row-actions">
                      <button onClick={() => saveSupply(supply.id)}>Guardar</button>
                      <button onClick={() => setEditingSupplyId(null)}>Cancelar</button>
                    </div>
                  </div>
                ) : (
                  <>
                    <strong>{supply.name}</strong>
                    <span>{supply.currentStock} {supply.unitOfMeasure} - {supply.active ? 'Activo' : 'Inactivo'}</span>
                    <div className="row-actions">
                      <button onClick={() => startSupplyEdit(supply)}>Editar</button>
                      <button onClick={() => toggleSupply(supply)}>{supply.active ? 'Desactivar' : 'Activar'}</button>
                    </div>
                  </>
                )}
              </article>
            ))}
            {supplies.length === 0 && <p className="muted">Aun no hay insumos.</p>}
          </div>
        </div>
      </section>
    </AppShell>
  );
}
