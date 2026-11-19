/**
 * DES-05: los códigos (CMF-2026, TKT-8F2A1C) van en fuente monoespaciada.
 *
 * <p>En una fuente normal es fácil confundir el 0 con la O, o el 1 con la l. Como estos códigos
 * se leen en voz alta y se teclean en la puerta del evento, la fuente monoespaciada reduce los
 * errores de transcripción.</p>
 */
export default function CodigoTexto({ children }: { children: string }) {
  return <span className="font-mono">{children}</span>
}
