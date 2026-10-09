package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.util.EMCFormat;
import moze_intel.projecte.utils.EMCHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.text.NumberFormat;

// This mixin changes ProjectE's formatting to ours
@SuppressWarnings("unused")
@Mixin(value = EMCHelper.class, remap = false)
public class EMCFormatterMixin {
    @Final
    @Shadow
    private static final NumberFormat EMC_FORMATTER = EMCFormat.INSTANCE;
}
