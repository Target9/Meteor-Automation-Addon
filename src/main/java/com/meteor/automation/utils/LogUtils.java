package com.meteor.automation.utils;

import meteordevelopment.meteorclient.mixininterface.IChatHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class LogUtils {
    protected static MinecraftClient mc = MinecraftClient.getInstance();

    // Info logging method
    public static void info(String txt) {
        assert mc.world != null;

        MutableText message = Text.literal("");
        message.append(Formatting.GRAY + "[" + Formatting.BLUE + "Auto" + Formatting.WHITE + "mation" + Formatting.GRAY + "] " + Formatting.GRAY);
        message.append(txt);

        IChatHud chatHud = (IChatHud) mc.inGameHud.getChatHud();
        chatHud.meteor$add(message, 0);
    }

    // Error logging method
    public static void error(String txt) {
        assert mc.world != null;

        MutableText message = Text.literal("");
        message.append(Formatting.GRAY + "[" + Formatting.RED + "Error" + Formatting.GRAY + "] " + Formatting.RED); // Using red for errors
        message.append(txt);

        IChatHud chatHud = (IChatHud) mc.inGameHud.getChatHud();
        chatHud.meteor$add(message, 0);
    }

    // Warning logging method
    public static void warning(String txt) {
        assert mc.world != null;

        MutableText message = Text.literal("");
        message.append(Formatting.GRAY + "[" + Formatting.YELLOW + "Warning" + Formatting.GRAY + "] " + Formatting.YELLOW); // Using yellow for warnings
        message.append(txt);

        IChatHud chatHud = (IChatHud) mc.inGameHud.getChatHud();
        chatHud.meteor$add(message, 0);
    }
}
