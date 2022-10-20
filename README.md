# Grails Adapter for Inertia.js

Grails plugin for using [Inertia.js](https://inertiajs.com/) to build single-page apps without building an API.

## What is Inertia.js?

Inertia.js lets you, in its own words, *“quickly build modern single-page React, Vue and Svelte apps using classic server-side routing and controllers”.*

Using Inertia.js allows using your favorite MVC server-side framework (Grails obviously) with your favorite client-side SPA framework - no need to build a separate API.

## Installation
If you don't have an application already:
```shell
me@my:~$ grails create-app myapp
```

Add the plugin dependency to the project:
```groovy
// ~/myapp/build.gradle

dependencies {
    //...
    // Replace $inertiaPluginVersion with a suitable release version for your project, or define it in ~/myapp/gradle.properties
    implementation "io.github.matrei:grails-inertia-plugin:$inertiaPluginVersion"
    //...
}
```
To add the client dependencies and workflow to a Grails project, create the following files **(Vue 3 example)**:
```javascript
// ~/myapp/package.json (versions @ 2022-10-11) 
```
```json
{
  "name": "myapp",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "serve": "vite --port 3000",
    "build": "vite build"
  },
  "dependencies": {
    "@inertiajs/inertia": "^0.11.0",
    "@inertiajs/inertia-vue3": "^0.6.0",
    "vue": "^3.2.40"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^3.1.2",
    "vite": "^3.1.7"
  }
}
```
```javascript
// ~/myapp/vite.config.js

import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ command }) => ({
  base: command === 'serve' ? '' : '/static/dist/',
  publicDir: false,
  build: {
    manifest: true,
    outDir: 'src/main/resources/public/dist',
    assetsDir: 'js',
    rollupOptions: {
      input: 'src/main/javascript/main.js'
    }
  },
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src/main/javascript', import.meta.url))
    }
  },
  server: {
    // Needed for changes to picked up when running in WSL on Windows
    watch: {
      usePolling: true
    }
  }
}))
```
```javascript
// ~/myapp/src/main/javascript/main.js

import { createApp, h } from 'vue'
import { createInertiaApp } from '@inertiajs/inertia-vue3'

createInertiaApp({
  resolve: async (name) => {
    const pages = import.meta.glob('./Pages/**/*.vue')
    return (await pages[`./Pages/${name}.vue`]()).default
  },
  setup ({el, App, props, plugin}) {
    createApp({ render: () => h(App, props) })
      .use(plugin)
      .mount(el)
  }
})
```
It can be a good idea to add the following entries to your .gitignore
```gitignore
# ~/myapp/.gitignore
# ...
node_modules
src/main/resources/public/dist
```
And run:
```shell
me@my:~/myapp$ npm install
```

## Usage
In your controllers, you can now select which JavaScript Page Component to render and pass the values of the props to it.
```groovy
// grails-app/controllers/myapp/BookController.groovy

package myapp

class BookController {
    
    BookService bookService
    
    def index() {
        def books = bookService.listBooks()
        renderInertia 'Books/Index', [books: books]
    }
}
```
Here is an example Vue 3 Single File Component to that will render the books as a list.
```vue
<!-- src/main/javascript/Pages/Books/Index.vue -->

<script setup>
const props = defineProps({
  books: Array
})
</script>
<template>
  <ul>
    <li v-for="book in books">{{ book.name }}</li>
  </ul>
</template>
```
For development with [Hot Module Replacement](https://vitejs.dev/guide/features.html#hot-module-replacement) of the application run: (in separate terminals)
```shell
me@my:~/myapp$ npm run serve
```
```shell
me@my:~/myapp$ ./gradlew bootRun
```
For production or test, first build production version of JavaScript app:
```shell
me@my:~/myapp$ npm run build
```
and then run whatever you want to do:
```shell
me@my:~/myapp$ ./gradlew integrationTest
me@my:~/myapp$ ./gradlew bootJar
```
