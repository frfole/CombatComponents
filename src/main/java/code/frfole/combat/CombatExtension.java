package code.frfole.combat;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.extensions.Extension;
import net.minestom.server.inventory.EquipmentHandler;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
            .defaultValue(List.of(ComponentHolderType.ITEM_IN_MAIN_HAND));
    private @NotNull Map<String, BiConsumer<CombatContext, @Nullable NBT>> allComponents = Map.of();


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

    /**
     * Run attack for the given entities.
     * @param attacker the attacker
     * @param target   the target of attack
     * @return the result of attack
     */
    public @NotNull AttackResult attack(@NotNull Entity attacker, @NotNull Entity target) {
        List<CombatComponent> components = new ArrayList<>();
        for (ComponentHolderType holderType : attacker.getTag(COMPONENT_PREFERENCES_TAG)) {
            components.addAll(switch (holderType) {
                case ENTITY -> attacker.getTag(COMBAT_TAG);
                case ITEM_IN_MAIN_HAND -> attacker instanceof EquipmentHandler ?
                        ((EquipmentHandler) attacker).getItemInMainHand().getTag(COMBAT_TAG) :
                        List.of();
                case ITEM_IN_OFF_HAND -> attacker instanceof EquipmentHandler ?
                        ((EquipmentHandler) attacker).getItemInOffHand().getTag(COMBAT_TAG) :
                        List.of();
                default -> List.of();
            });
        }
        CombatComponent[] componentsArr = components.toArray(new CombatComponent[0]);
        CombatContext context = new CombatContext(attacker, target);
        int idx = 0;
        while (idx >= 0 && idx < componentsArr.length) {
            CombatComponent component = componentsArr[idx];
            context.setTag(CombatContext.NEXT_COMPONENT_IDX_TAG, idx + 1);
            if (component.ignoreCanceled() && context.isCanceled()) {
                idx = context.getTag(CombatContext.NEXT_COMPONENT_IDX_TAG);
                continue;
            }
            BiConsumer<CombatContext, NBT> consumer = allComponents.get(component.name());
            if (consumer == null) {
                idx = context.getTag(CombatContext.NEXT_COMPONENT_IDX_TAG);
                MinecraftServer.getExceptionManager().handleException(new IllegalArgumentException("Unknown combat component: " + component.name()));
                continue;
            }
            consumer.accept(context, component.data());
            idx = context.getTag(CombatContext.NEXT_COMPONENT_IDX_TAG);
        }
        float damage = context.getDamage();
        boolean canceled = context.isCanceled();
        if (!canceled && target instanceof LivingEntity livingTarget) {
            livingTarget.damage(DamageType.fromEntity(attacker), damage);
        }
        return new AttackResult(canceled, damage, context.tagHandler().asCompound());
    }

    private void onEntityAttack(@NotNull EntityAttackEvent event) {
        attack(event.getEntity(), event.getTarget());
    }
}
