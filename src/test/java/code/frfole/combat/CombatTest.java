package code.frfole.combat;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.damage.EntityDamage;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTFloat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class CombatTest {

    @Test
    public void test() {
        // init server
        MinecraftServer server = MinecraftServer.init();
        InstanceContainer spawningInstance = MinecraftServer.getInstanceManager().createInstanceContainer();
        MinecraftServer.getGlobalEventHandler().addListener(PlayerLoginEvent.class, event -> event.setSpawningInstance(spawningInstance));

        // register components
        MinecraftServer.getGlobalEventHandler().addListener(CombatLoadingEvent.class, event -> {
            event.builder().addComponent("set", (context, data) -> {
                if (data instanceof NBTFloat nbtFloat)
                    context.modifyDamage(damage -> nbtFloat.getValue());
            });
            event.builder().addComponent("add_mul", (context, data) -> {
                if (data instanceof NBTCompound compound && compound.getFloat("add") != null && compound.getFloat("mul") != null) {
                    //noinspection ConstantConditions
                    context.modifyDamage(damage -> (damage + compound.getFloat("add")) * compound.getFloat("mul"));
                }
            });
            event.builder().addComponent("cancel", (context, data) -> context.setCanceled(true));
            event.builder().addComponent("uncancel", (context, data) -> context.setCanceled(false));
        });

        // listen to damage event
        AtomicReference<Float> atomicDamage = new AtomicReference<>(0f);
        MinecraftServer.getGlobalEventHandler().addListener(EntityDamageEvent.class, event -> {
            if (event.getDamageType() instanceof EntityDamage) {
                event.setCancelled(true);
                atomicDamage.set(event.getDamage());
            }
        });

        // start server
        server.start("localhost", 0);
        Assertions.assertDoesNotThrow(TestUtils::registerExtension, "Failed to register extensions");

        // spawn entities
        EntityCreature entity1 = new EntityCreature(EntityType.PAINTING);
        EntityCreature entity2 = new EntityCreature(EntityType.PAINTING);

        { // test no components
            entity1.attack(entity2);
            Assertions.assertEquals(0f, atomicDamage.get(), "no components");
        }

        { // test single component
            entity1.setItemInMainHand(ItemStack.builder(Material.STONE)
                    .set(CombatExtension.COMBAT_TAG, List.of(
                            new CombatComponent("add_mul", NBT.Compound(builder -> {
                                builder.put("add", new NBTFloat(5f));
                                builder.put("mul", new NBTFloat(5f));
                            }))
                    ))
                    .build());
            entity1.attack(entity2);
            Assertions.assertEquals(25f, atomicDamage.get(), "single component");
        }

        { // test multiple components
            entity1.setItemInMainHand(ItemStack.builder(Material.STONE)
                    .set(CombatExtension.COMBAT_TAG, List.of(
                            new CombatComponent("set", new NBTFloat(5f)),
                            new CombatComponent("add_mul", NBT.Compound(builder -> {
                                builder.put("add", new NBTFloat(5f));
                                builder.put("mul", new NBTFloat(5f));
                            }))
                    ))
                    .build());
            entity1.attack(entity2);
            Assertions.assertEquals(50f, atomicDamage.get(), "multiple components");
        }
        { // test canceling
            entity1.setItemInMainHand(ItemStack.builder(Material.STONE)
                    .set(CombatExtension.COMBAT_TAG, List.of(
                            new CombatComponent("cancel", null),
                            new CombatComponent("set", new NBTFloat(5f)),
                            new CombatComponent("uncancel", false, null),
                            new CombatComponent("add_mul", NBT.Compound(builder -> {
                                builder.put("add", new NBTFloat(5f));
                                builder.put("mul", new NBTFloat(5f));
                            }))
                    ))
                    .build());
            entity1.attack(entity2);
            Assertions.assertEquals(25f, atomicDamage.get(), "canceling");
        }
        { // test preferences
            entity1.setTag(CombatExtension.COMPONENT_PREFERENCES_TAG, List.of(ComponentHolderType.ENTITY, ComponentHolderType.ITEM_IN_MAIN_HAND));
            entity1.setTag(CombatExtension.COMBAT_TAG, List.of(
                    new CombatComponent("set", new NBTFloat(6f)),
                    new CombatComponent("add_mul", NBT.Compound(builder -> {
                        builder.put("add", new NBTFloat(5f));
                        builder.put("mul", new NBTFloat(5f));
                    }))
            ));
            entity1.setItemInMainHand(ItemStack.builder(Material.STONE)
                    .set(CombatExtension.COMBAT_TAG, List.of(
                            new CombatComponent("add_mul", NBT.Compound(builder -> {
                                builder.put("add", new NBTFloat(3f));
                                builder.put("mul", new NBTFloat(3f));
                            }))
                    ))
                    .build());
            entity1.attack(entity2);
            Assertions.assertEquals(174f, atomicDamage.get(), "preferences");
        }
        MinecraftServer.stopCleanly();
    }
}
