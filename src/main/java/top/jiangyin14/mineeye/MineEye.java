package top.jiangyin14.mineeye;

import com.sun.net.httpserver.HttpServer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.jiangyin14.mineeye.handler.ChatHandler;
import top.jiangyin14.mineeye.handler.CoordinateHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

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
			httpServer.createContext("/v1/player/chat/send", new ChatHandler());
			httpServer.createContext("/v1/player/info/coords", new CoordinateHandler());
			httpServer.setExecutor(Executors.newCachedThreadPool());
			httpServer.start();
			LOGGER.info("HTTP server started on port 27632");
		} catch (IOException e) {
			LOGGER.error("Failed to start HTTP server", e);
		}
	}

}