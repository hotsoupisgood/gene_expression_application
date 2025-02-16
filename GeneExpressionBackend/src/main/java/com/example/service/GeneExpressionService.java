package com.example.service;
import com.example.entities.GeneCache;
import com.example.repository.GeneCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Inside GeneExpressionService class

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Optional;

@Service
public class GeneExpressionService {
    private final GeneCacheRepository geneCacheRepository; 
    private static final Logger logger = LoggerFactory.getLogger(GeneExpressionService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeneExpressionService(GeneCacheRepository geneCacheRepository, WebClient.Builder webClientBuilder) {
        this.geneCacheRepository = geneCacheRepository;
        this.webClient = webClientBuilder.baseUrl("https://api.platform.opentargets.org/api/v4/graphql").build();
    }
    public Flux<ExpressionResult> getSpecificityMultiple(String gene_ids, String tissueOfInterest) {
        return Flux.fromArray(gene_ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .flatMap(s -> this.getSpecificity(s, tissueOfInterest));
    }

    public Mono<ExpressionResult> getSpecificity(String gene_id, String tissueOfInterest) {

        // Check if data exists in the database
        Optional<GeneCache> cachedResult = geneCacheRepository.findByGeneIdAndTissueOfInterest(gene_id, tissueOfInterest);
        if (cachedResult.isPresent()) {
            logger.info("✅ Cache hit for gene_id: {} and tissue: {}", gene_id, tissueOfInterest);
            GeneCache cache = cachedResult.get();
            return Mono.just(new ExpressionResult(cache.getTissueOfInterest(), cache.getHighestTissue(), cache.getSpecificity()));
        }
        logger.info("Cache miss for gene_id: {} and tissue: {}. Fetching from API...", gene_id, tissueOfInterest);
        return OpenTargetRequest(gene_id)
            .map(response -> processResponse(response, tissueOfInterest))
            .doOnSuccess(result -> {
                // 3️⃣ **Store the result in the database for future use**
                GeneCache geneCache = new GeneCache(gene_id, result.getTissueOfInterest(), result.getHighestTissue(), result.getSpecificity());
                geneCache.setTimestamp(new Date());
                geneCacheRepository.save(geneCache);
                logger.info("Saved new entry to cache for gene_id: {}", gene_id);
            })
                .doOnError(error -> logger.error("Error fetching specificity data", error));
    }
    private Mono<String> OpenTargetRequest(String gene_id) {
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
            """, gene_id);
        return webClient.post()
                .bodyValue(new GraphQLRequest(queryString))
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(response -> logger.debug("Received response: {}", response));
    }
    private ExpressionResult processResponse(String jsonResponse, String tissueOfInterest) {
        try {
            logger.debug("Processing response from API");

            // Parse the root JSON
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode dataNode = root.path("data");
            JsonNode targetNode = dataNode.path("target");
            JsonNode expressions = targetNode.path("expressions");

            // Log the different levels of the JSON structure to verify parsing
            logger.debug("Data node: {}", dataNode);
            logger.debug("Target node: {}", targetNode);
            logger.debug("Expressions node: {}", expressions);

            // Check if there are any expressions
            if (expressions.isMissingNode() || !expressions.elements().hasNext()) {
                logger.warn("No expressions data found for response: {}", jsonResponse);
                return new ExpressionResult("NA", "NA", 0.0);
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
//                logger.debug("RNA value: {}", rnaNode.toString());

                // Convert organs to a comma-separated string
                String organs = tissueNode.toString()
                        .replaceAll("\"", "")
                        .replaceAll("\\[", "")
                        .replaceAll("]", "");
//                logger.debug("Organs: {}", organs);

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
            logger.debug("Calculated specificity: {} for highest tissue: {}", specificity, highestTissue);

            return new ExpressionResult(tissueOfInterest, highestTissue, specificity);

        } catch (Exception e) {
            logger.error("Error processing response: {}", jsonResponse, e);
            return new ExpressionResult("NA", "NA", 0.0);
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
        private final String tissueOfInterest;
        private final double specificity;

        public ExpressionResult(String tissueOfInterest, String highestTissue, double specificity) {
            this.highestTissue = highestTissue;
            this.tissueOfInterest = tissueOfInterest;
            this.specificity = specificity;
        }

        public String getHighestTissue() {
            return highestTissue;
        }

        public String getTissueOfInterest() {
            return tissueOfInterest;
        }

        public double getSpecificity() {
            return specificity;
        }
    }
}
