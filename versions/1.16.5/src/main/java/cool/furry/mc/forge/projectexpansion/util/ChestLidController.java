package cool.furry.mc.forge.projectexpansion.util;

import net.minecraft.util.math.MathHelper;

// backport of minecraft's ChestLidController from 1.17
public class ChestLidController {
    private boolean shouldBeOpen;
    private float openness;
    private float oOpenness;

    public void tickLid() {
        this.oOpenness = this.openness;
        if (!this.shouldBeOpen && this.openness > 0.0F) {
            this.openness = Math.max(this.openness - 0.1F, 0.0F);
        } else if (this.shouldBeOpen && this.openness < 1.0F) {
            this.openness = Math.min(this.openness + 0.1F, 1.0F);
        }
    }

    public float getOpenness(float partialTicks) {
        return MathHelper.lerp(partialTicks, this.oOpenness, this.openness);
    }

    public void shouldBeOpen(boolean shouldBeOpen) {
        this.shouldBeOpen = shouldBeOpen;
    }
}
