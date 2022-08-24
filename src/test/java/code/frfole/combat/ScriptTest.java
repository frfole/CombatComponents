package code.frfole.combat;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.tag.Tag;
import org.jglrxavpok.hephaistos.nbt.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ScriptTest {
    @Test
    public void cooldown() throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        final Tag<Long> lastHit = Tag.Long("lastHit").defaultValue(-1L);

        CombatExtension extension = new CombatExtension();
        ComponentsBuilder componentsBuilder = new ComponentsBuilder();
        componentsBuilder.addComponent("cooldown", (context, data) -> {
            if (data instanceof NBTLong delayTime) {
                long lastHitTime = context.getAttacker().getTag(lastHit);
                long currentTime = System.currentTimeMillis();
                if (lastHitTime == -1L || delayTime.getValue() + lastHitTime < currentTime) {
                    context.getAttacker().setTag(lastHit, currentTime);
                } else {
                    context.setCanceled(true);
                }
            }
        });
        TestUtils.setCombatComponents(extension, componentsBuilder);

        Entity entity1 = new Entity(EntityType.ARROW);
        Entity entity2 = new Entity(EntityType.ARROW);
        entity1.setTag(CombatExtension.COMPONENT_PREFERENCES_TAG, List.of(ComponentHolderType.ENTITY));
        entity1.setTag(CombatExtension.COMBAT_TAG, List.of(
                new CombatComponent("cooldown", new NBTLong(80L))
        ));

        Assertions.assertEquals(new AttackResult(false, 0f, NBTCompound.EMPTY), extension.attack(entity1, entity2).withCompound(NBTCompound.EMPTY));
        Assertions.assertEquals(new AttackResult(true, 0f, NBTCompound.EMPTY), extension.attack(entity1, entity2).withCompound(NBTCompound.EMPTY));
        Assertions.assertEquals(new AttackResult(true, 0f, NBTCompound.EMPTY), extension.attack(entity1, entity2).withCompound(NBTCompound.EMPTY));
        Assertions.assertEquals(new AttackResult(false, 0f, NBTCompound.EMPTY), extension.attack(entity2, entity1).withCompound(NBTCompound.EMPTY));
        Assertions.assertEquals(new AttackResult(false, 0f, NBTCompound.EMPTY), extension.attack(entity2, entity1).withCompound(NBTCompound.EMPTY));
        Thread.sleep(100);
        Assertions.assertEquals(new AttackResult(false, 0f, NBTCompound.EMPTY), extension.attack(entity1, entity2).withCompound(NBTCompound.EMPTY));
        Assertions.assertEquals(new AttackResult(true, 0f, NBTCompound.EMPTY), extension.attack(entity1, entity2).withCompound(NBTCompound.EMPTY));
    }

    @Test
    public void branched() throws NoSuchFieldException, IllegalAccessException {
        CombatExtension extension = new CombatExtension();
        ComponentsBuilder componentsBuilder = new ComponentsBuilder();
        componentsBuilder.addComponent("push", (context, data) -> {
            if (!(data instanceof NBTCompound compound)) return;
            String variableName = compound.getString("variable");
            if (variableName == null) return;
            context.setTag(Tag.NBT(variableName), compound.get("value"));
        });
        componentsBuilder.addComponent("ife", (context, data) -> {
            if (!(data instanceof NBTCompound compound)) return;
            String variableName1 = compound.getString("var_1");
            String variableName2 = compound.getString("var_2");
            String label = compound.getString("else_label");
            if (variableName1 == null || variableName2 == null || label == null) return;
            if (context.getTag(Tag.NBT(variableName1)).equals(context.getTag(Tag.NBT(variableName2)))) return;
            context.setCanceled(true);
            context.setTag(Tag.String("goto_label"), label);
        });
        componentsBuilder.addComponent("label", (context, data) -> {
            if (!(data instanceof NBTString label)) return;
            if (!label.getValue().equals(context.getTag(Tag.String("goto_label")))) return;
            context.setCanceled(false);
        });
        componentsBuilder.addComponent("goto", (context, data) -> {
            if (!(data instanceof NBTString label)) return;
            context.setTag(Tag.String("goto_label"), label.getValue());
            context.setCanceled(true);
        });
        componentsBuilder.addComponent("set_damage", (context, data) -> {
            if (!(data instanceof NBTFloat value)) return;
            context.modifyDamage(damage -> value.getValue());
        });
        TestUtils.setCombatComponents(extension, componentsBuilder);

        Entity entity1 = new Entity(EntityType.ARROW);
        Entity entity2 = new Entity(EntityType.ARROW);
        entity1.setTag(CombatExtension.COMPONENT_PREFERENCES_TAG, List.of(ComponentHolderType.ENTITY));
        entity1.setTag(CombatExtension.COMBAT_TAG, List.of(
                new CombatComponent("push", NBT.Compound(builder -> {
                    builder.put("variable", new NBTString("x"));
                    builder.put("value", new NBTLong(10L));
                })),
                new CombatComponent("push", NBT.Compound(builder -> {
                    builder.put("variable", new NBTString("y"));
                    builder.put("value", new NBTLong(10L));
                })),
                new CombatComponent("ife", NBT.Compound(builder -> {
                    builder.put("var_1", new NBTString("x"));
                    builder.put("var_2", new NBTString("y"));
                    builder.put("else_label", new NBTString("if_else_1"));
                })),
                new CombatComponent("set_damage", new NBTFloat(10f)),
                new CombatComponent("goto", new NBTString("if_end_1")),
                new CombatComponent("label", false, new NBTString("if_else_1")),
                new CombatComponent("set_damage", new NBTFloat(5f)),
                new CombatComponent("label", false, new NBTString("if_end_1"))
        ));

        System.out.println(extension.attack(entity1, entity2));
    }

    @Test
    public void flowControl() throws NoSuchFieldException, IllegalAccessException {
        CombatExtension extension = new CombatExtension();
        ComponentsBuilder componentsBuilder = new ComponentsBuilder();
        componentsBuilder.addComponent("set", (context, data) -> {
            if (!(data instanceof NBTFloat newDamage)) return;
            context.setTag(CombatContext.DAMAGE_TAG, newDamage.getValue());
        });
        componentsBuilder.addComponent("add", (context, data) -> {
            if (!(data instanceof NBTFloat newDamage)) return;
            context.modifyDamage(curDamage -> curDamage + newDamage.getValue());
        });
        componentsBuilder.addComponent("skip", (context, data) -> {
            if (!(data instanceof NBTInt skipAmount)) return;
            context.tagHandler().updateTag(CombatContext.NEXT_COMPONENT_IDX_TAG, idx -> idx + skipAmount.getValue());
        });
        TestUtils.setCombatComponents(extension, componentsBuilder);

        Entity entity1 = new Entity(EntityType.ARROW);
        Entity entity2 = new Entity(EntityType.ARROW);
        entity1.setTag(CombatExtension.COMPONENT_PREFERENCES_TAG, List.of(ComponentHolderType.ENTITY));

        entity1.setTag(CombatExtension.COMBAT_TAG, List.of(
                new CombatComponent("skip", new NBTInt(1)),
                new CombatComponent("set", new NBTFloat(10f)),
                new CombatComponent("add", new NBTFloat(8f))
        ));
        Assertions.assertEquals(
                new AttackResult(false, 8f, NBTCompound.EMPTY),
                extension.attack(entity1, entity2).withCompound(NBTCompound.EMPTY),
                "forward"
        );

        entity1.setTag(CombatExtension.COMBAT_TAG, List.of(
                new CombatComponent("skip", new NBTInt(2)),
                new CombatComponent("add", new NBTFloat(1f)),
                new CombatComponent("skip", new NBTInt(1)),
                new CombatComponent("skip", new NBTInt(-3))
        ));
        Assertions.assertEquals(
                new AttackResult(false, 1f, NBTCompound.EMPTY),
                extension.attack(entity1, entity2).withCompound(NBTCompound.EMPTY),
                "backward"
        );
    }
}
