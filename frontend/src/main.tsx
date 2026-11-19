/**
 * Punto de entrada de la aplicación: es el primer módulo que ejecuta el navegador.
 *
 * Engancha React al <div id="root"> de index.html y monta los tres proveedores que toda la
 * aplicación necesita. El ORDEN de anidamiento importa:
 *
 *   QueryClientProvider  → la caché de datos (React Query); debe envolver a Auth porque
 *                          AuthProvider la limpia con queryClient.clear() al cerrar sesión.
 *   AuthProvider         → la sesión (token, usuario y roles); debe envolver al router porque
 *                          los guardias de ruta (RutaProtegida, RutaPorRol) leen la sesión.
 *   RouterProvider       → las rutas; dentro de él vive toda la interfaz.
 */
import { QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router/dom'
import './index.css'
import { AuthProvider } from './funcionalidades/auth/AuthContext'
import { queryClient } from './lib/queryClient'
import { router } from './router'

// Si falta el contenedor del HTML, se falla de inmediato en vez de montar sobre null
// (mismo criterio de "fallar rápido" que el backend aplica al validar la clave del JWT).
const raiz = document.getElementById('root')
if (!raiz) throw new Error('No se encontró el elemento #root')

createRoot(raiz).render(
  // StrictMode solo actúa en desarrollo: monta cada componente dos veces para delatar
  // efectos mal escritos. No se ejecuta en la compilación de producción.
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>
  </StrictMode>,
)
