package top.jiangyin14.mineeye;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.network.message.LastSeenMessagesCollector;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MineEye implements ModInitializer {
	public static final String MOD_ID = "mineeye";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("MineEye Initialized!");
		// Start HTTP server when mod initializes
		startHttpServer();
	}

	private void startHttpServer() {
		try {
			HttpServer httpServer = HttpServer.create(new InetSocketAddress(27632), 0);
			// Create API route "/v1/player/chat/send"
			httpServer.createContext("/v1/player/chat/send", new ChatHandler());
			// Keep the original endpoint for backward compatibility
			httpServer.createContext("/sendChat", new ChatHandler());
			httpServer.setExecutor(Executors.newCachedThreadPool());
			httpServer.start();
			LOGGER.info("HTTP server started on port 27632");
		} catch (IOException e) {
			LOGGER.error("Failed to start HTTP server", e);
		}
	}

	// API request handler
	private class ChatHandler implements HttpHandler {
		private final Pattern messagePattern = Pattern.compile("\"message\"\\s*:\\s*\"(.*?)\"");

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if ("POST".equals(exchange.getRequestMethod())) {
				// Read request body
				InputStream is = exchange.getRequestBody();
				String requestBody = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
						.lines().collect(Collectors.joining("\n"));
				is.close();

				// Extract message from JSON
				String message = extractMessageFromJson(requestBody);

				if (message == null || message.isEmpty()) {
					sendResponse(exchange, 400, "{\"success\":false,\"error\":\"Missing 'message' field in JSON\"}");
					return;
				}

				// Send the message as the player
				boolean success = sendPlayerMessage(message);

				// Return response
				String response = success
						? "{\"success\":true,\"message\":\"Message sent as player\"}"
						: "{\"success\":false,\"error\":\"Failed to send message\"}";
				sendResponse(exchange, 200, response);
			} else {
				// Only allow POST requests
				sendResponse(exchange, 405, "{\"success\":false,\"error\":\"Method not allowed\"}");
			}
		}

		private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
			OutputStream os = exchange.getResponseBody();
			os.write(response.getBytes(StandardCharsets.UTF_8));
			os.close();
		}

		private String extractMessageFromJson(String json) {
			// Simple regex to extract message field from JSON
			Matcher matcher = messagePattern.matcher(json);
			if (matcher.find()) {
				return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
			}
			return null;
		}
	}

	// Method to send a message as the player
	private boolean sendPlayerMessage(String message) {
		try {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client == null || client.getNetworkHandler() == null || client.player == null) {
				LOGGER.error("Client, network handler, or player is null");
				return false;
			}

			ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();

			client.execute(() -> {
				try {
					// For modern Minecraft versions - direct packet sending
					Instant instant = Instant.now();
					long randomLong = NetworkEncryptionUtils.SecureRandomUtil.nextLong();

					// Get the LastSeenMessagesCollector using our accessor
					if (networkHandler instanceof ClientPlayNetworkHandlerAccessor accessor) {
						LastSeenMessagesCollector collector = accessor.getLastSeenMessagesCollector();
						LastSeenMessagesCollector.LastSeenMessages lastSeen = collector.collect();

						// Create and send the chat packet
						networkHandler.sendPacket(new ChatMessageC2SPacket(
								message,
								instant,
								randomLong,
								null,  // No signature data
								lastSeen.update()
						));
                        LOGGER.info("Sent message as player: {}", message);
					} else {
						LOGGER.error("Cannot access LastSeenMessagesCollector - message not sent");
					}
				} catch (Exception e) {
					LOGGER.error("Error sending chat packet", e);
				}
			});
			return true;
		} catch (Exception e) {
			LOGGER.error("Failed to send player message", e);
			return false;
		}
	}
}