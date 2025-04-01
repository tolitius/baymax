import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';

export default defineConfig({
  plugins: [svelte()],
  build: {
    outDir: '../../resources/public/js',
    emptyOutDir: true,
    rollupOptions: {
      input: './main.ts',
      output: {
        entryFileNames: 'bundle.js',
        chunkFileNames: '[name].js',
        assetFileNames: '[name].[ext]'
      }
    }
  }
});
