package top.jiangyin14.minembot;

import com.sun.net.httpserver.HttpServer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.jiangyin14.minembot.handler.InfoHandler.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class MineMBot implements ModInitializer {
	public static final String MOD_ID = "minembot";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("MineMBot Initialized!");
		// Start HTTP server when mod initializes
		startHttpServer();
	}

	private void startHttpServer() {
		try {
			HttpServer httpServer = HttpServer.create(new InetSocketAddress(27632), 0);
			httpServer.createContext("/v1/player/chat/send", new ChatHandler());
			httpServer.createContext("/v1/player/info/coords", new CoordinateHandler());
			httpServer.createContext("/v1/player/info/biome", new BiomeHandler());
			httpServer.createContext("/v1/player/info/time", new TimeHandler());
			httpServer.createContext("/v1/player/info/facing", new FacingHandler());
			httpServer.setExecutor(Executors.newCachedThreadPool());
			httpServer.start();
			LOGGER.info("HTTP server started on port 27632");
		} catch (IOException e) {
			LOGGER.error("Failed to start HTTP server", e);
		}
	}

}