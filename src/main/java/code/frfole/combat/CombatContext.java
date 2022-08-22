package code.frfole.combat;

import net.minestom.server.entity.Entity;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagHandler;
import net.minestom.server.tag.Taggable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * Holds the context of a combat.
 */
@SuppressWarnings("unused")
public final class CombatContext implements Taggable {
    /**
     * The tag used to represent the damage in the {@link CombatContext}.
     */
    public static final Tag<@NotNull Float> DAMAGE_TAG = Tag.Float("damage").defaultValue(0f);
    /**
     * The tag used to indicate the attack is canceled.
     */
    public static final Tag<@NotNull Boolean> CANCEL_TAG = Tag.Boolean("cancel").defaultValue(false);
    private final @NotNull Entity attacker;
    private final @NotNull Entity target;
    @SuppressWarnings("UnstableApiUsage")
    private final @NotNull TagHandler tagHandler = TagHandler.newHandler();

    /**
     * Creates a new combat context.
     * @param attacker the attacker
     * @param target   the target
     */
    public CombatContext(@NotNull Entity attacker, @NotNull Entity target) {
        this.attacker = attacker;
        this.target = target;
    }

    /**
     * Gets the attacker.
     * @return the attacking entity
     */
    @Contract(pure = true)
    public @NotNull Entity getAttacker() {
        return attacker;
    }

    /**
     * Gets the entity, who is attacked.
     * @return the attacked entity
     */
    @Contract(pure = true)
    public @NotNull Entity getTarget() {
        return target;
    }

    @Contract(pure = true)
    @Override
    public @NotNull TagHandler tagHandler() {
        return tagHandler;
    }

    /**
     * Modifies the damage.
     * @param operator the operator to modify the damage
     */
    @Contract(mutates = "this")
    public void modifyDamage(UnaryOperator<@NotNull Float> operator) {
        //noinspection UnstableApiUsage
        tagHandler.updateTag(DAMAGE_TAG, operator);
    }

    /**
     * Gets the current damage.
     * @return the current damage
     */
    @Contract(pure = true)
    public float getDamage() {
        return getTag(DAMAGE_TAG);
    }

    /**
     * Gets the current cancel status.
     * @return true if the attack is canceled, false otherwise
     */
    @Contract(pure = true)
    public boolean isCanceled() {
        return getTag(CANCEL_TAG);
    }

    /**
     * Sets the cancel status.
     * @param cancel true if the attack is canceled, false otherwise
     */
    @Contract(mutates = "this")
    public void setCanceled(boolean cancel) {
        setTag(CANCEL_TAG, cancel);
    }
}
