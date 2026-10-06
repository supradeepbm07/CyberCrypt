package com.cybercrypt.repository;
import com.cybercrypt.model.CaseFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CaseRepository extends JpaRepository<CaseFile, UUID> {
    Optional<CaseFile> findByTrackingId(String trackingId);
}
