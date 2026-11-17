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

/** `eventDate`/`birthDate` (LocalDate "YYYY-MM-DD"): se arma en hora local para no correr un día. */
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
