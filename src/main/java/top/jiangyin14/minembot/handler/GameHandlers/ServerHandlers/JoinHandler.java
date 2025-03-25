package top.jiangyin14.minembot.handler.GameHandlers.ServerHandlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class JoinHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JoinHandler.class);
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

            if (!requestJson.has("server_ip") || requestJson.get("server_ip").getAsString().isEmpty()) {
                sendResponse(exchange, 400, "Missing or empty server_ip parameter");
                return;
            }

            String serverIp = requestJson.get("server_ip").getAsString();
            LOGGER.info("Received request to join server: {}", serverIp);

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                sendResponse(exchange, 500, "Minecraft client not available");
                return;
            }

            // Connect to the server on the game thread
            client.execute(() -> {
                try {
                    // Create a server info object
                    ServerInfo serverInfo = new ServerInfo("Server", serverIp, ServerInfo.ServerType.OTHER);
                    ServerAddress serverAddress = ServerAddress.parse(serverIp);

                    // Connect to the server
                    ConnectScreen.connect(
                            new TitleScreen(),
                            client,
                            serverAddress,
                            serverInfo,
                            false  // Added missing fifth parameter - false indicates this is not from server list
                    );

                    LOGGER.info("Connecting to server: {}", serverIp);
                } catch (Exception e) {
                    LOGGER.error("Failed to connect to server", e);
                }
            });

            sendResponse(exchange, 200, "Connecting to server: " + serverIp);

        } catch (Exception e) {
            LOGGER.error("Error processing join request", e);
            sendResponse(exchange, 500, "Error: " + e.getMessage());
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        QuitHandler.doExchange(exchange, statusCode, message, GSON);
    }
}
