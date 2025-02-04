declare module 'vant' {
  import type { App, Plugin } from 'vue'

  export const showToast: (options: string | {
    type?: 'success' | 'fail' | 'loading';
    message?: string;
    duration?: number;
  }) => void

  export const showDialog: (options: {
    title?: string;
    message: string;
    showCancelButton?: boolean;
    confirmButtonText?: string;
    cancelButtonText?: string;
  }) => Promise<boolean>

  export interface ActionSheetAction {
    name: string;
    value: string;
    color?: string;
    disabled?: boolean;
  }

  const _default: {
    install: (app: App) => void;
  } & Plugin;

  export default _default;
} 