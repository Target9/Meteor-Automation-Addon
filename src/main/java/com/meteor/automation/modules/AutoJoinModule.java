package com.meteor.automation.modules;

import com.meteor.automation.MeteorAutomation;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import com.meteor.automation.utils.LogUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

public class AutoJoinModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // Setting for enabling auto join
    private final Setting<Boolean> autoJoinEnabled = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-auto-join")
        .description("Automatically joins the desired game mode when in the lobby.")
        .defaultValue(true)
        .build());

    // Item setting for specifying the item used to open the menu
    private final Setting<Item> joinItem = sgGeneral.add(new ItemSetting.Builder()
        .name("join-item")
        .description("The item required to open the menu to join a game mode.")
        .defaultValue(Items.STONE) // Use Items.STONE directly
        .build());

    // Enum for join methods
    private enum JoinMethod {
        NPC,
        ITEM,
        CHAT_COMMAND
    }

    // Setting for selecting the join method
    private final Setting<JoinMethod> joinMethod = sgGeneral.add(new EnumSetting.Builder<JoinMethod>()
        .name("join-method")
        .description("Choose the method to join.")
        .defaultValue(JoinMethod.NPC)
        .build());

    private Entity targetedNpc; // Store the targeted NPC for joining

    public AutoJoinModule() {
        super(MeteorAutomation.UTILITY, "auto-join", "Automatically joins the server from the lobby.");
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (autoJoinEnabled.get() && isInLobby(event.screen)) {
            handleJoin();
        }
    }

    private boolean isInLobby(Screen screen) {
        return screen.getClass().getSimpleName().equals("MainMenuScreen"); // Change as per your lobby name
    }

    private void handleJoin() {
        switch (joinMethod.get()) {
            case NPC:
                clickNpc();
                break;
            case ITEM:
                useItemToOpenMenu();
                break;
            case CHAT_COMMAND:
                sendChatCommand();
                break;
            default:
                LogUtils.error("Invalid join method selected!");
        }
    }

    private void clickNpc() {
        if (targetedNpc != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.interactionManager.interactEntity(mc.player, targetedNpc, Hand.MAIN_HAND);
            LogUtils.info("Clicked on NPC to join the game.");
        } else {
            LogUtils.error("No NPC targeted for clicking.");
        }
    }

    @EventHandler
    private void onAttackEntity(AttackEntityEvent event) {
        // Set the targeted NPC when the player attacks it
        Entity entity = event.getEntity();
        if (entity != null) {
            targetedNpc = entity;
            LogUtils.info("Targeted NPC set to: " + entity.getName().getString());
        }
    }

    private void useItemToOpenMenu() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.getMainHandStack().getItem() == joinItem.get()) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND); // Simulate right-click
            LogUtils.info("Used item to open the menu for joining.");
        } else {
            LogUtils.error("Join item is not in hand.");
        }
    }

    private void sendChatCommand() {
        String command = "/join <your-game-mode>"; // Replace with the actual command
        ChatUtils.sendPlayerMsg(command); // Send the command to chat using ChatUtils
        LogUtils.info("Sent chat command to join the game.");
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        // Handle cleanup if necessary
        targetedNpc = null; // Reset targeted NPC when leaving the game
    }

    // Custom event class for AttackEntityEvent
    public static class AttackEntityEvent {
        private final Entity entity;

        public AttackEntityEvent(Entity entity) {
            this.entity = entity;
        }

        public Entity getEntity() {
            return entity;
        }
    }
}
