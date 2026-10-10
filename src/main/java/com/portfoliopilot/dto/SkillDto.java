package com.portfoliopilot.dto;

import com.portfoliopilot.entity.Skill;
import com.portfoliopilot.entity.SkillLevel;

public record SkillDto(Long id, String name, SkillLevel level) {

    public static SkillDto from(Skill s) {
        return new SkillDto(s.getId(), s.getName(), s.getLevel());
    }
}
