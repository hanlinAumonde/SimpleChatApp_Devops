package com.devStudy.chatapp.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.devStudy.chatapp.auth.utils.ConstantValues.ATTEMPTS_PREFIX;
import static com.devStudy.chatapp.auth.utils.ConstantValues.CODE_PREFIX;

@Service
public class VerificationCodeService {
    private static final Logger logger = LoggerFactory.getLogger(VerificationCodeService.class);

    private final EmailService emailService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${chatroomApp.redis.expirationTime}")
    private int expirationTime;

    @Autowired
    VerificationCodeService(EmailService emailService, RedisTemplate<String, Object> redisTemplate) {
        this.emailService = emailService;
        this.redisTemplate = redisTemplate;
    }

    public void sendCode(String email) {
        String code = generateRandomCode();
        logger.info("Sending code to {}, code : {}", email, code);

        redisTemplate.opsForValue().set(CODE_PREFIX + email,
                                        code,
                                        expirationTime,
                                        TimeUnit.SECONDS);

        sendEmailWithCode(email, code);
    }

    public boolean validateCode(String email, String code) throws BadCredentialsException {
        String storedCode = (String) redisTemplate.opsForValue().get(CODE_PREFIX + email);
        if (storedCode == null) {
            throw new BadCredentialsException("Il n'y a pas de code valable pour votre compte, veuillez le redemander");
        }
        if (storedCode.equals(code)) {
            invalideteCode(email);
            return true;
        }
        return false;
    }

    public int incrementLoginAttempts(String email) {
        String key = ATTEMPTS_PREFIX + email;
        Long attempts = redisTemplate.opsForValue().increment(key);

        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, expirationTime, TimeUnit.SECONDS);
        }

        return attempts != null ? attempts.intValue() : 1;
    }

    public void invalideteCode(String email) {
        redisTemplate.delete(CODE_PREFIX + email);
        redisTemplate.delete(ATTEMPTS_PREFIX + email);
    }

    private String generateRandomCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    private void sendEmailWithCode(String email, String code) {
        String subject = "Verification Code";
        String body = String.format("""
                      Bonjour,
                      
                        Voici votre code de vérification : %s
                        Ce code est valide pendant 5 minutes.
                        Merci de ne pas le partager avec qui que ce soit.
                      
                      Cordialement,
                      Votre équipe de support
                      """, code);
        emailService.sendSimpleMessage(email, subject, body);
    }
}