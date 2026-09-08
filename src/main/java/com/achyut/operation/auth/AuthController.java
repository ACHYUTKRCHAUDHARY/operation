package com.achyut.operation.auth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.auth.jwt-expiration-seconds:28800}")
    private long expirationSeconds;
    @Value("${app.auth.secure-cookie:false}")
    private boolean secureCookie;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AppUser user = userRepository.findByEmailIgnoreCase(request.email())
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

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletResponse response){
        ResponseCookie cookie=ResponseCookie.from("operation_token","").httpOnly(true).secure(secureCookie).sameSite("Strict").path("/").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE,cookie.toString());
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank @Size(min = 8) String password) {}
    public record AuthResponse(String token, String fullName, String email, String role) {}
}
