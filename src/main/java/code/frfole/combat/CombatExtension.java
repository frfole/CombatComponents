package code.frfole.combat;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.extensions.Extension;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBT;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public final class CombatExtension extends Extension {
    public static final Tag<List<CombatComponent>> COMBAT_TAG = Tag.Structure("combat_components", CombatComponent.class)
            .list()
            .defaultValue(List.of());
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
            List<CombatComponent> components = attacker.getItemInMainHand().getTag(COMBAT_TAG);
            CombatContext context = new CombatContext(attacker, target);
            for (CombatComponent component : components) {
                BiConsumer<CombatContext, NBT> consumer = allComponents.get(component.name());
                if (consumer == null) {
                    MinecraftServer.getExceptionManager().handleException(new IllegalArgumentException("Unknown combat component: " + component.name()));
                    continue;
                }
                consumer.accept(context, component.data());
            }
            target.damage(DamageType.fromEntity(attacker), context.getDamage());
        }
    }
}
