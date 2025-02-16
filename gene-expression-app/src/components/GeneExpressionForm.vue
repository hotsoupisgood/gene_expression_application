<template>
    <div>
      <h2>Gene Expression Query</h2>
      <form @submit.prevent="fetchGeneExpression">
        <div>
          <label for="geneId">Ensembl ID:</label>
          <input type="text" id="geneId" v-model="geneId" required />
        </div>
        <div>
          <label for="tissueOfInterest">Tissue Type:</label>
          <input type="text" id="tissueOfInterest" v-model="tissueOfInterest" />
        </div>
        <button type="submit">Submit</button>
      </form>
  
      <div v-if="response">
        <h3>Response:</h3>
        <p>Highest Tissue: {{ response.highestTissue }}</p>
        <p>Specificity: {{ response.specificity }}</p>
      </div>
    </div>
    <div>
      Explore gene activity across human tissues using expression data from the Open Targets platform. Enter a gene ID to view expression levels by tissue for a clear snapshot of gene activity.
    </div>
  </template>
  
  <script lang="ts">
  import { defineComponent } from 'vue';
  
  const apiUrl = "/api/gene/expression";

  interface ResponseData {
    highestTissue: string;
    specificity: number;
  }
  
  export default defineComponent({
    data() {
      return {
        geneId: '' as string,
        tissueOfInterest: '' as string,
        response: null as ResponseData | null
      };
    },
    methods: {
      async fetchGeneExpression() {
        try {
          const queryParams = new URLSearchParams({
            geneId: this.geneId,
            tissueOfInterest: this.tissueOfInterest
          });
  
          const res = await fetch(`${apiUrl}?${queryParams}`);
          if (!res.ok) throw new Error("Error fetching data");
  
          const data = await res.json();
          this.response = data;
        } catch (error) {
          console.error("Error:", error);
          this.response = { highestTissue: "N/A", specificity: NaN };
        }
      }
    }
  });
  </script>
  
  <style scoped>
  form {
    margin-bottom: 1em;
  }
  form div {
    margin-bottom: 0.5em;
  }
  label {
    display: inline-block;
    width: 150px;
  }
  button {
    padding: 0.5em;
  }
  </style>
  