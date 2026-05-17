package com.chenhaonee.agents.app;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chenhaonee.agents.app.interfaces.http.NotificationController;
import com.chenhaonee.agents.domain.notify.model.NotificationEvent;
import com.chenhaonee.agents.domain.notify.service.MessageCenter;
import java.util.Map;
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
    void shouldTriggerNotificationByEventCode() throws Exception {
        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventCode": "questions_raised",
                                  "variables": {
                                    "taskTitle": "测试任务",
                                    "turnNo": "1",
                                    "questionCount": "2"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.errorMessage").value("通知发送已触发，请查看日志和消息列表"));

        verify(messageCenter).send(eq(NotificationEvent.QUESTIONS_RAISED), eq(Map.of(
                "taskTitle", "测试任务",
                "turnNo", "1",
                "questionCount", "2")));
    }
}
