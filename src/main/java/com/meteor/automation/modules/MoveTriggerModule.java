package com.meteor.automation.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.MinecraftClient;

import com.meteor.automation.MeteorAutomation;
import com.meteor.automation.utils.LogUtils;
import com.meteor.automation.utils.DiscordWebhook; // Import your DiscordWebhook class

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class MoveTriggerModule extends Module {
    private BlockPos lastPosition; // Store the player's last position
    private DiscordWebhook webhook; // Save the webhook instance
    private final File configDirectory = new File(System.getProperty("user.home"), "DiscordNotifiers"); // Directory for config
    private final File webhookFile = new File(configDirectory, "discord_webhook.txt"); // Webhook file path

    // Use a StringListSetting to manage chat messages that will act as commands
    private final Setting<List<String>> chatCommands = settings.getDefaultGroup().add(new StringListSetting.Builder()
        .name("chat-commands")
        .description("List of commands to execute upon movement.")
        .defaultValue(List.of("You moved!")) // Sample command or additional commands
        .build()
    );

    // New toggle setting to control sending messages to Discord
    public Setting<Boolean> sendToDiscord = settings.getDefaultGroup().add(new BoolSetting.Builder()
        .name("send-to-discord")
        .description("Toggle to send messages to Discord upon movement.")
        .defaultValue(true) // Default is true, can adjust as needed
        .build());

    public MoveTriggerModule() {
        super(MeteorAutomation.UTILITY, "move-trigger", "Triggers actions when the player moves.");
        lastPosition = null; // Initialize lastPosition to null at startup
        loadWebhookFromFile(); // Load the webhook URL from the file
    }

    // Setting groups and settings for the module
    public SettingGroup sgGeneral = settings.getDefaultGroup();
    public Setting<Boolean> enableChatMessage = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-chat-message")
        .description("Send commands upon movement.")
        .defaultValue(true)
        .build());

    public Setting<Boolean> disableOnLeave = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-leave")
        .description("Disable this module when the game is left.")
        .defaultValue(true) // Default is true, can adjust as needed
        .build());

    @Override
    public void onActivate() {
        // Record the current position when the module is activated
        if (mc.player != null) {
            lastPosition = mc.player.getBlockPos(); // Set lastPosition to the player's current position
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Post event) {
        if (mc.player != null) {
            PlayerEntity player = mc.player; // Get the player entity
            BlockPos currentPosition = player.getBlockPos(); // Get the player's current position

            // Check if the player has moved (by checking if the position has changed)
            if (!currentPosition.equals(lastPosition)) {
                lastPosition = currentPosition; // Update last known position

                // Check if messages should be sent to Discord
                if (sendToDiscord.get()) {
                    sendDiscordMessage(player.getName().getString() + " has moved!"); // Customize the message as needed
                }

                // Execute all commands immediately upon movement
                if (enableChatMessage.get()) {
                    sendAllChatCommands(); // Send commands when the player moves
                }
            }
        }
    }

    private void sendAllChatCommands() {
        // Send all chat commands from the list
        if (enableChatMessage.get() && !chatCommands.get().isEmpty()) {
            for (String command : chatCommands.get()) {
                sendChatCommand(command); // Send each command
            }
        }
    }

    private void sendChatCommand(String command) {
        // Send a packet with the command
        MinecraftClient.getInstance().player.networkHandler.sendChatMessage(command); // Send the command
    }

    private void sendDiscordMessage(String message) {
        if (webhook != null) {
            try {
                webhook.setContent(message);
                webhook.execute(); // Send the message to Discord
                LogUtils.info("Message sent to Discord: " + message); // Log the message
            } catch (IOException e) {
                LogUtils.error("Failed to send message to Discord: " + e.getMessage());
            }
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disableOnLeave.get()) toggle();
    }

    // Method to load the webhook from a configuration file
    private void loadWebhookFromFile() {
        if (webhookFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(webhookFile))) {
                String url = reader.readLine();
                if (url != null && !url.isEmpty()) {
                    webhook = new DiscordWebhook(url); // Initialize the webhook with the loaded URL
                    LogUtils.info("Webhook URL loaded from file: " + url);
                }
            } catch (IOException e) {
                LogUtils.error("Failed to load webhook URL from file: " + e.getMessage());
            }
        }
    }
}
