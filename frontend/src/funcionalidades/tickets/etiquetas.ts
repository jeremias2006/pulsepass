/**
 * Traducción de los tipos de entrada a texto para la interfaz.
 *
 * <p>Los valores del enum viajan en inglés porque así los define el dominio del backend; la
 * traducción es cosa de la presentación y vive aquí, no en los datos.</p>
 *
 * <p>Es un `Record<TicketType, string>` y no un objeto suelto: si mañana el backend añadiera un
 * tipo de entrada, TypeScript marcaría este mapa como incompleto y no habría etiquetas vacías
 * en pantalla.</p>
 */
import type { TicketType } from './tipos'

export const ETIQUETA_TIPO: Record<TicketType, string> = {
  GENERAL: 'General',
  VIP: 'VIP',
  BACKSTAGE: 'Backstage',
  STUDENT: 'Estudiante',
}
