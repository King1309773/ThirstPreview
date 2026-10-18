package dev.rlcraftceaddons.thirstpreview.client;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class GraveStoneBarPreviewTest {
    private static final String TYPE =
            "dev.rlcraftceaddons.thirstpreview.client.GraveStoneBarPreview";
    private static final String TARGET = "com.zzt.grave_stone.client.hud.PotionBarsRenderer";
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void missingOptionalModLoadsAndNoOpsWithoutMinecraft() throws Exception {
        Object preview = target(TYPE).getDeclaredConstructor().newInstance();
        invoke(preview, "beginFrame", new Class<?>[0]);
        invoke(preview, "capture", new Class<?>[] {
                int.class, int.class, boolean.class, boolean.class}, 320, 240, true, false);
        invoke(preview, "render", new Class<?>[] {
                int.class, int.class, int.class, int.class, float.class}, 320, 240, 7, 15, 1F);
    }

    @Test
    public void missingTargetIsProbedOnlyOnce() throws Exception {
        AtomicInteger lookups = new AtomicInteger();
        ClassLoader missingTarget = new ClassLoader(getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve)
                    throws ClassNotFoundException {
                if (TARGET.equals(name)) {
                    lookups.incrementAndGet();
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }
        };
        Constructor<?> constructor = target(TYPE + "$ReflectiveConfig")
                .getDeclaredConstructor(ClassLoader.class);
        constructor.setAccessible(true);
        Object config = constructor.newInstance(missingTarget);
        assertNull(invoke(config, "read", new Class<?>[0]));
        assertNull(invoke(config, "read", new Class<?>[0]));
        assertEquals(1, lookups.get());
    }

    @Test
    public void nonlocalAndDirectoryCodeSourcesAreRejected() throws Exception {
        assertNull(verifiedJar(new URL("https://example.invalid/GraveStone.jar")));
        assertNull(verifiedJar(temporary.getRoot().toURI().toURL()));
        assertNull(verifiedJar(null));
    }

    @Test
    public void localJarWithAnotherFingerprintIsRejected() throws Exception {
        File jar = temporary.newFile("GraveStone-1.0.0.jar");
        Files.write(jar.toPath(), new byte[] {1, 2, 3, 4});
        assertNull(verifiedJar(jar.toURI().toURL()));
    }

    @Test
    public void onlyTheVerifiedTargetJarIsAcceptedWhenProvided() throws Exception {
        String jar = System.getProperty("thirstpreview.gravestoneJar");
        Assume.assumeNotNull(jar);
        assertEquals(new File(jar).getCanonicalFile(),
                verifiedJar(new File(jar).toURI().toURL()));
    }

    @Test
    public void everyDisplayAndTakeoverSwitchIsRequired() throws Exception {
        assertTrue((Boolean) invoke(settings(true, true, true, true, true),
                "active", new Class<?>[0]));
        for (int disabled = 0; disabled < 4; disabled++) {
            boolean[] values = {true, true, true, true, true};
            values[disabled] = false;
            assertFalse((Boolean) invoke(settings(values), "active", new Class<?>[0]));
        }
        assertTrue((Boolean) invoke(settings(true, true, true, true, false),
                "active", new Class<?>[0]));
    }

    @Test
    public void anyConfigurationChangeInvalidatesTheSnapshot() throws Exception {
        Object before = settings(true, true, true, true, true);
        Class<?> settingsType = target(TYPE + "$Settings");
        assertTrue((Boolean) invoke(before, "sameAs", new Class<?>[] {settingsType},
                settings(true, true, true, true, true)));
        assertFalse((Boolean) invoke(before, "sameAs", new Class<?>[] {settingsType},
                new Object[] {null}));
        for (int changed = 0; changed < 5; changed++) {
            boolean[] values = {true, true, true, true, true};
            values[changed] = false;
            assertFalse((Boolean) invoke(before, "sameAs", new Class<?>[] {settingsType},
                    settings(values)));
        }
    }

    @Test
    public void commonVisibilityAndNativeDisplayPreventProbing() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        Object preview = preview(() -> {
            reads.incrementAndGet();
            return null;
        });
        invoke(preview, "beginFrame", new Class<?>[0]);
        invoke(preview, "capture", new Class<?>[] {
                int.class, int.class, boolean.class, boolean.class}, 320, 240, false, false);
        invoke(preview, "beginFrame", new Class<?>[0]);
        invoke(preview, "capture", new Class<?>[] {
                int.class, int.class, boolean.class, boolean.class}, 320, 240, true, true);
        assertEquals(0, reads.get());
    }

    @Test
    public void aReadFailureDisablesOnlyThisBridgeAndWarnsOnce() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        AtomicInteger warnings = new AtomicInteger();
        Logger logger = Logger.getLogger(TYPE);
        boolean previousUseParent = logger.getUseParentHandlers();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) { warnings.incrementAndGet(); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        try {
            Object preview = preview(() -> {
                reads.incrementAndGet();
                throw new IllegalAccessException("configuration unavailable");
            });
            for (int frame = 0; frame < 2; frame++) {
                invoke(preview, "beginFrame", new Class<?>[0]);
                invoke(preview, "capture", new Class<?>[] {
                        int.class, int.class, boolean.class, boolean.class}, 320, 240, true, false);
                invoke(preview, "render", new Class<?>[] {
                        int.class, int.class, int.class, int.class, float.class}, 320, 240, 7, 15, 1F);
            }
            assertEquals(1, reads.get());
            assertEquals(1, warnings.get());
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(previousUseParent);
        }
    }

    @Test
    public void overlayColorsStayTranslucentAndDistinguishGainFromLoss() throws Exception {
        assertEquals(0x8080DFFF, color(true, 1F));
        assertEquals(0x80FF6060, color(false, Float.POSITIVE_INFINITY));
        assertEquals(0x0080DFFF, color(true, -1F));
        assertEquals(0x0080DFFF, color(true, Float.NaN));
    }

    @Test
    public void aDrawingFailureIsContainedAndDisablesFurtherDrawing() throws Exception {
        assertDrawingFailureQuarantined(new IllegalStateException("state capture unavailable"));
    }

    @Test
    public void aDrawingLinkageFailureIsContainedAndDisablesFurtherDrawing() throws Exception {
        assertDrawingFailureQuarantined(new NoClassDefFoundError("optional drawing dependency"));
    }

    private static void assertDrawingFailureQuarantined(Throwable failure) throws Exception {
        AtomicInteger reads = new AtomicInteger();
        AtomicInteger subsequentDraws = new AtomicInteger();
        AtomicInteger warnings = new AtomicInteger();
        Logger logger = Logger.getLogger(TYPE);
        boolean previousUseParent = logger.getUseParentHandlers();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) { warnings.incrementAndGet(); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        try {
        Object preview = preview(() -> {
            reads.incrementAndGet();
            return null;
        });
        invoke(preview, "renderSafely", new Class<?>[] {Runnable.class}, (Runnable) () -> {
            if (failure instanceof RuntimeException) {
                throw (RuntimeException) failure;
            }
            throw (Error) failure;
        });
        invoke(preview, "renderSafely", new Class<?>[] {Runnable.class},
                (Runnable) subsequentDraws::incrementAndGet);
        assertNull(invoke(preview, "readSettings", new Class<?>[0]));
        assertEquals(0, reads.get());
        assertEquals(0, subsequentDraws.get());
        assertEquals(1, warnings.get());
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(previousUseParent);
        }
    }

    private static Object settings(boolean... flags) throws Exception {
        Constructor<?> constructor = target(TYPE + "$Settings").getDeclaredConstructor(
                boolean.class, boolean.class, boolean.class, boolean.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(flags[0], flags[1], flags[2], flags[3], flags[4]);
    }

    private interface Read { Object read() throws Exception; }

    private static Object preview(Read read) throws Exception {
        Class<?> accessType = target(TYPE + "$ConfigAccess");
        Object access = Proxy.newProxyInstance(accessType.getClassLoader(), new Class<?>[] {accessType},
                (proxy, method, arguments) -> read.read());
        Constructor<?> constructor = target(TYPE).getDeclaredConstructor(accessType);
        constructor.setAccessible(true);
        return constructor.newInstance(access);
    }

    private static Object verifiedJar(URL source) throws Exception {
        return invokeStatic("verifiedJar", new Class<?>[] {URL.class}, source);
    }

    private static int color(boolean gain, float alpha) throws Exception {
        return (Integer) invokeStatic("overlayColor", new Class<?>[] {boolean.class, float.class},
                gain, alpha);
    }

    private static Object invokeStatic(String name, Class<?>[] parameters, Object... arguments)
            throws Exception {
        Method method = target(TYPE).getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method.invoke(null, arguments);
    }

    private static Object invoke(Object receiver, String name, Class<?>[] parameters,
                                 Object... arguments) throws Exception {
        Method method;
        try {
            method = receiver.getClass().getDeclaredMethod(name, parameters);
        } catch (NoSuchMethodException missingBehavior) {
            fail("The requested bar bridge behavior is missing: " + name);
            return null;
        }
        method.setAccessible(true);
        return method.invoke(receiver, arguments);
    }

    private static Class<?> target(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException missingImplementation) {
            fail("The requested optional bar bridge is missing: " + name);
            return null;
        }
    }
}
