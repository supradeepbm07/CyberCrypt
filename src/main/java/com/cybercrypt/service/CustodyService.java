package com.cybercrypt.service;

import com.cybercrypt.model.CustodyEvent;
import com.cybercrypt.repository.CustodyRepository;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class CustodyService {
    private final CustodyRepository repo;

    public CustodyService(CustodyRepository repo) { this.repo = repo; }

    public CustodyEvent log(UUID caseId, UUID evidenceId, String actor, String action, String details) {
        CustodyEvent e = new CustodyEvent();
        e.setCaseId(caseId);
        e.setEvidenceId(evidenceId);
        e.setActor(actor == null || actor.isBlank() ? "SYSTEM" : actor);
        e.setAction(action);
        e.setDetails(details);
        String previous = repo.findByCaseIdOrderByEventTimeAsc(caseId).stream()
                .reduce((a,b)->b).map(CustodyEvent::getEventHash).orElse("GENESIS");
        e.setPreviousHash(previous);
        // Hash-chain record: any changed prior event breaks the chain during audit.
        String payload = caseId + "|" + evidenceId + "|" + e.getActor() + "|" + action + "|" + details + "|" + previous + "|" + System.nanoTime();
        e.setEventHash(sha256(payload));
        return repo.save(e);
    }

    public List<CustodyEvent> forCase(UUID caseId) {
        return repo.findByCaseIdOrderByEventTimeAsc(caseId);
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
