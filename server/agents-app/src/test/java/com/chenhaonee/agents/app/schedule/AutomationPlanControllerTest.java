package com.chenhaonee.agents.app.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chenhaonee.agents.app.application.schedule.AutomationPlanCommandApplicationService;
import com.chenhaonee.agents.app.application.schedule.AutomationPlanQueryApplicationService;
import com.chenhaonee.agents.app.interfaces.http.schedule.AutomationPlanController;
import com.chenhaonee.agents.app.interfaces.http.schedule.AutomationPlanHttpAssembler;
import com.chenhaonee.agents.common.domain.Status;
import com.chenhaonee.agents.domain.schedule.model.AutomationPlan;
import com.chenhaonee.agents.domain.schedule.model.AutomationPlan.DeliveryMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AutomationPlanControllerTest {

    private final AutomationPlanQueryApplicationService queryService = org.mockito.Mockito.mock(AutomationPlanQueryApplicationService.class);
    private final AutomationPlanCommandApplicationService commandService = org.mockito.Mockito.mock(AutomationPlanCommandApplicationService.class);
    private final AutomationPlanHttpAssembler assembler = new AutomationPlanHttpAssembler();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new AutomationPlanController(queryService, commandService, assembler)).build();

    private AutomationPlan samplePlan;

    @BeforeEach
    void setUp() {
        samplePlan = createPlan("plan-001", "每日晨报", "agent-001", "0 8 * * *", DeliveryMode.PUSH, Status.ENABLED);
    }

    @Test
    void shouldReturnPlanDetail() throws Exception {
        when(queryService.getPlan("plan-001")).thenReturn(samplePlan);

        mockMvc.perform(get("/api/v1/automation-plans/{planCode}", "plan-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.planCode").value("plan-001"))
                .andExpect(jsonPath("$.data.name").value("每日晨报"))
                .andExpect(jsonPath("$.data.agentCode").value("agent-001"))
                .andExpect(jsonPath("$.data.scheduleRule").value("0 8 * * *"))
                .andExpect(jsonPath("$.data.deliveryMode").value("PUSH"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentPlan() throws Exception {
        when(queryService.getPlan("not-exist"))
                .thenThrow(new IllegalArgumentException("automation plan not found: not-exist"));

        mockMvc.perform(get("/api/v1/automation-plans/{planCode}", "not-exist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldCreatePlan() throws Exception {
        when(commandService.createPlan(eq("晨报"), eq("agent-001"), eq("搜索新闻"), eq("0 8 * * *"), eq(DeliveryMode.PUSH)))
                .thenReturn(samplePlan);

        mockMvc.perform(post("/api/v1/automation-plans")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "晨报",
                                "agentCode", "agent-001",
                                "instruction", "搜索新闻",
                                "scheduleRule", "0 8 * * *",
                                "deliveryMode", "PUSH"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("每日晨报"));
    }

    @Test
    void shouldRejectCreateWithMissingFields() throws Exception {
        mockMvc.perform(post("/api/v1/automation-plans")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdatePlan() throws Exception {
        AutomationPlan updated = createPlan("plan-001", "更新后的晨报", "agent-001", "0 9 * * *", DeliveryMode.CONVERSATION, Status.ENABLED);
        when(commandService.updatePlan(eq("plan-001"), any(), any(), any(), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/automation-plans/{planCode}", "plan-001")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "更新后的晨报",
                                "scheduleRule", "0 9 * * *",
                                "deliveryMode", "CONVERSATION"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("更新后的晨报"))
                .andExpect(jsonPath("$.data.scheduleRule").value("0 9 * * *"));
    }

    @Test
    void shouldEnablePlan() throws Exception {
        when(commandService.enablePlan("plan-001")).thenReturn(samplePlan);

        mockMvc.perform(post("/api/v1/automation-plans/{planCode}/enable", "plan-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldDisablePlan() throws Exception {
        AutomationPlan disabled = createPlan("plan-001", "每日晨报", "agent-001", "0 8 * * *", DeliveryMode.PUSH, Status.DISABLED);
        when(commandService.disablePlan("plan-001")).thenReturn(disabled);

        mockMvc.perform(post("/api/v1/automation-plans/{planCode}/disable", "plan-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));
    }

    @Test
    void shouldDeletePlan() throws Exception {
        doNothing().when(commandService).deletePlan("plan-001");

        mockMvc.perform(post("/api/v1/automation-plans/{planCode}/delete", "plan-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldListPlansWithPagination() throws Exception {
        Page<AutomationPlan> page = new PageImpl<>(List.of(samplePlan), PageRequest.of(0, 20), 1);
        when(queryService.listPlans(0, 20, null)).thenReturn(page);

        mockMvc.perform(get("/api/v1/automation-plans")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].planCode").value("plan-001"));
    }

    @Test
    void shouldListPlansWithStatusFilter() throws Exception {
        Page<AutomationPlan> page = new PageImpl<>(List.of(samplePlan), PageRequest.of(0, 20), 1);
        when(queryService.listPlans(0, 20, Status.ENABLED)).thenReturn(page);

        mockMvc.perform(get("/api/v1/automation-plans")
                        .param("page", "0")
                        .param("size", "20")
                        .param("status", "ENABLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }

    private AutomationPlan createPlan(String code, String name, String agentCode, String scheduleRule, DeliveryMode deliveryMode, Status status) {
        AutomationPlan plan = new AutomationPlan();
        plan.setCode(code);
        plan.setName(name);
        plan.setAgentCode(agentCode);
        plan.setInstruction("搜索并总结今天的新闻");
        plan.setScheduleRule(scheduleRule);
        plan.setDeliveryMode(deliveryMode);
        plan.setStatus(status);
        plan.setCreateTime(new Date(System.currentTimeMillis() - 86400000));
        plan.setUpdateTime(new Date());
        return plan;
    }
}
