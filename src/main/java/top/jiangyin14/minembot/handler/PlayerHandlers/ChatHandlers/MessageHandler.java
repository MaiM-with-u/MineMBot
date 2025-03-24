package top.jiangyin14.minembot.handler.PlayerHandlers.ChatHandlers;

import com.alibaba.fastjson2.JSONObject;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandler.class);
    private static final Pattern PRIVATE_MESSAGE_PATTERN = Pattern.compile("^\\[(.+) -> (.+)] (.+)$");
    private static final Pattern NORMAL_MESSAGE_PATTERN = Pattern.compile("^<(.+)> (.+)$");

    private final WebSocketServer webSocketServer;
    private final Set<WebSocket> connectedClients = Collections.synchronizedSet(new HashSet<>());
    private final SimpleDateFormat realTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private boolean isInitialized = false;

    public MessageHandler() {
        webSocketServer = new WebSocketServer(new InetSocketAddress("127.0.0.1", 27633)) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                LOGGER.info("New WebSocket connection established: {}", conn.getRemoteSocketAddress());
                connectedClients.add(conn);
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                LOGGER.info("WebSocket connection closed: {}", conn.getRemoteSocketAddress());
                connectedClients.remove(conn);
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                // We don't process incoming messages in this implementation
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                LOGGER.error("WebSocket error occurred", ex);
                if (conn != null) {
                    connectedClients.remove(conn);
                }
            }

            @Override
            public void onStart() {
                LOGGER.info("Chat WebSocket server started on ws://127.0.0.1:27633/chatflow");
                setConnectionLostTimeout(30);
            }
        };
    }

    /**
     * Initializes the WebSocket server and registers chat message listeners
     */
    public void initialize() {
        if (!isInitialized) {
            // Start WebSocket server in a daemon thread
            Thread serverThread = new Thread(webSocketServer);
            serverThread.setDaemon(true);
            serverThread.start();

            // Register chat message listeners
            registerChatListeners();

            isInitialized = true;
            LOGGER.info("Chat message handler initialized");
        }
    }

    /**
     * Registers listeners for chat messages
     */
    private void registerChatListeners() {
        // Listen for chat messages (player messages)
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> handleChatMessage(message));

        // Note: GAME_MESSAGE is not available in your version of Fabric API
        // You may need to use a different approach to capture system messages
    }

    /**
     * Processes a chat message and sends it to connected WebSocket clients
     * @param message The chat message to process
     */
    private void handleChatMessage(Text message) {
        if (connectedClients.isEmpty()) {
            // No clients connected, no need to process
            return;
        }

        String messageText = message.getString();
        JSONObject payload = new JSONObject();

        // Add real-world time
        payload.put("realTime", realTimeFormat.format(new Date()));

        // Get in-game time
        MinecraftClient mcClient = MinecraftClient.getInstance();
        if (mcClient != null && mcClient.world != null) {
            long gameTime = mcClient.world.getTime();
            long timeOfDay = gameTime % 24000;
            int hours = (int) ((timeOfDay / 1000 + 6) % 24);
            int minutes = (int) ((timeOfDay % 1000) * 60 / 1000);
            payload.put("gameTime", String.format("%02d:%02d", hours, minutes));
            payload.put("gameTick", gameTime);
        }

        // Parse message format to determine type and extract components
        Matcher privateMatcher = PRIVATE_MESSAGE_PATTERN.matcher(messageText);
        if (privateMatcher.matches()) {
            payload.put("isPrivate", true);
            payload.put("sender", privateMatcher.group(1));
            payload.put("recipient", privateMatcher.group(2));
            payload.put("content", privateMatcher.group(3));
            payload.put("type", "private");
        } else {
            Matcher normalMatcher = NORMAL_MESSAGE_PATTERN.matcher(messageText);
            if (normalMatcher.matches()) {
                payload.put("isPrivate", false);
                payload.put("sender", normalMatcher.group(1));
                payload.put("content", normalMatcher.group(2));
                payload.put("type", "chat");
            } else {
                // System message or other format
                payload.put("isPrivate", false);
                payload.put("sender", "System");
                payload.put("content", messageText);
                payload.put("type", "system");
            }
        }

        // Add raw message
        payload.put("rawMessage", messageText);

        // Send to all connected clients
        String jsonMessage = payload.toJSONString();
        for (WebSocket client : connectedClients) {
            client.send(jsonMessage);
        }
    }
}