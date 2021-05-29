package me.realized.duels.validator;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.arena.ArenaManagerImpl;
import me.realized.duels.config.Config;
import me.realized.duels.config.Lang;
import me.realized.duels.data.UserManagerImpl;
import me.realized.duels.request.RequestManager;
import me.realized.duels.spectate.SpectateManagerImpl;
import me.realized.duels.util.validate.BiValidator;

public abstract class BaseBiValidator<T1, T2> implements BiValidator<T1, T2> {

    protected final DuelsPlugin plugin;

    protected final Config config;
    protected final Lang lang;
    protected final UserManagerImpl userManager;
    protected final ArenaManagerImpl arenaManager;
    protected final SpectateManagerImpl spectateManager;
    protected final RequestManager requestManager;

    public BaseBiValidator(final DuelsPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.lang = plugin.getLang();
        this.userManager = plugin.getUserManager();
        this.arenaManager = plugin.getArenaManager();
        this.spectateManager = plugin.getSpectateManager();
        this.requestManager = plugin.getRequestManager();
    }

    @Override
    public boolean shouldValidate() {
        return true;
    }
}