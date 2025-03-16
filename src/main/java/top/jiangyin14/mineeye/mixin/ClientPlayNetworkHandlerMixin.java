package top.jiangyin14.mineeye.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.message.LastSeenMessagesCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import top.jiangyin14.mineeye.ClientPlayNetworkHandlerAccessor;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin implements ClientPlayNetworkHandlerAccessor {
	@Accessor
	public abstract LastSeenMessagesCollector getLastSeenMessagesCollector();
}
