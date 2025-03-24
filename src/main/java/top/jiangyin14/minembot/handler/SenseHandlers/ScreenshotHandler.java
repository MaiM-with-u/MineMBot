package top.jiangyin14.minembot.handler.SenseHandlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Return the screenshot for vl models
 * @author jiangyin14
 */
public class ScreenshotHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenshotHandler.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendErrorResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        File tempDir = null;
        File screenshotFile;

        try {
            CompletableFuture<File> futureScreenshotFile = new CompletableFuture<>();
            MinecraftClient client = MinecraftClient.getInstance();

            if (client == null) {
                sendErrorResponse(exchange, 500, "Minecraft client not available");
                return;
            }

            // Use Minecraft's run directory to create our temp directory
            File gameDir = client.runDirectory;
            tempDir = new File(gameDir, "minembot-temp-" + System.currentTimeMillis());

            if (!tempDir.exists() && !tempDir.mkdirs()) {
                sendErrorResponse(exchange, 500, "Failed to create temporary directory");
                return;
            }

            final File tempDir_final = tempDir;
            LOGGER.info("Taking screenshot, saving to: {}", tempDir_final.getAbsolutePath());

            // Schedule screenshot capture on the main game thread
            client.execute(() -> {
                try {
                    ScreenshotRecorder.saveScreenshot(
                            tempDir_final,
                            "screenshot.png",
                            client.getFramebuffer(),
                            (messageText) -> {
                                LOGGER.info("Screenshot saved: {}", messageText.getString());
                                // The screenshot might be in a nested 'screenshots' directory
                                File expected = new File(tempDir_final, "screenshot.png");
                                File nested = new File(new File(tempDir_final, "screenshots"), "screenshot.png");

                                if (expected.exists()) {
                                    futureScreenshotFile.complete(expected);
                                } else if (nested.exists()) {
                                    futureScreenshotFile.complete(nested);
                                } else {
                                    futureScreenshotFile.completeExceptionally(
                                            new IOException("Screenshot file not found at expected locations"));
                                }
                            }
                    );
                } catch (Exception e) {
                    futureScreenshotFile.completeExceptionally(e);
                    LOGGER.error("Failed to capture screenshot", e);
                }
            });

            // Wait for the screenshot to be saved with a timeout
            try {
                screenshotFile = futureScreenshotFile.get(10, TimeUnit.SECONDS);
                LOGGER.info("Screenshot file ready: {}", screenshotFile.getAbsolutePath());
            } catch (TimeoutException e) {
                LOGGER.error("Timeout waiting for screenshot", e);
                sendErrorResponse(exchange, 500, "Timeout waiting for screenshot");
                return;
            }

            // Read the file into memory
            if (!screenshotFile.exists()) {
                LOGGER.error("Screenshot file does not exist: {}", screenshotFile.getAbsolutePath());
                sendErrorResponse(exchange, 500, "Screenshot file not found");
                return;
            }

            byte[] imageData = Files.readAllBytes(screenshotFile.toPath());
            LOGGER.info("Read {} bytes from screenshot file", imageData.length);

            // Send the response
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, imageData.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(imageData);
                os.flush();
                LOGGER.info("Screenshot data sent successfully");
            }

        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error capturing screenshot", e);
            sendErrorResponse(exchange, 500, "Failed to capture screenshot: " + e.getMessage());
        } finally {
            // Clean up temporary files - recursively delete directory
            deleteRecursively(tempDir);
        }
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    deleteRecursively(f);
                }
            }
        }

        if (!file.delete()) {
            LOGGER.warn("Failed to delete file: {}", file.getAbsolutePath());
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        LOGGER.error("Sending error response: {} - {}", statusCode, message);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        byte[] responseBytes = message.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
            os.flush();
        }
    }
}