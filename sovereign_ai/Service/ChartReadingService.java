
package com.example.sovereign_ai.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.content.Media;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

@Service
public class ChartReadingService {

    private static final Logger log =
            LoggerFactory.getLogger(ChartReadingService.class);

    private static final long MAX_IMAGE_SIZE =
            10L * 1024 * 1024;

    private final ChatClient visionClient;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ChartReadingService(
            ChatClient.Builder builder,
            @Value("${localmind.vision.model:qwen2.5vl:3b}")
            String visionModel
    ) {

        this.visionClient = builder
                .defaultOptions(
                        ChatOptions.builder()
                                .model(visionModel)
                )
                .build();
        log.info(
                "LocalMind Vision Service initialized with model: {}",
                visionModel
        );
    }

    // =====================================================
    // UNIVERSAL GRAPH / IMAGE READING
    // =====================================================

    public String readChart(
            MultipartFile image,
            String userQuestion
    ) throws Exception {

        validateImage(image);

        String question =
                userQuestion == null || userQuestion.isBlank()
                        ? "Explain this image."
                        : userQuestion.trim();

        log.info(
                "Reading visual: {}",
                image.getOriginalFilename()
        );

        byte[] imageBytes = image.getBytes();

        MimeType mimeType = MimeTypeUtils.parseMimeType(
                image.getContentType()
        );

        Media media = new Media(
                mimeType,
                new ByteArrayResource(imageBytes)
        );

        String instruction = """
                You are LocalMind AI's universal
                visual understanding assistant.

                Analyze the attached image carefully.

                You can interpret:
                - Bar charts
                - Line graphs
                - Pie charts
                - Scatter plots
                - Scientific graphs
                - Flowcharts
                - Network diagrams
                - System architecture diagrams
                - Organizational charts
                - Other visual information

                ANALYSIS INSTRUCTIONS:

                1. Identify what type of visual
                   is present.

                2. Describe the information shown.

                3. Read visible titles, labels,
                   legends, axes, and units.

                4. Identify important patterns,
                   trends, and relationships.

                5. Extract numerical values only
                   when they are clearly readable.

                6. Clearly distinguish exact values
                   from visual estimates.

                7. Do not invent missing values,
                   labels, or relationships.

                8. If part of the image is unclear,
                   explain the uncertainty.

                9. Distinguish observations from
                   possible explanations.

                10. Do not assume the image belongs
                    to any specific industry.

                11. Answer the user's actual question.

                USER QUESTION:
                """ + question;

        UserMessage message = UserMessage.builder()
                .text(instruction)
                .media(List.of(media))
                .build();

        // =================================================
        // SEND IMAGE TO LOCAL OLLAMA VISION MODEL
        // =================================================

        try {

            String response = visionClient.prompt()
                    .messages(message)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {

                throw new IllegalStateException(
                        "Vision model returned an empty response."
                );
            }

            log.info(
                    "Visual analysis completed successfully"
            );

            return response;

        } catch (Exception e) {

            log.error(
                    "LocalMind vision analysis failed",
                    e
            );

            throw new IllegalStateException(
                    "Unable to analyze the uploaded image. " +
                            "Check that Ollama is running and " +
                            "the configured vision model is installed.",
                    e
            );
        }
    }

    // =====================================================
    // IMAGE VALIDATION
    // =====================================================

    private void validateImage(
            MultipartFile image
    ) {

        if (image == null || image.isEmpty()) {

            throw new IllegalArgumentException(
                    "Please upload a valid image."
            );
        }

        if (image.getSize() > MAX_IMAGE_SIZE) {

            throw new IllegalArgumentException(
                    "Image exceeds the 10 MB limit."
            );
        }

        String contentType = image.getContentType();

        if (contentType == null || contentType.isBlank()) {

            throw new IllegalArgumentException(
                    "Image content type is missing."
            );
        }

        String type = contentType
                .toLowerCase(Locale.ROOT);

        if (!type.equals("image/png")
                && !type.equals("image/jpeg")
                && !type.equals("image/webp")) {

            throw new IllegalArgumentException(
                    "Unsupported image format. " +
                            "Use PNG, JPEG, or WEBP."
            );
        }
    }
}
