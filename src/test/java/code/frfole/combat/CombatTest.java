package code.frfole.combat;

import com.google.gson.Gson;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.damage.EntityDamage;
import net.minestom.server.entity.fakeplayer.FakePlayer;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.extensions.DiscoveredExtension;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTFloat;
import org.junit.jupiter.api.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class CombatTest {

    @Test
    public void test() throws InterruptedException {
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
        Phaser phaser = new Phaser(2);
        AtomicReference<Float> atomicDamage = new AtomicReference<>(0f);
        MinecraftServer.getGlobalEventHandler().addListener(EntityDamageEvent.class, event -> {
            if (event.getDamageType() instanceof EntityDamage) {
                event.setCancelled(true);
                atomicDamage.set(event.getDamage());
                phaser.arrive();
            }
        });

        // start server
        server.start("localhost", 0);
        Assertions.assertDoesNotThrow(CombatTest::registerExtensions, "Failed to register extensions");

        // spawn fake players
        AtomicReferenceArray<FakePlayer> fakePlayersRef = new AtomicReferenceArray<>(2);
        CountDownLatch latch = new CountDownLatch(2);
        FakePlayer.initPlayer(UUID.randomUUID(), "fakePlayer", newValue -> {
            fakePlayersRef.set((int) (latch.getCount() - 1), newValue);
            latch.countDown();
        });
        FakePlayer.initPlayer(UUID.randomUUID(), "fakePlayer", newValue -> {
            fakePlayersRef.set((int) (latch.getCount() - 1), newValue);
            latch.countDown();
        });
        Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS), "Fake players not spawned");
        FakePlayer fakePlayer1 = fakePlayersRef.get(0);
        FakePlayer fakePlayer2 = fakePlayersRef.get(1);

        { // test no components
            fakePlayer1.getController().attackEntity(fakePlayer2);
            phaser.arriveAndAwaitAdvance();
            Assertions.assertEquals(0f, atomicDamage.get(), "no components");
        }

        { // test single component
            fakePlayer1.setItemInMainHand(ItemStack.builder(Material.STONE)
                    .set(CombatExtension.COMBAT_TAG, List.of(
                            new CombatComponent("add_mul", NBT.Compound(builder -> {
                                builder.put("add", new NBTFloat(5f));
                                builder.put("mul", new NBTFloat(5f));
                            }))
                    ))
                    .build());
            fakePlayer1.getController().attackEntity(fakePlayer2);
            phaser.arriveAndAwaitAdvance();
            Assertions.assertEquals(25f, atomicDamage.get(), "single component");
        }

        { // test multiple components
            fakePlayer1.setItemInMainHand(ItemStack.builder(Material.STONE)
                    .set(CombatExtension.COMBAT_TAG, List.of(
                            new CombatComponent("set", new NBTFloat(5f)),
                            new CombatComponent("add_mul", NBT.Compound(builder -> {
                                builder.put("add", new NBTFloat(5f));
                                builder.put("mul", new NBTFloat(5f));
                            }))
                    ))
                    .build());
            fakePlayer1.getController().attackEntity(fakePlayer2);
            phaser.arriveAndAwaitAdvance();
            Assertions.assertEquals(50f, atomicDamage.get(), "multiple components");
        }
        { // test canceling
            fakePlayer1.setItemInMainHand(ItemStack.builder(Material.STONE)
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
            fakePlayer1.getController().attackEntity(fakePlayer2);
            phaser.arriveAndAwaitAdvance();
            Assertions.assertEquals(25f, atomicDamage.get(), "canceling");
        }
        { // test preferences
            fakePlayer1.setTag(CombatExtension.COMPONENT_PREFERENCES_TAG, List.of(ComponentHolderType.ENTITY, ComponentHolderType.ITEM));
            fakePlayer1.setTag(CombatExtension.COMBAT_TAG, List.of(
                    new CombatComponent("set", new NBTFloat(6f)),
                    new CombatComponent("add_mul", NBT.Compound(builder -> {
                        builder.put("add", new NBTFloat(5f));
                        builder.put("mul", new NBTFloat(5f));
                    }))
            ));
            fakePlayer1.setItemInMainHand(ItemStack.builder(Material.STONE)
                    .set(CombatExtension.COMBAT_TAG, List.of(
                            new CombatComponent("add_mul", NBT.Compound(builder -> {
                                builder.put("add", new NBTFloat(3f));
                                builder.put("mul", new NBTFloat(3f));
                            }))
                    ))
                    .build());
            fakePlayer1.getController().attackEntity(fakePlayer2);
            phaser.arriveAndAwaitAdvance();
            Assertions.assertEquals(174f, atomicDamage.get(), "preferences");
        }
        MinecraftServer.stopCleanly();
    }

    private static void registerExtensions() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        // prepare the extension
        InputStream resourceAsStream = ClassLoader.getSystemResourceAsStream("extension.json");
        Assertions.assertNotNull(resourceAsStream, "extension.json not found");
        DiscoveredExtension extension = new Gson().fromJson(new InputStreamReader(resourceAsStream), DiscoveredExtension.class);
        DiscoveredExtension.verifyIntegrity(extension);
        Method createClassLoaderMethod = extension.getClass().getDeclaredMethod("createClassLoader");
        createClassLoaderMethod.setAccessible(true);
        createClassLoaderMethod.invoke(extension);

        // load extensions
        Method loadMethod = MinecraftServer.getExtensionManager().getClass().getDeclaredMethod("loadExtensionList", List.class);
        loadMethod.setAccessible(true);
        loadMethod.invoke(MinecraftServer.getExtensionManager(), List.of(extension));
    }
}
