package com.achyut.operation.idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    private final IdempotencyService service;

    @Value("${app.idempotency.ttl-hours:24}")
    private long ttlHours;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String contentType = request.getContentType();
        return !("POST".equals(method) || "PATCH".equals(method))
            || !request.getRequestURI().startsWith("/api/")
            || request.getRequestURI().startsWith("/api/auth/")
            || request.getRequestURI().startsWith("/api/public/")
            || (contentType != null && contentType.startsWith("multipart/"))
            || request.getHeader(IDEMPOTENCY_KEY) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        String key = request.getHeader(IDEMPOTENCY_KEY).trim();
        if (key.isBlank() || key.length() > 200) {
            conflict(response, "Idempotency-Key must contain 1 to 200 characters");
            return;
        }

        byte[] body = request.getInputStream().readAllBytes();
        CachedBodyRequest wrappedRequest = new CachedBodyRequest(request, body);
        String method = request.getMethod();
        String path = request.getRequestURI() + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
        String clientScope = sha256(firstNonBlank(request.getHeader("Authorization"), request.getHeader("Cookie"), request.getRemoteAddr()));
        String fingerprint = sha256(method + "\n" + path + "\n" + new String(body, StandardCharsets.UTF_8));

        IdempotencyService.Reservation reservation = service.reserve(
            key, method, path, clientScope, fingerprint, Duration.ofHours(ttlHours));
        IdempotencyRecord record = reservation.record();

        if (!record.getRequestFingerprint().equals(fingerprint)) {
            conflict(response, "Idempotency-Key was already used with a different request payload");
            return;
        }

        if (!reservation.acquired()) {
            if (record.getState() == IdempotencyRecord.State.COMPLETED) {
                replay(response, record);
            } else {
                response.setHeader("Retry-After", "1");
                conflict(response, "A request with this Idempotency-Key is already processing");
            }
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
            int status = wrappedResponse.getStatus();
            byte[] responseBody = wrappedResponse.getContentAsByteArray();
            if (status >= 200 && status < 300) {
                service.complete(record.getId(), status, wrappedResponse.getContentType(),
                    new String(responseBody, StandardCharsets.UTF_8));
                wrappedResponse.setHeader("Idempotency-Key", key);
            } else {
                service.release(record.getId());
            }
            wrappedResponse.copyBodyToResponse();
        } catch (IOException | ServletException | RuntimeException ex) {
            service.release(record.getId());
            throw ex;
        }
    }

    private void replay(HttpServletResponse response, IdempotencyRecord record) throws IOException {
        response.setStatus(record.getResponseStatus() == null ? 200 : record.getResponseStatus());
        if (record.getResponseContentType() != null) response.setContentType(record.getResponseContentType());
        response.setHeader("Idempotency-Replayed", "true");
        response.setHeader("Idempotency-Key", record.getIdempotencyKey());
        if (record.getResponseBody() != null) response.getWriter().write(record.getResponseBody());
    }

    private void conflict(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message.replace("\"", "\\\"") + "\"}");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "anonymous";
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;
        CachedBodyRequest(HttpServletRequest request, byte[] body) { super(request); this.body = body; }

        @Override public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return input.read(); }
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener readListener) { }
            };
        }
        @Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8)); }
    }
}
