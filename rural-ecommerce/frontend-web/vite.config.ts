import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import os from 'os'

// 获取本机 IP 地址
function getLocalIp() {
  const interfaces = os.networkInterfaces()
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name] || []) {
      if (iface.family === 'IPv4' && !iface.internal) {
        return iface.address
      }
    }
  }
  return 'localhost'
}

export default defineConfig({
  plugins: [
    vue(),
    {
      name: 'show-ip',
      configureServer(server) {
        server.httpServer?.once('listening', () => {
          const port = server.config.server.port
          const ip = getLocalIp()
          console.log(`\n  ➜  Local:   http://localhost:${port}/`)
          console.log(`  ➜  Network: http://${ip}:${port}/\n`)
        })
      }
    }
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 3000,
    host: '0.0.0.0',
    allowedHosts: ['.cpolar.cn', '.vip.cpolar.cn', '.trycloudflare.com',],
    proxy: {
      '/api': {
        target: 'https://xiangcundianshangwuliuguanlixitong-production.up.railway.app',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      '/uploads': {
        target: 'https://xiangcundianshangwuliuguanlixitong-production.up.railway.app',
        changeOrigin: true,
      },
    },
  },
})
