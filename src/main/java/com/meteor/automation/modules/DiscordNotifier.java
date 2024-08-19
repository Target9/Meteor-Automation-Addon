package com.meteor.automation.modules;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.entity.player.PlayerEntity; // For detecting players
import net.minecraft.util.math.BlockPos;

import com.meteor.automation.MeteorAutomation;
import com.meteor.automation.utils.ChunkUtils;
import com.meteor.automation.utils.DiscordWebhook;
import com.meteor.automation.utils.LogUtils;
import meteordevelopment.meteorclient.systems.friends.Friends; // Import for Friends system

import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class DiscordNotifier extends Module {

    private final String name = "Meteor Automation Addon";
    private final String avatar = "https://cdn.discordapp.com/attachments/1209202131106537553/1272496116268793856/icon.png?ex=66bb2fdb&is=66b9de5b&hm=447d57b17be5d18584b84b82150b48ceb11f5ce438fcc723dea1ecc8c34916f2&";

    private DiscordWebhook hook; // Private hook variable

    // File to store the webhook URL
    private final File configDirectory = new File(System.getProperty("user.home"), "DiscordNotifiers");
    private final File webhookFile = new File(configDirectory, "discord_webhook.txt"); // Use the newly created directory

    public SettingGroup sgGeneral = settings.getDefaultGroup();
    public SettingGroup sgNotifs = settings.createGroup("Notifications");

    public DiscordNotifier() {
        super(MeteorAutomation.UTILITY, "discord-notifier", "Sends notifications to a Discord webhook on certain events.");

        // Setup configuration directory
        if (!configDirectory.exists()) {
            configDirectory.mkdirs(); // Create the directory if it does not exist
        }

        // Load the webhook URL from the file if it exists
        loadWebhookFromFile();
    }

    // Method to get access to the webhook safely
    public DiscordWebhook getWebhook() {
        return hook; // Public getter method for the hook
    }

    // Discord webhook settings
    private final Setting<String> link = sgGeneral.add(new StringSetting.Builder()
        .name("webhook-URL")
        .description("Discord Webhook URL to send messages to")
        .defaultValue("")
        .onChanged(url -> {
            if (url.isEmpty()) {
                hook = null; // Set hook to null if the URL is empty
                LogUtils.info("Webhook URL set to null."); // Inform about the change without revealing the URL
            } else {
                hook = new DiscordWebhook(url); // Set the valid URL to the webhook class
                saveWebhookToFile(url); // Save the new webhook URL to file
            }
        })
        .build());

    private final Setting<DiscordNotifier.PingModes> pingMode = sgGeneral.add(new EnumSetting.Builder<DiscordNotifier.PingModes>()
        .name("ping-mode")
        .description("How the notifier should ping")
        .defaultValue(PingModes.NoPing)
        .build());

    private final Setting<String> userId = sgGeneral.add(new StringSetting.Builder()
        .name("discord-ID")
        .description("ID of the user to ping")
        .defaultValue("")
        .visible(() -> pingMode.get() == PingModes.User)
        .build());

    private final Setting<Boolean> stashNotifier = sgNotifs.add(new BoolSetting.Builder()
        .name("stash-notifier")
        .defaultValue(true)
        .build());

    private final Setting<Integer> chestLimit = sgNotifs.add(new IntSetting.Builder()
        .name("chest-limit")
        .description("How many chests until you get notified")
        .sliderRange(0, 100)
        .defaultValue(50)
        .visible(stashNotifier::get)
        .build());

    private final Setting<Integer> shulkerLimit = sgNotifs.add(new IntSetting.Builder()
        .name("shulker-limit")
        .description("How many shulkers until you get notified")
        .sliderRange(0, 100)
        .defaultValue(50)
        .visible(stashNotifier::get)
        .build());

    private final Setting<Boolean> deathNotifier = sgNotifs.add(new BoolSetting.Builder()
        .name("death-notifier")
        .description("Get notified on death")
        .defaultValue(true)
        .build());

    @Override
    public void onActivate() {
        // Load the webhook from file when the module is activated
        loadWebhookFromFile();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onOpenScreen(OpenScreenEvent event) throws IOException {
        assert mc.player != null;
        if (hook == null) return;

        if (event.screen instanceof DeathScreen && deathNotifier.get()) {
            BlockPos pos = mc.player.getBlockPos();
            String posStr = "X: " + pos.getX() + " Y: " + pos.getY() + " Z: " + pos.getZ();
            readyHook(hook);
            hook.addEmbed(new DiscordWebhook.EmbedObject()
                .setTitle("You have died! (" + mc.player.getName() + ")")
                .setColor(Color.YELLOW)
                .addField("Coordinates:", posStr, false)
                .addField("Dimension:", PlayerUtils.getDimension().toString(), false)
                .setThumbnail(avatar));
            hook.execute();
            hook.clearEmbeds();
        }
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) throws IOException {
        assert mc.player != null;
        if (hook == null) return;

        // Handle stash notifications as before
        if (stashNotifier.get()) {
            BlockPos pos = event.chunk().getPos().getStartPos();
            String posStr = "X: " + pos.getX() + " Z: " + pos.getZ();
            int chestCount = ChunkUtils.getChestCount(event.chunk());
            int shulkerCount = ChunkUtils.getShulkerCount(event.chunk());
            if (chestCount > chestLimit.get() || shulkerCount > shulkerLimit.get()) {
                readyHook(hook);
                hook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle("Unusual chest or shulker amount! (" + mc.player.getName() + ")")
                    .setColor(Color.YELLOW)
                    .addField("Coordinates:", posStr, false)
                    .addField("Chest Amount:", String.valueOf(chestCount), false)
                    .addField("Shulker Amount:", String.valueOf(shulkerCount), false)
                    .setThumbnail(avatar));
                hook.execute();
                hook.clearEmbeds();
            }
        }
    }

    private void readyHook(DiscordWebhook hook) {
        assert mc.player != null;
        if (hook == null) return;

        String mention = "";
        switch (pingMode.get()) {
            case Everyone -> mention = "@everyone";
            case User -> mention = "<@" + userId.get() + ">";
            case NoPing -> mention = "";
        }

        hook.setContent(mention);
        hook.setAvatarUrl(avatar);
        hook.setUsername(name);
    }

    private void saveWebhookToFile(String url) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(webhookFile))) {
            writer.write(url);
            LogUtils.info("Webhook URL saved to file."); // Change log output to not reveal the URL
        } catch (IOException e) {
            LogUtils.error("Failed to save webhook URL to file: " + e.getMessage());
        }
    }

    private void loadWebhookFromFile() {
        if (webhookFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(webhookFile))) {
                String url = reader.readLine();
                if (url != null && !url.isEmpty()) {
                    // Set the loaded URL to the Setting group without logging the URL
                    link.set(url);
                    hook = new DiscordWebhook(url); // Initialize the webhook only if valid
                }
            } catch (IOException e) {
                LogUtils.error("Failed to load webhook URL from file: " + e.getMessage());
            }
        }
    }

    public enum PingModes {
        User,
        Everyone,
        NoPing
    }
}
