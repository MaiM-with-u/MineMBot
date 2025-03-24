package top.jiangyin14.minembot.handler.PlayerHandlers.InfoHandlers;

import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.MinecraftClient;
import top.jiangyin14.minembot.handler.BaseHandler;

import java.io.IOException;

public class TimeHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            MinecraftClient client = getClient();
            if (client == null) {
                sendResponse(exchange, 503, "Client not initialized");
                return;
            }
            client.execute(() -> {
                try {
                    JSONObject timeObj = new JSONObject();
                    if (client.world != null) {
                        timeObj.put("time", client.world.getTimeOfDay());
                    }

                    // day:[0,12000]
                    // night:(12000,24000]
                    if (client.world != null) {
                        timeObj.put("status", client.world.getTimeOfDay() <= 12000 ? "day" : "night");
                    }

                    LOGGER.info("Current time: {}", timeObj.toJSONString());
                    sendResponse(exchange, 200, timeObj.toJSONString());
                } catch (IOException e) {
                    LOGGER.error("Response failed", e);
                }
            });
        }
    }
}
