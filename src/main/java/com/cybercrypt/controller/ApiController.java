package com.cybercrypt.controller;

import com.cybercrypt.model.*;
import com.cybercrypt.repository.*;
import com.cybercrypt.service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ApiController {
    private final CaseRepository cases;
    private final EvidenceRepository evidenceRepo;
    private final CustodyService custody;
    private final HashService hashService;
    private final PriorityService priority;
    private final KeywordService keywords;
    private final ReportService reports;
    private final OtpService otpService;

    public ApiController(CaseRepository cases, EvidenceRepository evidenceRepo,
                         CustodyService custody, HashService hashService,
                         PriorityService priority, KeywordService keywords,
                         ReportService reports, OtpService otpService) {
        this.cases = cases; this.evidenceRepo = evidenceRepo; this.custody = custody;
        this.hashService = hashService; this.priority = priority; this.keywords = keywords;
        this.reports = reports; this.otpService = otpService;
    }

    @PostMapping("/auth/request-otp")
    public Map<String,String> requestOtp(@RequestBody Map<String,String> body) {
        String destination = body.getOrDefault("destination","");
        if (destination.isBlank()) return Map.of("error","Phone/email is required");
        String otp = otpService.generate(destination);
        return Map.of("message","Demo OTP generated", "otp", otp,
                "note","Academic prototype only. Replace with a real OTP provider for production.");
    }

    @PostMapping("/auth/verify-otp")
    public Map<String,Object> verifyOtp(@RequestBody Map<String,String> body) {
        String destination = body.getOrDefault("destination","");
        String otp = body.getOrDefault("otp","");
        boolean ok = otpService.verify(destination, otp);
        return Map.of("authenticated", ok);
    }

    @GetMapping("/cases")
    public List<CaseFile> allCases() { return cases.findAll(); }

    @GetMapping("/cases/{id}")
    public ResponseEntity<?> getCase(@PathVariable UUID id) {
        return cases.findById(id).<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/track/{trackingId}")
    public ResponseEntity<?> track(@PathVariable String trackingId) {
        return cases.findByTrackingId(trackingId).<ResponseEntity<?>>map(c -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("case", c);
            m.put("evidence", evidenceRepo.findByCaseId(c.getId()));
            m.put("custody", custody.forCase(c.getId()));
            return ResponseEntity.ok(m);
        }).orElseGet(() -> ResponseEntity.status(404).body(Map.of("error","Tracking ID not found")));
    }

    @PostMapping("/cases")
    public ResponseEntity<?> create(@RequestBody Map<String,Object> body) {
        CaseFile c = new CaseFile();
        c.setReporterName((String) body.getOrDefault("reporterName",""));
        c.setReporterEmail((String) body.getOrDefault("reporterEmail",""));
        c.setPhone((String) body.getOrDefault("phone",""));
        c.setCrimeType((String) body.getOrDefault("crimeType","Other"));
        c.setDescription((String) body.getOrDefault("description",""));
        Object loss = body.get("financialLoss");
        c.setFinancialLoss(loss == null || loss.toString().isBlank() ? 0.0 : Double.parseDouble(loss.toString()));
        c.setAnonymous(Boolean.parseBoolean(String.valueOf(body.getOrDefault("anonymous","false"))));

        if (c.isAnonymous()) {
            c.setReporterName("ANONYMOUS");
            c.setReporterEmail("");
            c.setPhone("");
        }

        priority.score(c);
        c = cases.save(c);
        custody.log(c.getId(), null, "SYSTEM", "CASE_REGISTERED",
                "Complaint registered with priority " + c.getPriority());
        return ResponseEntity.ok(c);
    }

    @PostMapping("/cases/{id}/evidence")
    public ResponseEntity<?> upload(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        try {
            CaseFile c = cases.findById(id).orElseThrow();
            if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","Empty file"));

            String sha = hashService.sha256(file);
            Optional<Evidence> duplicate = evidenceRepo.findBySha256(sha);

            Path dir = Path.of("uploads", c.getTrackingId());
            Files.createDirectories(dir);
            String stored = System.currentTimeMillis() + "_" +
                    file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = dir.resolve(stored);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            Evidence e = new Evidence();
            e.setCaseId(id);
            e.setOriginalFileName(file.getOriginalFilename());
            e.setStoredPath(target.toString());
            e.setContentType(file.getContentType());
            e.setSizeBytes(file.getSize());
            e.setSha256(sha);
            e.setUploadedAt(LocalDateTime.now());
            e = evidenceRepo.save(e);

            String detail = duplicate.isPresent()
                    ? "SHA-256 matches existing evidence " + duplicate.get().getId()
                    : "SHA-256 recorded at upload";
            custody.log(id, e.getId(), "REPORTER", "UPLOAD", detail);

            return ResponseEntity.ok(Map.of(
                    "evidence", e,
                    "duplicate", duplicate.isPresent(),
                    "message", detail
            ));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/cases/{id}/keywords")
    public Map<String,Object> detectKeywords(@PathVariable UUID id) {
        CaseFile c = cases.findById(id).orElseThrow();
        List<String> found = keywords.detect(c.getDescription());
        return Map.of("keywords", found, "emergency", found.stream()
                .anyMatch(k -> Set.of("threat","blackmail","ransomware","terror","weapon").contains(k)));
    }

    @PostMapping("/cases/{id}/assign")
    public ResponseEntity<?> assign(@PathVariable UUID id, @RequestBody Map<String,String> body) {
        CaseFile c = cases.findById(id).orElseThrow();
        c.setAssignedOfficer(body.getOrDefault("officer","Investigation Desk"));
        c.setStatus("INVESTIGATION_ASSIGNED");
        cases.save(c);
        custody.log(id, null, body.getOrDefault("actor","ADMIN"), "ASSIGN",
                "Assigned to " + c.getAssignedOfficer());
        return ResponseEntity.ok(c);
    }

    @GetMapping("/cases/{id}/evidence/{evidenceId}/download")
    public void downloadEvidence(@PathVariable UUID id, @PathVariable UUID evidenceId,
                                  HttpServletResponse response) throws Exception {
        Evidence e = evidenceRepo.findById(evidenceId).orElseThrow();
        if (!e.getCaseId().equals(id)) {
            response.sendError(400, "Evidence mismatch");
            return;
        }
        custody.log(id, evidenceId, "INVESTIGATOR", "DOWNLOAD", "Evidence downloaded for review");
        Path path = Path.of(e.getStoredPath());
        response.setContentType(e.getContentType() == null ? "application/octet-stream" : e.getContentType());
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
                e.getOriginalFileName().replaceAll("[^a-zA-Z0-9._-]", "_") + "\"");
        Files.copy(path, response.getOutputStream());
    }

    @PostMapping("/cases/{id}/verify/{evidenceId}")
    public ResponseEntity<?> verify(@PathVariable UUID id, @PathVariable UUID evidenceId) {
        Evidence e = evidenceRepo.findById(evidenceId).orElseThrow();
        if (!e.getCaseId().equals(id)) return ResponseEntity.badRequest().body(Map.of("error","Evidence mismatch"));

        try {
            String current = hashService.sha256(new MultipartFileAdapter(e.getStoredPath(), e.getOriginalFileName(), e.getContentType()));
            boolean valid = current.equalsIgnoreCase(e.getSha256());
            custody.log(id, evidenceId, "INVESTIGATOR", "VERIFY",
                    valid ? "SHA-256 verified; no file change detected" : "HASH MISMATCH; evidence changed");
            return ResponseEntity.ok(Map.of("verified", valid, "storedHash", e.getSha256(), "currentHash", current));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/cases/{id}/status")
    public ResponseEntity<?> status(@PathVariable UUID id, @RequestBody Map<String,String> body) {
        CaseFile c = cases.findById(id).orElseThrow();
        c.setStatus(body.getOrDefault("status","UNDER_REVIEW"));
        c.setVerdict(body.getOrDefault("verdict", c.getVerdict()));
        cases.save(c);
        custody.log(id, null, body.getOrDefault("actor","INVESTIGATOR"), "STATUS_CHANGE",
                "Status changed to " + c.getStatus());
        return ResponseEntity.ok(c);
    }

    @GetMapping("/cases/{id}/qr")
    public void qr(@PathVariable UUID id, HttpServletResponse response) throws Exception {
        CaseFile c = cases.findById(id).orElseThrow();
        response.setContentType("image/png");
        response.getOutputStream().write(reports.qr("CYBERCRYPT|" + c.getTrackingId()));
    }

    @GetMapping("/cases/{id}/report.pdf")
    public void report(@PathVariable UUID id, HttpServletResponse response) throws Exception {
        CaseFile c = cases.findById(id).orElseThrow();
        byte[] pdf = reports.pdf(c, evidenceRepo.findByCaseId(id), custody.forCase(id));
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + c.getTrackingId() + "-report.pdf\"");
        response.getOutputStream().write(pdf);
    }

    static class MultipartFileAdapter implements MultipartFile {
        private final Path path; private final String name; private final String type;
        MultipartFileAdapter(String p, String n, String t) { path=Path.of(p); name=n; type=t; }
        public String getName(){return "file";}
        public String getOriginalFilename(){return name;}
        public String getContentType(){return type;}
        public boolean isEmpty(){try{return Files.size(path)==0;}catch(Exception e){return true;}}
        public long getSize(){try{return Files.size(path);}catch(Exception e){return 0;}}
        public byte[] getBytes() throws java.io.IOException{return Files.readAllBytes(path);}
        public java.io.InputStream getInputStream() throws java.io.IOException{return Files.newInputStream(path);}
        public void transferTo(java.io.File dest) throws java.io.IOException{Files.copy(path,dest.toPath(),StandardCopyOption.REPLACE_EXISTING);}
    }
}
