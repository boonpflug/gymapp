/// <reference types="vite/client" />

declare module 'sockjs-client/dist/sockjs';

interface ImportMetaEnv {
  readonly VITE_API_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
