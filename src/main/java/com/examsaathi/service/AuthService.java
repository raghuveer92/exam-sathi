package com.examsaathi.service;

import com.examsaathi.dto.request.LoginRequest;
import com.examsaathi.dto.request.RegisterRequest;
import com.examsaathi.dto.response.AuthResponse;
import com.examsaathi.dto.response.UserResponse;
import com.examsaathi.entity.Role;
import com.examsaathi.entity.User;
import com.examsaathi.exception.BadRequestException;
import com.examsaathi.repository.RoleRepository;
import com.examsaathi.repository.UserRepository;
import com.examsaathi.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication service — register, login, and token management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;

    /** Register a new student */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered: " + request.getEmail());
        }

        Role studentRole = roleRepository.findByName(Role.RoleName.ROLE_STUDENT)
            .orElseThrow(() -> new IllegalStateException("ROLE_STUDENT not found in DB. Run DataInitializer."));

        User user = User.builder()
            .fullName(request.getFullName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .phone(request.getPhone())
            .roles(Set.of(studentRole))
            .build();

        userRepository.save(user);
        log.info("New student registered: {}", user.getEmail());

        String token = tokenProvider.generateToken(user.getEmail());
        return buildAuthResponse(token, user);
    }

    /** Login with email and password */
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow();

        String token = tokenProvider.generateToken(auth);
        return buildAuthResponse(token, user);
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
            .accessToken(token)
            .tokenType("Bearer")
            .expiresIn(tokenProvider.getExpirationMs())
            .user(userMapper.toResponse(user))
            .build();
    }
}
