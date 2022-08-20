package code.frfole.combat;

import net.minestom.server.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Event called when {@link CombatComponent} can be registered.
 * @param builder the builder to use to register {@link CombatComponent}.
 */
public record CombatLoadingEvent(@NotNull ComponentsBuilder builder) implements Event {
}
