import type { ReactNode } from 'react';

type ActionButtonProps = {
  children: ReactNode;
  variant?: 'primary' | 'secondary';
};

export function ActionButton({ children, variant = 'secondary' }: ActionButtonProps) {
  return <button className={`action-button action-button--${variant}`}>{children}</button>;
}
