import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import Vant from 'vant'
import 'vant/lib/index.css'
import '@vant/touch-emulator'

const app = createApp(App)
app.use(router)
app.use(Vant)
app.use(createPinia())
app.mount('#app') 