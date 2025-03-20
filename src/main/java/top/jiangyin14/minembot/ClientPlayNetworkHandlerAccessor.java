package top.jiangyin14.minembot;

import net.minecraft.network.message.LastSeenMessagesCollector;

public interface ClientPlayNetworkHandlerAccessor {
    LastSeenMessagesCollector getLastSeenMessagesCollector();
}