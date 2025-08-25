package com.devStudy.chatapp.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static com.devStudy.chatapp.auth.utils.ConstantValues.ATTEMPTS_PREFIX;
import static com.devStudy.chatapp.auth.utils.ConstantValues.CODE_PREFIX;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private VerificationCodeService verificationCodeService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final int EXPIRATION_TIME = 300; // 5分钟

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(verificationCodeService, "expirationTime", EXPIRATION_TIME);
    }

    @Test
    void testSendCode_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // 执行测试
        verificationCodeService.sendCode(TEST_EMAIL);

        // 验证Redis存储操作
        verify(valueOperations).set(
                eq(CODE_PREFIX + TEST_EMAIL),
                anyString(),
                eq((long)EXPIRATION_TIME),
                eq(TimeUnit.SECONDS)
        );

        // 验证邮件发送
        verify(emailService).sendSimpleMessage(
                eq(TEST_EMAIL),
                eq("Verification Code"),
                anyString()
        );
    }

    @Test
    void testSendCode_EmailServiceCalled() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        verificationCodeService.sendCode(TEST_EMAIL);

        // 验证邮件服务被调用，并且邮件内容包含验证码
        verify(emailService).sendSimpleMessage(
                eq(TEST_EMAIL),
                eq("Verification Code"),
                argThat(body -> body.contains("Voici votre code de vérification"))
        );
    }

    @Test
    void testValidateCode_CorrectCode_Success() {
        String testCode = "123456";
        when(valueOperations.get(CODE_PREFIX + TEST_EMAIL)).thenReturn(testCode);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        boolean result = verificationCodeService.validateCode(TEST_EMAIL, testCode);

        assertTrue(result);
        // 验证代码被删除
        verify(redisTemplate).delete(CODE_PREFIX + TEST_EMAIL);
        verify(redisTemplate).delete(ATTEMPTS_PREFIX + TEST_EMAIL);
    }

    @Test
    void testValidateCode_IncorrectCode_False() {
        String storedCode = "123456";
        String inputCode = "654321";
        when(valueOperations.get(CODE_PREFIX + TEST_EMAIL)).thenReturn(storedCode);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        boolean result = verificationCodeService.validateCode(TEST_EMAIL, inputCode);

        assertFalse(result);
        // 验证代码没有被删除
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void testValidateCode_NoCodeStored_ThrowsException() {
        when(valueOperations.get(CODE_PREFIX + TEST_EMAIL)).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> verificationCodeService.validateCode(TEST_EMAIL, "123456")
        );

        assertEquals("Il n'y a pas de code valable pour votre compte, veuillez le redemander", 
                     exception.getMessage());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void testValidateCode_EmptyCode_False() {
        String storedCode = "123456";
        when(valueOperations.get(CODE_PREFIX + TEST_EMAIL)).thenReturn(storedCode);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        boolean result = verificationCodeService.validateCode(TEST_EMAIL, "");

        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void testValidateCode_NullCode_False() {
        String storedCode = "123456";
        when(valueOperations.get(CODE_PREFIX + TEST_EMAIL)).thenReturn(storedCode);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        boolean result = verificationCodeService.validateCode(TEST_EMAIL, null);

        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void testIncrementLoginAttempts_FirstAttempt() {
        String key = ATTEMPTS_PREFIX + TEST_EMAIL;
        when(valueOperations.increment(key)).thenReturn(1L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        int result = verificationCodeService.incrementLoginAttempts(TEST_EMAIL);

        assertEquals(1, result);
        verify(valueOperations).increment(key);
        verify(redisTemplate).expire(key, EXPIRATION_TIME, TimeUnit.SECONDS);
    }

    @Test
    void testIncrementLoginAttempts_SubsequentAttempts() {
        String key = ATTEMPTS_PREFIX + TEST_EMAIL;
        when(valueOperations.increment(key)).thenReturn(3L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        int result = verificationCodeService.incrementLoginAttempts(TEST_EMAIL);

        assertEquals(3, result);
        verify(valueOperations).increment(key);
        // 只有第一次尝试时才设置过期时间
        verify(redisTemplate, never()).expire(eq(key), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testIncrementLoginAttempts_NullReturn() {
        String key = ATTEMPTS_PREFIX + TEST_EMAIL;
        when(valueOperations.increment(key)).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        int result = verificationCodeService.incrementLoginAttempts(TEST_EMAIL);

        assertEquals(1, result);
        verify(valueOperations).increment(key);
    }

    @Test
    void testInvalidateCode_Success() {
        verificationCodeService.invalideteCode(TEST_EMAIL);

        verify(redisTemplate).delete(CODE_PREFIX + TEST_EMAIL);
        verify(redisTemplate).delete(ATTEMPTS_PREFIX + TEST_EMAIL);
    }

    @Test
    void testGenerateRandomCode_Format() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // 通过反射测试private方法
        verificationCodeService.sendCode(TEST_EMAIL);

        // 验证存储的验证码格式正确（6位数字）
        verify(valueOperations).set(
                eq(CODE_PREFIX + TEST_EMAIL),
                argThat(code -> {
                    String codeStr = code.toString();
                    return codeStr.matches("\\d{6}") && codeStr.length() == 6;
                }),
                eq((long)EXPIRATION_TIME),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void testEmailContent_ContainsRequiredElements() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        verificationCodeService.sendCode(TEST_EMAIL);

        verify(emailService).sendSimpleMessage(
                eq(TEST_EMAIL),
                eq("Verification Code"),
                argThat(body -> {
                    return body.contains("Bonjour") &&
                           body.contains("code de vérification") &&
                           body.contains("5 minutes") &&
                           body.contains("Cordialement");
                })
        );
    }

    @Test
    void testIncrementLoginAttempts_HighAttemptCount() {
        String key = ATTEMPTS_PREFIX + TEST_EMAIL;
        when(valueOperations.increment(key)).thenReturn(10L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        int result = verificationCodeService.incrementLoginAttempts(TEST_EMAIL);

        assertEquals(10, result);
        verify(valueOperations).increment(key);
        // 确保高次数尝试时不重新设置过期时间
        verify(redisTemplate, never()).expire(eq(key), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testSendCode_EmailServiceException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Email service unavailable"))
                .when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());

        // 验证即使邮件发送失败，Redis操作仍然完成
        assertThrows(RuntimeException.class, () -> verificationCodeService.sendCode(TEST_EMAIL));

        verify(valueOperations).set(
                eq(CODE_PREFIX + TEST_EMAIL),
                anyString(),
                eq((long)EXPIRATION_TIME),
                eq(TimeUnit.SECONDS)
        );
    }
}
