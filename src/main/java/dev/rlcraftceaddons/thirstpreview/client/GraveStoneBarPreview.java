package dev.rlcraftceaddons.thirstpreview.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.client.GuiIngameForge;

/** Optional, read-only adapter for one verified GraveStone hydration renderer. */
public final class GraveStoneBarPreview {
    private static final Logger LOGGER = Logger.getLogger(GraveStoneBarPreview.class.getName());
    private static final String SUPPORTED_SHA256 =
            "5D0013C23B177281688D5FA191922C61F06FC60A1530FC700196F85109BBB354";
    private final GraveStoneBarMath.Frame frame = new GraveStoneBarMath.Frame();
    private final ConfigAccess config;
    private Settings capturedSettings;
    private boolean disabled;

    public GraveStoneBarPreview() {
        this(new ReflectiveConfig(GraveStoneBarPreview.class.getClassLoader()));
    }

    GraveStoneBarPreview(ConfigAccess config) {
        this.config = config;
    }

    public void beginFrame() {
        frame.beginFrame();
        capturedSettings = null;
    }

    /** Called at Post(ALL), LOW, before the target reserves its hydration row. */
    public void capture(int scaledWidth, int scaledHeight, boolean commonVisible, boolean nativeDisplay) {
        capturedSettings = null;
        if (!commonVisible || nativeDisplay) {
            frame.discard();
            return;
        }
        Settings settings = readSettings();
        if (settings == null || !settings.active()) {
            frame.discard();
            return;
        }
        frame.capture(scaledWidth, scaledHeight, GuiIngameForge.left_height,
                GuiIngameForge.right_height, settings.rightSide);
        capturedSettings = settings;
    }

    /** Called at Post(ALL), LOWEST, by a listener registered after GraveStone. */
    public void render(int scaledWidth, int scaledHeight, int current, int restored, float alpha) {
        Settings before = capturedSettings;
        capturedSettings = null;
        if (before == null) {
            frame.discard();
            return;
        }
        Settings now = readSettings();
        if (!before.sameAs(now) || !now.active()) {
            frame.discard();
            return;
        }
        GraveStoneBarMath.Anchor anchor = frame.take(scaledWidth, scaledHeight,
                GuiIngameForge.left_height, GuiIngameForge.right_height, now.rightSide);
        GraveStoneBarMath.Segment segment = GraveStoneBarMath.between(current, restored);
        if (anchor == null || segment == null) {
            return;
        }
        int color = overlayColor(segment.gain, alpha);
        if ((color >>> 24) == 0) {
            return;
        }
        renderSafely(() -> {
            try (PreviewRenderState state = PreviewRenderState.capture()) {
                Gui.drawRect(anchor.x + segment.left, anchor.y + segment.top,
                        anchor.x + segment.right, anchor.y + segment.bottom, color);
            }
        });
    }

    private void renderSafely(Runnable drawing) {
        if (disabled) {
            return;
        }
        try {
            drawing.run();
        } catch (RuntimeException | LinkageError failure) {
            disable(failure);
        }
    }

    private Settings readSettings() {
        if (disabled) {
            return null;
        }
        try {
            return config.read();
        } catch (Exception | LinkageError failure) {
            disable(failure);
            return null;
        }
    }

    private void disable(Throwable failure) {
        // The optional bridge must never interrupt native previews or tooltips.
        if (!disabled) {
            disabled = true;
            capturedSettings = null;
            frame.discard();
            LOGGER.log(Level.WARNING, "GraveStone bar preview disabled after an adapter failure ("
                    + failure.getClass().getSimpleName() + ").");
        }
    }

    private static int overlayColor(boolean gain, float alpha) {
        float clamped = Float.isNaN(alpha) ? 0F : Math.max(0F, Math.min(1F, alpha));
        int opacity = Math.round(clamped * 128F);
        return (opacity << 24) | (gain ? 0x80DFFF : 0xFF6060);
    }

    interface ConfigAccess {
        Settings read() throws Exception;
    }

    static final class Settings {
        final boolean enabled;
        final boolean simpleDifficulty;
        final boolean overrideSimpleDifficulty;
        final boolean hydration;
        final boolean rightSide;

        Settings(boolean enabled, boolean simpleDifficulty, boolean overrideSimpleDifficulty,
                 boolean hydration, boolean rightSide) {
            this.enabled = enabled;
            this.simpleDifficulty = simpleDifficulty;
            this.overrideSimpleDifficulty = overrideSimpleDifficulty;
            this.hydration = hydration;
            this.rightSide = rightSide;
        }

