package code.frfole.combat;

import com.google.gson.Gson;
import net.minestom.server.MinecraftServer;
import net.minestom.server.extensions.DiscoveredExtension;
import org.junit.jupiter.api.Assertions;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class TestUtils {
    public static void registerExtension() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
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

    public static void setCombatComponents(CombatExtension extension, ComponentsBuilder builder) throws NoSuchFieldException, IllegalAccessException {
        Field field = extension.getClass().getDeclaredField("allComponents");
        field.setAccessible(true);
        field.set(extension, builder.getComponents());
    }
}
