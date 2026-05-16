package com.chenhaonee.agents.connect.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

class AgentMessageBlockPayloadsAttachmentTest {

    @Test
    void image_payload_should_contain_meta_fields() {
        String payload = AgentMessageBlockPayloads.image(
                "agentX/image/2026-05-02/uuid-foo.png",
                "image/png",
                "foo.png",
                12528L,
                "https://oss.example.com/agents/agentX/image/2026-05-02/uuid-foo.png"
        );

        JSONObject json = JSON.parseObject(payload);
        assertThat(json.getString("ossKey")).isEqualTo("agentX/image/2026-05-02/uuid-foo.png");
        assertThat(json.getString("mime")).isEqualTo("image/png");
        assertThat(json.getString("filename")).isEqualTo("foo.png");
        assertThat(json.getLong("size")).isEqualTo(12528L);
        assertThat(json.getString("url")).contains("uuid-foo.png");
        assertThat(json.containsKey("data")).isFalse();
    }

    @Test
    void document_payload_should_contain_meta_fields() {
        String payload = AgentMessageBlockPayloads.document(
                "agentX/file/2026-05-02/uuid-bar.pdf",
                "application/pdf",
                "bar.pdf",
                13264L,
                "https://oss.example.com/agents/agentX/file/2026-05-02/uuid-bar.pdf"
        );

        JSONObject json = JSON.parseObject(payload);
        assertThat(json.getString("ossKey")).isEqualTo("agentX/file/2026-05-02/uuid-bar.pdf");
        assertThat(json.getString("mime")).isEqualTo("application/pdf");
        assertThat(json.getString("filename")).isEqualTo("bar.pdf");
        assertThat(json.getLong("size")).isEqualTo(13264L);
        assertThat(json.getString("url")).contains("uuid-bar.pdf");
    }

    @Test
    void payload_should_omit_url_when_null() {
        String payload = AgentMessageBlockPayloads.image(
                "agentX/image/2026-05-02/foo.png",
                "image/png",
                "foo.png",
                100L,
                null
        );

        JSONObject json = JSON.parseObject(payload);
        assertThat(json.containsKey("url")).isFalse();
    }
}
