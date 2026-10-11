package com.deepseek.harness4j.observability;

import com.deepseek.harness4j.util.Jdk8;

import com.deepseek.harness4j.RunResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservabilityTest {

    @Test
    void test_langfuse_and_otel_trace_export() {
        List<Map<String, Object>> events = Jdk8.listOf(
                Jdk8.mapOf("type", "assistant/chunk", "data", Jdk8.mapOf("chunk", Jdk8.mapOf("type", "reasoning-delta", "text", "CoT step 1"))),
                Jdk8.mapOf("type", "tool/call", "time", 1000L, "data", Jdk8.mapOf("callId", "c1", "name", "bash", "arguments", "ls")),
                Jdk8.mapOf("type", "tool/result", "time", 1250L, "data", Jdk8.mapOf("callId", "c1", "message", "file.txt")),
                Jdk8.mapOf("type", "assistant/message", "data", Jdk8.mapOf(
                        "content", Jdk8.listOf(Jdk8.mapOf("type", "text", "text", "Done")),
                        "usage", Jdk8.mapOf("inputTokens", 100, "outputTokens", 50, "reasoningTokens", 30, "cacheReadTokens", 10, "cacheWriteTokens", 0))),
                Jdk8.mapOf("type", "turn/end", "data", Jdk8.mapOf("reason", Jdk8.mapOf("kind", "completed")))
        );

        RunResult result = new RunResult("sess-123", "Done", "completed", events, Jdk8.listOf(), "/tmp/sessions");

        // Langfuse trace export
        Map<String, Object> langfuse = LangfuseExporter.exportTrace(result, "test-trace");
        assertNotNull(langfuse);
        assertEquals("sess-123", langfuse.get("sessionId"));
        assertEquals("Done", langfuse.get("output"));
        assertTrue(((List<?>) langfuse.get("generations")).size() > 0);
        assertTrue(((List<?>) langfuse.get("toolSpans")).size() > 0);

        // OpenTelemetry GenAI Span export
        Map<String, Object> otel = OtelTraceExporter.exportGenAiSpan(result, "deepseek-reasoner");
        assertNotNull(otel);
        assertEquals("gen_ai.chat", otel.get("span.name"));

        @SuppressWarnings("unchecked")
        Map<String, Object> attrs = (Map<String, Object>) otel.get("attributes");
        assertEquals("deepseek", attrs.get("gen_ai.system"));
        assertEquals("deepseek-reasoner", attrs.get("gen_ai.request.model"));
        assertEquals(100, attrs.get("gen_ai.usage.input_tokens"));
        assertEquals(50, attrs.get("gen_ai.usage.output_tokens"));
        assertEquals(30, attrs.get("gen_ai.usage.reasoning_tokens"));
        assertEquals("CoT step 1", attrs.get("gen_ai.reasoning"));
    }
}
