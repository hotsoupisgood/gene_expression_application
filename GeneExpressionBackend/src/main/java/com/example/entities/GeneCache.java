package com.example.entities;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "gene_expression_cache")
public class GeneCache {
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="seq")
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;

    @Column(name = "gene_id", nullable = false, length = 255)
    private String geneId;
    private String tissueOfInterest;
    private String highest_tissue;
    private double specificity;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "timestamp", nullable = false)
    private Date timestamp;

    public GeneCache() {}

    public GeneCache(String geneId, String tissueOfInterest, String highest_tissue, double specificity) {
        this.geneId             = geneId;
        this.tissueOfInterest   = tissueOfInterest;
        this.highest_tissue     = highest_tissue;
        this.specificity        = specificity;
    }

    public String getId() {
        return geneId;
    }
    public void setId(String gene_id) {
        this.geneId = gene_id;
    }

    public String getTissueOfInterest() {
        return tissueOfInterest;
    }
    public void setTissueOfInterest(String tissue_of_interest) {
        this.tissueOfInterest = tissue_of_interest;
    }
    public String getHighestTissue() {
        return highest_tissue;
    }
    public void setHighestTissue(String tissue_of_inthighest_tissueerest) {
        this.highest_tissue = highest_tissue;
    }
    public double getSpecificity() {
        return specificity;
    }
    public void setSpecificity(double specificity) {
        this.specificity = specificity;
    }
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    @Override
    public String toString() {
        return "gene{" +
                ", id='" + geneId + '\'' +
                ", Tissue=" + tissueOfInterest +
                '}';
    }
}