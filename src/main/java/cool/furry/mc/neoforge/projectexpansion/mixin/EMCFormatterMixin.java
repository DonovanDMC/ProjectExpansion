package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.util.EMCFormat;
import moze_intel.projecte.utils.EMCHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.text.NumberFormat;

// This mixin changes ProjectE's formatting to ours
@Mixin(EMCHelper.class)
public class EMCFormatterMixin {
    @Final
    @Shadow(remap = false)
    private static final NumberFormat EMC_FORMATTER = EMCFormat.INSTANCE;
}
