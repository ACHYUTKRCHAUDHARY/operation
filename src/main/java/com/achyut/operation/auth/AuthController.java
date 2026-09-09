package com.achyut.operation.auth;

import com.achyut.operation.customer.Customer;
import com.achyut.operation.customer.CustomerRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AppUserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.auth.jwt-expiration-seconds:28800}")
    private long expirationSeconds;
    @Value("${app.auth.secure-cookie:false}")
    private boolean secureCookie;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        AppUser user = AppUser.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(request.password()))
            .fullName(request.fullName().trim())
            .role(AppUser.Role.CUSTOMER)
            .enabled(true)
            .build();
        AppUser saved = userRepository.save(user);

        customerRepository.findByEmailIgnoreCase(email).orElseGet(() -> customerRepository.save(
            Customer.builder()
                .name(saved.getFullName())
                .email(saved.getEmail())
                .build()
        ));

        return new RegisterResponse(saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole().name());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AppUser user = userRepository.findByEmailIgnoreCase(request.email().trim())
            .filter(AppUser::isEnabled)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        String token=jwtService.generate(user);
        ResponseCookie cookie=ResponseCookie.from("operation_token",token).httpOnly(true).secure(secureCookie).sameSite("Strict").path("/").maxAge(expirationSeconds).build();
        response.addHeader(HttpHeaders.SET_COOKIE,cookie.toString());
        return new AuthResponse(token, user.getFullName(), user.getEmail(), user.getRole().name());
    }

    @GetMapping("/me")
    public AuthResponse me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        AppUser user = userRepository.findByEmailIgnoreCase(authentication.getName())
            .filter(AppUser::isEnabled)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is unavailable"));
        return new AuthResponse(null, user.getFullName(), user.getEmail(), user.getRole().name());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletResponse response){
        ResponseCookie cookie=ResponseCookie.from("operation_token","").httpOnly(true).secure(secureCookie).sameSite("Strict").path("/").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE,cookie.toString());
    }

    public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 100) String fullName,
        @Email @NotBlank @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 72) String password
    ) {}
    public record RegisterResponse(Long id, String fullName, String email, String role) {}
    public record LoginRequest(@Email @NotBlank String email, @NotBlank @Size(min = 8) String password) {}
    public record AuthResponse(String token, String fullName, String email, String role) {}
}
