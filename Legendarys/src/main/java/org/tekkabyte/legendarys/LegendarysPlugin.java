package org.tekkabyte.legendarys;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.tekkabyte.legendarys.command.LegendaryCommand;
import org.tekkabyte.legendarys.command.ViewLegendariesCommand;
import org.tekkabyte.legendarys.glow.GlowManager;
import org.tekkabyte.legendarys.gui.LegendaryViewerGui;
import org.tekkabyte.legendarys.legend.LegendaryService;
import org.tekkabyte.legendarys.legend.LegendaryServiceImpl;
import org.tekkabyte.legendarys.legendaryeffects.*;
import org.tekkabyte.legendarys.listener.LegendaryProtectionListener;
import org.tekkabyte.legendarys.listener.LegendaryViewerListener;
import org.tekkabyte.legendarys.lp.LuckPermsStarSuffixManager;
import org.tekkabyte.legendarys.storage.TemplateStorage;
import org.tekkabyte.legendarys.storage.TemplateStorageYaml;
import org.tekkabyte.legendarys.util.CooldownManager;
import org.tekkabyte.legendarys.util.SwordOfFlightHoldTask;

public class LegendarysPlugin extends JavaPlugin {

    private LuckPermsStarSuffixManager lpStarSuffixManager;
    private LegendaryService legendaryService;
    private TemplateStorage templateStorage;
    private GlowManager glowManager;
    private LegendaryViewerGui viewerGui;

    private CooldownManager cooldowns;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        lpStarSuffixManager = new LuckPermsStarSuffixManager(this);
        legendaryService = new LegendaryServiceImpl(this);

        templateStorage = new TemplateStorageYaml(this);
        templateStorage.load();

        glowManager = new GlowManager(this, legendaryService, lpStarSuffixManager);
        viewerGui = new LegendaryViewerGui(this, legendaryService, templateStorage);

        cooldowns = new CooldownManager();

        var legendaryCmd = new LegendaryCommand(this, legendaryService, templateStorage, viewerGui, glowManager);
        getCommand("legendary").setExecutor(legendaryCmd);
        getCommand("legendary").setTabCompleter(legendaryCmd);

        var viewCmd = new ViewLegendariesCommand(viewerGui);
        getCommand("viewlegendaries").setExecutor(viewCmd);

        var pm = Bukkit.getPluginManager();

        pm.registerEvents(new ParrySwordListener(this, legendaryService), this);
        pm.registerEvents(new ExcaliburListener(this, legendaryService, cooldowns), this);
        pm.registerEvents(new EvokerWandListener(this, legendaryService, cooldowns), this);
        pm.registerEvents(new IronGolemHammerListener(this, legendaryService, cooldowns), this);
        pm.registerEvents(new StormbreakerListener(this, legendaryService, cooldowns), this);
        pm.registerEvents(new DarkenedBladeListener(this, legendaryService, cooldowns), this);
        pm.registerEvents(new EnderKatanaListener(this, legendaryService, cooldowns), this);
        pm.registerEvents(new ExplosiveCrossbowListener(this, legendaryService, cooldowns), this);
        pm.registerEvents(new SwordOfFlightListener(this, legendaryService, cooldowns), this);
        pm.registerEvents(new LegendaryProtectionListener(this, legendaryService, glowManager), this);
        pm.registerEvents(new LegendaryViewerListener(this, viewerGui, templateStorage, legendaryService, glowManager), this);

        glowManager.start();
        Bukkit.getOnlinePlayers().forEach(glowManager::requestUpdate);

        new SwordOfFlightHoldTask(this, legendaryService).start();
    }

    @Override
    public void onDisable() {
        if (glowManager != null) glowManager.stop();
        if (templateStorage != null) templateStorage.save();
        if (lpStarSuffixManager != null) lpStarSuffixManager.shutdown();
    }
}