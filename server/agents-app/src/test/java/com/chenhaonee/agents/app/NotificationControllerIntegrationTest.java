package com.chenhaonee.agents.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chenhaonee.agents.AgentsApplication;
import com.chenhaonee.agents.common.notify.NotificationChannel;
import com.chenhaonee.agents.domain.notify.model.Notification;
import com.chenhaonee.agents.domain.notify.model.Notification.NotificationStatus;
import com.chenhaonee.agents.domain.notify.model.NotifyConfig;
import com.chenhaonee.agents.domain.notify.repository.NotificationRepository;
import com.chenhaonee.agents.domain.notify.repository.NotifyConfigRepository;
import com.chenhaonee.agents.domain.profile.model.OwnerProfile;
import com.chenhaonee.agents.domain.profile.repository.OwnerProfileRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = AgentsApplication.class,
        properties = {
                "app.bark.server-url=http://127.0.0.1:9",
                "app.notify.scheduler.enabled=false",
                "app.notify.config-code.questions-raised=questions_raised",
                "app.notify.config-code.turn-hanging=turn_hanging",
                "app.notify.config-code.task-succeeded=task_succeeded"
        })
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OwnerProfileRepository ownerProfileRepository;

    @Autowired
    private NotifyConfigRepository notifyConfigRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        notifyConfigRepository.deleteAll();
        ownerProfileRepository.deleteAll();

        OwnerProfile ownerProfile = new OwnerProfile();
        ownerProfile.updateProfile("测试用户", null, "debug-device-key", "本地调试");
        ownerProfile.updatePreferences("Asia/Shanghai", "zh-CN");
        ownerProfileRepository.save(ownerProfile);

        NotifyConfig notifyConfig = new NotifyConfig();
        notifyConfig.setCode("bark_debug");
        notifyConfig.setName("Bark 调试");
        notifyConfig.setDeliveryMode(NotifyConfig.DeliveryMode.INSTANT);
        notifyConfig.setChannels("BARK");
        notifyConfigRepository.save(notifyConfig);
    }

    @Test
    void shouldUseBarkNotifierInsteadOfMissingNotifierError() throws Exception {
        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "configCode": "bark_debug",
                                  "subject": "本地调试主题",
                                  "content": "本地调试内容"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());

        Notification notification = notifications.getFirst();
        assertEquals("bark_debug", notification.getConfigCode());
        assertEquals(NotificationChannel.BARK, notification.getChannel());
        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertNotNull(notification.getErrorMessage());
        assertFalse(notification.getErrorMessage().contains("no notifier"));
    }
}
