<template>
    <div>
      <h2>Gene Expression Query</h2>
      <form @submit.prevent="fetchGeneExpression">
        <div>
          <label for="geneId">Ensembl ID:</label>
          <input type="text" id="geneId" v-model="geneId" placeholder="e.g. ENSG1, ENSG2, ENSG3" required />
        </div>
        <div>
          <label for="tissueOfInterest">Tissue Type:</label>
          <input type="text" id="tissueOfInterest" v-model="tissueOfInterest"  placeholder="e.g. brain" />
        </div>
        <button type="submit">Submit</button>
      </form>
  
      <div v-if="response">
        <h3>Response:</h3>
        <p>Highest Tissue: {{ response.highestTissue }}</p>
        <p>Specificity: {{ response.specificity }}</p>
      </div>
      <div id="plotlyChart"  v-show="showChart"></div>
      <div v-show="showChart"><input v-model="outputSpec"></input></div>
    </div>
    <div id="about">
      <h4>About</h4>
      Explore gene activity across human tissues using expression data from the Open Targets platform. Enter a gene ID to view expression levels by tissue for a clear snapshot of gene activity.
    </div>
  </template>
  
  <script lang="ts">
  import { defineComponent,onMounted, ref } from 'vue';
  import * as Plotly from "plotly.js-dist-min"; 
  const apiUrl = "http://127.0.0.1:8080/api/gene/expressions";
  // const apiUrl = "/api/gene/expression";
  let plotlyChart: any;
  interface ResponseData {
    highestTissue: string;
    specificity: number;
  }
  
  export default defineComponent({
    data() {
      return {
        geneId: '' as string,
        tissueOfInterest: '' as string,
        response: null as ResponseData | null,
        showChart: false as boolean,
        outputSpec: '' as String

      };
    },
    onMounted: {
      async () {
        plotlyChart = document.getElementById("plotlyChart");
        console.log('onmount');
      }
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
          const fakedata = await res.json();

          // const fakedata =     [
          //   {
          //     "geneID": "ens123",
          //     "highestTissue": "brain",
          //     "tissueOfInterest": "brain",
          //     "specificity": 0.7
          //   },
          //   {
          //     "geneID": "ens123",
          //     "highestTissue": "brain",
          //     "tissueOfInterest": "brain",
          //     "specificity": 0.7
          //   },
          //   {
          //     "geneID": "ens123",
          //     "highestTissue": "brain",
          //     "tissueOfInterest": "brain",
          //     "specificity": 0.7
          //   }
          // ];
          const highestTissueArray            =  fakedata.map((x:{ tissueOfInterest: string; }) => x.tissueOfInterest);
          const geneIdArray                   =  fakedata.map((x:{ geneId: string; }) => x.geneId);
          const specificityArray              =  fakedata.map((x:{ specificity:      number; }) => x.specificity);
          this.outputSpec = specificityArray.join(', ')
          const layout = {
            title: {
              text: `Gene Expression Levels in ${this.tissueOfInterest}`
            },
            xaxis: { title: {text: "Gene ID"} },
            yaxis: { title: {text: "Expression Level"} },
          };
          const data: Plotly.BarData[] = [
            {
              // x: this.geneId.split(',').map((x: string) => x.trim()),
              x: geneIdArray,
              y: specificityArray,
              type: 'bar'
            }
          ];
          Plotly.newPlot('plotlyChart', data, layout);
          this.showChart = true;

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
  