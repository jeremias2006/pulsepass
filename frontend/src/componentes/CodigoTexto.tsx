/** DES-05: los códigos (CMF-2026, TKT-8F2A1C) van en fuente monoespaciada. */
export default function CodigoTexto({ children }: { children: string }) {
  return <span className="font-mono">{children}</span>
}
