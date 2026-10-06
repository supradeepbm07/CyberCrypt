package com.cybercrypt;

import com.cybercrypt.model.CaseFile;
import com.cybercrypt.repository.CaseRepository;
import com.cybercrypt.service.CustodyService;
import com.cybercrypt.service.PriorityService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoDataLoader implements CommandLineRunner {
    private final CaseRepository cases;
    private final PriorityService priority;
    private final CustodyService custody;

    public DemoDataLoader(CaseRepository cases, PriorityService priority, CustodyService custody) {
        this.cases = cases;
        this.priority = priority;
        this.custody = custody;
    }

    @Override
    public void run(String... args) {
        seed("CC-2026-DEMO01", "Financial Fraud", 250000,
                "Urgent fraudulent bank transfer reported after a phishing call requested account credentials.",
                "INVESTIGATION_ASSIGNED", "Cyber Crime Investigation Desk", null);
        seed("CC-2026-DEMO02", "Identity Theft", 15000,
                "Unknown person used copied identity details to attempt unauthorized account access.",
                "REGISTERED", null, null);
        seed("CC-2026-DEMO03", "Cyber Harassment", 0,
                "Repeated abusive and threatening messages were received through an online account.",
                "CLOSED", "Investigator Priya", "Demo case reviewed and closed for project presentation.");
    }

    private void seed(String trackingId, String type, double loss, String description,
                      String status, String officer, String verdict) {
        if (cases.findByTrackingId(trackingId).isPresent()) return;
        CaseFile c = new CaseFile();
        c.setTrackingId(trackingId);
        c.setReporterName("DEMO REPORTER");
        c.setReporterEmail("demo@cybercrypt.local");
        c.setPhone("9000000000");
        c.setCrimeType(type);
        c.setFinancialLoss(loss);
        c.setDescription(description);
        c.setAnonymous(false);
        c.setStatus(status);
        c.setAssignedOfficer(officer);
        c.setVerdict(verdict);
        priority.score(c);
        c = cases.save(c);
        custody.log(c.getId(), null, "SYSTEM", "DEMO_CASE_SEEDED",
                "Sample record created for academic project review");
        if ("INVESTIGATION_ASSIGNED".equals(status)) {
            custody.log(c.getId(), null, "ADMIN", "ASSIGN", "Assigned to " + officer);
        } else if ("CLOSED".equals(status)) {
            custody.log(c.getId(), null, "INVESTIGATOR", "STATUS_CHANGE", "Status changed to CLOSED");
        }
    }
}
