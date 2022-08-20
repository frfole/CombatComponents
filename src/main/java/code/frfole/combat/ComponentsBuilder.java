package code.frfole.combat;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBT;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Builder for creating map of components identifiers and their corresponding actions.
 */
public class ComponentsBuilder {
    private final Map<String, BiConsumer<CombatContext, NBT>> components = new HashMap<>();

    /**
     * Adds a component to the builder.
     * @param name the identifier of the component
     * @param action the component action
     * @return this builder
     */
    @Contract(mutates = "this", value = "_, _ -> this")
    public ComponentsBuilder addComponent(@NotNull String name, @NotNull BiConsumer<@NotNull CombatContext, @NotNull NBT> action) {
        components.put(name, action);
        return this;
    }

    /**
     * Gets the map of component identifiers to theirs actions.
     * @return the map of components
     */
    public @NotNull Map<String, BiConsumer<CombatContext, NBT>> getComponents() {
        return Map.copyOf(components);
    }
}
