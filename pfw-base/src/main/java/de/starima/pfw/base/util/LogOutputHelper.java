package de.starima.pfw.base.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Hilfsmethoden für die Log-Ausgabe komplexer Datenstrukturen.
 *
 * <p>Erzeugt eine JSON-ähnliche, defensive String-Darstellung für Maps,
 * Collections, Arrays und skalare Werte. Optimiert für Logging:
 *
 * <ul>
 *   <li>maximale Rekursionstiefe</li>
 *   <li>Begrenzung der Eintragsanzahl</li>
 *   <li>Zyklenerkennung</li>
 *   <li>Maskierung sensibler Schlüssel</li>
 *   <li>optionales Pretty-Print</li>
 * </ul>
 *
 * <p>Wichtig: Die Ausgabe ist bewusst "JSON-ähnlich", aber nicht zwingend
 * valides JSON, da Kürzungen wie {@code "...(12 more)"} vorkommen können.
 */
public final class LogOutputHelper {

    private static final Config DEFAULT_CONFIG = Config.builder().build();

    private LogOutputHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static String toLogString(Object model) {
        return toLogString(model, DEFAULT_CONFIG);
    }

    public static String toLogString(Object model, Config config) {
        StringBuilder sb = new StringBuilder(256);
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        appendValue(sb, model, null, 0, visited, config != null ? config : DEFAULT_CONFIG);
        return sb.toString();
    }

    public static String toPrettyLogString(Object model) {
        return toLogString(model, Config.builder().prettyPrint(true).build());
    }

    public static String toLogString(Object model, String separator) {
        return toLogString(model, Config.builder().separator(separator).build());
    }

    // =========================================================================
    // Rekursion
    // =========================================================================

    private static void appendValue(
            StringBuilder sb,
            Object value,
            String currentKey,
            int depth,
            Set<Object> visited,
            Config config
    ) {
        if (value == null) {
            sb.append("null");
            return;
        }

        if (depth >= config.getMaxDepth()) {
            sb.append("\"...\"");
            return;
        }

        if (isScalar(value)) {
            appendScalar(sb, value, currentKey, config);
            return;
        }

        if (visited.contains(value)) {
            sb.append("\"<cycle:")
                    .append(value.getClass().getSimpleName())
                    .append("@")
                    .append(Integer.toHexString(System.identityHashCode(value)))
                    .append(">\"");
            return;
        }

        visited.add(value);
        try {
            if (value instanceof Map<?, ?> map) {
                appendMap(sb, map, depth, visited, config);
            } else if (value instanceof Collection<?> col) {
                appendCollection(sb, col, depth, visited, config);
            } else if (value.getClass().isArray()) {
                appendArray(sb, value, depth, visited, config);
            } else {
                // Fremde Objekte bewusst nicht reflektieren, sondern defensiv toString()
                String rendered = safeToString(value);
                sb.append('"').append(escapeString(truncate(rendered, config.getMaxStringLength()))).append('"');
            }
        } finally {
            visited.remove(value);
        }
    }

    private static void appendMap(
            StringBuilder sb,
            Map<?, ?> map,
            int depth,
            Set<Object> visited,
            Config config
    ) {
        sb.append('{');
        if (map.isEmpty()) {
            sb.append('}');
            return;
        }

        int count = 0;
        boolean first = true;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (count >= config.getMaxEntries()) {
                if (!first) {
                    sb.append(',');
                }
                appendLineBreakAndIndent(sb, depth + 1, config);
                sb.append("\"...\": \"(").append(map.size() - config.getMaxEntries()).append(" more entries)\"");
                break;
            }

            if (!first) {
                sb.append(',');
            }
            appendLineBreakAndIndent(sb, depth + 1, config);

            String key = String.valueOf(entry.getKey());
            sb.append('"').append(escapeString(key)).append('"');
            sb.append(config.isPrettyPrint() ? ": " : ":");

            if (config.isSensitiveKey(key)) {
                sb.append("\"").append(config.getMaskText()).append('"');
            } else {
                appendValue(sb, entry.getValue(), key, depth + 1, visited, config);
            }

            first = false;
            count++;
        }

