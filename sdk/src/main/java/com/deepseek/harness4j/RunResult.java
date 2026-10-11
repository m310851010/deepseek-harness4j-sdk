package com.deepseek.harness4j;

import com.deepseek.harness4j.model.Notification;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of one {@link Session#run(String, java.util.function.Consumer)} interval.
 *
 * <p>Python dataclass:
 * <pre>{@code
 * @dataclass(slots=True)
 * class RunResult:
 *     session_id: str
 *     final_response: str
 *     finish_reason: str | None
 *     events: list[JsonObject]
 *     notifications: list[Notification]
 *     session_root: str | None = None
 * }</pre>
 *
 * @param sessionId     the root session id of the run interval
 * @param finalResponse the last committed root-session assistant text in the interval
 * @param finishReason  the {@code kind} of the last root-session {@code turn/end} in the
 *                      interval, or {@code null} when no turn ended
 * @param events        root-session events only, in wire order
 * @param notifications root session and all known descendant notifications in wire order
 * @param sessionRoot   the configured session persistence root, or {@code null}
 */
public final class RunResult {

    private final String sessionId;
    private final String finalResponse;
    private final String finishReason;
    private final List<Map<String, Object>> events;
    private final List<Notification> notifications;
    private final String sessionRoot;

    public RunResult(
            String sessionId,
            String finalResponse,
            String finishReason,
            List<Map<String, Object>> events,
            List<Notification> notifications,
            String sessionRoot) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(finalResponse, "finalResponse");
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(notifications, "notifications");
        this.sessionId = sessionId;
        this.finalResponse = finalResponse;
        this.finishReason = finishReason;
        this.events = events;
        this.notifications = notifications;
        this.sessionRoot = sessionRoot;
    }

    /** The root session id of the run interval. */
    public String sessionId() {
        return sessionId;
    }

    /** The last committed root-session assistant text in the interval. */
    public String finalResponse() {
        return finalResponse;
    }

    /** The {@code kind} of the last root-session {@code turn/end}, or {@code null}. */
    public String finishReason() {
        return finishReason;
    }

    /** Root-session events only, in wire order. */
    public List<Map<String, Object>> events() {
        return events;
    }

    /** Root session and descendant notifications in wire order. */
    public List<Notification> notifications() {
        return notifications;
    }

    /** The configured session persistence root, or {@code null}. */
    public String sessionRoot() {
        return sessionRoot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RunResult)) {
            return false;
        }
        RunResult that = (RunResult) o;
        return Objects.equals(sessionId, that.sessionId)
                && Objects.equals(finalResponse, that.finalResponse)
                && Objects.equals(finishReason, that.finishReason)
                && Objects.equals(events, that.events)
                && Objects.equals(notifications, that.notifications)
                && Objects.equals(sessionRoot, that.sessionRoot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, finalResponse, finishReason, events, notifications, sessionRoot);
    }

    @Override
    public String toString() {
        return "RunResult[sessionId=" + sessionId
                + ", finalResponse=" + finalResponse
                + ", finishReason=" + finishReason
                + ", events=" + events
                + ", notifications=" + notifications
                + ", sessionRoot=" + sessionRoot
                + ']';
    }

    /**
     * The full CoT reasoning chain: every {@code reasoning-delta} text in this interval,
     * concatenated in wire order. Empty string when the model produced no reasoning.
     */
    public String reasoningContent() {
        return SessionSupport.extractReasoningContent(events);
    }

    /**
     * Aggregated token accounting across every {@code assistant/message} that carried a
     * {@code usage} object, summed over all turns in the interval.
     */
    public TokenUsage tokenUsage() {
        return SessionSupport.extractTokenUsage(events);
    }

    /**
     * Structured tool-call tree: each {@code tool/call} paired with its matching
     * {@code tool/result} by {@code callId}, in wire order.
     */
    public List<ToolCallRecord> toolCalls() {
        return SessionSupport.extractToolCalls(events);
    }

    /**
     * Token accounting for one interval, mapping the upstream TokenUsage vocabulary
     * (input/output/cacheRead/cacheWrite/reasoning) to the conventional prompt/completion
     * split used by LLM SDKs.
     */
    public static final class TokenUsage {

        public static final TokenUsage EMPTY = new TokenUsage(0, 0, 0, 0, 0, 0);

        private final int promptTokens;
        private final int completionTokens;
        private final int reasoningTokens;
        private final int cacheReadTokens;
        private final int cacheWriteTokens;
        private final int totalTokens;

        public TokenUsage(
                int promptTokens,
                int completionTokens,
                int reasoningTokens,
                int cacheReadTokens,
                int cacheWriteTokens,
                int totalTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.reasoningTokens = reasoningTokens;
            this.cacheReadTokens = cacheReadTokens;
            this.cacheWriteTokens = cacheWriteTokens;
            this.totalTokens = totalTokens;
        }

        public int promptTokens() {
            return promptTokens;
        }

        public int completionTokens() {
            return completionTokens;
        }

        public int reasoningTokens() {
            return reasoningTokens;
        }

        public int cacheReadTokens() {
            return cacheReadTokens;
        }

        public int cacheWriteTokens() {
            return cacheWriteTokens;
        }

        public int totalTokens() {
            return totalTokens;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TokenUsage)) {
                return false;
            }
            TokenUsage that = (TokenUsage) o;
            return promptTokens == that.promptTokens
                    && completionTokens == that.completionTokens
                    && reasoningTokens == that.reasoningTokens
                    && cacheReadTokens == that.cacheReadTokens
                    && cacheWriteTokens == that.cacheWriteTokens
                    && totalTokens == that.totalTokens;
        }

        @Override
        public int hashCode() {
            return Objects.hash(promptTokens, completionTokens, reasoningTokens,
                    cacheReadTokens, cacheWriteTokens, totalTokens);
        }

        @Override
        public String toString() {
            return "TokenUsage[promptTokens=" + promptTokens
                    + ", completionTokens=" + completionTokens
                    + ", reasoningTokens=" + reasoningTokens
                    + ", cacheReadTokens=" + cacheReadTokens
                    + ", cacheWriteTokens=" + cacheWriteTokens
                    + ", totalTokens=" + totalTokens
                    + ']';
        }
    }

    /**
     * One paired tool invocation: the {@code tool/call} request and the {@code tool/result}
     * outcome, matched by {@code callId}. Arguments are the raw JSON string exactly as the
     * model produced it; {@code isError} is true when the result carried an {@code error}
     * object.
     */
    public static final class ToolCallRecord {

        private final String callId;
        private final String toolName;
        private final String argumentsJson;
        private final Object result;
        private final boolean isError;
        private final long durationMs;

        public ToolCallRecord(
                String callId,
                String toolName,
                String argumentsJson,
                Object result,
                boolean isError,
                long durationMs) {
            this.callId = callId;
            this.toolName = toolName;
            this.argumentsJson = argumentsJson;
            this.result = result;
            this.isError = isError;
            this.durationMs = durationMs;
        }

        public String callId() {
            return callId;
        }

        public String toolName() {
            return toolName;
        }

        public String argumentsJson() {
            return argumentsJson;
        }

        public Object result() {
            return result;
        }

        public boolean isError() {
            return isError;
        }

        public long durationMs() {
            return durationMs;
        }

        /** Alias for argumentsJson. */
        public String arguments() {
            return argumentsJson;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ToolCallRecord)) {
                return false;
            }
            ToolCallRecord that = (ToolCallRecord) o;
            return isError == that.isError
                    && durationMs == that.durationMs
                    && Objects.equals(callId, that.callId)
                    && Objects.equals(toolName, that.toolName)
                    && Objects.equals(argumentsJson, that.argumentsJson)
                    && Objects.equals(result, that.result);
        }

        @Override
        public int hashCode() {
            return Objects.hash(callId, toolName, argumentsJson, result, isError, durationMs);
        }

        @Override
        public String toString() {
            return "ToolCallRecord[callId=" + callId
                    + ", toolName=" + toolName
                    + ", argumentsJson=" + argumentsJson
                    + ", result=" + result
                    + ", isError=" + isError
                    + ", durationMs=" + durationMs
                    + ']';
        }
    }
}
