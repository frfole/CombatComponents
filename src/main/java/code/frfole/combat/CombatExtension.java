package code.frfole.combat;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.extensions.Extension;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public final class CombatExtension extends Extension {
    /**
     * The tag used to represent list of combat components.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static final Tag<List<CombatComponent>> COMBAT_TAG = Tag.Structure("combat_components", CombatComponent.class)
            .list()
            .defaultValue(List.of());
    /**
     * The tag used to represent the preference of combat components.
     * Components are sorted by their holder with order defined by this tag.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static final Tag<List<ComponentHolderType>> COMPONENT_PREFERENCES_TAG = Tag.Integer("component_preferences")
            .map(ComponentHolderType::fromInteger, ComponentHolderType::ordinal)
            .list()
            .defaultValue(List.of(ComponentHolderType.ITEM));
    private @NotNull Map<String, BiConsumer<CombatContext, NBT>> allComponents = Map.of();


    @Override
    public void initialize() {
        CombatLoadingEvent loadingEvent = new CombatLoadingEvent(new ComponentsBuilder());
        MinecraftServer.getGlobalEventHandler().call(loadingEvent);
        this.allComponents = loadingEvent.builder().getComponents();
        getEventNode().addListener(EntityAttackEvent.class, this::onEntityAttack);
    }

    @Override
    public void terminate() {
        this.allComponents = Map.of();
    }

    private void onEntityAttack(@NotNull EntityAttackEvent event) {
        if (event.getEntity() instanceof LivingEntity attacker && event.getTarget() instanceof LivingEntity target) {
            List<CombatComponent> components = new ArrayList<>();
            for (ComponentHolderType holderType : event.getEntity().getTag(COMPONENT_PREFERENCES_TAG)) {
                components.addAll(switch (holderType) {
                    case ENTITY -> attacker.getTag(COMBAT_TAG);
                    case ITEM -> attacker.getItemInMainHand().getTag(COMBAT_TAG);
                    default -> List.of();
                });
            }
            CombatContext context = new CombatContext(attacker, target);
            for (CombatComponent component : components) {
                if (component.ignoreCanceled() && context.isCanceled()) continue;
                BiConsumer<CombatContext, NBT> consumer = allComponents.get(component.name());
                if (consumer == null) {
                    MinecraftServer.getExceptionManager().handleException(new IllegalArgumentException("Unknown combat component: " + component.name()));
                    continue;
                }
                consumer.accept(context, component.data());
            }
            if (!context.isCanceled()) {
                target.damage(DamageType.fromEntity(attacker), context.getDamage());
            }
        }
    }
}
