package com.chenhaonee.agents.connect.capability.impl;

import com.chenhaonee.agents.common.oss.OssObjectService;
import com.chenhaonee.agents.connect.capability.SkillApi;
import com.chenhaonee.agents.domain.agent.model.Skill;
import com.chenhaonee.agents.domain.agent.repository.SkillRepository;
import org.springframework.stereotype.Component;

/**
 * SkillApi 的 connect 模块实现。
 * <p>
 * 通过 domain 层的 SkillRepository + OssObjectService 实现 Skill 内容获取。
 */
@Component
public class DefaultSkillApi implements SkillApi {

    private final SkillRepository skillRepository;
    private final OssObjectService ossObjectService;

    public DefaultSkillApi(SkillRepository skillRepository, OssObjectService ossObjectService) {
        this.skillRepository = skillRepository;
        this.ossObjectService = ossObjectService;
    }

    @Override
    public String getSkillContent(String skillCode) {
        Skill skill = skillRepository.findByCode(skillCode)
                .orElseThrow(() -> new IllegalArgumentException("skill not found: " + skillCode));

        if (!skill.isEnabled()) {
            throw new IllegalStateException("skill is not enabled: " + skillCode);
        }

        return ossObjectService.readAsString(skill.getOssKey());
    }
}
