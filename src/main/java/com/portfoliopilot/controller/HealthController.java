package com.portfoliopilot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Increment 1 probe: proves HTTP layer works (/health)
 * and TiDB connectivity works (/health/db).
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "OK", "service", "portfolio-pilot");
    }

    @GetMapping("/health/db")
    public ResponseEntity<Map<String, String>> dbHealth() {
        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        if (one != null && one == 1) {
            return ResponseEntity.ok(Map.of("status", "OK", "database", "TiDB reachable"));
        }
        return ResponseEntity.status(503).body(Map.of("status", "DOWN", "database", "unexpected result"));
    }
}
