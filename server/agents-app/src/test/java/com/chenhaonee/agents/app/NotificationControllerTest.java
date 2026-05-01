package com.chenhaonee.agents.app;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chenhaonee.agents.app.interfaces.http.NotificationController;
import com.chenhaonee.agents.domain.notify.service.MessageCenter;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NotificationControllerTest {

    private final MessageCenter messageCenter = mock(MessageCenter.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new NotificationController(messageCenter))
            .build();

    @Test
    void shouldTriggerNotificationByConfigCode() throws Exception {
        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "configCode": "questions_raised",
                                  "subject": "测试主题",
                                  "content": "测试内容"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.errorMessage").value("通知发送已触发，请查看日志和消息列表"));

        verify(messageCenter).send("questions_raised", "测试主题", "测试内容");
    }
}
