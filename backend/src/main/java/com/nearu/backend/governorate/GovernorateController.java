package com.nearu.backend.governorate;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governorates")
public class GovernorateController {

    private final GovernorateService service;

    public GovernorateController(GovernorateService service) {
        this.service = service;
    }

    @GetMapping
    public List<GovernorateResponse> list() {
        return service.list();
    }
}
