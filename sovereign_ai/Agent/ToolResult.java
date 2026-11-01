
package com.example.sovereign_ai.Agent;

public record ToolResult(
        String tool,
        String status,
        Object data,
        String error,
        long durationMs
) {

    public static ToolResult success(
            String tool,
            Object data,
            long durationMs
    ) {
        return new ToolResult(
                tool,
                "SUCCESS",
                data,
                null,
                durationMs
        );
    }

    public static ToolResult failure(
            String tool,
            String error,
            long durationMs
    ) {
        return new ToolResult(
                tool,
                "FAILED",
                null,
                error,
                durationMs
        );
    }

    public static ToolResult skipped(
            String tool,
            String reason
    ) {
        return new ToolResult(
                tool,
                "SKIPPED",
                null,
                reason,
                0
        );
    }
}
