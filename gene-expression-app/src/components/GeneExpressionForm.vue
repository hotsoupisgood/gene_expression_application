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
      <!-- <div id="plotlyChart"  v-show="showChart"></div> -->
       <h3 v-show="showChart">Grouped Plots</h3>
      <div id="plotlyChart"     class="plots" v-show="showChart"></div>
      <div v-show="showChart"><label>Values: </label><input v-model="outputSpec"></input></div>
      <h3 v-show="showChart">Seperated Plots</h3>
      <div id="plot-container"  class="plots" v-show="showChart"></div>
    </div>
    <div id="about">
      <h4>About</h4>
      <p>Explore gene activity across human tissues using expression data from the Open Targets platform. Enter ensembl ID's to view expression levels by a chosen tissue. Example ensembl id's: ENSG00000132535, ENSG00000226913, ENSG00000175164, ENSG00000254087</p>
    </div>

  </template>
  
  <script lang="ts">
  import { defineComponent } from 'vue';
  import * as Plotly from "plotly.js-dist-min"; 
  const apiUrl = "http://127.0.0.1:8080/api/gene/expressions";
  // const apiUrl = "/api/gene/expressions";
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
    methods: {
      async fetchGeneExpression() {
        try {
          // Make query
          const queryParams = new URLSearchParams({
            geneId: this.geneId,
            tissueOfInterest: this.tissueOfInterest
          });
  
          const res = await fetch(`${apiUrl}?${queryParams}`);
          if (!res.ok) throw new Error("Error fetching data");
          const fakedata = await res.json();

          // const fakedata =     [
          //   {
          //     "geneId": "ens123",
          //     "highestTissue": "brain",
          //     "tissueOfInterest": "brain",
          //     "specificity": 0.7
          //   },
          //   {
          //     "geneId": "ens123",
          //     "highestTissue": "brain",
          //     "tissueOfInterest": "brain",
          //     "specificity": 0.7
          //   },
          //   {
          //     "geneId": "ens123",
          //     "highestTissue": "brain",
          //     "tissueOfInterest": "brain",
          //     "specificity": 0.7
          //   }
          // ];
          // Make graph


        /*
        Make layout for the grouped graph
        ===============================
        */
        const layout = {
          autosize: true,
          width: window.innerWidth-50,
          title: {
            text: `Gene Expression Levels in ${this.tissueOfInterest}`
          },
          xaxis: { title: {text: "Gene ID"} },
          yaxis: { title: {text: "Expression Level"} },
        };

        /*
        Make grouped graph
        ===============================
        */
          const tissues: string[] = Array.from(new Set(fakedata.map((d:{ tissueOfInterest:string; }) => d.tissueOfInterest)));
          const traces_new  = tissues.map(tissue => {
            return {
                x: fakedata
                      .filter((d:{ tissueOfInterest:string; }) => d.tissueOfInterest === tissue)
                      .map((d:{ geneId:string; })      => d.geneId),
                y: fakedata
                      .filter((d:{ tissueOfInterest:string; }) => d.tissueOfInterest === tissue)
                      .map((d:{ specificity:string; }) => d.specificity),
                name: tissue,
                type:  'bar'  as const  
            };
        }) as Plotly.Data[];

        /*
        Make graph for each tissue type
        ===============================
        */
        const container = document.getElementById("plot-container"); // Get the predefined container
        // ✅ Clear previous plots before adding new ones
        if (container) {
            container.innerHTML = ''; // Removes all existing child elements
        }
        tissues.forEach((tissue: string) => {
          /*
          Make layout for the seperate graphs
          ===============================
          */
          const layout_sep = {
            autosize: true,
            width: window.innerWidth-50,
            title: {
              text: `Gene Expression Levels in ${tissue}`
            },
            xaxis: { title: {text: "Gene ID"} },
            yaxis: { title: {text: "Expression Level"} },
          };

          const tissueData = fakedata.filter((d:{ tissueOfInterest:string; }) => d.tissueOfInterest === tissue);

          const trace = {
              x: tissueData.map((d:{ geneId:string; }) => d.geneId),
              y: tissueData.map((d:{ specificity:string; }) => d.specificity),
              name: tissue,
              type: 'bar' as const
          };

          const divId   = `plot-${tissue.replace(/\s+/g, '-')}`;
          const inputId = `input-${tissue.replace(/\s+/g, '-')}`;

          // Check if the container exists
          if (container) {
              const div = document.createElement('div'); // Create a new div for this plot
              div.id = divId;
              container.appendChild(div); // Append inside the container instead of document.body

              Plotly.newPlot(divId, [trace], layout_sep); // Render the plot inside this div
              const inputContainer = document.createElement('div');
              inputContainer.innerHTML = `
                  <div>
                      <label>Values: </label>
                      <input id="${inputId}" type="text" value="${tissueData.map((d:{ specificity:string; }) => d.specificity).join(', ')}" readonly>
                  </div>
              `;
              container.appendChild(inputContainer); // Append the input after the plot

          } else {
              console.error("Plot container not found!");
          }
      });
          
          
          // const geneIdArray                   =  fakedata.map((x:{ geneId:       string; }) => x.geneId);

          
        const specificityArray              =  fakedata.map((x:{ specificity:  number; }) => x.specificity);
        this.outputSpec = specificityArray.join(', ')
          

        Plotly.newPlot('plotlyChart', traces_new, layout);

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
  