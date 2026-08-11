import type { ReactNode } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { clearSession, getStoredUser } from '../auth/authStorage';

type AppShellProps = {
  title: string;
  subtitle?: string;
  children: ReactNode;
};

export function AppShell({ title, subtitle, children }: AppShellProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const user = getStoredUser();
  const showOfficeNav = user?.baseRole === 'ADMIN_OFICINA' || user?.baseRole === 'OFICINA';
  const isAdmin = user?.baseRole === 'ADMIN_OFICINA';

  function logout() {
    clearSession();
    navigate('/login');
  }

  return (
    <main className="app-shell">
      <header className="page-header">
        <div className="page-title-block">
          <h1>{title}</h1>
          {subtitle && <p className="subtitle">{subtitle}</p>}
        </div>
        <div className="brand brand--header" aria-label="Indherco">
          <img src="/brand/indherco-logo-horizontal.png" alt="Indherco" />
        </div>
      </header>
      {showOfficeNav && (
        <nav className="top-nav">
          <Link className={location.pathname === '/oficina' ? 'active' : ''} to="/oficina">Inicio</Link>
          <Link className={location.pathname === '/oficina/historial' ? 'active' : ''} to="/oficina/historial">Historial</Link>
          <Link className={location.pathname === '/oficina/cierres' ? 'active' : ''} to="/oficina/cierres">Cierres</Link>
          <Link className={location.pathname === '/oficina/inventario-oficina' ? 'active' : ''} to="/oficina/inventario-oficina">Inventario oficina</Link>
          {isAdmin && <Link className={location.pathname === '/oficina/auditoria' ? 'active' : ''} to="/oficina/auditoria">Auditoria</Link>}
          {isAdmin && <Link className={location.pathname === '/oficina/catalogo' ? 'active' : ''} to="/oficina/catalogo">Productos e insumos</Link>}
          {isAdmin && <Link className={location.pathname === '/oficina/usuarios' ? 'active' : ''} to="/oficina/usuarios">Usuarios</Link>}
          <button onClick={logout}>Cerrar sesion</button>
        </nav>
      )}
      {children}
    </main>
  );
}
