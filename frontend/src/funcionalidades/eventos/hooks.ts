/**
 * Hooks de eventos: lo que consumen las pantallas. Envuelven las funciones de `api.ts` con
 * React Query, que aporta caché, estados de carga y error, y refresco automático.
 *
 * <p>Dos tipos de hook, y la diferencia es importante:</p>
 * <ul>
 *   <li><b>useQuery</b> para LEER: se dispara solo al montar la pantalla y guarda el resultado
 *       en la caché bajo su `queryKey`. Dos pantallas que pidan lo mismo provocan una sola
 *       llamada al backend.</li>
 *   <li><b>useMutation</b> para ESCRIBIR: no se ejecuta sola, la dispara el usuario, y al
 *       terminar invalida las consultas que quedaron desactualizadas.</li>
 * </ul>
 *
 * <p>El `staleTime` es cuánto tiempo se considera fresco un dato: durante ese lapso, volver a la
 * pantalla no genera ninguna petición. Aquí los valores van de 30 a 60 segundos porque el
 * catálogo cambia poco; comparar con los 15 segundos de los tickets (que cambian al comprar o
 * cancelar) y los 5 minutos de sedes y artistas (prácticamente estáticos).</p>
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  asociarArtista,
  crearEvento,
  listarEventosPorArtista,
  listarEventosPublicados,
  obtenerEvento,
  publicarEvento,
} from './api'

export const useEventosPublicados = () =>
  useQuery({
    // La queryKey identifica el dato en la caché; también es lo que se invalida tras publicar.
    queryKey: ['eventos', 'publicados'],
    queryFn: listarEventosPublicados,
    staleTime: 60_000,
  })

export const useEventosPorArtista = (stageName: string | null) =>
  useQuery({
    queryKey: ['eventos', 'artista', stageName],
    queryFn: () => listarEventosPorArtista(stageName ?? ''),
    // `enabled: false` deja el hook en espera sin llamar al backend. Hace falta porque los hooks
    // de React no se pueden invocar condicionalmente: el hook siempre se declara, y es este
    // interruptor el que decide si la consulta llega a ejecutarse.
    enabled: Boolean(stageName),
    staleTime: 60_000,
  })

export const useEvento = (eventCode: string | undefined) =>
  useQuery({
    queryKey: ['evento', eventCode],
    queryFn: () => obtenerEvento(eventCode ?? ''),
    enabled: Boolean(eventCode),
    staleTime: 30_000,
  })

/**
 * Crear un evento (ADMIN). El evento nace en DRAFT, así que NO se invalida el catálogo público:
 * un borrador no aparece ahí todavía.
 */
export function useCrearEvento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: crearEvento,
    // setQueryData guarda en la caché el evento que acaba de devolver el backend, de modo que al
    // navegar a su pantalla de gestión los datos ya están y no hace falta volver a pedirlos.
    onSuccess: (evento) => qc.setQueryData(['evento', evento.eventCode], evento), // siembra la caché
  })
}

/**
 * Publicar un evento (ADMIN): DRAFT → PUBLISHED. Aquí sí hay que invalidar el catálogo, porque
 * el evento acaba de volverse visible para el público.
 */
export function usePublicarEvento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: publicarEvento,
    onSuccess: async (evento) => {
      qc.setQueryData(['evento', evento.eventCode], evento)
      // invalidateQueries marca el dato como obsoleto: cualquier pantalla que lo esté mostrando
      // lo vuelve a pedir sola. Es lo que hace que el catálogo se actualice sin recargar.
      await qc.invalidateQueries({ queryKey: ['eventos', 'publicados'] })
    },
  })
}

/**
 * Asociar un artista a un evento (ADMIN). Se invalida la lista "eventos por artista" porque ese
 * artista pasa a tener un evento más; no hace falta precisar cuál, basta el prefijo de la clave.
 */
export function useAsociarArtista(eventCode: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (artistId: number) => asociarArtista(eventCode, artistId),
    onSuccess: async (evento) => {
      qc.setQueryData(['evento', eventCode], evento)
      // Invalidación por prefijo: ['eventos','artista'] alcanza a todas las claves que empiezan
      // así, sea cual sea el nombre artístico que llevan al final.
      await qc.invalidateQueries({ queryKey: ['eventos', 'artista'] })
    },
  })
}
