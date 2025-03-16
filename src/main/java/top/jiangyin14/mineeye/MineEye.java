package top.jiangyin14.mineeye;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class MineEye implements ModInitializer {
	public static final String MOD_ID = "mineeye";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private MinecraftServer server;
	private static final Gson GSON = new Gson();

	// Add singleton instance
	private static MineEye instance;

	public MineEye() {
		instance = this;
	}

	public static MineEye getInstance() {
		return instance;
	}

	@Override
	public void onInitialize() {
		LOGGER.info("MineEye Initialized!");
		startHttpServer();
	}
	private void startHttpServer() {
		try {
			HttpServer httpServer = HttpServer.create(new InetSocketAddress(27632), 0);
			// Updated API endpoint
			httpServer.createContext("/v1/user/message/send", new ChatHandler());
			httpServer.setExecutor(Executors.newCachedThreadPool());
			httpServer.start();
			LOGGER.info("HTTP server started on port 27632");
		} catch (IOException e) {
			LOGGER.error("Failed to start HTTP server", e);
		}
	}

	public void setServer(MinecraftServer server) {
		this.server = server;
		LOGGER.info("MinecraftServer has been set: {}", server != null ? "Success" : "Null");
	}

	@SuppressWarnings("UnusedMixin")
    @Mixin(MinecraftServer.class)
	public static class MinecraftServerMixin {
		@Inject(method = "runServer", at = @At("HEAD"))
		private void onServerStart(CallbackInfo ci) {
			MinecraftServer server = (MinecraftServer) (Object) this;
			MineEye.getInstance().setServer(server);
		}
	}

	private class ChatHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			LOGGER.info("Received request to /v1/user/message/send");

			if ("POST".equals(exchange.getRequestMethod())) {
				exchange.getResponseHeaders().add("Content-Type", "application/json");

				try {
					// Read request body
					InputStream is = exchange.getRequestBody();
					String requestBody = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
							.lines().collect(Collectors.joining("\n"));
					is.close();

					LOGGER.info("Request body: {}", requestBody);

					// Parse JSON
					JsonObject jsonRequest = GSON.fromJson(requestBody, JsonObject.class);
					LOGGER.info("Parsed JSON successfully");

					// Check if message field exists
					if (!jsonRequest.has("message")) {
						LOGGER.warn("Missing 'message' field in request");
						sendErrorResponse(exchange, 400, "Missing 'message' field");
						return;
					}

					// Extract message content
					String message = jsonRequest.get("message").getAsString();
					LOGGER.info("Message to send: {}", message);

					// Broadcast message to all players
					LOGGER.info("Server status: {}", server != null ? "Available" : "Not Available");
					if (server != null) {
						LOGGER.info("Broadcasting message to players");
						server.getPlayerManager().broadcast(Text.of(message), false);
						sendSuccessResponse(exchange);
					} else {
						LOGGER.warn("Cannot send message - server is null");
						sendErrorResponse(exchange, 503, "Server not available");
					}
				} catch (JsonParseException e) {
					LOGGER.error("JSON parsing error: {}", e.getMessage());
					sendErrorResponse(exchange, 400, "Invalid JSON format");
				} catch (Exception e) {
					LOGGER.error("Error processing request", e);
					sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
				}
			} else {
				LOGGER.warn("Received non-POST request: {}", exchange.getRequestMethod());
				exchange.sendResponseHeaders(405, -1);
			}
		}
	}

		private void sendSuccessResponse(HttpExchange exchange) throws IOException {
			String response = "{\"status\":\"success\",\"message\":\"Message sent\"}";
			exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(response.getBytes(StandardCharsets.UTF_8));
			}
		}

		private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
			String response = String.format("{\"status\":\"error\",\"message\":\"%s\"}", message);
			exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(response.getBytes(StandardCharsets.UTF_8));
			}
		}
	}
