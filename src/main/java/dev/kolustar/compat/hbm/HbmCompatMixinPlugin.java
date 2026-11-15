package dev.kolustar.compat.hbm;

import net.neoforged.fml.loading.LoadingModList;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Applies HBM Continuum compatibility mixins only when HBM is installed. */
public final class HbmCompatMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    private static Boolean enabled;

    /**
     * Mixins are applied while classes load, potentially before NeoForge configs are available, so
     * the common config file is read directly. It is a single boolean in a file we own.
     */
    private static boolean configured() {
        Boolean known = enabled;
        if (known != null) return known;
        boolean value = true;
        try {
            var file =
                    net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("kolustar-common.toml");
            if (java.nio.file.Files.exists(file)) {
                var matcher =
                        java.util.regex.Pattern.compile(
                                        "(?m)^\\s*hbmCompat\\s*=\\s*(true|false)")
                                .matcher(java.nio.file.Files.readString(file));
                if (matcher.find()) value = Boolean.parseBoolean(matcher.group(1));
            }
        } catch (Throwable ignored) {
            // A missing or unreadable config keeps the compatibility mixins on.
        }
        enabled = value;
        return value;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // HBM's multi-block renderers can double-draw or clip under the planet projection; the
        // config switch isolates the compatibility tweaks when diagnosing model problems.
        if (!configured()) return false;
        return LoadingModList.get().getModFileById("hbm") != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {}

    @Override
    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {}
}
