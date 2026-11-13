import { HttpErrorResponse } from '@angular/common/http';
export function errorMessage(error: any): string {
  const e = error as HttpErrorResponse;
  return e.status === 0 ? 'No se pudo conectar con Spring Boot (localhost:8080).'
    : (e.error?.error || e.error?.message || e.message || 'Error al procesar la solicitud');
}
export function fmtMoney(value: any): string {
  return new Intl.NumberFormat('es-CL', {style:'currency',currency:'CLP',maximumFractionDigits:0}).format(Number(value)||0);
}
export function fmtDate(v: any): string {
  if (!v) return '—';
  return String(v).substring(0,10).split('-').reverse().join('-');
}
export function daysBetween(a: any, b: any): number {
  const start = new Date(String(a).substring(0,10) + 'T12:00:00');
  const end = new Date(String(b).substring(0,10) + 'T12:00:00');
  return Math.round((end.getTime()-start.getTime()) / 86400000);
}
export function fail(component: any, error: any) { component.error = errorMessage(error); }
