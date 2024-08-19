package com.meteor.automation;

import com.meteor.automation.modules.PlayerTriggerModule;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import com.meteor.automation.modules.DiscordNotifier;
import com.meteor.automation.modules.MoveTriggerModule;

public class MeteorAutomation extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category UTILITY = new Category("Utils");

    @Override
    public void onInitialize() {
        LOG.info("Initializing MeteorAutomation Addon");

        // Modules
        Modules.get().add(new DiscordNotifier());
        Modules.get().add(new MoveTriggerModule());
        Modules.get().add(new PlayerTriggerModule());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(UTILITY);
    }

    @Override
    public String getPackage() {
        return "com.meteor.automation";
    }
}
