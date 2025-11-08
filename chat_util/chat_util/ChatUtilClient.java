package me.cutebow.chat_util;

import me.cutebow.chat_util.config.ChatUtilConfig;
import net.fabricmc.api.ClientModInitializer;

public class ChatUtilClient implements ClientModInitializer {
    public static ChatUtilConfig CONFIG;

    @Override
    public void onInitializeClient() {
        CONFIG = ChatUtilConfig.load();
    }
}
