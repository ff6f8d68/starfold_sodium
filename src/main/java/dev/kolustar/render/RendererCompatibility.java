package dev.kolustar.render;

import java.util.function.Predicate;

/** One policy for early mixin selection and the client projection guard; no game classes here. */
public final class RendererCompatibility {
    /**
     * Sodium and Iris are handled by {@link SodiumSupport}, which also checks the Sodium shader
     * version and whether an Iris pack owns the world shaders. Embeddium and Canvas replace the
     * renderer in ways this projection has no shader hook for.
     */
    public static boolean supported(Predicate<String> loaded) {
        return !loaded.test("embeddium") && !loaded.test("canvas");
    }

    public static boolean apply(String mixin, Predicate<String> loaded) {
        if (mixin.equals("dev.kolustar.mixin.SentryShipMuzzleMixin"))
            return loaded.test("sentrymechanicalarm")
                    && loaded.test("tacz")
                    && loaded.test("sable");
        if (mixin.startsWith("dev.kolustar.mixin.DevPort"))
            return Boolean.getBoolean("kolustar.portProbe");
        // Both hook Sodium's own chunk program; without Sodium they cannot resolve a target.
        if (mixin.startsWith("dev.kolustar.mixin.Sodium")) return loaded.test("sodium");
        // Third-party weather renderers target their own classes; only apply when they exist.
        if (mixin.equals("dev.kolustar.mixin.Weather2SceneEnhancerMixin")
                || mixin.equals("dev.kolustar.mixin.Weather2DimensionsMixin"))
            return loaded.test("weather2");
        if (mixin.equals("dev.kolustar.mixin.ParticleRainSpawnerMixin"))
            return loaded.test("particlerain");
        // Sodium, Embeddium and Canvas overwrite renderSectionLayer and setupRender, so the
        // section observer would have no draw call left to wrap. Frame start lives separately.
        if (mixin.equals("dev.kolustar.mixin.GroundNativeDrawMixin"))
            return !loaded.test("sodium") && !loaded.test("embeddium") && !loaded.test("canvas");
        // Both setupRender and allChanged belong to vanilla's terrain compiler/view area.
        // Replacing that compiler must not leave half of its projected observer installed.
        if (mixin.equals("dev.kolustar.mixin.HullBottomCacheMixin")
                || mixin.equals("dev.kolustar.mixin.PilotPoseProbeMixin")
                || mixin.equals("dev.kolustar.mixin.SableShipWriteMixin")
                || mixin.equals("dev.kolustar.mixin.SableShipStorageMixin")
                || mixin.equals("dev.kolustar.mixin.SableShipSerializationMixin")
                || mixin.equals("dev.kolustar.mixin.SableShipSplitMixin")
                || mixin.equals("dev.kolustar.mixin.SableFuelMassMixin")
                || mixin.equals("dev.kolustar.mixin.SableShipReadinessMixin"))
            return loaded.test("sable");
        if (mixin.equals("dev.kolustar.mixin.HelmFitMixin")
                || mixin.equals("dev.kolustar.mixin.HelmCollisionPoseMixin")
                || mixin.equals("dev.kolustar.mixin.HelmPerspectiveMixin")
                || mixin.equals("dev.kolustar.mixin.HelmCameraMixin")
                || mixin.equals("dev.kolustar.mixin.HelmMouseMixin")) return loaded.test("sable");
        return true;
    }

    /** A new client level (including reconnect) is a new entry, not every tick or face seam. */
    static final class Notice {
        private Object level;
        private boolean announced;

        boolean enter(Object current, boolean planet, boolean player, boolean supported) {
            if (level != current) {
                level = current;
                announced = false;
            }
            if (current == null || !planet || !player || supported || announced) return false;
            announced = true;
            return true;
        }
    }

    private RendererCompatibility() {}
}
