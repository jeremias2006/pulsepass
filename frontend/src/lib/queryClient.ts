/**
 * Configuración única de React Query para toda la aplicación.
 *
 * El QueryClient es la caché central: guarda el resultado de cada consulta bajo su `queryKey`,
 * de modo que dos pantallas que piden lo mismo provocan una sola llamada al backend. Se crea
 * aquí (y no en main.tsx) para poder importarlo también desde otros módulos.
 */
import { QueryClient } from '@tanstack/react-query'
import { ApiError } from './http'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Política de reintentos: NO reintentar los errores 4xx. Un 400, 401, 403, 404 o 409
      // es una respuesta deliberada del backend (dato inválido, sin permisos, no existe,
      // regla de negocio): insistir devolvería exactamente lo mismo y solo gastaría tiempo.
      // En cambio un 5xx o una caída de red (status 0) sí se reintenta una vez.
      retry: (intentos, error) =>
        !(error instanceof ApiError && error.status >= 400 && error.status < 500) && intentos < 1,
      // Por defecto React Query vuelve a pedir los datos al regresar a la pestaña. Se desactiva
      // para no generar tráfico inesperado: el refresco se controla con `staleTime` en cada
      // hook y con `invalidateQueries` después de cada escritura.
      refetchOnWindowFocus: false,
    },
    // Las mutaciones (POST/PATCH) nunca se reintentan: repetirlas podría duplicar una compra
    // o volver a marcar un ticket como usado. Una escritura se dispara una sola vez.
    mutations: { retry: false },
  },
})
