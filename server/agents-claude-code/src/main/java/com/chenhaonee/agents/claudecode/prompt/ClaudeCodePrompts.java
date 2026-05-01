package com.chenhaonee.agents.claudecode.prompt;

import com.chenhaonee.agents.connect.capability.AgentConfigApi;
import com.chenhaonee.agents.connect.spi.context.AgentProfile;
import com.chenhaonee.agents.connect.spi.context.OwnerContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 加载并渲染 Claude Code 的 prompt 模板。
 * 模板文件位于 classpath:prompts/claude-code/ 下，用 {variable} 占位符替换。
 */
@Component
public class ClaudeCodePrompts {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCodePrompts.class);

    private static final String TEMPLATE_BASE = "prompts/claude-code/";

    private final AgentConfigApi agentConfigApi;

    public ClaudeCodePrompts(AgentConfigApi agentConfigApi) {
        this.agentConfigApi = agentConfigApi;
    }

    /** 渲染 chat 模式的 CLAUDE.md 内容 */
    public String renderClaudeMdChat(String agentCode) {
        return render("claude.md.tmpl", buildHomeTemplateVars(agentCode));
    }

    /** 渲染 soul.md 内容 */
    public String renderSoulMd(String agentCode) {
        return render("soul.md.tmpl", buildHomeTemplateVars(agentCode));
    }

    /** 渲染 owner.md 内容 */
    public String renderOwnerMd(String agentCode) {
        return render("owner.md.tmpl", buildHomeTemplateVars(agentCode));
    }

    /** 渲染 memory/overview.md 内容 */
    public String renderMemoryOverviewMd() {
        return render("memory-overview.md.tmpl", Map.of());
    }

    /** 渲染 tool.md 内容 */
    public String renderToolMd() {
        return render("tool.md.tmpl", Map.of());
    }

    /** 渲染 task 模式追加到 CLAUDE.md 的 Current Task 段落 */
    public String renderClaudeMdTaskAppend(String taskCode, String turnCode) {
        return render("claude-md-task-append.md.tmpl", Map.of(
                "taskCode", taskCode,
                "turnCode", turnCode
        ));
    }

    /** 渲染 task 模式的 CLAUDE.md 完整内容（chat variant + task append） */
    public String renderClaudeMdTask(String agentCode, String taskCode, String turnCode) {
        return renderClaudeMdChat(agentCode) + renderClaudeMdTaskAppend(taskCode, turnCode);
    }

    /** 渲染 task.md 文件内容 */
    public String renderTaskMd(String taskCode, String taskTitle, String taskContent) {
        return render("task-md.md.tmpl", Map.of(
                "taskCode", taskCode,
                "taskTitle", nullSafe(taskTitle),
                "taskContent", nullSafe(taskContent)
        ));
    }

    /** 渲染 task 模式的 --system-prompt 内容 */
    public String renderTaskSystemPrompt(String taskCode) {
        return render("system-prompt-task.txt", Map.of("taskCode", taskCode));
    }

    /** 渲染 task 模式的 turn user prompt（发送到 claude stdin）*/
    public String renderTurnUserPrompt(String taskCode, String turnCode, String previousSessionId) {
        String resumeHint = (previousSessionId != null && !previousSessionId.isBlank())
                ? "\nResuming prior session " + previousSessionId + ". Pick up where you left off."
                : "";
        return render("turn-user-prompt-task.txt", Map.of(
                "taskCode", taskCode,
                "turnCode", turnCode,
                "resumeHint", resumeHint
        ));
    }

    private String render(String templateName, Map<String, String> vars) {
        String template = loadTemplate(templateName);
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            template = template.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return template;
    }

    private String loadTemplate(String templateName) {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_BASE + templateName);
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load prompt template: {}", templateName, e);
            throw new IllegalStateException("Prompt template not found: " + templateName, e);
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private Map<String, String> buildHomeTemplateVars(String agentCode) {
        AgentProfile profile = agentConfigApi.getAgentProfile(agentCode);
        OwnerContext owner = agentConfigApi.getOwnerContext();
        Map<String, String> vars = new HashMap<>();
        vars.put("agentName", nullSafe(profile.name()));
        vars.put("agentResponsibility", nullSafe(profile.responsibility()));
        vars.put("ownerDisplayName", nullSafe(owner.displayName()));
        vars.put("ownerLocale", nullSafe(owner.locale()));
        vars.put("ownerTimezone", nullSafe(owner.timezone()));
        vars.put("ownerBio", nullSafe(owner.bio()));
        return vars;
    }
}
