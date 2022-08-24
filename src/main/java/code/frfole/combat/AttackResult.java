package code.frfole.combat;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

/**
 * Represents the result of an attack.
 * @param isCanceled whether the attack was canceled
 * @param damage the damage dealt
 * @param compound the data set in the {@link CombatContext}
 */
public record AttackResult(boolean isCanceled, float damage, @NotNull NBTCompound compound) {
    @Contract(pure = true)
    public @NotNull AttackResult withCompound(@NotNull NBTCompound compound) {
        return new AttackResult(isCanceled, damage, compound);
    }
}
