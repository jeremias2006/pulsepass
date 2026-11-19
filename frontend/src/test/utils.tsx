import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter, type RouteObject } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { AuthProvider } from '../funcionalidades/auth/AuthContext'

/** Monta un árbol de rutas con Router + Query + Auth (PRD §17: renderConProveedores). */
export function renderConProveedores(rutas: RouteObject[], inicial = '/') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  const router = createMemoryRouter(rutas, { initialEntries: [inicial] })
  const vista = render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>,
  )
  return { router, queryClient, ...vista }
}
