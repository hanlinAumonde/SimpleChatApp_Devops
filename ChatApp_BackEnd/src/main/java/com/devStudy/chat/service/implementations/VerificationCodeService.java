package com.devStudy.chat.service.implementations;

import com.devStudy.chat.dao.UserRepository;
import com.devStudy.chat.model.User;
import com.devStudy.chat.service.interfaces.VerificationCodeServiceInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationCodeService implements VerificationCodeServiceInt {
    private static final Logger logger = LoggerFactory.getLogger(VerificationCodeService.class);
    // 依赖用户存储库来获取用户信息
    private UserRepository userRepository;
    private EmailService emailService;

    private final Map<String, VerificationCodeInfo> codeStorage = new ConcurrentHashMap<>();
    private final Map<String, Integer> attemptsStorage = new ConcurrentHashMap<>();

    @Autowired
    VerificationCodeService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    public void sendCode(String email) {
        String code = generateRandomCode();
        logger.info("Sending code to {}, code : {}", email, code);

        codeStorage.put(email, new VerificationCodeInfo(code, System.currentTimeMillis() + 300000));

        sendEmailWithCode(email, code);
    }

    @Override
    public boolean validateCode(String email, String code) throws BadCredentialsException {
        VerificationCodeInfo storedInfo = codeStorage.get(email);
        if (storedInfo == null) {
            throw new BadCredentialsException("Il n'y a pas de code valable pour votre compte, veuillez le redemander");
        }
        if (System.currentTimeMillis() > storedInfo.expiryTime) {
            invalideteCode(email);
            throw new BadCredentialsException("Le code a expiré, veuillez le redemander");
        }
        if (storedInfo.code.equals(code)) {
            invalideteCode(email);
            return true;
        }
        return false;
    }

    @Override
    public int incrementLoginAttempts(String email) {
        return attemptsStorage.merge(email, 1, Integer::sum);
    }

    @Override
    public void invalideteCode(String email) {
        codeStorage.remove(email);
        attemptsStorage.remove(email);
    }

    private String generateRandomCode() {
        return String.format("%06d", new Random().nextInt(1000000)); // 生成6位随机数
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

    private static class VerificationCodeInfo {
        private final String code;
        private final long expiryTime;

        public VerificationCodeInfo(String code, long expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
        }
    }
}
