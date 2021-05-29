package me.realized.duels.validator.validators.match;

import java.util.Collection;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.setting.Settings;
import me.realized.duels.validator.BaseBiValidator;
import org.bukkit.entity.Player;

public class ModeValidator extends BaseBiValidator<Collection<Player>, Settings> {

    public ModeValidator(final DuelsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean validate(final Collection<Player> players, final Settings settings) {
        if (!settings.isOwnInventory() && settings.getKit() == null) {
            lang.sendMessage(players, "DUEL.start-failure.mode-unselected");
            return false;
        }

        return true;
    }
}