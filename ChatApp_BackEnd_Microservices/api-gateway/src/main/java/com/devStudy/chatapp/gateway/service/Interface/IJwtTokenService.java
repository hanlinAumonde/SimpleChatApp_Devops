package com.devStudy.chatapp.gateway.service.Interface;

import org.springframework.web.server.ServerWebExchange;
import java.util.Date;

public interface IJwtTokenService {
    public String validateTokenAndGetEmail(String token);
    public Date getExpirationDate(String token);
    public String getTokenFromRequest(ServerWebExchange exchange);
}
