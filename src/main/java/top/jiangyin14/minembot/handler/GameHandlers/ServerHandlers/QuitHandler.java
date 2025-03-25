package top.jiangyin14.minembot.handler.GameHandlers.ServerHandlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class QuitHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuitHandler.class);
    private static final Gson GSON = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            LOGGER.info("Received request to return to main menu");

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                sendResponse(exchange, 500, "Minecraft client not available");
                return;
            }

            // Send response before disconnecting
            sendResponse(exchange, 200, "Returning to main menu");

            // Schedule disconnection on the game thread
            client.execute(() -> {
                LOGGER.info("Disconnecting from current world/server");
                if (client.world != null) {
                    // Disconnect and return to title screen
                    client.world.disconnect();
                    client.disconnect();
                    client.setScreen(new TitleScreen());
                    LOGGER.info("Successfully returned to main menu");
                } else {
                    LOGGER.info("No world to disconnect from, already at main menu");
                }
            });

        } catch (Exception e) {
            LOGGER.error("Error processing quit request", e);
            sendResponse(exchange, 500, "Error: " + e.getMessage());
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        doExchange(exchange, statusCode, message, GSON);
    }

    static void doExchange(HttpExchange exchange, int statusCode, String message, Gson gson) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", statusCode < 400 ? "success" : "error");
        responseJson.addProperty("message", message);
        byte[] responseBytes = gson.toJson(responseJson).getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
            os.flush();
        }
    }
}