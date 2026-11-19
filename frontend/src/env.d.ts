/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Base de la API. En desarrollo: "/api" (proxy de Vite). Es público por definición: no pongas secretos aquí. */
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
