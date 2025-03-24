package top.jiangyin14.minembot.handler.PlayerHandlers.ActionHandlers;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.MinecraftClient;
import top.jiangyin14.minembot.handler.BaseHandler;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Goto the coords that provided in API
 * @author jiangyin14
 */

public class GotoHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            // Read the request body
            InputStream is = exchange.getRequestBody();
            String requestBody = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            is.close();

            try {
                // Parse the JSON
                JSONObject json = JSONObject.parseObject(requestBody);

                // Extract x and z coordinates
                if (!json.containsKey("x") || !json.containsKey("z")) {
                    sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Missing x or z coordinates\"}");
                    return;
                }

                double x = json.getDoubleValue("x");
                double z = json.getDoubleValue("z");

                // Get the client
                MinecraftClient client = getClient();
                if (client == null) {
                    sendResponse(exchange, 503, "{\"success\":false,\"error\":\"Client not initialized\"}");
                    return;
                }

                // Execute the goto command on the game thread
                client.execute(() -> {
                    try {
                        if (client.player == null) {
                            sendResponse(exchange, 403, "{\"success\":false,\"error\":\"Player not in game\"}");
                            return;
                        }

                        // Call the goto method with the coordinates
                        gotoCoordinates((int) x, (int) z);

                        // Send success response
                        JSONObject response = new JSONObject();
                        response.put("success", true);
                        response.put("message", "Path set to coordinates x=" + x + ", z=" + z);
                        sendResponse(exchange, 200, response.toJSONString());
                    } catch (IOException e) {
                        LOGGER.error("Response failed", e);
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Error processing request", e);
                sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid JSON or coordinates\"}");
            }
        } else {
            // Only accept POST requests
            sendResponse(exchange, 405, "{\"success\":false,\"error\":\"Method not allowed\"}");
        }
    }

    /**
     * Sets a goal to navigate to the specified coordinates
     * @param x X coordinate
     * @param z Z coordinate
     */
    private void gotoCoordinates(int x, int z)
    {
        BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().clearAllKeys();
        Goal goal = new GoalXZ(x, z);
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(goal);
    }
}