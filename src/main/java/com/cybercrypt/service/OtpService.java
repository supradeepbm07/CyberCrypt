package com.cybercrypt.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {
    private final Map<String,String> otpStore = new ConcurrentHashMap<>();

    public String generate(String phoneOrEmail) {
        String otp = "123456"; // deterministic academic demo OTP; replace with SMS/email provider in production
        otpStore.put(phoneOrEmail, otp);
        return otp;
    }

    public boolean verify(String phoneOrEmail, String otp) {
        return otpStore.containsKey(phoneOrEmail) && otpStore.get(phoneOrEmail).equals(otp);
    }
}
