/**
 * Formato de precios y fechas para la interfaz, en convención colombiana (es-CO).
 *
 * <p>Se usa `Intl`, la API estándar del navegador, en lugar de construir los textos a mano: ella
 * resuelve el separador de miles, el símbolo de moneda y los nombres de los meses. Los
 * formateadores se crean UNA vez a nivel de módulo porque instanciarlos es costoso y aquí se
 * invocan en cada tarjeta de la lista.</p>
 *
 * <p>El cuidado principal de este archivo son las zonas horarias: el backend envía fechas sin
 * zona (`LocalDate` y `LocalDateTime` de Java) y Colombia está en UTC-5, así que una conversión
 * descuidada adelanta o atrasa un día. Ver el detalle en `formatearFecha`.</p>
 */
const moneda = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
})
export const formatearPrecio = (valor: number): string => moneda.format(valor)

const fecha = new Intl.DateTimeFormat('es-CO', { day: '2-digit', month: 'short', year: 'numeric' })
const fechaHora = new Intl.DateTimeFormat('es-CO', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

/**
 * `eventDate`/`birthDate` (LocalDate "YYYY-MM-DD"): se arma en hora local para no correr un día.
 *
 * <p>No se puede usar `new Date("2026-11-20")`: el estándar obliga a interpretar ese formato como
 * medianoche UTC, que en Colombia (UTC-5) es el 19 a las 19:00, de modo que la pantalla mostraría
 * un día menos. Por eso se parte el texto y se construye con el constructor de tres argumentos,
 * que sí trabaja en hora local. El mes va con -1 porque en JavaScript enero es 0.</p>
 */
export function formatearFecha(iso: string): string {
  const [a, m, d] = iso.slice(0, 10).split('-').map(Number)
  if (!a || !m || !d) return iso
  return fecha.format(new Date(a, m - 1, d))
}

/** `purchaseDate` (LocalDateTime sin zona, "2026-11-05T14:32:10"): se interpreta como hora local. */
export function formatearFechaHora(iso: string): string {
  const f = new Date(iso)
  return Number.isNaN(f.getTime()) ? iso : fechaHora.format(f)
}

/** Mañana en "YYYY-MM-DD": el backend exige que `eventDate` sea estrictamente posterior a hoy. */
export function mananaISO(): string {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  return d.toLocaleDateString('en-CA')
}

/** Hoy en "YYYY-MM-DD" (hora local). */
export function hoyISO(): string {
  return new Date().toLocaleDateString('en-CA')
}
