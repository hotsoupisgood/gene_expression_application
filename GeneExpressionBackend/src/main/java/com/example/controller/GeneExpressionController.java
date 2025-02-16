package com.example.controller;

import com.example.service.GeneExpressionService;
import com.example.service.GeneExpressionService.ExpressionResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/gene")
public class GeneExpressionController {

    private final GeneExpressionService geneExpressionService;

    public GeneExpressionController(GeneExpressionService geneExpressionService) {
        this.geneExpressionService = geneExpressionService;
    }

    @GetMapping("/expression")
    public Mono<ExpressionResult> getGeneExpression(
            @RequestParam String geneId,
            @RequestParam String tissueOfInterest) {
        return geneExpressionService.getSpecificity(geneId, tissueOfInterest);
    }

    @GetMapping("/expressions")
    public Flux<ExpressionResult> getGeneExpressions(
            @RequestParam String geneId,
            @RequestParam String tissueOfInterest) {
        return geneExpressionService.getSpecificityMultiple(geneId, tissueOfInterest);
    }
}
