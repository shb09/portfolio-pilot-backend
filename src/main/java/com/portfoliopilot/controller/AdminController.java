package com.portfoliopilot.controller;

import com.portfoliopilot.dto.AdminStatsDto;
import com.portfoliopilot.dto.AdminUserDto;
import com.portfoliopilot.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ADMIN only. @PreAuthorize is the real boundary — the frontend role
 * check is UX only. No passwords, hashes, or tokens ever leave here.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public AdminStatsDto stats() {
        return adminService.stats();
    }

    @GetMapping("/users")
    public List<AdminUserDto> users() {
        return adminService.users();
    }
}
