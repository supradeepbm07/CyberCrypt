# CyberCrypt — Complete Java Project

**Intelligent Cyber Crime Reporting & Digital Evidence Protection System**

This implementation follows the supplied zeroth-review presentation:
- Complaint registration / anonymous reporting
- Secret tracking ID in `CC-YYYY-XXXXXX` style
- Digital Evidence Vault
- SHA-256 hash generated on upload
- Integrity verification and tamper detection
- Rule-based priority/risk scoring from 0–100
- Financial-loss and crime-type based triage
- Suspicious keyword detection
- Duplicate evidence detection through SHA-256
- Investigator assignment
- Digital chain-of-custody event logging
- Evidence download action is logged
- Hash-chained custody records for tamper-evident audit history
- Demo OTP login flow
- QR complaint receipt
- One-click PDF case report
- Case closure and verdict recording
- H2 database persistence
- Responsive browser dashboard

## Technology
Java 17, Spring Boot, Spring Web, Spring Data JPA, H2, ZXing, Apache PDFBox, HTML/CSS/JavaScript.

## Run
Prerequisites:
1. JDK 17+
2. Maven 3.9+

From this folder:

```bash
mvn spring-boot:run
```

Open:
`http://localhost:8080`

Or package:

```bash
mvn clean package
java -jar target/cybercrypt-1.0.0.jar
```

## Demo flow
1. Report Crime → create a test complaint.
2. Save the generated tracking ID.
3. Upload an image/PDF/chat export as evidence.
4. The server stores the file and records SHA-256.
5. Investigator Dashboard → open the case.
6. Verify evidence to recompute the hash.
7. Assign an investigator.
8. Run keyword detection.
9. Download the PDF report or open the QR receipt.
10. Close the case with a verdict.

## Important demo/security note
This is an academic prototype, not a production police/forensic system. The OTP login described in the presentation is represented as a future integration boundary rather than a real SMS OTP service. For production, add real identity/OTP infrastructure, authorization roles, encrypted evidence storage, immutable/WORM audit storage, malware scanning, secure key management, legal retention rules, rate limiting, and a real government/police integration.

## Project structure
- `model/` — JPA domain models
- `repository/` — database access
- `service/` — hashing, scoring, keyword detection, custody and report generation
- `controller/` — REST API and web routing
- `static/` — complete frontend
- `uploads/` — evidence storage created at runtime
- `data/` — H2 database created at runtime

## Main API
- `POST /api/cases`
- `GET /api/cases`
- `GET /api/track/{trackingId}`
- `POST /api/cases/{id}/evidence`
- `POST /api/cases/{id}/verify/{evidenceId}`
- `POST /api/cases/{id}/assign`
- `GET /api/cases/{id}/keywords`
- `POST /api/cases/{id}/status`
- `GET /api/cases/{id}/qr`
- `GET /api/cases/{id}/report.pdf`

### Demo OTP
Open **Investigator Login**, enter any destination, click **Send OTP**, then use `123456`. This is intentionally deterministic for classroom demonstration and is **not** secure authentication for production.

## Project Review Demo Enhancements

This review-ready build includes:
- Project overview and technology stack section on the home page.
- Live investigator dashboard summary cards for Total, Pending, Under Investigation, and Resolved cases.
- Visual case-progress timeline from registration to resolution.
- Clearer complaint and OTP validation messages.
- Three clearly labelled sample records seeded automatically for academic demonstration when they are not already present.
- Quick tracking demo using `CC-2026-DEMO01` from the Track Case page.

### Suggested review flow
1. Open **Report Crime** and explain the project objective, risk scoring, SHA-256 integrity, and chain of custody.
2. Submit a complaint or use the seeded sample case.
3. Open **Track Case** → click **Open CC-2026-DEMO01**.
4. Open **Investigator Login**, request the demo OTP, verify it, and enter the dashboard.
5. Show the dashboard summary cards, priority queue, case progress, evidence vault, and custody timeline.
