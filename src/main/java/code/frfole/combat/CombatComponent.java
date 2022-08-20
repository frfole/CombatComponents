package code.frfole.combat;

import org.jglrxavpok.hephaistos.nbt.NBT;

/**
 * CombatComponent is used to store the combat data of an item.
 * @param name the identifier of the component
 * @param data additional data of the component
 */
public record CombatComponent(String name, NBT data) {
}
