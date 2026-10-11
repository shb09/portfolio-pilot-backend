package com.portfoliopilot.dto;

import java.util.List;

/** Row-by-row outcome of a CSV price import. Nothing is silently dropped. */
public record PriceImportResult(
        int totalRows,
        int imported,
        int skippedDuplicates,
        List<ImportError> errors) {

    public record ImportError(int row, String message) {
    }
}
