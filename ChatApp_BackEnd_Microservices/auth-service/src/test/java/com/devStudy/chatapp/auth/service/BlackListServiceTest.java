package com.devStudy.chatapp.auth.service;

import com.devStudy.chatapp.auth.service.Implementation.BlackListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static com.devStudy.chatapp.auth.utils.ConstantValues.BLACKLIST_PREFIX;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlackListServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private BlackListService blackListService;

    private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";
    private static final String BLACKLIST_KEY = BLACKLIST_PREFIX + TEST_TOKEN;

    @BeforeEach
    void setUp() {
        // setUp方法保持简单，避免不必要的stubbing
    }

    @Test
    void testAddTokenToBlackList_ValidTTL_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime + 3600000; // 1小时后过期
        long expectedTTL = expirationTime - currentTime;

        blackListService.addTokenToBlackList(TEST_TOKEN, expirationTime);

        verify(valueOperations).set(
                eq(BLACKLIST_KEY),
                eq("blackListedToken"),
                longThat(ttl -> Math.abs(ttl - expectedTTL) < 1000), // 允许1秒误差
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void testAddTokenToBlackList_ExpiredToken_NoAction() {
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime - 3600000; // 1小时前已过期

        blackListService.addTokenToBlackList(TEST_TOKEN, expirationTime);

        // 过期的token不应该被添加到黑名单
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testAddTokenToBlackList_ZeroTTL_NoAction() {
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime; // 当前时间，TTL为0

        blackListService.addTokenToBlackList(TEST_TOKEN, expirationTime);

        // TTL为0的token不应该被添加到黑名单
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testAddTokenToBlackList_NegativeTTL_NoAction() {
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime - 1000; // 1秒前过期

        blackListService.addTokenToBlackList(TEST_TOKEN, expirationTime);

        // 负TTL的token不应该被添加到黑名单
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testAddTokenToBlackList_VeryShortTTL_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime + 100; // 100毫秒后过期
        long expectedTTL = 100;

        blackListService.addTokenToBlackList(TEST_TOKEN, expirationTime);

        verify(valueOperations).set(
                eq(BLACKLIST_KEY),
                eq("blackListedToken"),
                longThat(ttl -> Math.abs(ttl - expectedTTL) < 50), // 允许50毫秒误差
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void testIsTokenInBlackList_TokenExists_ReturnsTrue() {
        when(redisTemplate.hasKey(BLACKLIST_KEY)).thenReturn(true);

        boolean result = blackListService.isTokenInBlackList(TEST_TOKEN);

        assertTrue(result);
        verify(redisTemplate).hasKey(BLACKLIST_KEY);
    }

    @Test
    void testIsTokenInBlackList_TokenNotExists_ReturnsFalse() {
        when(redisTemplate.hasKey(BLACKLIST_KEY)).thenReturn(false);

        boolean result = blackListService.isTokenInBlackList(TEST_TOKEN);

        assertFalse(result);
        verify(redisTemplate).hasKey(BLACKLIST_KEY);
    }

    @Test
    void testIsTokenInBlackList_RedisReturnsNull_ThrowsNullPointerException() {
        when(redisTemplate.hasKey(BLACKLIST_KEY)).thenReturn(null);

        // 实际上会抛出NullPointerException，因为代码没有处理null情况
        // 这个测试揭示了代码中的一个潜在bug
        assertThrows(
                NullPointerException.class,
                () -> blackListService.isTokenInBlackList(TEST_TOKEN)
        );

        verify(redisTemplate).hasKey(BLACKLIST_KEY);
    }

    @Test
    void testAddTokenToBlackList_NullToken_HandlesGracefully() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime + 3600000;

        // 测试null token
        blackListService.addTokenToBlackList(null, expirationTime);

        verify(valueOperations).set(
                eq(BLACKLIST_PREFIX + null), // 会变成 "token:blacklist:null"
                eq("blackListedToken"),
                anyLong(),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void testIsTokenInBlackList_NullToken_HandlesGracefully() {
        when(redisTemplate.hasKey(BLACKLIST_PREFIX + null)).thenReturn(false);

        boolean result = blackListService.isTokenInBlackList(null);

        assertFalse(result);
        verify(redisTemplate).hasKey(BLACKLIST_PREFIX + null);
    }

    @Test
    void testAddTokenToBlackList_EmptyToken_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        String emptyToken = "";
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime + 3600000;

        blackListService.addTokenToBlackList(emptyToken, expirationTime);

        verify(valueOperations).set(
                eq(BLACKLIST_PREFIX + emptyToken),
                eq("blackListedToken"),
                anyLong(),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void testIsTokenInBlackList_EmptyToken_ReturnsFalse() {
        String emptyToken = "";
        when(redisTemplate.hasKey(BLACKLIST_PREFIX + emptyToken)).thenReturn(false);

        boolean result = blackListService.isTokenInBlackList(emptyToken);

        assertFalse(result);
        verify(redisTemplate).hasKey(BLACKLIST_PREFIX + emptyToken);
    }

    @Test
    void testMultipleTokens_DifferentBlacklistStatus() {
        String token1 = "token1";
        String token2 = "token2";
        
        when(redisTemplate.hasKey(BLACKLIST_PREFIX + token1)).thenReturn(true);
        when(redisTemplate.hasKey(BLACKLIST_PREFIX + token2)).thenReturn(false);

        assertTrue(blackListService.isTokenInBlackList(token1));
        assertFalse(blackListService.isTokenInBlackList(token2));
    }

    @Test
    void testRedisException_HandleGracefully() {
        when(redisTemplate.hasKey(BLACKLIST_KEY))
                .thenThrow(new RuntimeException("Redis connection error"));

        assertThrows(
                RuntimeException.class,
                () -> blackListService.isTokenInBlackList(TEST_TOKEN)
        );
    }

    @Test
    void testAddTokenToBlackList_RedisException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime + 3600000;

        doThrow(new RuntimeException("Redis connection error"))
                .when(valueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));

        assertThrows(
                RuntimeException.class,
                () -> blackListService.addTokenToBlackList(TEST_TOKEN, expirationTime)
        );
    }

    @Test
    void testBlacklistKey_CorrectPrefix() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime + 3600000;

        blackListService.addTokenToBlackList(TEST_TOKEN, expirationTime);

        // 验证使用了正确的前缀
        verify(valueOperations).set(
                eq(BLACKLIST_PREFIX + TEST_TOKEN),
                anyString(),
                anyLong(),
                any(TimeUnit.class)
        );
    }

    @Test
    void testBlacklistValue_CorrectConstant() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime + 3600000;

        blackListService.addTokenToBlackList(TEST_TOKEN, expirationTime);

        // 验证使用了正确的值
        verify(valueOperations).set(
                anyString(),
                eq("blackListedToken"),
                anyLong(),
                any(TimeUnit.class)
        );
    }

    @Test
    void testTimeUnit_IsMilliseconds() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime + 3600000;

        blackListService.addTokenToBlackList(TEST_TOKEN, expirationTime);

        // 验证使用了毫秒作为时间单位
        verify(valueOperations).set(
                anyString(),
                anyString(),
                anyLong(),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void testTTL_Calculation_Precision() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime + 123456; // 特定的TTL值

        blackListService.addTokenToBlackList(TEST_TOKEN, expirationTime);

        verify(valueOperations).set(
                eq(BLACKLIST_KEY),
                eq("blackListedToken"),
                longThat(ttl -> Math.abs(ttl - 123456) < 100), // 允许100毫秒误差
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void testLongRunningToken_LargeTTL() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime + (30L * 24 * 60 * 60 * 1000); // 30天后过期
        long expectedTTL = 30L * 24 * 60 * 60 * 1000;

        blackListService.addTokenToBlackList(TEST_TOKEN, expirationTime);

        verify(valueOperations).set(
                eq(BLACKLIST_KEY),
                eq("blackListedToken"),
                longThat(ttl -> Math.abs(ttl - expectedTTL) < 1000),
                eq(TimeUnit.MILLISECONDS)
        );
    }
}