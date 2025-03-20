package top.jiangyin14.minembot.handler.InfoHandler;

import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import top.jiangyin14.minembot.handler.BaseHandler;

import java.io.IOException;

/**
 * Obtain the biome where the player is located
 * @author ChangingSelf
 */
public class BiomeHandler extends BaseHandler {
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

                    World world = client.player.getWorld();
                    Vec3d pos = client.player.getPos();

                    BlockPos blockPos = new BlockPos(
                        (int) pos.getX(),
                        (int) pos.getY(),
                        (int) pos.getZ()
                    );

                    RegistryEntry<Biome> registryEntry = world.getBiome(blockPos);

                    RegistryKey<Biome> key = registryEntry.getKey().orElse(null);
                    if (key == null){
                        sendResponse(exchange, 404, "biome registry key not exist");
                        return;
                    }

                    String biomeName = key.getValue().getPath();

                    LOGGER.info("Obtain the biome where the player is located: {}", biomeName);

                    JSONObject jsonObject = new JSONObject();

                    jsonObject.put("name", biomeName);

                    sendResponse(exchange, 200, jsonObject.toJSONString());
                } catch (IOException e) {
                    LOGGER.error("Response failed", e);
                }
            });
        }
    }
}