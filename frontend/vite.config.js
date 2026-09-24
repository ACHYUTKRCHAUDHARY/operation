import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'node:path'
export default defineConfig({
  plugins:[react()],
  build:{outDir:resolve(__dirname,'../src/main/resources/static/react'),emptyOutDir:true,sourcemap:false},
  server:{proxy:{'/api':'http://localhost:8080','/actuator':'http://localhost:8080'}}
})