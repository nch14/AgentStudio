package com.chenhaonee.agents.connect.capability.impl;

import com.chenhaonee.agents.common.oss.OssObjectService;
import com.chenhaonee.agents.domain.agent.model.Skill;
import com.chenhaonee.agents.domain.agent.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class DefaultSkillApiTest {

    private DefaultSkillApi skillApi;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private OssObjectService ossObjectService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        skillApi = new DefaultSkillApi(skillRepository, ossObjectService);
    }

    @Test
    void getSkillContent_success() {
        String skillCode = "test-skill";
        String ossKey = "skills/test-skill.md";
        String content = "hello skill";

        Skill skill = new Skill();
        skill.setCode(skillCode);
        skill.setOssKey(ossKey);
        skill.enable();

        when(skillRepository.findByCode(skillCode)).thenReturn(Optional.of(skill));
        when(ossObjectService.readAsString(ossKey)).thenReturn(content);

        String result = skillApi.getSkillContent(skillCode);

        assertEquals(content, result);
    }

    @Test
    void getSkillContent_skillNotFound() {
        String skillCode = "not-found";
        when(skillRepository.findByCode(skillCode)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> skillApi.getSkillContent(skillCode));
    }

    @Test
    void getSkillContent_skillDisabled() {
        String skillCode = "disabled-skill";
        Skill skill = new Skill();
        skill.setCode(skillCode);
        skill.disable();

        when(skillRepository.findByCode(skillCode)).thenReturn(Optional.of(skill));

        assertThrows(IllegalStateException.class, () -> skillApi.getSkillContent(skillCode));
    }
}
