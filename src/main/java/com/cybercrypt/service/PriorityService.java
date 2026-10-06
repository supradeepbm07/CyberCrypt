package com.cybercrypt.service;

import com.cybercrypt.model.CaseFile;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class PriorityService {
    public void score(CaseFile c) {
        int score = 0;
        double loss = c.getFinancialLoss() == null ? 0 : c.getFinancialLoss();
        String type = c.getCrimeType() == null ? "" : c.getCrimeType().toLowerCase(Locale.ROOT);
        String text = c.getDescription() == null ? "" : c.getDescription().toLowerCase(Locale.ROOT);

        if (loss >= 10_000_000) score += 45;
        else if (loss >= 1_000_000) score += 35;
        else if (loss >= 100_000) score += 25;
        else if (loss >= 10_000) score += 15;

        if (type.contains("financial") || type.contains("bank") || type.contains("investment")) score += 20;
        if (type.contains("identity") || type.contains("account")) score += 15;
        if (type.contains("ransomware") || type.contains("extortion")) score += 25;
        if (text.contains("urgent") || text.contains("threat") || text.contains("blackmail")) score += 15;
        if (text.contains("child") || text.contains("minor")) score += 10;

        score = Math.min(100, score);
        c.setRiskScore(score);
        c.setPriority(score >= 70 ? "CRITICAL" : score >= 45 ? "HIGH" : score >= 20 ? "MEDIUM" : "LOW");
    }
}
