/**
 * Juego de iconos propio, en SVG.
 *
 * <p>Se dibujan a mano en lugar de instalar una librería de iconos: son pocos y así no se añade
 * una dependencia ni peso al paquete final. Todos comparten el componente `Base`, que fija el
 * tamaño, el grosor de trazo y los remates.</p>
 *
 * <p>`stroke="currentColor"` hace que cada icono tome el color del texto que lo rodea, de modo
 * que funciona igual en modo claro y oscuro sin definir colores por icono.</p>
 */
/** SVG de trazo, decorativos (aria-hidden). Añade el resto del set según docs/diseno/Sistema.png. */
function Base(props: ComponentProps<'svg'>) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className="size-5"
      {...props}
    />
  )
}
export const IconoCalendario = (p: ComponentProps<'svg'>) => (
  <Base {...p}>
    <rect x="3" y="5" width="18" height="16" rx="2" />
    <path d="M16 3v4M8 3v4M3 10h18" />
  </Base>
)
export const IconoUbicacion = (p: ComponentProps<'svg'>) => (
  <Base {...p}>
    <path d="M12 21s7-6.2 7-11a7 7 0 1 0-14 0c0 4.8 7 11 7 11Z" />
    <circle cx="12" cy="10" r="2.5" />
  </Base>
)
export const IconoMusica = (p: ComponentProps<'svg'>) => (
  <Base {...p}>
    <path d="M9 18V5l11-2v13" />
    <circle cx="6" cy="18" r="3" />
    <circle cx="17" cy="16" r="3" />
  </Base>
)
export const IconoMonitor = (p: ComponentProps<'svg'>) => (
  <Base {...p}>
    <rect x="3" y="4" width="18" height="12" rx="2" />
    <path d="M8 20h8M12 16v4" />
  </Base>
)
export const IconoBalon = (p: ComponentProps<'svg'>) => (
  <Base {...p}>
    <circle cx="12" cy="12" r="9" />
    <path d="M12 3v18M3 12h18" />
  </Base>
)
export const IconoTemplo = (p: ComponentProps<'svg'>) => (
  <Base {...p}>
    <path d="M3 9 12 3l9 6M5 9v9M9.5 9v9M14.5 9v9M19 9v9M3 21h18" />
  </Base>
)
export const IconoBirrete = (p: ComponentProps<'svg'>) => (
  <Base {...p}>
    <path d="m2 9 10-5 10 5-10 5L2 9Z" />
    <path d="M6 11.5V16c0 1.5 2.7 3 6 3s6-1.5 6-3v-4.5" />
  </Base>
)
export const IconoEstrella = (p: ComponentProps<'svg'>) => (
  <Base {...p}>
    <path d="m12 3 2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9L12 3Z" />
  </Base>
)
export const IconoAdvertencia = (p: ComponentProps<'svg'>) => (
  <Base {...p}>
    <path d="M12 3 2 21h20L12 3Z" />
    <path d="M12 10v5M12 18h.01" />
  </Base>
)
export const IconoCheck = (p: ComponentProps<'svg'>) => (
  <Base {...p}>
    <circle cx="12" cy="12" r="9" />
    <path d="m8 12 3 3 5-6" />
  </Base>
)
export const IconoMenu = (p: ComponentProps<'svg'>) => (
  <Base {...p}>
    <path d="M4 6h16M4 12h16M4 18h16" />
  </Base>
)
export const IconoCerrar = (p: ComponentProps<'svg'>) => (
  <Base {...p}>
    <path d="M6 6l12 12M18 6 6 18" />
  </Base>
)
