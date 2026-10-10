package com.portfoliopilot.dto;

import com.portfoliopilot.entity.Achievement;

public record AchievementDto(Long id, String title, String description, String date, String link) {

    public static AchievementDto from(Achievement a) {
        return new AchievementDto(a.getId(), a.getTitle(), a.getDescription(), a.getDate(), a.getLink());
    }
}
