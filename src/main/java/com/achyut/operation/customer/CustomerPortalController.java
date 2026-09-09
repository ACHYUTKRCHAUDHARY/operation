package com.achyut.operation.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer-portal")
@RequiredArgsConstructor
public class CustomerPortalController {
    private final CustomerPortalService customerPortalService;

    @GetMapping("/dashboard")
    public CustomerPortalService.DashboardResponse dashboard(Authentication authentication) {
        return customerPortalService.dashboard(authentication.getName());
    }
}
