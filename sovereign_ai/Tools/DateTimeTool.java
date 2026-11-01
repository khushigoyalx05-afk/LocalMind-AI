
package com.example.sovereign_ai.Tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class DateTimeTool {

    @Tool(description = """
            Get the current date and time from the server.
            Use this tool when the user asks for the current
            date, time or day.
            The returned time uses the server's timezone.
            """)
    public String getCurrentDateTime() {

        ZonedDateTime now = ZonedDateTime.now(
                ZoneId.systemDefault()
        );

        return now.format(
                DateTimeFormatter.ofPattern(
                        "EEEE, dd MMMM yyyy, hh:mm:ss a z"
                )
        );
    }
}
