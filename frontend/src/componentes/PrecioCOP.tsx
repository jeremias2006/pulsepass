import { formatearPrecio } from '../lib/formato'

export default function PrecioCOP({ valor }: { valor: number }) {
  return <span>{formatearPrecio(valor)}</span>
}
