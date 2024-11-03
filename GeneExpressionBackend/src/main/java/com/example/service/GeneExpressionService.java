package com.example.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Inside GeneExpressionService class

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Iterator;

@Service
public class GeneExpressionService {

    private static final Logger logger = LoggerFactory.getLogger(GeneExpressionService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeneExpressionService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.platform.opentargets.org/api/v4/graphql").build();
    }

    public Mono<ExpressionResult> getSpecificity(String geneId, String tissueOfInterest) {
        String queryString = String.format("""
            query targetAnnotation {
              target(ensemblId: "%s") {
                id
                expressions {
                  tissue {
                    organs
                  }
                  rna {
                    value
                    level
                    zscore
                  }
                }
              } 
            }
            """, geneId);


        return webClient.post()
                .bodyValue(new GraphQLRequest(queryString))
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(response -> logger.info("Received response: {}", response))
                .map(response -> processResponse(response, tissueOfInterest))
                .doOnError(error -> logger.error("Error fetching specificity data", error));
    }
    private ExpressionResult processResponse(String jsonResponse, String tissueOfInterest) {
        try {
            logger.info("Processing response from API");

            // Parse the root JSON
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode dataNode = root.path("data");
            JsonNode targetNode = dataNode.path("target");
            JsonNode expressions = targetNode.path("expressions");

            // Log the different levels of the JSON structure to verify parsing
            logger.info("Data node: {}", dataNode);
            logger.info("Target node: {}", targetNode);
            logger.info("Expressions node: {}", expressions);

            // Check if there are any expressions
            if (expressions.isMissingNode() || !expressions.elements().hasNext()) {
                logger.warn("No expressions data found for response: {}", jsonResponse);
                return new ExpressionResult("NA", 0.0);
            }

            double total = 0.0;
            double tissueTotal = 0.0;
            String highestTissue = "NA";
            double highestValue = 0.0;

            // Iterate over each expression in the expressions array
            for (JsonNode expression : expressions) {
                // Extract RNA value and organs array
                JsonNode rnaNode = expression.path("rna");
                JsonNode tissueNode = expression.path("tissue").path("organs");

                double value = rnaNode.path("value").asDouble(0.0);
//                logger.info("RNA value: {}", rnaNode.toString());

                // Convert organs to a comma-separated string
                String organs = tissueNode.toString()
                        .replaceAll("\"", "")
                        .replaceAll("\\[", "")
                        .replaceAll("]", "");
//                logger.info("Organs: {}", organs);

                // Track the tissue with the highest expression
                if (value > highestValue) {
                    highestValue = value;
                    highestTissue = organs;
                }

                // Sum up the total and brain-specific values
                total += value;

                if (organs.contains(tissueOfInterest)) {
                    tissueTotal += value;
                }
            }


            // Calculate the specificity percentage for brain tissues
            double specificity = total > 0 ? Math.round((tissueTotal / total) * 100.0) / 100.0 : 0.0;
            logger.info("Calculated specificity: {} for highest tissue: {}", specificity, highestTissue);

            return new ExpressionResult(highestTissue, specificity);

        } catch (Exception e) {
            logger.error("Error processing response: {}", jsonResponse, e);
            return new ExpressionResult("NA", 0.0);
        }
    }


    private static class GraphQLRequest {
        private final String query;

        public GraphQLRequest(String query) {
            this.query = query;
        }

        public String getQuery() {
            return query;
        }
    }

    public static class ExpressionResult {
        private final String highestTissue;
        private final double specificity;

        public ExpressionResult(String highestTissue, double specificity) {
            this.highestTissue = highestTissue;
            this.specificity = specificity;
        }

        public String getHighestTissue() {
            return highestTissue;
        }

        public double getSpecificity() {
            return specificity;
        }
    }
}