        boolean active() {
            return enabled && simpleDifficulty && overrideSimpleDifficulty && hydration;
        }

        boolean sameAs(Settings other) {
            return other != null && enabled == other.enabled
                    && simpleDifficulty == other.simpleDifficulty
                    && overrideSimpleDifficulty == other.overrideSimpleDifficulty
                    && hydration == other.hydration && rightSide == other.rightSide;
        }
    }

    /** No optional class is initialized or referenced by a Java type descriptor. */
    private static final class ReflectiveConfig implements ConfigAccess {
        private static final String RENDERER = "com.zzt.grave_stone.client.hud.PotionBarsRenderer";
        private static final String CONFIG = "com.zzt.grave_stone.config.ModConfig";
        private final ClassLoader loader;
        private boolean probed;
        private Field enabled;
        private Field simpleDifficulty;
        private Field overrideSimpleDifficulty;
        private Field hydration;
        private Field rightSide;

        private ReflectiveConfig(ClassLoader loader) {
            this.loader = loader;
        }

        @Override
        public Settings read() throws Exception {
            if (!probed) {
                probed = true;
                if (!probe()) {
                    return null;
                }
            }
            if (enabled == null) {
                return null;
            }
            return new Settings(enabled.getBoolean(null), simpleDifficulty.getBoolean(null),
                    overrideSimpleDifficulty.getBoolean(null), hydration.getBoolean(null),
                    rightSide.getBoolean(null));
        }

        private boolean probe() throws Exception {
            Class<?> renderer;
            try {
                renderer = Class.forName(RENDERER, false, loader);
            } catch (ClassNotFoundException absent) {
                return false;
            }
            CodeSource rendererSource = renderer.getProtectionDomain().getCodeSource();
            File verified = verifiedJar(rendererSource == null ? null : rendererSource.getLocation());
            if (verified == null) {
                return false;
            }
            Class<?> config = Class.forName(CONFIG, false, loader);
            CodeSource configSource = config.getProtectionDomain().getCodeSource();
            if (configSource == null || !verified.equals(localJar(configSource.getLocation()))) {
                return false;
            }
            // Exact public field names verified with javap for the pinned binary.
            simpleDifficulty = booleanField(config, "enableSimpleDifficultyBars");
            overrideSimpleDifficulty = booleanField(config, "potionBarsOverrideSimpleDifficulty");
            hydration = booleanField(config, "potionBarHydration");
            rightSide = booleanField(config, "potionBarsHydrationRightSide");
            enabled = booleanField(config, "potionBarsEnabled");
            return true;
        }

        private static Field booleanField(Class<?> owner, String name) throws NoSuchFieldException {
            Field field = owner.getField(name);
            if (field.getDeclaringClass() != owner || field.getType() != boolean.class
                    || !Modifier.isStatic(field.getModifiers())) {
                throw new NoSuchFieldException(name + " is not a public static boolean");
            }
            return field;
        }
    }

    /** Hashing happens only during ReflectiveConfig's first probe, never per frame. */
    private static File verifiedJar(URL location)
            throws IOException, URISyntaxException, NoSuchAlgorithmException {
        File jar = localJar(location);
        if (jar == null) {
            return null;
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        try (InputStream input = new FileInputStream(jar)) {
            int length;
            while ((length = input.read(buffer)) != -1) {
                digest.update(buffer, 0, length);
            }
        }
        char[] digits = "0123456789ABCDEF".toCharArray();
        StringBuilder fingerprint = new StringBuilder(64);
        for (byte value : digest.digest()) {
            fingerprint.append(digits[(value >>> 4) & 15]).append(digits[value & 15]);
        }
        return SUPPORTED_SHA256.contentEquals(fingerprint) ? jar : null;
    }

    private static File localJar(URL location) throws IOException, URISyntaxException {
        if (location == null || !"file".equalsIgnoreCase(location.getProtocol())
                || (location.getAuthority() != null && !location.getAuthority().isEmpty())) {
            return null;
        }
        File jar = new File(location.toURI()).getCanonicalFile();
        if (!jar.isFile() || !jar.getName().toLowerCase(Locale.ROOT).endsWith(".jar")
                || jar.getPath().startsWith("\\\\")) {
            return null;
        }
        return jar;
    }
}
