# Feature Checklist — CyberCrypt

| Presentation feature | Implementation |
|---|---|
| Complaint registration | Web form + `POST /api/cases` |
| Anonymous reporting | Anonymous checkbox + generated tracking ID |
| SHA-256 evidence hash | `HashService` |
| Evidence vault | Runtime `uploads/<trackingId>/` |
| Priority scoring | `PriorityService`, 0–100 |
| Financial-loss thresholds | ₹10k / ₹100k / ₹1M / ₹10M bands |
| Crime classification | Crime-type selector + rules |
| Investigation assignment | Assignment endpoint + dashboard |
| Chain of custody | `CustodyEvent` + timeline |
| Keyword detection | `KeywordService` |
| Duplicate detection | SHA-256 comparison |
| QR receipt | ZXing QR PNG endpoint |
| PDF report | Apache PDFBox |
| Case closure | Status + verdict |
| Audit trail | Database-backed custody events |
| OTP login | Reserved for production integration |
