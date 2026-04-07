import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import * as echarts from 'echarts'
import VueECharts from 'vue-echarts'

import App from './App.vue'
import router from './router'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.component('v-chart', VueECharts)
app.config.globalProperties.$echarts = echarts

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
