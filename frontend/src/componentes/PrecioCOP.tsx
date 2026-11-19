/**
 * Muestra un precio en pesos colombianos.
 *
 * <p>Parece trivial, pero centraliza una decisión: todos los importes del sitio se escriben
 * igual. El número llega del backend como BigDecimal convertido a número y aquí solo se
 * formatea; el frontend nunca calcula precios.</p>
 */
import { formatearPrecio } from '../lib/formato'

export default function PrecioCOP({ valor }: { valor: number }) {
  return <span>{formatearPrecio(valor)}</span>
}
