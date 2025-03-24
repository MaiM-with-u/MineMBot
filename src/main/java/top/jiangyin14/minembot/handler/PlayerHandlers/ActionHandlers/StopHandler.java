package top.jiangyin14.minembot.handler.PlayerHandlers.ActionHandlers;

import baritone.api.BaritoneAPI;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.MinecraftClient;
import top.jiangyin14.minembot.handler.BaseHandler;

import java.io.IOException;

/**
 * Stops any ongoing Baritone pathing when called
 * @author jiangyin14
 */
public class StopHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            // Get the client
            MinecraftClient client = getClient();
            if (client == null) {
                sendResponse(exchange, 503, "{\"success\":false,\"error\":\"Client not initialized\"}");
                return;
            }

            // Execute the stop command on the game thread
            client.execute(() -> {
                try {
                    if (client.player == null) {
                        sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Player not in game\"}");
                        return;
                    }

                    // Call the stop method
                    stopBaritone();

                    // Send success response
                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Stopped all Baritone processes");
                    sendResponse(exchange, 200, response.toJSONString());
                } catch (IOException e) {
                    LOGGER.error("Response failed", e);
                }
            });
        } else {
            // Only accept GET requests
            sendResponse(exchange, 405, "{\"success\":false,\"error\":\"Method not allowed\"}");
        }
    }

    /**
     * Stops all ongoing Baritone processes
     */
    private void stopBaritone() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().clearAllKeys();
    }
}