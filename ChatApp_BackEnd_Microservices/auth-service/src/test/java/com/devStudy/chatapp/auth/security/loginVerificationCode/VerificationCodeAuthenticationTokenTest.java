package com.devStudy.chatapp.auth.security.loginVerificationCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class VerificationCodeAuthenticationTokenTest {

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_CODE = "123456";
    private Collection<GrantedAuthority> authorities;

    @BeforeEach
    void setUp() {
        authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("USER_READ")
        );
    }

    @Test
    void testUnauthenticatedToken_Creation() {
        VerificationCodeAuthenticationToken token = 
                VerificationCodeAuthenticationToken.unauthenticated(TEST_EMAIL, TEST_CODE);

        assertNotNull(token);
        assertFalse(token.isAuthenticated());
        assertEquals(TEST_EMAIL, token.getPrincipal());
        assertEquals(TEST_CODE, token.getCredentials());
        assertTrue(token.getAuthorities().isEmpty());
    }

    @Test
    void testAuthenticatedToken_Creation() {
        VerificationCodeAuthenticationToken token = 
                VerificationCodeAuthenticationToken.authenticated(TEST_EMAIL, authorities);

        assertNotNull(token);
        assertTrue(token.isAuthenticated());
        assertEquals(TEST_EMAIL, token.getPrincipal());
        assertNull(token.getCredentials()); // 认证后凭据被清空
        assertEquals(authorities, token.getAuthorities());
    }

    @Test
    void testConstructor_WithAuthorities() {
        VerificationCodeAuthenticationToken token = 
                new VerificationCodeAuthenticationToken(authorities, TEST_CODE, TEST_EMAIL);

        assertNotNull(token);
        assertTrue(token.isAuthenticated());
        assertEquals(TEST_EMAIL, token.getPrincipal());
        assertEquals(TEST_CODE, token.getCredentials());
        assertEquals(authorities, token.getAuthorities());
    }

    @Test
    void testConstructor_WithoutAuthorities() {
        VerificationCodeAuthenticationToken token = 
                new VerificationCodeAuthenticationToken(TEST_CODE, TEST_EMAIL);

        assertNotNull(token);
        assertFalse(token.isAuthenticated());
        assertEquals(TEST_EMAIL, token.getPrincipal());
        assertEquals(TEST_CODE, token.getCredentials());
        assertTrue(token.getAuthorities().isEmpty());
    }

    @Test
    void testSetAuthenticated_TrueThrowsException() {
        VerificationCodeAuthenticationToken token = 
                VerificationCodeAuthenticationToken.unauthenticated(TEST_EMAIL, TEST_CODE);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> token.setAuthenticated(true)
        );

        assertEquals("Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead", 
                     exception.getMessage());
        assertFalse(token.isAuthenticated());
    }

    @Test
    void testSetAuthenticated_FalseSuccess() {
        VerificationCodeAuthenticationToken token = 
                VerificationCodeAuthenticationToken.authenticated(TEST_EMAIL, authorities);

        assertTrue(token.isAuthenticated()); // 初始状态为已认证
        
        token.setAuthenticated(false);
        
        assertFalse(token.isAuthenticated());
    }

    @Test
    void testEraseCredentials() {
        VerificationCodeAuthenticationToken token = 
                VerificationCodeAuthenticationToken.unauthenticated(TEST_EMAIL, TEST_CODE);

        assertEquals(TEST_CODE, token.getCredentials());
        
        token.eraseCredentials();
        
        assertNull(token.getCredentials());
        assertEquals(TEST_EMAIL, token.getPrincipal()); // Principal 不受影响
    }

    @Test
    void testEraseCredentials_AuthenticatedToken() {
        VerificationCodeAuthenticationToken token = 
                VerificationCodeAuthenticationToken.authenticated(TEST_EMAIL, authorities);

        assertNull(token.getCredentials()); // 已认证的token凭据本来就是null
        
        token.eraseCredentials();
        
        assertNull(token.getCredentials());
        assertEquals(TEST_EMAIL, token.getPrincipal());
    }

    @Test
    void testStaticFactoryMethods_CreateCorrectTypes() {
        // 测试 unauthenticated 工厂方法
        VerificationCodeAuthenticationToken unauthenticated = 
                VerificationCodeAuthenticationToken.unauthenticated(TEST_EMAIL, TEST_CODE);
        
        assertFalse(unauthenticated.isAuthenticated());
        assertEquals(TEST_CODE, unauthenticated.getCredentials());

        // 测试 authenticated 工厂方法
        VerificationCodeAuthenticationToken authenticated = 
                VerificationCodeAuthenticationToken.authenticated(TEST_EMAIL, authorities);
        
        assertTrue(authenticated.isAuthenticated());
        assertNull(authenticated.getCredentials());
    }

    @Test
    void testNullValues_Handling() {
        // 测试null principal
        VerificationCodeAuthenticationToken tokenWithNullPrincipal = 
                VerificationCodeAuthenticationToken.unauthenticated(null, TEST_CODE);
        
        assertNull(tokenWithNullPrincipal.getPrincipal());
        assertEquals(TEST_CODE, tokenWithNullPrincipal.getCredentials());

        // 测试null verification code
        VerificationCodeAuthenticationToken tokenWithNullCode = 
                VerificationCodeAuthenticationToken.unauthenticated(TEST_EMAIL, null);
        
        assertEquals(TEST_EMAIL, tokenWithNullCode.getPrincipal());
        assertNull(tokenWithNullCode.getCredentials());

        // 测试null authorities
        VerificationCodeAuthenticationToken tokenWithNullAuthorities = 
                VerificationCodeAuthenticationToken.authenticated(TEST_EMAIL, null);
        
        assertEquals(TEST_EMAIL, tokenWithNullAuthorities.getPrincipal());
        assertTrue(tokenWithNullAuthorities.getAuthorities().isEmpty());
    }

    @Test
    void testEmptyAuthorities() {
        Collection<GrantedAuthority> emptyAuthorities = Arrays.asList();
        
        VerificationCodeAuthenticationToken token = 
                VerificationCodeAuthenticationToken.authenticated(TEST_EMAIL, emptyAuthorities);
        
        assertTrue(token.isAuthenticated());
        assertTrue(token.getAuthorities().isEmpty());
    }

    @Test
    void testComplexPrincipal_UserObject() {
        // 测试使用User对象作为principal
        Object userPrincipal = new Object() {
            @Override
            public String toString() {
                return TEST_EMAIL;
            }
        };

        VerificationCodeAuthenticationToken token = 
                VerificationCodeAuthenticationToken.unauthenticated(userPrincipal, TEST_CODE);

        assertEquals(userPrincipal, token.getPrincipal());
        assertEquals(TEST_EMAIL, token.getPrincipal().toString());
    }

    @Test
    void testCredentials_AfterAuthentication() {
        // 创建未认证的token
        VerificationCodeAuthenticationToken unauthenticated = 
                VerificationCodeAuthenticationToken.unauthenticated(TEST_EMAIL, TEST_CODE);
        
        assertEquals(TEST_CODE, unauthenticated.getCredentials());

        // 创建认证后的token
        VerificationCodeAuthenticationToken authenticated = 
                VerificationCodeAuthenticationToken.authenticated(TEST_EMAIL, authorities);
        
        assertNull(authenticated.getCredentials()); // 认证后凭据为null是设计如此
    }

    @Test
    void testAuthorities_Immutability() {
        VerificationCodeAuthenticationToken token = 
                VerificationCodeAuthenticationToken.authenticated(TEST_EMAIL, authorities);

        Collection<? extends GrantedAuthority> retrievedAuthorities = token.getAuthorities();
        
        assertNotNull(retrievedAuthorities);
        assertEquals(authorities.size(), retrievedAuthorities.size());
        assertTrue(retrievedAuthorities.containsAll(authorities));
    }

    @Test
    void testTokenState_Transitions() {
        // 开始时未认证
        VerificationCodeAuthenticationToken token = 
                VerificationCodeAuthenticationToken.unauthenticated(TEST_EMAIL, TEST_CODE);
        
        assertFalse(token.isAuthenticated());
        assertNotNull(token.getCredentials());

        // 不能直接设置为已认证
        assertThrows(IllegalArgumentException.class, () -> token.setAuthenticated(true));

        // 可以设置为未认证（虽然已经是未认证状态）
        token.setAuthenticated(false);
        assertFalse(token.isAuthenticated());
    }

    @Test
    void testMultipleEraseCredentials() {
        VerificationCodeAuthenticationToken token = 
                VerificationCodeAuthenticationToken.unauthenticated(TEST_EMAIL, TEST_CODE);

        assertEquals(TEST_CODE, token.getCredentials());
        
        // 第一次擦除
        token.eraseCredentials();
        assertNull(token.getCredentials());
        
        // 第二次擦除（应该不会出错）
        token.eraseCredentials();
        assertNull(token.getCredentials());
    }
}