package com.meteor.automation.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules; // Importing the Modules class
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.MinecraftClient;

import com.meteor.automation.MeteorAutomation;
import com.meteor.automation.utils.LogUtils;

import java.util.List;

public class MoveTriggerModule extends Module {
    private BlockPos lastPosition;

    // Use a StringListSetting to manage chat messages that will act as commands
    private final Setting<List<String>> chatCommands = settings.getDefaultGroup().add(new StringListSetting.Builder()
        .name("chat-commands")
        .description("List of commands to execute upon movement.")
        .defaultValue(List.of("/say You moved!")) // Sample command
        .build()
    );

    public MoveTriggerModule() {
        super(MeteorAutomation.UTILITY, "move-trigger", "Triggers actions when the player moves.");
        lastPosition = null; // Initialize lastPosition to null at startup
    }

    // Setting groups and settings for the module
    public SettingGroup sgGeneral = settings.getDefaultGroup();
    public Setting<Boolean> enableChatMessage = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-chat-message")
        .description("Send commands upon movement.")
        .defaultValue(true)
        .build());

    public Setting<Boolean> disableModules = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-modules")
        .description("Disable specific modules upon movement.")
        .defaultValue(true)
        .build());

    public Setting<String> modulesToDisable = sgGeneral.add(new StringSetting.Builder()
        .name("modules-to-disable")
        .description("Comma-separated list of modules to disable.")
        .defaultValue("exampleModule1,exampleModule2") // Replace with actual module names
        .build());

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Post event) {
        if (mc.player != null) {
            PlayerEntity player = mc.player;
            BlockPos currentPosition = player.getBlockPos();

            // Check if the player has moved (by checking if the position has changed)
            if (lastPosition == null || !currentPosition.equals(lastPosition)) {
                lastPosition = currentPosition; // Update last known position

                // Execute all commands immediately upon movement
                if (enableChatMessage.get()) {
                    sendAllChatCommands();
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

    private void disableSpecifiedModules() {
        // Get a comma-separated list of module names and convert it to an array
        String[] modules = modulesToDisable.get().split(",");

        // Loop through each module name
        for (String moduleName : modules) {
            Module foundModule = Modules.get().get(moduleName.trim()); // Find the module by name

            // Disable the module if it is found and active
            if (foundModule != null) {
                if (foundModule.isActive()) {
                    foundModule.toggle(); // Disable the module
                    LogUtils.info(moduleName.trim() + " module has been disabled.");
                } else {
                    LogUtils.warning(moduleName.trim() + " module was already disabled.");
                }
            } else {
                LogUtils.warning("Module " + moduleName.trim() + " not found."); // Log if not found
            }
        }
    }

    private void sendChatCommand(String command) {
        // Send a packet with the command
        MinecraftClient.getInstance().player.networkHandler.sendChatMessage(command); // Send the command
    }
}
