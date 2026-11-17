import type { ComponentType } from 'react'
import { createBrowserRouter, type RouteObject } from 'react-router'
import Layout from './layouts/Layout'
import RutaPorRol from './layouts/RutaPorRol'
import RutaProtegida from './layouts/RutaProtegida'
import SoloAnonimo from './layouts/SoloAnonimo'
import Cargando from './componentes/Cargando'
import ErrorPagina from './pages/ErrorPagina'
import Inicio from './pages/Inicio'

type Cargador = () => Promise<{ default: ComponentType }>

/** Carga perezosa: cada página es un chunk aparte (excepto Layout e Inicio, FE-012). */
const perezosa = (cargar: Cargador) => async () => ({ Component: (await cargar()).default })

// DES-04: columna estrecha (max-w-3xl) para formularios y páginas de gestión.
const estrecho = { estrecho: true }

/** Se exporta el árbol (no solo el router) para montarlo con createMemoryRouter en las pruebas. */
export const rutas: RouteObject[] = [
  {
    path: '/',
    Component: Layout,
    ErrorBoundary: ErrorPagina,
    // Evita el aviso "No HydrateFallback element" de React Router en la primera carga.
    HydrateFallback: () => <Cargando />,
    children: [
      { index: true, Component: Inicio, handle: { completo: true } },
      { path: 'eventos', lazy: perezosa(() => import('./pages/CatalogoEventos')) },
      { path: 'eventos/:eventCode', lazy: perezosa(() => import('./pages/DetalleEvento')) },
      { path: 'artistas', lazy: perezosa(() => import('./pages/ListaArtistas')) },
      { path: 'artistas/:id', lazy: perezosa(() => import('./pages/DetalleArtista')) },
      { path: 'sedes', lazy: perezosa(() => import('./pages/ListaSedes')) },
      { path: 'sedes/:code', lazy: perezosa(() => import('./pages/DetalleSede')) },
      {
        Component: SoloAnonimo,
        children: [
          { path: 'login', handle: estrecho, lazy: perezosa(() => import('./pages/Login')) },
          { path: 'registro', handle: estrecho, lazy: perezosa(() => import('./pages/Registro')) },
        ],
      },
      {
        Component: RutaProtegida, // requiere sesión
        children: [
          { path: 'cuenta', handle: estrecho, lazy: perezosa(() => import('./pages/Cuenta')) },
          {
            element: <RutaPorRol roles={['USER']} />,
            children: [
              { path: 'mis-tickets', lazy: perezosa(() => import('./pages/MisTickets')) },
              {
                path: 'mis-tickets/:ticketCode',
                handle: estrecho,
                lazy: perezosa(() => import('./pages/DetalleTicket')),
              },
              {
                path: 'compra/confirmacion/:ticketCode',
                handle: estrecho,
                lazy: perezosa(() => import('./pages/ConfirmacionCompra')),
              },
            ],
          },
          {
            element: <RutaPorRol roles={['STAFF', 'ADMIN']} />,
            children: [
              {
                path: 'staff/validacion',
                handle: estrecho,
                lazy: perezosa(() => import('./pages/ValidacionIngreso')),
              },
              {
                path: 'staff/eventos/:eventCode/tickets',
                lazy: perezosa(() => import('./pages/TicketsPagadosEvento')),
              },
            ],
          },
          {
            element: <RutaPorRol roles={['ADMIN']} />,
            children: [
              {
                path: 'admin/eventos',
                handle: estrecho,
                lazy: perezosa(() => import('./pages/AdminEventos')),
              },
              {
                path: 'admin/eventos/nuevo',
                handle: estrecho,
                lazy: perezosa(() => import('./pages/AdminCrearEvento')),
              },
              {
                path: 'admin/eventos/:eventCode',
                handle: estrecho,
                lazy: perezosa(() => import('./pages/AdminGestionEvento')),
              },
            ],
          },
        ],
      },
      { path: '403', lazy: perezosa(() => import('./pages/SinPermisos')) },
      { path: '*', lazy: perezosa(() => import('./pages/NoEncontrada')) },
    ],
  },
]

// Un solo enfoque de enrutamiento: createBrowserRouter + RouterProvider (react-router/dom).
export const router = createBrowserRouter(rutas)
