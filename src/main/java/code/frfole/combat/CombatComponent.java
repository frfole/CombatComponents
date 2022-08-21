package code.frfole.combat;

import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBT;

/**
 * {@inheritDoc}
 */
public record CombatComponent(String name, boolean ignoreCanceled, @Nullable NBT data) {

    /**
     * CombatComponent is used to store the combat data of an item.
     * @param name the identifier of the component
     * @param data additional data of the component
     */
    public CombatComponent(String name, @Nullable  NBT data) {
        this(name, true, data);
    }

    /**
     * CombatComponent is used to store the combat data of an item.
     * @param name the identifier of the component
     * @param ignoreCanceled if true, the component will be ignored if the combat is canceled
     * @param data additional data of the component
     */
    public CombatComponent {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
    }
}
