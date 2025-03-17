package top.jiangyin14.mineeye.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * the base class for all handlers
 * @author ChangingSelf
 */
public abstract class BaseHandler implements HttpHandler {
    public final Logger LOGGER = LoggerFactory.getLogger(getClass());// logger for all classes that extend BaseHandler

    protected void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    protected MinecraftClient getClient() {
        return MinecraftClient.getInstance();
    }
}