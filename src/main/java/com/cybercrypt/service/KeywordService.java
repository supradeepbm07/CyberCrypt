package com.cybercrypt.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class KeywordService {
    private final List<String> keywords = List.of(
        "urgent", "threat", "blackmail", "ransom", "ransomware",
        "weapon", "suicide", "terror", "otp", "password", "stolen",
        "extortion", "malware", "phishing", "investment scam"
    );

    public List<String> detect(String text) {
        if (text == null) return List.of();
        String lower = text.toLowerCase(Locale.ROOT);
        return keywords.stream().filter(lower::contains).distinct().toList();
    }
}
