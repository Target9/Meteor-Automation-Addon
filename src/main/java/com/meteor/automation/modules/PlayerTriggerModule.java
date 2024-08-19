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

    private final Set<PlayerEntity> detectedPlayers = new HashSet<>();
    private final Set<PlayerEntity> playersInCycle = new HashSet<>();
    private int messageIndex = 0;
    private int timer = 0;

    public PlayerTriggerModule() {
        super(MeteorAutomation.UTILITY, "player-trigger", "Notifies when a player enters a defined range.");
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
                        handlePlayerEnter();
                    }
                }
            }
        }

        // Remove players who have left the range
        detectedPlayers.retainAll(currentPlayersInRange);
    }

    private void handlePlayerEnter() {
        // Reset message index when a new player enters the range
        messageIndex = 0;

        // Send the first message or continue cycle if a player was in the range previously
        sendNextMessage();
    }

    private void sendNextMessage() {
        if (playersInCycle.isEmpty()) return;

        if (messageIndex < messages.get().size()) {
            for (PlayerEntity player : playersInCycle) {
                String playerName = player.getName().getString();
                String message = messages.get().get(messageIndex).replace("{player}", playerName); // Replace placeholder
                ChatUtils.sendPlayerMsg(message); // Send the message
            }
            messageIndex++; // Increment the message index
            timer = delay.get(); // Set the timer for the next message
        } else {
            // Check for new players in the next tick
            playersInCycle.clear(); // Clear cycle list to check for new players
            handlePlayerEnter();
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
}
