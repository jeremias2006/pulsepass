/**
 * Mapa de rutas de la aplicación: qué pantalla corresponde a cada URL y qué acceso exige.
 *
 * <p>Es el equivalente en el frontend de las reglas por URL de `SecurityConfig`: las rutas se
 * anidan dentro de guardias (`SoloAnonimo`, `RutaProtegida`, `RutaPorRol`) que heredan todos
 * sus hijos, igual que allí se agrupan por rol. Conviene mantener los dos en correspondencia.</p>
 *
 * <p><b>Importante:</b> estos guardias son NAVEGACIÓN, no seguridad. Solo redirigen para evitar
 * que alguien llegue a una pantalla que no podría usar. Quien de verdad protege los datos es el
 * backend (`SecurityConfig` + `@PreAuthorize`): forzar la ruta de admin a mano muestra la
 * pantalla, pero todas sus peticiones responden 403.</p>
 */
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

/**
 * Carga perezosa: cada página es un chunk aparte (excepto Layout e Inicio, FE-012).
 *
 * <p>El `import()` dinámico hace que Vite genere un archivo JavaScript por página, que el
 * navegador descarga solo al visitarla. Así la primera carga no arrastra las pantallas de
 * administración ni las de staff, que la mayoría de visitantes nunca abrirá.</p>
 */
const perezosa = (cargar: Cargador) => async () => ({ Component: (await cargar()).default })

// DES-04: columna estrecha (max-w-3xl) para formularios y páginas de gestión.
// Se declara en la ruta y lo lee Layout con useMatches: así el ancho lo decide el enrutado y
// no cada página, que se mantiene ignorante de su presentación.
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
      // --- Públicas (sin sesión). Corresponden a los permitAll de SecurityConfig ---
      { index: true, Component: Inicio, handle: { completo: true } },
      { path: 'eventos', lazy: perezosa(() => import('./pages/CatalogoEventos')) },
      { path: 'eventos/:eventCode', lazy: perezosa(() => import('./pages/DetalleEvento')) },
      { path: 'artistas', lazy: perezosa(() => import('./pages/ListaArtistas')) },
      { path: 'artistas/:id', lazy: perezosa(() => import('./pages/DetalleArtista')) },
      { path: 'sedes', lazy: perezosa(() => import('./pages/ListaSedes')) },
      { path: 'sedes/:code', lazy: perezosa(() => import('./pages/DetalleSede')) },
      // --- Solo para quien NO tiene sesión: con sesión iniciada, SoloAnonimo redirige al inicio ---
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
          // Compra y consulta de tickets propios: espejo de POST /api/tickets (hasRole USER)
          // y de las rutas de tickets, donde @PreAuthorize comprueba además la propiedad.
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
          // Control de acceso el día del evento: PATCH /api/tickets/*/use y
          // GET /api/events/*/tickets/paid, ambos hasAnyRole("STAFF","ADMIN").
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
          // Gestión de eventos: POST /api/events, PATCH /api/events/*/publish y
          // POST /api/events/*/artists/*, todos hasRole("ADMIN").
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
      // --- Páginas de error. "*" captura cualquier URL no declarada arriba y debe ir al final ---
      { path: '403', lazy: perezosa(() => import('./pages/SinPermisos')) },
      { path: '*', lazy: perezosa(() => import('./pages/NoEncontrada')) },
    ],
  },
]

// Un solo enfoque de enrutamiento: createBrowserRouter + RouterProvider (react-router/dom).
export const router = createBrowserRouter(rutas)
