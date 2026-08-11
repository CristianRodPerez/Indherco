import { Navigate, Route, Routes } from 'react-router-dom';
import { LoginPage } from '../pages/login/LoginPage';
import { OperatorHomePage } from '../pages/operator/OperatorHomePage';
import { OfficeDashboardPage } from '../pages/office/OfficeDashboardPage';
import { UsersPage } from '../pages/office/UsersPage';
import { CatalogPage } from '../pages/office/CatalogPage';
import { MovementHistoryPage } from '../pages/office/MovementHistoryPage';
import { OfficeInventoryPage } from '../pages/office/OfficeInventoryPage';
import { AuditPage } from '../pages/office/AuditPage';
import { DailyClosingsPage } from '../pages/office/DailyClosingsPage';

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/operador" element={<OperatorHomePage />} />
      <Route path="/oficina" element={<OfficeDashboardPage />} />
      <Route path="/oficina/usuarios" element={<UsersPage />} />
      <Route path="/oficina/catalogo" element={<CatalogPage />} />
      <Route path="/oficina/historial" element={<MovementHistoryPage />} />
      <Route path="/oficina/inventario-oficina" element={<OfficeInventoryPage />} />
      <Route path="/oficina/cierres" element={<DailyClosingsPage />} />
      <Route path="/oficina/auditoria" element={<AuditPage />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