        appendClosingIndent(sb, depth, config);
        sb.append('}');
    }

    private static void appendCollection(
            StringBuilder sb,
            Collection<?> col,
            int depth,
            Set<Object> visited,
            Config config
    ) {
        sb.append('[');
        if (col.isEmpty()) {
            sb.append(']');
            return;
        }

        int count = 0;
        boolean first = true;

        for (Object element : col) {
            if (count >= config.getMaxEntries()) {
                if (!first) {
                    sb.append(config.getSeparator());
                }
                if (config.isPrettyPrint()) {
                    appendLineBreakAndIndent(sb, depth + 1, config);
                }
                sb.append("\"...(").append(col.size() - config.getMaxEntries()).append(" more)\"");
                break;
            }

            if (!first) {
                sb.append(config.getSeparator());
            }
            if (config.isPrettyPrint()) {
                appendLineBreakAndIndent(sb, depth + 1, config);
            }

            appendValue(sb, element, null, depth + 1, visited, config);
            first = false;
            count++;
        }

        appendClosingIndent(sb, depth, config);
        sb.append(']');
    }

    private static void appendArray(
            StringBuilder sb,
            Object array,
            int depth,
            Set<Object> visited,
            Config config
    ) {
        int length = Array.getLength(array);
        sb.append('[');
        if (length == 0) {
            sb.append(']');
            return;
        }

        int limit = Math.min(length, config.getMaxEntries());

        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append(config.getSeparator());
            }
            if (config.isPrettyPrint()) {
                appendLineBreakAndIndent(sb, depth + 1, config);
            }
            appendValue(sb, Array.get(array, i), null, depth + 1, visited, config);
        }

        if (length > config.getMaxEntries()) {
            sb.append(config.getSeparator());
            if (config.isPrettyPrint()) {
                appendLineBreakAndIndent(sb, depth + 1, config);
            }
            sb.append("\"...(").append(length - config.getMaxEntries()).append(" more)\"");
        }

        appendClosingIndent(sb, depth, config);
        sb.append(']');
    }

    private static void appendScalar(StringBuilder sb, Object value, String currentKey, Config config) {
        if (currentKey != null && config.isSensitiveKey(currentKey)) {
            sb.append('"').append(config.getMaskText()).append('"');
            return;
        }

        if (value instanceof String str) {
            sb.append('"').append(escapeString(truncate(str, config.getMaxStringLength()))).append('"');
        } else if (value instanceof Character ch) {
            sb.append('"').append(escapeString(String.valueOf(ch))).append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Enum<?> e) {
            sb.append('"').append(escapeString(e.name())).append('"');
        } else {
            sb.append('"').append(escapeString(truncate(safeToString(value), config.getMaxStringLength()))).append('"');
        }
    }

    private static boolean isScalar(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>;
    }

    private static String safeToString(Object value) {
        try {
            return String.valueOf(value);
        } catch (Exception e) {
            return "<toString-error:" + e.getClass().getSimpleName() + ">";
        }
    }

    private static String truncate(String s, int maxLength) {
        if (s == null) {
            return "";
        }
        if (maxLength <= 0 || s.length() <= maxLength) {
            return s;
        }
        return s.substring(0, Math.max(0, maxLength)) + "...(" + (s.length() - maxLength) + " more chars)";
    }

    /**
     * Escaped Sonderzeichen für die JSON-ähnliche Ausgabe.
     */
    private static String escapeString(String s) {
        if (s == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                default -> {
                    if (c < 32) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static void appendLineBreakAndIndent(StringBuilder sb, int depth, Config config) {
        if (!config.isPrettyPrint()) {
            return;
        }
        sb.append('\n');
        sb.append(config.getIndent().repeat(Math.max(0, depth)));
    }

    private static void appendClosingIndent(StringBuilder sb, int depth, Config config) {
        if (!config.isPrettyPrint()) {
            return;
        }
        sb.append('\n');
        sb.append(config.getIndent().repeat(Math.max(0, depth)));
    }

    // =========================================================================
    // Config
    // =========================================================================

    public static final class Config {

        private final int maxDepth;
        private final int maxEntries;
        private final int maxStringLength;
        private final String separator;
        private final boolean prettyPrint;
        private final String indent;
        private final String maskText;
        private final Set<String> sensitiveKeys;

        private Config(Builder builder) {
            this.maxDepth = builder.maxDepth;
            this.maxEntries = builder.maxEntries;
            this.maxStringLength = builder.maxStringLength;
            this.separator = builder.separator;
            this.prettyPrint = builder.prettyPrint;
            this.indent = builder.indent;
            this.maskText = builder.maskText;
            this.sensitiveKeys = Set.copyOf(builder.sensitiveKeys);
        }

        public static Builder builder() {
            return new Builder();
        }

        public int getMaxDepth() {
            return maxDepth;
        }

        public int getMaxEntries() {
            return maxEntries;
        }

        public int getMaxStringLength() {
            return maxStringLength;
        }

        public String getSeparator() {
            return prettyPrint ? "," : separator;
        }

        public boolean isPrettyPrint() {
            return prettyPrint;
        }

        public String getIndent() {
            return indent;
        }

        public String getMaskText() {
            return maskText;
        }

        public boolean isSensitiveKey(String key) {
            if (key == null) {
                return false;
            }
            String normalized = key.trim().toLowerCase();
            return sensitiveKeys.contains(normalized);
        }

        public static final class Builder {
            private int maxDepth = 5;
            private int maxEntries = 50;
            private int maxStringLength = 500;
            private String separator = ", ";
            private boolean prettyPrint = false;
            private String indent = "  ";
            private String maskText = "***";
            private Set<String> sensitiveKeys = Set.of(
                    "password",
                    "passwd",
                    "pwd",
                    "secret",
                    "token",
                    "access_token",
                    "refresh_token",
                    "authorization",
                    "api_key",
                    "apikey",
                    "client_secret",
                    "sessionid",
                    "set-cookie",
                    "cookie"
            );

            private Builder() {
            }

            public Builder maxDepth(int maxDepth) {
                this.maxDepth = maxDepth;
                return this;
            }

            public Builder maxEntries(int maxEntries) {
                this.maxEntries = maxEntries;
                return this;
            }

            public Builder maxStringLength(int maxStringLength) {
                this.maxStringLength = maxStringLength;
                return this;
            }

            public Builder separator(String separator) {
                this.separator = separator != null ? separator : ", ";
                return this;
            }

            public Builder prettyPrint(boolean prettyPrint) {
                this.prettyPrint = prettyPrint;
                return this;
            }

            public Builder indent(String indent) {
                this.indent = indent != null ? indent : "  ";
                return this;
            }

            public Builder maskText(String maskText) {
                this.maskText = maskText != null ? maskText : "***";
                return this;
            }

            public Builder sensitiveKeys(Set<String> sensitiveKeys) {
                this.sensitiveKeys = sensitiveKeys != null
                        ? sensitiveKeys.stream().map(String::toLowerCase).collect(java.util.stream.Collectors.toUnmodifiableSet())
                        : Set.of();
                return this;
            }

            public Config build() {
                return new Config(this);
            }
        }
    }
}
