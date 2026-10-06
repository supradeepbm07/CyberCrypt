package com.cybercrypt.repository;
import com.cybercrypt.model.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {
    List<Evidence> findByCaseId(UUID caseId);
    Optional<Evidence> findBySha256(String sha256);
}
