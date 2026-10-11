package com.deepseek.harness4j.cordis;

import com.deepseek.harness4j.util.Jdk8;

import java.util.ArrayList;
import java.util.List;

/**
 * Strong-typed security and isolation policy configuration for Cordis sandbox plugins.
 */
public final class SandboxPolicy {

    private final String mode;
    private final String workspaceRoot;
    private final boolean allowNetwork;
    private final boolean readOnlyRoot;
    private final List<String> allowedCommands;
    private final List<String> blockedCommands;
    private final List<String> readOnlyPaths;
    private final Integer timeoutSeconds;

    public SandboxPolicy(
            String mode,
            String workspaceRoot,
            boolean allowNetwork,
            boolean readOnlyRoot,
            List<String> allowedCommands,
            List<String> blockedCommands,
            List<String> readOnlyPaths,
            Integer timeoutSeconds) {
        this.mode = mode;
        this.workspaceRoot = workspaceRoot;
        this.allowNetwork = allowNetwork;
        this.readOnlyRoot = readOnlyRoot;
        this.allowedCommands = allowedCommands;
        this.blockedCommands = blockedCommands;
        this.readOnlyPaths = readOnlyPaths;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String mode() {
        return mode;
    }

    public String workspaceRoot() {
        return workspaceRoot;
    }

    public boolean allowNetwork() {
        return allowNetwork;
    }

    public boolean readOnlyRoot() {
        return readOnlyRoot;
    }

    public List<String> allowedCommands() {
        return allowedCommands;
    }

    public List<String> blockedCommands() {
        return blockedCommands;
    }

    public List<String> readOnlyPaths() {
        return readOnlyPaths;
    }

    public Integer timeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SandboxPolicy)) {
            return false;
        }
        SandboxPolicy that = (SandboxPolicy) o;
        return allowNetwork == that.allowNetwork
                && readOnlyRoot == that.readOnlyRoot
                && java.util.Objects.equals(mode, that.mode)
                && java.util.Objects.equals(workspaceRoot, that.workspaceRoot)
                && java.util.Objects.equals(allowedCommands, that.allowedCommands)
                && java.util.Objects.equals(blockedCommands, that.blockedCommands)
                && java.util.Objects.equals(readOnlyPaths, that.readOnlyPaths)
                && java.util.Objects.equals(timeoutSeconds, that.timeoutSeconds);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(mode, workspaceRoot, allowNetwork, readOnlyRoot,
                allowedCommands, blockedCommands, readOnlyPaths, timeoutSeconds);
    }

    @Override
    public String toString() {
        return "SandboxPolicy[mode=" + mode
                + ", workspaceRoot=" + workspaceRoot
                + ", allowNetwork=" + allowNetwork
                + ", readOnlyRoot=" + readOnlyRoot
                + ", allowedCommands=" + allowedCommands
                + ", blockedCommands=" + blockedCommands
                + ", readOnlyPaths=" + readOnlyPaths
                + ", timeoutSeconds=" + timeoutSeconds
                + ']';
    }

    public static final String MODE_DANGER_FULL_ACCESS = "danger-full-access";
    public static final String MODE_RESTRICTED = "restricted";
    public static final String MODE_READ_ONLY = "read-only";

    public static Builder builder() {
        return new Builder();
    }

    public static SandboxPolicy defaultPolicy() {
        return builder().build();
    }

    public static final class Builder {
        private String mode = MODE_DANGER_FULL_ACCESS;
        private String workspaceRoot;
        private boolean allowNetwork = true;
        private boolean readOnlyRoot = false;
        private final List<String> allowedCommands = new ArrayList<>();
        private final List<String> blockedCommands = new ArrayList<>();
        private final List<String> readOnlyPaths = new ArrayList<>();
        private Integer timeoutSeconds;

        public Builder mode(String mode) {
            this.mode = mode;
            return this;
        }

        public Builder workspaceRoot(String workspaceRoot) {
            this.workspaceRoot = workspaceRoot;
            return this;
        }

        public Builder allowNetwork(boolean allowNetwork) {
            this.allowNetwork = allowNetwork;
            return this;
        }

        public Builder readOnlyRoot(boolean readOnlyRoot) {
            this.readOnlyRoot = readOnlyRoot;
            return this;
        }

        public Builder allowCommand(String command) {
            this.allowedCommands.add(command);
            return this;
        }

        public Builder blockCommand(String command) {
            this.blockedCommands.add(command);
            return this;
        }

        public Builder readOnlyPath(String path) {
            this.readOnlyPaths.add(path);
            return this;
        }

        public Builder timeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public SandboxPolicy build() {
            return new SandboxPolicy(
                    mode,
                    workspaceRoot,
                    allowNetwork,
                    readOnlyRoot,
                    Jdk8.copyOf(allowedCommands),
                    Jdk8.copyOf(blockedCommands),
                    Jdk8.copyOf(readOnlyPaths),
                    timeoutSeconds);
        }
    }
}
