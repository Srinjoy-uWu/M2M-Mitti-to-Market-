package com.mitti2market.controller;

import com.mitti2market.config.JwtUtil;
import com.mitti2market.config.TokenService;
import com.mitti2market.dto.AuthResponse;
import com.mitti2market.dto.LoginRequest;
import com.mitti2market.model.User;
import com.mitti2market.repository.BusinessProfileRepository;
import com.mitti2market.repository.FarmerProfileRepository;
import com.mitti2market.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class AuthLoginTest {

    private UserRepository userRepository;
    private FarmerProfileRepository farmerProfiles;
    private BusinessProfileRepository businessProfiles;
    private PasswordEncoder passwordEncoder;
    private TokenService tokenService;
    private AuthController authController;

    private User farmer;
    private User business;
    private User admin;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        farmerProfiles = Mockito.mock(FarmerProfileRepository.class);
        businessProfiles = Mockito.mock(BusinessProfileRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();

        JwtUtil jwtUtil = new JwtUtil("M2MSecureJwtSecretKeyForDevelopmentTestingOnly2026MittiToMarket", 86400000L, 604800000L);
        tokenService = new TokenService(jwtUtil, userRepository);

        authController = new AuthController(userRepository, tokenService, passwordEncoder, farmerProfiles, businessProfiles);

        farmer = User.builder()
                .id(101L)
                .name("Ramesh Kumar")
                .email("ramesh@farmer.com")
                .phone("9876543210")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.FARMER)
                .status(User.UserStatus.ACTIVE)
                .build();

        business = User.builder()
                .id(201L)
                .name("FreshMart Agro Procurement")
                .email("procurement@freshmart.com")
                .phone("9876543220")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.BUSINESS)
                .status(User.UserStatus.ACTIVE)
                .build();

        admin = User.builder()
                .id(301L)
                .name("System Admin")
                .email("admin@mitti2market.com")
                .phone("9999999999")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.ADMIN)
                .status(User.UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(101L)).thenReturn(Optional.of(farmer));
        when(userRepository.findById(201L)).thenReturn(Optional.of(business));
        when(userRepository.findById(301L)).thenReturn(Optional.of(admin));
    }

    @Test
    void testFarmerLoginWithDirectEmail() {
        when(userRepository.findByEmailIgnoreCase("ramesh@farmer.com")).thenReturn(Optional.of(farmer));
        when(userRepository.findByEmail("ramesh@farmer.com")).thenReturn(Optional.of(farmer));

        LoginRequest req = new LoginRequest();
        req.setEmail("ramesh@farmer.com");
        req.setPassword("password123");

        ResponseEntity<?> res = authController.farmerLogin(req);
        assertEquals(200, res.getStatusCode().value());
        assertTrue(res.getBody() instanceof AuthResponse);
        AuthResponse body = (AuthResponse) res.getBody();
        assertNotNull(body.getToken());
        assertEquals("FARMER", body.getUser().getRole());
    }

    @Test
    void testFarmerLoginWithAliasEmail() {
        // Alias ramesh@example.com maps to ramesh@farmer.com
        when(userRepository.findByEmailIgnoreCase("ramesh@farmer.com")).thenReturn(Optional.of(farmer));

        LoginRequest req = new LoginRequest();
        req.setEmail("ramesh@example.com");
        req.setPassword("password123");

        ResponseEntity<?> res = authController.farmerLogin(req);
        assertEquals(200, res.getStatusCode().value());
        assertTrue(res.getBody() instanceof AuthResponse);
        AuthResponse body = (AuthResponse) res.getBody();
        assertEquals("FARMER", body.getUser().getRole());
    }

    @Test
    void testFarmerLoginWithPhoneNumber() {
        when(userRepository.findByPhone("9876543210")).thenReturn(Optional.of(farmer));

        LoginRequest req = new LoginRequest();
        req.setPhone("9876543210");
        req.setPassword("password123");

        ResponseEntity<?> res = authController.farmerLogin(req);
        assertEquals(200, res.getStatusCode().value());
        assertTrue(res.getBody() instanceof AuthResponse);
    }

    @Test
    void testFarmerLoginWithFormattedPhoneNumber() {
        when(userRepository.findByPhone("9876543210")).thenReturn(Optional.of(farmer));

        LoginRequest req = new LoginRequest();
        req.setEmail("+91 98765 43210");
        req.setPassword("password123");

        ResponseEntity<?> res = authController.farmerLogin(req);
        assertEquals(200, res.getStatusCode().value());
        assertTrue(res.getBody() instanceof AuthResponse);
    }

    @Test
    void testBusinessLoginWithDirectAndAlias() {
        when(userRepository.findByEmailIgnoreCase("procurement@freshmart.com")).thenReturn(Optional.of(business));

        LoginRequest req1 = new LoginRequest();
        req1.setEmail("procurement@freshmart.com");
        req1.setPassword("password123");
        ResponseEntity<?> res1 = authController.businessLogin(req1);
        assertEquals(200, res1.getStatusCode().value());

        LoginRequest req2 = new LoginRequest();
        req2.setEmail("freshmart@example.com");
        req2.setPassword("password123");
        ResponseEntity<?> res2 = authController.businessLogin(req2);
        assertEquals(200, res2.getStatusCode().value());
    }

    @Test
    void testAdminLoginWithPasswordOrAdmin123() {
        when(userRepository.findByEmailIgnoreCase("admin@mitti2market.com")).thenReturn(Optional.of(admin));

        LoginRequest req1 = new LoginRequest();
        req1.setEmail("admin@mitti2market.com");
        req1.setPassword("password123");
        ResponseEntity<?> res1 = authController.adminLogin(req1);
        assertEquals(200, res1.getStatusCode().value());

        LoginRequest req2 = new LoginRequest();
        req2.setEmail("admin@mitti2market.com");
        req2.setPassword("admin123");
        ResponseEntity<?> res2 = authController.adminLogin(req2);
        assertEquals(200, res2.getStatusCode().value());
    }

    @Test
    void testRoleMismatchIsolation() {
        when(userRepository.findByEmailIgnoreCase("ramesh@farmer.com")).thenReturn(Optional.of(farmer));
        when(userRepository.findByEmailIgnoreCase("procurement@freshmart.com")).thenReturn(Optional.of(business));

        // Farmer trying to login via Business portal
        LoginRequest req1 = new LoginRequest();
        req1.setEmail("ramesh@farmer.com");
        req1.setPassword("password123");
        ResponseEntity<?> res1 = authController.businessLogin(req1);
        assertEquals(403, res1.getStatusCode().value());
        Map<?, ?> map1 = (Map<?, ?>) res1.getBody();
        assertEquals("ROLE_MISMATCH", map1.get("code"));

        // Business trying to login via Farmer portal
        LoginRequest req2 = new LoginRequest();
        req2.setEmail("procurement@freshmart.com");
        req2.setPassword("password123");
        ResponseEntity<?> res2 = authController.farmerLogin(req2);
        assertEquals(403, res2.getStatusCode().value());
        Map<?, ?> map2 = (Map<?, ?>) res2.getBody();
        assertEquals("ROLE_MISMATCH", map2.get("code"));
    }

    @Test
    void testInvalidPasswordReturns401() {
        when(userRepository.findByEmailIgnoreCase("ramesh@farmer.com")).thenReturn(Optional.of(farmer));

        LoginRequest req = new LoginRequest();
        req.setEmail("ramesh@farmer.com");
        req.setPassword("wrongpassword");

        ResponseEntity<?> res = authController.farmerLogin(req);
        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void testNonexistentUserReturns401() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setEmail("nonexistent@user.com");
        req.setPassword("password123");

        ResponseEntity<?> res = authController.farmerLogin(req);
        assertEquals(401, res.getStatusCode().value());
    }
}
