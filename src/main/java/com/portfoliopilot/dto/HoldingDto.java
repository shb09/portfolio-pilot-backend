package com.portfoliopilot.dto;

import com.portfoliopilot.entity.Holding;

public record HoldingDto(Long id, String symbol, String assetCategory, Double quantity) {

    public static HoldingDto from(Holding h) {
        return new HoldingDto(h.getId(), h.getSymbol(), h.getAssetCategory(), h.getQuantity());
    }
}
