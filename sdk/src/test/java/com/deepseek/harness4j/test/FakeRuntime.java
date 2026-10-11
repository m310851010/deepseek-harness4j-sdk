package com.deepseek.harness4j.test;

import com.deepseek.harness4j.util.Jdk8;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test-only fake JSON-RPC runtime launched as a subprocess by the SDK tests.
 *
 * <p>This is the Java port of the inline Python "fake_runtime.py" / "fake_bridge.py"
 * scripts embedded in {@code python/sdk/tests/test_client.py}. The behavior is selected by
 * the {@code FR_SCENARIO} environment variable so each test maps to one scripted sequence.
 */
public final class FakeRuntime {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FakeRuntime() {
    }

    public static void main(String[] args) throws Exception {
        String scenario = System.getenv().getOrDefault("FR_SCENARIO", "none");
        PrintStream out = System.out;

        switch (scenario) {
            case "non-json-first":
                out.println("node warning: experimental loader");
                break;
            case "sleep-forever": {
                System.err.println("bridge is still starting");
                Thread.sleep(60_000);
                return;
            
            }
            case "crash": {
                System.err.println("fatal bridge exploded");
                System.exit(42);
                return;
            
            }
            default: {
                // fall through to the reader loop
            
            } break;
        }

        if ("ignore-sigterm".equals(scenario)) {
            Thread hook = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(10_000);
                    } catch (InterruptedException ignored) {
                        // keep the JVM alive against SIGTERM (port of SIG_IGN)
                    }
                }
            });
            hook.setDaemon(false);
            Runtime.getRuntime().addShutdownHook(hook);
        }

        if ("bundled".equals(scenario)) {
            Map<String, Object> dump = new LinkedHashMap<>();
            dump.put("DSH_CORDIS_CONFIG", System.getenv().get("DSH_CORDIS_CONFIG"));
            Jdk8.writeString(Paths.get(System.getenv("ENV_DUMP")), json(dump));
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if ("seen-writes".equals(scenario)) {
                    Jdk8.writeString(
                            Paths.get(System.getenv("SEEN")), line + "\n",
                            java.nio.file.StandardOpenOption.CREATE,
                            java.nio.file.StandardOpenOption.APPEND);
                }
                Map<String, Object> message;
                try {
                    message = MAPPER.readValue(line, new TypeReference<Map<String, Object>>() {
                    });
                } catch (Exception ignored) {
                    continue;
                }
                Object id = message.get("id");
                Object method = message.get("method");
                if (method instanceof String) {
                    handleRequest(scenario, id, (String) method, message);
                } else if (id != null) {
                    handleResponse(scenario, message);
                }
            }
        }
    }

    private static void handleRequest(String scenario, Object id, String method,
                                      Map<String, Object> message) throws Exception {
        PrintStream out = System.out;
        Map<String, Object> params = paramsOf(message);
        switch (method) {
            case "initialize": {
                switch (scenario) {
                    case "main-turn": {
                        writeDump(System.getenv("ENV_DUMP"), envDump());
                        writeDump(System.getenv("INIT_DUMP"), params);
                    
                    } break;
                    case "capture-cwd": {
                        Map<String, Object> capture = new LinkedHashMap<>();
                        capture.put("process", System.getProperty("user.dir"));
                        capture.put("environment", System.getenv("DSH_CWD"));
                        capture.put("wire", params.get("cwd"));
                        Jdk8.writeString(Paths.get(System.getenv("CAPTURE")), json(capture));
                    
                    } break;
                    case "initialize-error": {
                        respondError(id, -32000, "bad initialize");
                        return;
                    
                    }
                    default: {
                        // plain initialize response
                    
                    } break;
                }
                respond(id, Jdk8.mapOf("serverInfo", Jdk8.mapOf("name", fakeName(scenario))));
                if ("bridge-llm".equals(scenario)) {
                    emit("{\"jsonrpc\":\"2.0\",\"id\":\"bridge-req-1\",\"method\":\"llm.request\","
                            + "\"params\":{\"requestId\":\"req-1\",\"sessionId\":\"main\","
                            + "\"model\":\"dsagent\",\"messages\":[]}}");
                }
            
            } break;
            case "session/prompt": {
                switch (scenario) {
                    case "main-turn": {
                        emit(inbox(params));
                        emit(status(params, "running"));
                        respond(id, Jdk8.mapOf("messageId", "message-1"));
                        emit(assistantMessage(params, Jdk8.mapOf("role", "assistant", "content",
                                Jdk8.listOf(Jdk8.mapOf("type", "text", "text", "hello from runtime")))));
                        emit(turnEnd(params, 1, "completed"));
                        emit(turnEnd(params, 2, "max-tokens"));
                        emit(status(params, "idle"));
                    
                    } break;
                    case "callback-subagent": {
                        emit(inbox(params));
                        emit(status(params, "running"));
                        respond(id, Jdk8.mapOf("messageId", "message-1"));
                        emit(subagent("subagent.started", "main", "child", null));
                        emit(status(params, "idle"));
                    
                    } break;
                    case "malformed-turn-end": {
                        emit(inbox(params));
                        respond(id, Jdk8.mapOf("messageId", "message-1"));
                        emit(turnEnd(params, 1, null));
                        emit(status(params, "idle"));
                    
                    } break;
                    case "subagent-finished": {
                        emit(inbox(params));
                        emit(status(params, "running"));
                        respond(id, Jdk8.mapOf("messageId", "message-1"));
                        emit(subagent("subagent.started", "main", "child", null));
                        emit(subagent("subagent.finished", "main", "child", "completed"));
                        emit(status(params, "idle"));
                    
                    } break;
                    case "nested-tree": {
                        String root = String.valueOf(params.get("sessionId"));
                        emit(inbox(params));
                        emit(status(params, "running"));
                        respond(id, Jdk8.mapOf("messageId", "message-1"));
                        emit(subagent("subagent.started", root, "child", null));
                        emit(sessionEvent("child", Jdk8.mapOf("type", "assistant/message",
                                "data", Jdk8.mapOf("content",
                                        Jdk8.listOf(Jdk8.mapOf("type", "text", "text", "child response"))))));
                        emit(subagent("subagent.started", "child", "grandchild", null));
                        emit(sessionEvent("grandchild", Jdk8.mapOf("type", "assistant/message",
                                "data", Jdk8.mapOf("content",
                                        Jdk8.listOf(Jdk8.mapOf("type", "text", "text", "grandchild response"))))));
                        emit(subagent("subagent.finished", "child", "grandchild", null));
                        emit(subagent("subagent.finished", root, "child", null));
                        emit(sessionEvent(root, Jdk8.mapOf("type", "assistant/message",
                                "data", Jdk8.mapOf("content",
                                        Jdk8.listOf(Jdk8.mapOf("type", "text", "text", "root response"))))));
                        emit(status(params, "idle"));
                    
                    } break;
                    case "other-session": {
                        emit(assistantMessage(Jdk8.mapOf("sessionId", "other"), Jdk8.mapOf("content",
                                Jdk8.listOf(Jdk8.mapOf("type", "text", "text", "wrong session")))));
                        emit(status(Jdk8.mapOf("sessionId", "other"), "idle"));
                        emit(inbox(params));
                        emit(status(params, "running"));
                        respond(id, Jdk8.mapOf("messageId", "message-1"));
                        emit(assistantMessage(params, Jdk8.mapOf("content",
                                Jdk8.listOf(Jdk8.mapOf("type", "text", "text", "right session")))));
                        emit(status(params, "idle"));
                    
                    } break;
                    case "minimal-turn": {
                        emit(inbox(params));
                        emit(status(params, "running"));
                        respond(id, Jdk8.mapOf("messageId", "message-1"));
                        emit(assistantMessage(params, Jdk8.mapOf("content",
                                Jdk8.listOf(Jdk8.mapOf("type", "text", "text", "ok")))));
                        emit(status(params, "idle"));
                    
                    } break;
                    case "streaming-turn": {
                        emit(inbox(params));
                        emit(status(params, "running"));
                        respond(id, Jdk8.mapOf("messageId", "message-1"));
                        emit(sessionEvent(String.valueOf(params.get("sessionId")), Jdk8.mapOf(
                                "type", "assistant/chunk",
                                "data", Jdk8.mapOf("chunk", Jdk8.mapOf("type", "reasoning-delta", "text", "thinking...")))));
                        emit(sessionEvent(String.valueOf(params.get("sessionId")), Jdk8.mapOf(
                                "type", "assistant/chunk",
                                "data", Jdk8.mapOf("chunk", Jdk8.mapOf("type", "text-delta", "text", "streaming hello")))));
                        emit(sessionEvent(String.valueOf(params.get("sessionId")), Jdk8.mapOf(
                                "type", "tool/call",
                                "data", Jdk8.mapOf("callId", "call-1", "name", "bash", "arguments", "echo hi"))));
                        emit(sessionEvent(String.valueOf(params.get("sessionId")), Jdk8.mapOf(
                                "type", "tool/result",
                                "data", Jdk8.mapOf("callId", "call-1", "message", "hi\n"))));
                        emit(assistantMessage(params, Jdk8.mapOf("content",
                                Jdk8.listOf(Jdk8.mapOf("type", "text", "text", "streaming hello")))));
                        emit(turnEnd(params, 1, "completed"));
                        emit(status(params, "idle"));
                    
                    } break;
                    case "late-idle": {
                        int turn = nextTurn();
                        String messageId = "message-" + turn;
                        emit(inbox(params, messageId));
                        emit(status(params, "running"));
                        respond(id, Jdk8.mapOf("messageId", messageId));
                        if (turn == 1) {
                            emit(assistantMessage(params, Jdk8.mapOf("content",
                                    Jdk8.listOf(Jdk8.mapOf("type", "text", "text", "first")))));
                            emit(status(params, "idle"));
                        } else {
                            Thread.sleep(50);
                            emit(assistantMessage(params, Jdk8.mapOf("content",
                                    Jdk8.listOf(Jdk8.mapOf("type", "text", "text", "second")))));
                            emit(status(params, "idle"));
                        }
                    
                    } break;
                    case "bridge-llm": {
                        emit(notification("llm/request", Jdk8.mapOf(
                                "requestId", "req-1",
                                "sessionId", params.get("sessionId"),
                                "model", "dsagent",
                                "messages", Jdk8.listOf())));
                        respond(id, Jdk8.mapOf("messageId", "message-1"));
                    
                    } break;
                    case "tick": {
                        respond(id, Jdk8.mapOf("messageId", "message-1"));
                    
                    } break;
                    case "reject-prompt": {
                        respond(id, Jdk8.mapOf("accepted", false));
                    
                    } break;
                    default: {
                        respond(id, Jdk8.mapOf("messageId", "message-1"));
                    
                    } break;
                }
            
            } break;
            case "emit-first":
            case "emit-second": {
                emit(notification("tick", Jdk8.mapOf("source", method)));

            } break;
            case "shutdown": {
                if ("ignore-sigterm".equals(scenario)) {
                    Thread.sleep(60_000);
                    return;
                }
                respond(id, Jdk8.mapOf());
                return;
            
            }
            default: {
                // ignore unknown methods
            
            } break;
        }
    }

    private static void handleResponse(String scenario, Map<String, Object> message) {
        if ("bridge-llm".equals(scenario)) {
            emit(notification("response/seen", Jdk8.mapOf("result", message.get("result"))));
        }
    }

    private static int nextTurn() {
        int turn = ++TURN_COUNTER;
        return turn;
    }

    private static int TURN_COUNTER = 0;

    private static void writeDump(String path, Object value) throws Exception {
        if (path != null) {
            Jdk8.writeString(Paths.get(path), json(value));
        }
    }

    private static Map<String, Object> envDump() {
        Map<String, Object> dump = new LinkedHashMap<>();
        dump.put("DEEPSEEK_API_KEY", System.getenv("DEEPSEEK_API_KEY"));
        dump.put("DEEPSEEK_BASE_URL", System.getenv("DEEPSEEK_BASE_URL"));
        dump.put("DSH_CWD", System.getenv("DSH_CWD"));
        dump.put("DSH_SESSION_ROOT", System.getenv("DSH_SESSION_ROOT"));
        dump.put("DSH_CORDIS_CONFIG", System.getenv("DSH_CORDIS_CONFIG"));
        dump.put("DSH_SYSTEM_PROMPT", System.getenv("DSH_SYSTEM_PROMPT"));
        return dump;
    }

    private static String fakeName(String scenario) {
        return "bundled".equals(scenario) ? "bundled-runtime" : "fake-runtime";
    }

    private static Map<String, Object> paramsOf(Map<String, Object> message) {
        Object params = message.get("params");
        if (params instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) params;
            return map;
        }
        return new LinkedHashMap<>();
    }

    private static Map<String, Object> inbox(Map<String, Object> params) {
        return inbox(params, "message-1");
    }

    private static Map<String, Object> inbox(Map<String, Object> params, String messageId) {
        return sessionEvent(String.valueOf(params.get("sessionId")), Jdk8.mapOf(
                "type", "agent/inbox/spliced",
                "data", Jdk8.mapOf("target", "next-turn", "start", 0,
                        "inserted", Jdk8.listOf(Jdk8.mapOf("id", messageId)))));
    }

    private static Map<String, Object> status(Map<String, Object> params, String status) {
        return notification("session.status", Jdk8.mapOf(
                "sessionId", params.get("sessionId"),
                "status", status));
    }

    private static Map<String, Object> assistantMessage(Map<String, Object> params,
                                                       Map<String, Object> data) {
        return sessionEvent(String.valueOf(params.get("sessionId")),
                Jdk8.mapOf("type", "assistant/message", "data", data));
    }

    private static Map<String, Object> turnEnd(Map<String, Object> params, int turn, String kind) {
        Map<String, Object> reason = kind == null ? Jdk8.mapOf() : Jdk8.mapOf("kind", kind);
        return sessionEvent(String.valueOf(params.get("sessionId")),
                Jdk8.mapOf("type", "turn/end", "data", Jdk8.mapOf("turn", turn, "reason", reason)));
    }

    private static Map<String, Object> subagent(String method, String parent, String child,
                                                String stopReason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("parentSessionId", parent);
        payload.put("childSessionId", child);
        if (stopReason != null) {
            payload.put("status", "ok");
            payload.put("stopReason", stopReason);
        }
        return notification(method, payload);
    }

    private static Map<String, Object> sessionEvent(String sessionId, Object event) {
        return notification("session.event", Jdk8.mapOf("sessionId", sessionId, "event", event));
    }

    private static Map<String, Object> notification(String method, Map<String, Object> params) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("jsonrpc", "2.0");
        message.put("method", method);
        message.put("params", params);
        return message;
    }

    private static void respond(Object id, Object result) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("jsonrpc", "2.0");
        message.put("id", id);
        message.put("result", result);
        emit(json(message));
    }

    private static void respondError(Object id, int code, String messageText) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", messageText);
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("jsonrpc", "2.0");
        message.put("id", id);
        message.put("error", error);
        emit(json(message));
    }

    private static void emit(Map<String, Object> message) {
        emit(json(message));
    }

    private static void emit(String line) {
        System.out.println(line);
        System.out.flush();
    }

    private static String json(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception exc) {
            throw new IllegalStateException("could not serialize fake runtime message", exc);
        }
    }
}
