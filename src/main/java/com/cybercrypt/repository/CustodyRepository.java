package com.cybercrypt.repository;
import com.cybercrypt.model.CustodyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CustodyRepository extends JpaRepository<CustodyEvent, UUID> {
    List<CustodyEvent> findByCaseIdOrderByEventTimeAsc(UUID caseId);
}
