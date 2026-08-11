export type BaseRole = 'ADMIN_OFICINA' | 'OFICINA' | 'OPERADOR';

export type CurrentUser = {
  id: number;
  name: string;
  username: string;
  baseRole: BaseRole;
  canRegisterProduction: boolean;
  canRegisterDispatch: boolean;
  canRegisterConsumption: boolean;
};

export type AuthResponse = {
  token: string;
  user: CurrentUser;
};
