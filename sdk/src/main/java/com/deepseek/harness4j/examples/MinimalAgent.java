package com.deepseek.harness4j.examples;

import java.nio.file.Paths;

import com.deepseek.harness4j.DeepSeekHarness;
import com.deepseek.harness4j.DeepSeekHarnessConfig;
import com.deepseek.harness4j.RunResult;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Run one minimal-agent turn through the bundled runtime.
 *
 * <p>Line-by-line Java port of {@code examples/jsonrpc-agent/minimal.py}: parses one task and
 * prints the agent's final response.
 *
 * <pre>{@code
 * java -cp ... com.deepseek.harness4j.examples.MinimalAgent \
 *   --workspace /absolute/path/workspace \
 *   --session-root /absolute/path/sessions \
 *   --session-id example-001 \
 *   "Read package.json and print the list of scripts."
 * }</pre>
 *
 * <p>Arguments: positional {@code prompt}; options {@code --workspace} (default process cwd),
 * {@code --session-root} (default {@code .dsh-sessions}), {@code --session-id},
 * {@code --provider} (default {@code deepseek-official}), {@code --model} (default
 * {@code $DSH_MODEL} or {@code deepseek-v4-flash}), and {@code --max-tokens} (integer).
 */
public final class MinimalAgent {

    private static final String CONFIG_RESOURCE = "examples/jsonrpc-agent/minimal.cordis.yml";

    private MinimalAgent() {
    }

    public static void main(String[] args) throws Exception {
        String prompt = null;
        Path workspace = Paths.get("").toAbsolutePath();
        Path sessionRoot = Paths.get(".dsh-sessions").toAbsolutePath();
        String sessionId = null;
        String provider = "deepseek-official";
        String model = System.getenv().getOrDefault("DSH_MODEL", "deepseek-v4-flash");
        Integer maxTokens = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--workspace".equals(arg)) {
                workspace = Paths.get(args[++i]);
            } else if ("--session-root".equals(arg)) {
                sessionRoot = Paths.get(args[++i]);
            } else if ("--session-id".equals(arg)) {
                sessionId = args[++i];
            } else if ("--provider".equals(arg)) {
                provider = args[++i];
            } else if ("--model".equals(arg)) {
                model = args[++i];
            } else if ("--max-tokens".equals(arg)) {
                maxTokens = Integer.parseInt(args[++i]);
            } else {
                if (arg.startsWith("-")) {
                    throw new IllegalArgumentException("unknown option: " + arg);
                }
                prompt = arg;
            }
        }
        if (prompt == null) {
            throw new IllegalArgumentException("a prompt argument is required");
        }

        Path resolvedWorkspace = workspace.toAbsolutePath().normalize();
        Path resolvedSessionRoot = sessionRoot.toAbsolutePath().normalize();
        try (DeepSeekHarness harness = new DeepSeekHarness(DeepSeekHarnessConfig.builder()
                .provider(provider)
                .model(model)
                .maxTokens(maxTokens)
                .cwd(resolvedWorkspace.toString())
                .sessionRoot(resolvedSessionRoot.toString())
                .cordis(resolveConfig().toString())
                .build())) {
            RunResult result = harness.run(prompt, sessionId, null);
            System.out.println(result.finalResponse());
        }
    }

    private static Path resolveConfig() throws Exception {
        URL resource = MinimalAgent.class.getClassLoader().getResource(CONFIG_RESOURCE);
        if (resource == null) {
            throw new IllegalStateException(
                    "missing classpath resource " + CONFIG_RESOURCE + " (minimal.cordis.yml)");
        }
        if ("file".equals(resource.getProtocol())) {
            return Paths.get(URI.create(resource.toString()));
        }
        Path target = Files.createTempFile("deepseek-harness4j-sdk-minimal-", ".cordis.yml");
        try (InputStream in = resource.openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        target.toFile().deleteOnExit();
        return target;
    }
}
