package com.portfoliopilot.controller;

import com.portfoliopilot.dto.PriceImportResult;
import com.portfoliopilot.entity.AssetPrice;
import com.portfoliopilot.service.PriceService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** User-owned historical price data: CSV import plus read access. */
@RestController
@RequestMapping("/api/prices")
public class PriceController {

    private final PriceService service;

    public PriceController(PriceService service) {
        this.service = service;
    }

    @PostMapping("/import")
    public PriceImportResult importCsv(Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload a non-empty CSV file");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".csv")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .csv files are accepted");
        }
        try {
            return service.importCsv(authentication.getName(), file.getInputStream());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file");
        }
    }

    @GetMapping("/symbols")
    public List<String> symbols(Authentication authentication) {
        return service.symbols(authentication.getName());
    }

    @GetMapping
    public List<Map<String, Object>> recent(Authentication authentication,
            @RequestParam String symbol,
            @RequestParam(defaultValue = "60") int limit) {
        return service.recent(authentication.getName(), symbol, limit).stream()
                .map(p -> Map.<String, Object>of(
                        "symbol", p.getSymbol(),
                        "date", p.getPriceDate().toString(),
                        "close", p.getClose()))
                .collect(Collectors.toList());
    }
}
