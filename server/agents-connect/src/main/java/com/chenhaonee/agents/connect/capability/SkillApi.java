package com.chenhaonee.agents.connect.capability;

/**
 * Agent 可调用的 Skill 管理能力。
 */
public interface SkillApi {

    /**
     * 根据 skill code 获取 skill 内容。
     */
    String getSkillContent(String skillCode);
}
