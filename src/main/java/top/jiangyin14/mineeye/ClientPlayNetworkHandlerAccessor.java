package top.jiangyin14.mineeye;

import net.minecraft.network.message.LastSeenMessagesCollector;

public interface ClientPlayNetworkHandlerAccessor {
    LastSeenMessagesCollector getLastSeenMessagesCollector();
}