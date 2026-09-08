package com.achyut.operation.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AppUserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String token = bearer(request).orElseGet(() -> cookie(request, "operation_token").orElse(null));
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String email = jwtService.subject(token);
                userRepository.findByEmailIgnoreCase(email).filter(AppUser::isEnabled).ifPresent(user -> {
                    var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
                    var auth = new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            } catch (RuntimeException ignored) { SecurityContextHolder.clearContext(); }
        }
        chain.doFilter(request, response);
    }

    private Optional<String> bearer(HttpServletRequest request) {
        String header=request.getHeader("Authorization");
        return header!=null&&header.startsWith("Bearer ")?Optional.of(header.substring(7)):Optional.empty();
    }
    private Optional<String> cookie(HttpServletRequest request,String name){
        if(request.getCookies()==null)return Optional.empty();
        return Arrays.stream(request.getCookies()).filter(c->name.equals(c.getName())).map(Cookie::getValue).findFirst();
    }
}
