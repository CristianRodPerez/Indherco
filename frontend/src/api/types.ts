export type Product = {
  id: number;
  name: string;
  type?: string;
  unitOfMeasure: string;
  currentStock: number;
  minimumStock?: number;
  active: boolean;
};

export type Supply = {
  id: number;
  name: string;
  category?: string;
  unitOfMeasure: string;
  currentStock: number;
  minimumStock?: number;
  active: boolean;
};

export type Movement = {
  id: number;
  movementType: 'PRODUCCION' | 'DESPACHO' | 'CONSUMO' | 'ENTRADA_INSUMO' | 'ANULACION';
  entityType: 'PRODUCTO' | 'INSUMO';
  itemName: string;
  quantity: number;
  unitOfMeasure: string;
  previousStock: number;
  newStock: number;
  observation?: string;
  registeredBy: string;
  movementDate: string;
  registeredAt: string;
  status: 'ACTIVO' | 'ANULADO';
};

export type Alert = {
  id: number;
  type: string;
  message: string;
  level: 'INFO' | 'ADVERTENCIA' | 'CRITICA';
  active: boolean;
  createdAt: string;
};

export type Dashboard = {
  productionToday: number;
  productionMonth: number;
  dispatchToday: number;
  dispatchMonth: number;
  consumptionToday: number;
  consumptionMonth: number;
  productsStock: Product[];
  suppliesStock: Supply[];
  activeAlerts: Alert[];
  latestMovements: Movement[];
};

export type BaseRole = 'ADMIN_OFICINA' | 'OFICINA' | 'OPERADOR';

export type User = {
  id: number;
  name: string;
  username: string;
  active: boolean;
  baseRole: BaseRole;
  canRegisterProduction: boolean;
  canRegisterDispatch: boolean;
  canRegisterConsumption: boolean;
};

export type OfficeInventoryItem = {
  id: number;
  name: string;
  category?: string;
  unitOfMeasure: string;
  currentStock: number;
  minimumStock?: number;
  active: boolean;
};

export type OfficeInventoryMovement = {
  id: number;
  movementType: 'ENTRADA' | 'CONSUMO';
  itemId: number;
  itemName: string;
  quantity: number;
  unitOfMeasure: string;
  previousStock: number;
  newStock: number;
  observation?: string;
  registeredBy: string;
  movementDate: string;
  registeredAt: string;
};

export type DailyClosing = {
  id: number;
  closingDate: string;
  status: 'CERRADO' | 'REABIERTO';
  closedBy: string;
  reopenedBy?: string;
  totalProduction: number;
  totalDispatch: number;
  totalConsumption: number;
  observation?: string;
  closedAt: string;
  reopenedAt?: string;
  reopenReason?: string;
};

export type AuditLog = {
  id: number;
  userId?: number;
  username?: string;
  module: string;
  action: string;
  entity?: string;
  entityId?: number;
  occurredAt: string;
  ip?: string;
  userAgent?: string;
  correlationId?: string;
  previousDetail?: string;
  newDetail?: string;
  reason?: string;
};
