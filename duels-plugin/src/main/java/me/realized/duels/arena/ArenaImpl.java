package me.realized.duels.arena;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.arena.Arena;
import me.realized.duels.api.event.arena.ArenaSetPositionEvent;
import me.realized.duels.api.event.arena.ArenaStateChangeEvent;
import me.realized.duels.api.event.match.MatchEndEvent;
import me.realized.duels.api.event.match.MatchEndEvent.Reason;
import me.realized.duels.api.match.Match;
import me.realized.duels.duel.DuelManager.OpponentInfo;
import me.realized.duels.gui.BaseButton;
import me.realized.duels.kit.KitImpl;
import me.realized.duels.match.AbstractMatch;
import me.realized.duels.match.MatchState;
import me.realized.duels.match.matches.DuelMatch;
import me.realized.duels.match.matches.PartyDuelMatch;
import me.realized.duels.queue.Queue;
import me.realized.duels.setting.Settings;
import me.realized.duels.util.compat.Items;
import me.realized.duels.util.inventory.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArenaImpl extends BaseButton implements Arena {

    @Getter
    private final String name;
    @Getter
    private boolean disabled;
    @Getter
    private final Set<KitImpl> kits = new HashSet<>();
    @Getter
    private final Map<Integer, Location> positions = new HashMap<>();

    private AbstractMatch match;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    private Countdown countdown;
    @Getter
    @Setter(value = AccessLevel.PACKAGE)
    private boolean removed;

    public ArenaImpl(final DuelsPlugin plugin, final String name) {
        super(plugin, ItemBuilder
            .of(Items.EMPTY_MAP)
            .name(plugin.getLang().getMessage("GUI.arena-selector.buttons.arena.name", "name", name))
            .lore(plugin.getLang().getMessage("GUI.arena-selector.buttons.arena.lore-unavailable").split("\n"))
            .build()
        );
        this.name = name;
    }

    public boolean isAvailable() {
        return !isDisabled() && !isUsed() && getPosition(1) != null && getPosition(2) != null;
    }

    private void updateGui() {
        setLore(lang.getMessage("GUI.arena-selector.buttons.arena.lore-unavailable").split("\n"));
        arenaManager.getGui().calculatePages();
    }

    public DuelMatch startDuelMatch(final KitImpl kit, final Map<UUID, List<ItemStack>> items, final int bet, final Queue source) {
        this.match = new DuelMatch(plugin, this, kit, items, bet, source);
        updateGui();
        return (DuelMatch) match;
    }

    public PartyDuelMatch startPartyDuelMatch(final KitImpl kit) {
        this.match = new PartyDuelMatch(plugin, this, kit);
        updateGui();
        return (PartyDuelMatch) match;
    }

    public void endMatch(final UUID winner, final UUID loser, final Reason reason) {
        spectateManager.stopSpectating(this);

        final MatchEndEvent event = new MatchEndEvent(match, winner, loser, reason);
        Bukkit.getPluginManager().callEvent(event);

        final Queue source = match.getSource();
        match.setState(MatchState.FINISHED);
        match = null;

        if (source != null) {
            source.update();
            queueManager.getGui().calculatePages();
        }

        setLore(lang.getMessage("GUI.arena-selector.buttons.arena.lore-available").split("\n"));
        arenaManager.getGui().calculatePages();
    }

    public void startCountdown(final String kit, final Map<UUID, OpponentInfo> info) {
        final List<String> messages = config.getCdMessages();

        if (messages.isEmpty()) {
            return;
        }

        this.countdown = new Countdown(plugin, this, kit, info, messages, config.getTitles());
        countdown.runTaskTimer(plugin, 0L, 20L);
    }

    boolean isCounting() {
        return countdown != null;
    }

    public void add(final Player player) {
        if (!isUsed()) {
            return;
        }

        match.addPlayer(player);
    }

    public void remove(final Player player) {
        if (!isUsed()) {
            return;
        }

        match.removePlayer(player);
    }

    public boolean isEndGame() {
        return isUsed() && match.getState() == MatchState.END_GAME;
    }

    public Player first() {
        return isUsed() ? match.getAlivePlayers().iterator().next() : null;
    }

    public Set<Player> getPlayers() {
        return isUsed() ? match.getAllPlayers() : Collections.emptySet();
    }

    public boolean isBoundless() {
        return kits.isEmpty();
    }

    public boolean isBound(final KitImpl kit) {
        return kit != null && kits.contains(kit);
    }

    public void bind(final KitImpl kit) {
        if (isBound(kit)) {
            kits.remove(kit);
        } else {
            kits.add(kit);
        }
        arenaManager.saveArenas();
    }

    public void broadcast(final String message) {
        final List<Player> receivers = Lists.newArrayList(getPlayers());
        spectateManager.getSpectatorsImpl(this)
            .stream()
            .map(spectator -> Bukkit.getPlayer(spectator.getUuid()))
            .forEach(receivers::add);
        receivers.forEach(player -> player.sendMessage(message));
    }

    @Nullable
    @Override
    public Location getPosition(final int pos) {
        return positions.get(pos);
    }

    @Override
    public boolean setPosition(@Nullable final Player source, final int pos, @NotNull final Location location) {
        Objects.requireNonNull(location, "location");

        if (pos <= 0 || pos > 2) {
            return false;
        }

        final ArenaSetPositionEvent event = new ArenaSetPositionEvent(source, this, pos, location);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        positions.put(pos, location);
        arenaManager.saveArenas();
        setLore(lang.getMessage("GUI.arena-selector.buttons.arena.lore-" + (isAvailable() ? "available" : "unavailable")).split("\n"));
        arenaManager.getGui().calculatePages();
        return true;
    }

    @Override
    public boolean setPosition(final int pos, @NotNull final Location location) {
        return setPosition(null, pos, location);
    }

    @Override
    public boolean setDisabled(@Nullable final CommandSender source, final boolean disabled) {
        final ArenaStateChangeEvent event = new ArenaStateChangeEvent(source, this, disabled);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        this.disabled = event.isDisabled();
        arenaManager.saveArenas();
        setLore(lang.getMessage("GUI.arena-selector.buttons.arena.lore-" + (isAvailable() ? "available" : "unavailable")).split("\n"));
        arenaManager.getGui().calculatePages();
        return true;
    }

    @Override
    public boolean setDisabled(final boolean disabled) {
        return setDisabled(null, disabled);
    }

    @Override
    public boolean isUsed() {
        return this.match != null;
    }

    public AbstractMatch getMatchImpl() {
        return this.match;
    }

    @Nullable
    @Override
    public Match getMatch() {
        return getMatchImpl();
    }

    @Override
    public boolean has(@NotNull final Player player) {
        Objects.requireNonNull(player, "player");
        return isUsed() && match.hasAlivePlayer(player);
    }

    @Override
    public void onClick(final Player player) {
        if (!isAvailable()) {
            return;
        }

        final Settings settings = settingManager.getSafely(player);
        final KitImpl kit = settings.getKit();

        if (kit != null && !arenaManager.isSelectable(kit, this)) {
            lang.sendMessage(player, "ERROR.setting.arena-not-applicable", "kit", kit.getName());
            return;
        }

        settings.setArena(this);
        settings.openGui(player);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        final ArenaImpl arena = (ArenaImpl) other;
        return Objects.equals(name, arena.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}