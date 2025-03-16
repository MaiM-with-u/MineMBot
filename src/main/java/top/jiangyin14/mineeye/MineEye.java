package top.jiangyin14.mineeye;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import net.fabricmc.api.ModInitializer;
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

public class MineEye implements ModInitializer {
	public static final String MOD_ID = "mineeye";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// 保存服务器实例，方便后续发送聊天消息
	private MinecraftServer server;

	@Override
	public void onInitialize() {
		LOGGER.info("MineEye Initialized!");
		// 模组初始化时启动 HTTP 服务器
		startHttpServer();
	}

	// 在服务器启动时注入 MinecraftServer 实例
	public void setServer(MinecraftServer server) {
		this.server = server;
	}

	private void startHttpServer() {
		try {
			HttpServer httpServer = HttpServer.create(new InetSocketAddress(27632), 0);
			// 创建 API 路由 "/sendChat"
			httpServer.createContext("/sendChat", new ChatHandler());
			httpServer.setExecutor(Executors.newCachedThreadPool());
			httpServer.start();
			System.out.println("HTTP server started on port 27632");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 定义 API 请求的处理器
	private class ChatHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if ("POST".equals(exchange.getRequestMethod())) {
				// 读取请求体中的消息内容（假设为纯文本格式）
				InputStream is = exchange.getRequestBody();
				String message = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
						.lines().collect(Collectors.joining("\n"));
				is.close();

				// 调用游戏内 API，将消息广播到所有玩家
				if (server != null) {
					server.getPlayerManager().broadcast(Text.of(message), false);
				}

				// 返回响应
				String response = "消息已发送";
				exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
				OutputStream os = exchange.getResponseBody();
				os.write(response.getBytes(StandardCharsets.UTF_8));
				os.close();
			} else {
				// 只允许 POST 请求，其他请求返回 405 Method Not Allowed
				exchange.sendResponseHeaders(405, -1);
			}
		}
	}
}
