package com.devStudy.chatapp.auth.actuator;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthServiceInfoContributor implements InfoContributor {
    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> serviceInfo = new HashMap<>();
        serviceInfo.put("name", "Authentication Service");
        serviceInfo.put("description", "Handles user authentication and JWT management");
        serviceInfo.put("version", "1.0.0");
        serviceInfo.put("uptime", LocalDateTime.now().toString());

        builder.withDetail("service", serviceInfo);
    }
}