package com.meteor.automation.modules;

import com.meteor.automation.MeteorAutomation;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import com.meteor.automation.utils.LogUtils;
import com.meteor.automation.utils.DiscordWebhook;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerTriggerModule extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // Define a setting for messages to send
    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
        .name("messages")
        .description("Messages to send when a player enters range.")
        .defaultValue(List.of("Notice: You have entered my range!"))
        .build()
    );

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("Radius in blocks to detect players.")
        .defaultValue(64)
        .min(1)
        .sliderRange(1, 128)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay between each message in ticks.")
        .defaultValue(20)
        .min(1)
        .sliderMax(200)
        .build()
    );

    private final Setting<Boolean> disableOnDisconnect = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-disconnect")
        .description("Disables the module when you leave or are disconnected from a server.")
        .defaultValue(true)
        .build()
    );

    // Setting to control sending messages to Discord
    private final Setting<Boolean> sendToDiscord = sgGeneral.add(new BoolSetting.Builder()
        .name("send-to-discord")
        .description("Toggle to send messages to Discord upon player detection.")
        .defaultValue(true)
        .build()
    );

    // Setting to control sending messages to chat
    private final Setting<Boolean> sendToChat = sgGeneral.add(new BoolSetting.Builder()
        .name("chat")
        .description("Toggle to send messages to in-game chat upon player detection.")
        .defaultValue(true)
        .build()
    );

    private DiscordWebhook webhook;
    private final File configDirectory = new File(System.getProperty("user.home"), "DiscordNotifiers");
    private final File webhookFile = new File(configDirectory, "discord_webhook.txt");

    private final Set<PlayerEntity> detectedPlayers = new HashSet<>();
    private final Set<PlayerEntity> playersInCycle = new HashSet<>();
    private int messageIndex = 0;
    private int timer = 0;

    public PlayerTriggerModule() {
        super(MeteorAutomation.UTILITY, "player-trigger", "Notifies when a player enters a defined range.");
        loadWebhookFromFile();
    }

    @Override
    public void onActivate() {
        timer = 0; // Reset timer on activation
        messageIndex = 0; // Reset message index when activated
        detectedPlayers.clear();
        playersInCycle.clear();
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disableOnDisconnect.get() && event.screen instanceof DisconnectedScreen) {
            toggle();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disableOnDisconnect.get()) toggle();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // Count down the timer
        if (timer > 0) {
            timer--; // Decrease timer if active
            return; // Exit tick event if timer is active
        }

        Set<PlayerEntity> currentPlayersInRange = new HashSet<>();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity player && player != mc.player) { // Ensure entity is a player and it's not this player

                // Check if player is within specified range based on radius setting
                if (mc.player.distanceTo(player) <= radius.get()) {
                    currentPlayersInRange.add(player);

                    if (!detectedPlayers.contains(player)) {
                        detectedPlayers.add(player); // Add player to detected list
                        playersInCycle.add(player); // Add player to cycle list
                        handlePlayerEnter(player); // Handle player enter event with player parameter
                    }
                }
            }
        }

        // Remove players who have left the range
        detectedPlayers.retainAll(currentPlayersInRange);
    }

    private void handlePlayerEnter(PlayerEntity player) {
        // Reset message index when a new player enters the range
        messageIndex = 0;

        // Send the first message or continue cycle if a player was in the range previously
        sendNextMessage();

        // Send message to Discord if the toggle is enabled, only when player first enters
        if (sendToDiscord.get()) {
            sendDiscordMessage(player.getName().getString());
        }
    }

    private void sendNextMessage() {
        if (playersInCycle.isEmpty()) return;

        if (messageIndex < messages.get().size()) {
            for (PlayerEntity player : playersInCycle) {
                String playerName = player.getName().getString();
                String message = messages.get().get(messageIndex).replace("{player}", playerName); // Replace placeholder
                if (sendToChat.get()) {
                    ChatUtils.sendPlayerMsg(message); // Send the message to chat
                }
            }
            messageIndex++; // Increment the message index
            timer = delay.get(); // Set the timer for the next message
        } else {
            // Check for new players in the next tick
            playersInCycle.clear(); // Clear cycle list to check for new players
        }
    }

    @EventHandler
    private void onTickEvent(TickEvent.Post event) {
        // If the timer is active, decrement it
        if (timer > 0) {
            timer--; // Decrease timer if it's active
            return; // Exit tick event if timer is active
        }

        // This will handle sending the next message in the cycle
        sendNextMessage();
    }

    private void sendDiscordMessage(String playerName) {
        if (webhook != null) {
            try {
                DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject()
                    .setTitle("Player Entered Range")
                    .setDescription(playerName + " has entered the range!")
                    .setColor(Color.GREEN)
                    .setThumbnail("https://cdn.discordapp.com/attachments/1209202131106537553/1272496116268793856/icon.png?ex=66bb2fdb&is=66b9de5b&hm=447d57b17be5d18584b84b82150b48ceb11f5ce438fcc723dea1ecc8c34916f2&");

                webhook.addEmbed(embed);
                webhook.execute(); // Send the message to Discord
                webhook.clearEmbeds(); // Clear embeds after sending
                LogUtils.info("Message sent to Discord: " + playerName + " has entered the range!");
            } catch (IOException e) {
                LogUtils.error("Failed to send message to Discord: " + e.getMessage());
            }
        }
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
