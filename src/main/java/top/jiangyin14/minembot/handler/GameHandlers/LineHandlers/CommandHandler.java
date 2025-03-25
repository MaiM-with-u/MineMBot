package top.jiangyin14.minembot.handler.GameHandlers.LineHandlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CommandHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandHandler.class);
    private static final Gson GSON = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            // Parse request body
            JsonObject requestJson = GSON.fromJson(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                    JsonObject.class
            );

            if (!requestJson.has("command") || requestJson.get("command").getAsString().isEmpty()) {
                sendResponse(exchange, 400, "Missing or empty command parameter");
                return;
            }

            String command = requestJson.get("command").getAsString();
            LOGGER.info("Received request to execute command: {}", command);

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                sendResponse(exchange, 500, "Minecraft client not available");
                return;
            }

            // Create a future to track command execution
            CompletableFuture<Boolean> commandFuture = new CompletableFuture<>();

            // Execute the command on the game thread
            client.execute(() -> {
                try {
                    if (client.player == null || client.player.networkHandler == null) {
                        commandFuture.complete(false);
                        LOGGER.error("Player not available to execute command");
                        return;
                    }

                    // Remove leading slash if present
                    String processedCommand = command;
                    if (processedCommand.startsWith("/")) {
                        processedCommand = processedCommand.substring(1);
                    }

                    client.player.networkHandler.sendChatCommand(processedCommand);
                    LOGGER.info("Command executed: {}", command);
                    commandFuture.complete(true);
                } catch (Exception e) {
                    LOGGER.error("Failed to execute command", e);
                    commandFuture.completeExceptionally(e);
                }
            });

            // Wait for the command execution with a timeout
            try {
                boolean success = commandFuture.get(5, TimeUnit.SECONDS);
                if (success) {
                    sendResponse(exchange, 200, "Command executed: " + command);
                } else {
                    sendResponse(exchange, 500, "Failed to execute command: Player not available");
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOGGER.error("Error waiting for command execution", e);
                sendResponse(exchange, 500, "Error executing command: " + e.getMessage());
            }

        } catch (Exception e) {
            LOGGER.error("Error processing command request", e);
            sendResponse(exchange, 500, "Error: " + e.getMessage());
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", statusCode < 400 ? "success" : "error");
        responseJson.addProperty("message", message);
        byte[] responseBytes = GSON.toJson(responseJson).getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
            os.flush();
        }
    }
}