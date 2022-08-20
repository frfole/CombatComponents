package code.frfole.combat;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.damage.EntityDamage;
import net.minestom.server.entity.fakeplayer.FakePlayer;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.validate.Check;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTFloat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class CombatTest {

    private final static ItemStack item_1 = ItemStack.builder(Material.STONE)
            .set(CombatExtension.COMBAT_TAG, List.of(
                    new CombatComponent("add_mul", NBT.Compound(builder -> {
                        builder.put("add", new NBTFloat(5f));
                        builder.put("mul", new NBTFloat(5f));
                    }))
            ))
            .build();
    private final static ItemStack item_2 = ItemStack.builder(Material.STONE)
            .set(CombatExtension.COMBAT_TAG, List.of(
                    new CombatComponent("set", new NBTFloat(5f)),
                    new CombatComponent("add_mul", NBT.Compound(builder -> {
                        builder.put("add", new NBTFloat(5f));
                        builder.put("mul", new NBTFloat(5f));
                    }))
            ))
            .build();


    public static void main(String[] args) throws InterruptedException {
        MinecraftServer server = MinecraftServer.init();
        InstanceContainer spawningInstance = MinecraftServer.getInstanceManager().createInstanceContainer();

        MinecraftServer.getGlobalEventHandler().addListener(CombatLoadingEvent.class, event -> {
            event.builder().addComponent("set", (context, data) -> {
                if (data instanceof NBTFloat nbtFloat)
                    context.modifyDamage(damage -> nbtFloat.getValue());
            });
            event.builder().addComponent("add_mul", (context, data) -> {
                if (data instanceof NBTCompound compound && compound.getFloat("add") != null && compound.getFloat("mul") != null) {
                    context.modifyDamage(damage -> (damage + compound.getFloat("add")) * compound.getFloat("mul"));
                }
            });
        });
        Phaser phaser = new Phaser(2);
        AtomicReference<Float> atomicDamage = new AtomicReference<>(0f);
        MinecraftServer.getGlobalEventHandler().addListener(EntityDamageEvent.class, event -> {
            if (event.getDamageType() instanceof EntityDamage damage) {
                event.setCancelled(true);
                atomicDamage.set(event.getDamage());
                phaser.arrive();
            }
        });
        MinecraftServer.getGlobalEventHandler().addListener(PlayerLoginEvent.class, event -> {
            event.setSpawningInstance(spawningInstance);
        });

        server.start("localhost", 0);

        AtomicReference<FakePlayer> fakePlayer1ref = new AtomicReference<>();
        AtomicReference<FakePlayer> fakePlayer2ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);
        FakePlayer.initPlayer(UUID.randomUUID(), "fakePlayer1", newValue -> {
            fakePlayer1ref.set(newValue);
            latch.countDown();
        });
        FakePlayer.initPlayer(UUID.randomUUID(), "fakePlayer2", newValue -> {
            fakePlayer2ref.set(newValue);
            latch.countDown();
        });
        if (latch.await(10, TimeUnit.SECONDS)) {
            FakePlayer fakePlayer1 = fakePlayer1ref.get();
            FakePlayer fakePlayer2 = fakePlayer2ref.get();

            fakePlayer1.getController().attackEntity(fakePlayer2);
            phaser.arriveAndAwaitAdvance();
            Check.argCondition(atomicDamage.get() != 0f, "excepted 0, got {0}", atomicDamage.get());

            fakePlayer1.setItemInMainHand(item_1);
            fakePlayer1.getController().attackEntity(fakePlayer2);
            phaser.arriveAndAwaitAdvance();
            Check.argCondition(atomicDamage.get() != 25f, "excepted 25, got {0}", atomicDamage.get());

            fakePlayer1.setItemInMainHand(item_2);
            fakePlayer1.getController().attackEntity(fakePlayer2);
            phaser.arriveAndAwaitAdvance();
            Check.argCondition(atomicDamage.get() != 50f, "excepted 50, got {0}", atomicDamage.get());

            fakePlayer1.setItemInMainHand(item_1);
            fakePlayer1.getController().attackEntity(fakePlayer2);
            phaser.arriveAndAwaitAdvance();
            Check.argCondition(atomicDamage.get() != 25f, "excepted 25, got {0}", atomicDamage.get());
        }
        MinecraftServer.stopCleanly();
    }
}
