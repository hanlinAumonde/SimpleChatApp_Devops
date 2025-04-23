package com.devStudy.chat.service.implementations;

import com.devStudy.chat.service.interfaces.VerificationCodeServiceInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.devStudy.chat.service.utils.ConstantValues.ATTEMPTS_PREFIX;
import static com.devStudy.chat.service.utils.ConstantValues.CODE_PREFIX;

@Service
public class VerificationCodeService implements VerificationCodeServiceInt {
    private static final Logger logger = LoggerFactory.getLogger(VerificationCodeService.class);

    private final EmailService emailService;

    //private final Map<String, VerificationCodeInfo> codeStorage = new ConcurrentHashMap<>();
    //private final Map<String, Integer> attemptsStorage = new ConcurrentHashMap<>();

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${chatroomApp.redis.expirationTime}")
    private int expirationTime;

    @Autowired
    VerificationCodeService(EmailService emailService, RedisTemplate<String, Object> redisTemplate) {
        this.emailService = emailService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void sendCode(String email) {
        String code = generateRandomCode();
        logger.info("Sending code to {}, code : {}", email, code);

        //codeStorage.put(email, new VerificationCodeInfo(code, System.currentTimeMillis() + 300000));
        redisTemplate.opsForValue().set(CODE_PREFIX + email,
                                        code,
                                        expirationTime,
                                        TimeUnit.SECONDS);

        sendEmailWithCode(email, code);
    }

    @Override
    public boolean validateCode(String email, String code) throws BadCredentialsException {
        //VerificationCodeInfo storedInfo = codeStorage.get(email);
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

    @Override
    public int incrementLoginAttempts(String email) {
        //return attemptsStorage.merge(email, 1, Integer::sum);
        String key = ATTEMPTS_PREFIX + email;
        Long attempts = redisTemplate.opsForValue().increment(key);

        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, expirationTime, TimeUnit.SECONDS);
        }

        return attempts != null ? attempts.intValue() : 1;
    }

    @Override
    public void invalideteCode(String email) {
        //codeStorage.remove(email);
        //attemptsStorage.remove(email);
        redisTemplate.delete(CODE_PREFIX + email);
        redisTemplate.delete(ATTEMPTS_PREFIX + email);
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
}
