package com.chenhaonee.agents.domain.agent.factory;

import com.chenhaonee.agents.common.validator.ValidationUtils;
import com.chenhaonee.agents.domain.agent.model.Skill;
import org.springframework.stereotype.Component;

/**
 * Skill 资产工厂。
 */
@Component
public class SkillFactory {

    public Skill create(String code,
                        String name,
                        String briefDescription,
                        String summary,
                        String title,
                        String ossKey,
                        Long fileSize) {
        Skill skill = new Skill();
        skill.setCode(code);
        skill.setName(name);
        skill.setBriefDescription(briefDescription);
        skill.setSummary(summary);
        skill.setTitle(title);
        skill.setOssKey(ValidationUtils.requireText(ossKey, "ossKey"));
        skill.setFileSize(fileSize);
        skill.enable();
        return skill;
    }
}
