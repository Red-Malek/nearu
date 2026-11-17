package com.nearu.backend.auth;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Minimal role-guarded endpoints so US01 criterion 3 is testable. US06 and US02 replace them with real APIs. */
@RestController
public class RoleAreaController {

    @GetMapping("/api/customer/space")
    public Map<String, String> customerSpace() {
        return Map.of("space", "customer");
    }

    @GetMapping("/api/professional/space")
    public Map<String, String> professionalSpace() {
        return Map.of("space", "professional");
    }
}
