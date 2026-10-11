package com.deepseek.harness4j.tool;

import java.util.List;
import java.util.Map;

/**
 * Descriptor of a registered Java tool.
 */
public final class ToolDefinition {

    private final String name;
    private final String description;
    private final Map<String, Object> parametersSchema;
    private final List<ParameterDefinition> parameters;

    public ToolDefinition(
            String name,
            String description,
            Map<String, Object> parametersSchema,
            List<ParameterDefinition> parameters) {
        this.name = name;
        this.description = description;
        this.parametersSchema = parametersSchema;
        this.parameters = parameters;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Map<String, Object> parametersSchema() {
        return parametersSchema;
    }

    public List<ParameterDefinition> parameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ToolDefinition)) {
            return false;
        }
        ToolDefinition that = (ToolDefinition) o;
        return java.util.Objects.equals(name, that.name)
                && java.util.Objects.equals(description, that.description)
                && java.util.Objects.equals(parametersSchema, that.parametersSchema)
                && java.util.Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, description, parametersSchema, parameters);
    }

    @Override
    public String toString() {
        return "ToolDefinition[name=" + name
                + ", description=" + description
                + ", parametersSchema=" + parametersSchema
                + ", parameters=" + parameters
                + ']';
    }

    /** One parameter descriptor of a tool. */
    public static final class ParameterDefinition {

        private final String name;
        private final Class<?> type;
        private final String description;
        private final boolean required;

        public ParameterDefinition(
                String name,
                Class<?> type,
                String description,
                boolean required) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.required = required;
        }

        public String name() {
            return name;
        }

        public Class<?> type() {
            return type;
        }

        public String description() {
            return description;
        }

        public boolean required() {
            return required;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ParameterDefinition)) {
                return false;
            }
            ParameterDefinition that = (ParameterDefinition) o;
            return required == that.required
                    && java.util.Objects.equals(name, that.name)
                    && java.util.Objects.equals(type, that.type)
                    && java.util.Objects.equals(description, that.description);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, type, description, required);
        }

        @Override
        public String toString() {
            return "ParameterDefinition[name=" + name
                    + ", type=" + type
                    + ", description=" + description
                    + ", required=" + required
                    + ']';
        }
    }
}
