/**
 * Tabla genérica y adaptable. La usan la lista de tickets pagados de un evento y la de
 * validación de ingreso.
 *
 * <p>Es genérica en `T`: no sabe qué contienen las filas. Quien la usa le pasa las columnas, y
 * cada columna dice cómo pintar su celda a partir de una fila. Por eso vive en `componentes/` y
 * no en una funcionalidad.</p>
 *
 * <p>`clave` devuelve el identificador único de cada fila, que React necesita para saber qué
 * cambió entre dos pintados (aquí se pasa el código del ticket, nunca el índice del arreglo).</p>
 */
import type { ReactNode } from 'react'

export interface Columna<T> {
  encabezado: string
  celda: (fila: T) => ReactNode
}

interface TablaProps<T> {
  columnas: Columna<T>[]
  filas: T[]
  clave: (fila: T) => string | number
}

/**
 * md+: tabla normal. Móvil: cada fila se vuelve una tarjeta "Encabezado: valor" (PRD §9.15).
 *
 * <p>En una pantalla estrecha una tabla de varias columnas obliga a desplazarse en horizontal.
 * La solución es cambiar la presentación sin cambiar el marcado: en móvil los `display` pasan a
 * `block`, los encabezados se ocultan visualmente (pero siguen ahí para los lectores de
 * pantalla, con `sr-only`) y cada celda muestra su propio título mediante
 * `before:content-[attr(data-etiqueta)]`, que toma el texto del atributo `data-etiqueta`.</p>
 *
 * <p>Así el HTML sigue siendo una tabla de verdad, con `<th scope="col">`, que es lo que
 * necesitan los lectores de pantalla para anunciar bien los datos.</p>
 */
export default function Tabla<T>({ columnas, filas, clave }: TablaProps<T>) {
  return (
    <table className="block w-full text-left text-sm md:table">
      <thead className="sr-only md:not-sr-only md:table-header-group">
        <tr className="md:table-row">
          {columnas.map((c) => (
            <th
              key={c.encabezado}
              scope="col"
              className="px-3 py-2 font-semibold text-slate-600 dark:text-slate-400"
            >
              {c.encabezado}
            </th>
          ))}
        </tr>
      </thead>
      <tbody className="block md:table-row-group">
        {filas.map((fila) => (
          <tr
            key={clave(fila)}
            className="mb-3 block rounded-xl bg-white p-3 shadow-sm md:mb-0 md:table-row md:rounded-none md:bg-transparent md:p-0 md:shadow-none dark:bg-slate-900"
          >
            {columnas.map((c) => (
              <td
                key={c.encabezado}
                data-etiqueta={c.encabezado}
                className="flex justify-between gap-4 px-3 py-1.5 before:font-medium before:text-slate-500 before:content-[attr(data-etiqueta)] md:table-cell md:border-b md:border-slate-200 md:py-3 md:before:content-none dark:before:text-slate-400 md:dark:border-slate-800"
              >
                {c.celda(fila)}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
