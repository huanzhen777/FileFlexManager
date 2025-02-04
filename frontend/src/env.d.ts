/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly DEV: boolean
    readonly VITE_API_URL: string
    // 更多环境变量...
}

interface ImportMeta {
    readonly env: ImportMetaEnv
} 