package code.frfole.combat;

import net.minestom.server.entity.LivingEntity;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagHandler;
import net.minestom.server.tag.Taggable;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * Holds the context of a combat.
 */
public class CombatContext implements Taggable {
    /**
     * The tag used to represent the damage in the {@link CombatContext}.
     */
    public static final Tag<@NotNull Float> DAMAGE_TAG = Tag.Float("damage").defaultValue(0f);
    private final @NotNull LivingEntity attacker;
    private final @NotNull LivingEntity target;
    private final @NotNull TagHandler tagHandler = TagHandler.newHandler();

    public CombatContext(@NotNull LivingEntity attacker, @NotNull LivingEntity target) {
        this.attacker = attacker;
        this.target = target;
    }

    /**
     * Gets the attacker.
     * @return the attacking entity
     */
    public @NotNull LivingEntity getAttacker() {
        return attacker;
    }

    /**
     * Gets the entity, who is attacked.
     * @return the attacked entity
     */
    public @NotNull LivingEntity getTarget() {
        return target;
    }

    @Override
    public @NotNull TagHandler tagHandler() {
        return tagHandler;
    }

    /**
     * Modifies the damage.
     * @param operator the operator to modify the damage
     */
    public void modifyDamage(UnaryOperator<@NotNull Float> operator) {
        tagHandler.updateTag(DAMAGE_TAG, operator);
    }

    /**
     * Gets the current damage.
     * @return the current damage
     */
    public float getDamage() {
        return tagHandler.getTag(DAMAGE_TAG);
    }
}
