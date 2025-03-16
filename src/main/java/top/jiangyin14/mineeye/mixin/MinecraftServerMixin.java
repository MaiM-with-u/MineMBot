package top.jiangyin14.mineeye.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.jiangyin14.mineeye.MineEye;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
	@Inject(method = "runServer", at = @At("HEAD"))
	private void onServerStart(CallbackInfo ci) {
		MinecraftServer server = (MinecraftServer) (Object) this;
		MineEye.getInstance().setServer(server);
	}
}
