package com.devStudy.chatapp.auth.utils;

import com.devStudy.chatapp.auth.service.EmailService;
import com.devStudy.chatapp.auth.service.JwtTokenService;
import com.devStudy.chatapp.auth.service.UserService;
import com.devStudy.chatapp.auth.service.VerificationCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.devStudy.chatapp.auth.utils.ConstantValues.*;
import com.devStudy.chatapp.auth.model.User;

@Component
public class RabbitMQUtil {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQUtil.class);

    @Value("${chatroomApp.FrontEndURL:http://localhost:4200}")
    private String FrontEndURL;

    private final RabbitTemplate rabbitTemplate;
    private final UserService userService;
    private final EmailService emailService;
    private final JwtTokenService tokenService;
    private final VerificationCodeService verificationCodeService;

    @Autowired
    public RabbitMQUtil(RabbitTemplate rabbitTemplate, 
                       UserService userService, 
                       EmailService emailService, 
                       JwtTokenService tokenService, 
                       VerificationCodeService verificationCodeService) {
        this.rabbitTemplate = rabbitTemplate;
        this.userService = userService;
        this.emailService = emailService;
        this.tokenService = tokenService;
        this.verificationCodeService = verificationCodeService;
    }

    public Map<String,String> sendResetPwdEmailRequestToMQ(String email){
        Map<String,String> map = new HashMap<>();
        try{
            rabbitTemplate.send(RABBITMQ_EXCHANGE_NAME, ROUTING_KEY_RET_PASSWORD, new Message(email.getBytes(StandardCharsets.UTF_8)));
            map.put("status","request-sent");
            map.put("msg", """
                            Demande envoyée, si vous avez un compte avec cet email,
                            vous recevrez un email de réinitialisation de mot de passe
                            Veuillez reessayer dans 60s si vous n'avez pas reçu l'email
                          """);
        }catch (Exception e){
            logger.error("sendResetPwdEmailRequestToMQ error",e);
            map.put("status","error");
            map.put("msg","Erreur lors de l'envoi de la demande de réinitialisation de mot de passe");
        }
        return map;
    }

    public String sendVerificationCodeRequestToMQ(String email){
        try{
            rabbitTemplate.send(RABBITMQ_EXCHANGE_NAME, ROUTING_KEY_VERIFICATION_CODE, new Message(email.getBytes(StandardCharsets.UTF_8)));
        }catch (Exception e){
            logger.error("sendVerificationCodeRequestToMQ error",e);
        }
        return "Le code de vérification a été envoyé à votre adresse email, si vous n'avez pas reçu l'email, veuillez réessayer dans 60 secondes";
    }

    @RabbitListener(queues = RABBITMQ_QUEUE_Q1, concurrency = "1-3")
    public void sendResetPasswordEmail(String email) {
        try {
            Optional<User> user = userService.findUserOrAdmin(email, false);
            if(user.isPresent()) {
                String jwtToken = tokenService.generateJwtToken(email, TOKEN_FLAG_RESET_PASSWORD);
                String ResetPasswordLink = String.format("%s/reset-password?token=%s", FrontEndURL, jwtToken);
                logger.info("Reset Password Link : {}", ResetPasswordLink);
                String subject = "Reset Password";
                String content = String.format(
                        """
                                Bonjour,
                                
                                Cliquer sur le lien ci-dessous pour réinitialiser votre mot de passe :
                                %s
                                
                                Attention : ce lien n'est valide que pendant une demi-heure
                                
                                Bien cordialement,
                                Chat Team"""
                        , ResetPasswordLink);
                emailService.sendSimpleMessage(email, subject, content);
            }
        }catch (MailException mailException) {
            logger.error("Error while sending email to {}", email, mailException);
        }
    }

    @RabbitListener(queues = RABBITMQ_QUEUE_Q2, concurrency = "1-5")
    public void sendVerificationCodeEmail(String email) {
        verificationCodeService.sendCode(email);
    }
}