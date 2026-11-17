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

/** md+: tabla normal. Móvil: cada fila se vuelve una tarjeta "Encabezado: valor" (PRD §9.15). */
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
