package top.jiangyin14.mineeye.handler.InfoHandler;

import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import top.jiangyin14.mineeye.handler.BaseHandler;

import java.io.IOException;

/**
 * gain coordinate of player
 * @author ChangingSelf
 */
public class FacingHandler extends BaseHandler {
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
                    Vec3d pos = client.player.getEyePos();
                    BlockPos blockPos = new BlockPos((int)pos.getX(), (int)pos.getY(), (int)pos.getZ());
                    BlockState blockState = client.world.getBlockState(blockPos);
                    Block block = blockState.getBlock();
                    String blockName = block.getTranslationKey();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("block", blockName);
                    jsonObject.put("x", blockPos.getX());
                    jsonObject.put("y", blockPos.getY());
                    jsonObject.put("z", blockPos.getZ());

                    LOGGER.info("Player is facing to: {}", jsonObject.toJSONString());
                    sendResponse(exchange, 200, jsonObject.toJSONString());
                } catch (IOException e) {
                    LOGGER.error("Response failed", e);
                }
            });
        }
    }
}