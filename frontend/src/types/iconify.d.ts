declare module '@iconify/vue' {
    import type {DefineComponent} from 'vue'

    export const Icon: DefineComponent<{
        icon: string
        width?: string | number
        height?: string | number
        color?: string
        inline?: boolean
        hFlip?: boolean
        vFlip?: boolean
        flip?: string
        rotate?: number | string
        style?: Record<string, string>
    }>
} 