package com.portfoliopilot.service;

import com.portfoliopilot.dto.PriceImportResult;
import com.portfoliopilot.entity.AssetPrice;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.AssetPriceRepository;
import com.portfoliopilot.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * CSV price import. Expected columns per row: {@code date,symbol,close}
 * with ISO date (YYYY-MM-DD), symbol, positive finite close. A header row
 * is auto-detected and skipped. Every row is validated; failures are
 * reported with row numbers — nothing is silently dropped. Duplicates
 * (within the file or already stored) are counted, not re-inserted.
 */
@Service
public class PriceService {

    private static final int MAX_ROWS = 20000;
    private static final Pattern SYMBOL = Pattern.compile("^[A-Z0-9.\\-]{1,20}$");

    private final AssetPriceRepository priceRepository;
    private final UserRepository userRepository;

    public PriceService(AssetPriceRepository priceRepository, UserRepository userRepository) {
        this.priceRepository = priceRepository;
        this.userRepository = userRepository;
    }

    private User owner(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public List<String> symbols(String email) {
        return priceRepository.findDistinctSymbolsByUserId(owner(email).getId());
    }

    public List<AssetPrice> recent(String email, String symbol, int limit) {
        User user = owner(email);
        String upper = symbol.trim().toUpperCase();
        List<AssetPrice> all = priceRepository.findByUserIdAndSymbolOrderByPriceDate(user.getId(), upper);
        int from = Math.max(0, all.size() - Math.min(Math.max(limit, 1), 500));
        return all.subList(from, all.size());
    }

    @Transactional
    public PriceImportResult importCsv(String email, InputStream in) {
        User user = owner(email);
        List<PriceImportResult.ImportError> errors = new ArrayList<>();
        List<AssetPrice> toSave = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();
        int totalRows = 0;
        int skippedDuplicates = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (lineNumber == 1 && looksLikeHeader(trimmed)) {
                    continue;
                }
                totalRows++;
                if (totalRows > MAX_ROWS) {
                    errors.add(new PriceImportResult.ImportError(lineNumber, "row limit exceeded (20000)"));
                    break;
                }
                String[] cols = trimmed.split(",", -1);
                if (cols.length != 3) {
                    errors.add(new PriceImportResult.ImportError(lineNumber, "expected 3 columns: date,symbol,close"));
                    continue;
                }
                String dateRaw = cols[0].trim();
                String symbolRaw = cols[1].trim().toUpperCase();
                String closeRaw = cols[2].trim();

                LocalDate date;
                try {
                    date = LocalDate.parse(dateRaw);
                } catch (Exception e) {
                    errors.add(new PriceImportResult.ImportError(lineNumber, "invalid date '" + dateRaw + "' (use YYYY-MM-DD)"));
                    continue;
                }
                if (date.isAfter(LocalDate.now())) {
                    errors.add(new PriceImportResult.ImportError(lineNumber, "date '" + dateRaw + "' is in the future"));
                    continue;
                }
                if (!SYMBOL.matcher(symbolRaw).matches()) {
                    errors.add(new PriceImportResult.ImportError(lineNumber, "invalid symbol '" + cols[1].trim() + "'"));
                    continue;
                }
                double close;
                try {
                    close = Double.parseDouble(closeRaw);
                } catch (NumberFormatException e) {
                    errors.add(new PriceImportResult.ImportError(lineNumber, "invalid close '" + closeRaw + "'"));
                    continue;
                }
                if (!Double.isFinite(close) || close <= 0) {
                    errors.add(new PriceImportResult.ImportError(lineNumber, "close must be a positive finite number"));
                    continue;
                }
                String key = symbolRaw + "|" + date;
                if (!seenInFile.add(key)) {
                    errors.add(new PriceImportResult.ImportError(lineNumber, "duplicate row in file"));
                    continue;
                }
                if (priceRepository.existsByUserIdAndSymbolAndPriceDate(user.getId(), symbolRaw, date)) {
                    skippedDuplicates++;
                    continue;
                }
                AssetPrice price = new AssetPrice();
                price.setUser(user);
                price.setSymbol(symbolRaw);
                price.setPriceDate(date);
                price.setClose(close);
                toSave.add(price);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file");
        }
        priceRepository.saveAll(toSave);
        return new PriceImportResult(totalRows, toSave.size(), skippedDuplicates, errors);
    }

    private boolean looksLikeHeader(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith("date") || lower.contains("symbol") || lower.contains("close");
    }
}
