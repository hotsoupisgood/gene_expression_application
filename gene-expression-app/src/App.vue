<template>
  <div id="app">
    <GeneExpressionForm />
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import GeneExpressionForm from './components/GeneExpressionForm.vue';

export default defineComponent({
  components: {
    GeneExpressionForm
  }
});
</script>

<style scoped>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #7eb8f2;
}
</style>


<style scoped>
.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.vue:hover {
  filter: drop-shadow(0 0 2em #42b883aa);
}
</style>
