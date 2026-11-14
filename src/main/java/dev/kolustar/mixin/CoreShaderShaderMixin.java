package dev.kolustar.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.kolustar.render.ShaderUniforms;

import net.minecraft.client.renderer.ShaderInstance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderInstance.class)
public abstract class CoreShaderShaderMixin {
    // Wrap the whole apply, including other mods' TAIL uploads, rather than racing at TAIL.
    @WrapMethod(method = "apply")
    private void kolustar$capUniforms(Operation<Void> original) {
        original.call();
        var shader = (ShaderInstance) (Object) this;
        dev.kolustar.render.GroundClient.apply(shader);
        dev.kolustar.api.PlanetSurfaceRender.apply(shader);
    }

    @Inject(method = "updateLocations", at = @At("HEAD"))
    private void kolustar$relinked(CallbackInfo ci) {
        ShaderUniforms.invalidate((ShaderInstance) (Object) this);
    }
}
