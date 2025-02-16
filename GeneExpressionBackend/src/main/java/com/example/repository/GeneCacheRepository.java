package com.example.repository;

import com.example.entities.GeneCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeneCacheRepository extends JpaRepository<GeneCache, String> {
    Optional<GeneCache> findByGeneIdAndTissueOfInterest(String geneId, String tissueOfInterest);
}
