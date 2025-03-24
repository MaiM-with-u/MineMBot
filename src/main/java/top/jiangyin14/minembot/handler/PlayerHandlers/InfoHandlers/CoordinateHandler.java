package top.jiangyin14.minembot.handler.PlayerHandlers.InfoHandlers;

import com.alibaba.fastjson2.JSON;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import top.jiangyin14.minembot.handler.BaseHandler;

import java.io.IOException;

/**
 * gain coordinate of player
 * @author ChangingSelf
 */
public class CoordinateHandler extends BaseHandler {
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
                    if (client.player == null) {
                        sendResponse(exchange, 403, "Player not in game");
                        return;
                    }

                    Vec3d pos = client.player.getPos();
                    String posStr = JSON.toJSONString(pos);
                    LOGGER.info("Got player position: {}", posStr);
                    sendResponse(exchange, 200, posStr);
                } catch (IOException e) {
                    LOGGER.error("Response failed", e);
                }
            });
        }
    }
}