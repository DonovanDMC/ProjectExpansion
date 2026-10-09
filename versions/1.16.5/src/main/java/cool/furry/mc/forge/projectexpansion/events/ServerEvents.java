package cool.furry.mc.forge.projectexpansion.events;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.BlockCompactSun;
import cool.furry.mc.forge.projectexpansion.registries.Advancements;
import cool.furry.mc.forge.projectexpansion.registries.DamageSources;
import cool.furry.mc.forge.projectexpansion.registries.Items;
import cool.furry.mc.forge.projectexpansion.util.SunExposureHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.block.Block;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public class ServerEvents {
    static final ArrayList<SunExposureTimer> timers = new ArrayList<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (ServerPlayerEntity player: ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (player.isCreative() || player.gameMode.getGameModeForPlayer().equals(GameType.SPECTATOR)) continue;
            handleSunExposure(event, player);
            handleWeighedDown(event, player);
        }
    }

    private static void handleWeighedDown(@SuppressWarnings("unused") TickEvent.ServerTickEvent event, ServerPlayerEntity player) {
        int protectionAmount = SunExposureHelper.getProtectionAmount(player);
        int protectionLevel = SunExposureHelper.getProtectionLevel(player);

        boolean hasCompactSun = player.isHolding(Items.COMPACT_SUN.get());

        if (protectionAmount >= 100) return;

        if(!hasCompactSun) {
            for (ItemStack stack : player.inventory.items) {
                if (stack.getItem() == Items.COMPACT_SUN.get()) {
                    hasCompactSun = true;
                    break;
                }
            }
        }

        if (hasCompactSun) {
            player.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 5, 3 - protectionLevel, false, false));
        }
    }

    private static int getFireTime(ServerPlayerEntity player) {
        int time = 10;
        int protectionLevel = SunExposureHelper.getProtectionLevel(player);
        if (protectionLevel > 0) {
            time -= 2 + (2 * protectionLevel);
        }

        return time;
    }

    private static void handleSunExposure(TickEvent.ServerTickEvent event, ServerPlayerEntity player) {
        int protectionAmount = SunExposureHelper.getProtectionAmount(player);
        Set<SunExposureTimer> toRemove = new HashSet<>();
        RayTraceResult result = player.pick(10.0f, 0.0f, false);
        DamageSources damage = DamageSources.fromServer(ServerLifecycleHooks.getCurrentServer());
        if(result instanceof BlockRayTraceResult) {
            BlockRayTraceResult hit = ((BlockRayTraceResult) result);
            Block block = player.level.getBlockState(hit.getBlockPos()).getBlock();
            if (block instanceof BlockCompactSun) {
                SunExposureTimer timer = SunExposureTimer.addOrIncrement(player, block);
                if (timer.over()) {
                    if(protectionAmount < 100) {
                        player.addEffect(new EffectInstance(Effects.BLINDNESS, 50, 0, false, false));
                        // we don't want them taking fire damage while looking
                        player.addEffect(new EffectInstance(Effects.FIRE_RESISTANCE, 2, 0, false, false));
                        player.setRemainingFireTicks(2);
                        if(timer.time() % 15 == 0) {
                            float playerMaxHealth = player.getMaxHealth();
                            // deal 40% of the player's max health (4 hearts for 20) in damage every 15 ticks
                            player.hurt(damage.stareAtSun(), playerMaxHealth * 0.4f);
                        }
                        Advancement advancement = ServerLifecycleHooks.getCurrentServer().getAdvancements().getAdvancement(Advancements.BLINDED_BY_THE_LIGHT);
                        if (advancement != null) {
                            player.getAdvancements().award(advancement, "blinded_by_the_light");
                        }
                    }
                }
            } else {
                for (SunExposureTimer timer : timers) {
                    if (timer.player == player) {
                        toRemove.add(timer);
                    }
                }
            }
        } else {
            for (SunExposureTimer timer : timers) {
                if (timer.player == player) {
                    toRemove.add(timer);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            boolean applyEffects = toRemove.stream().anyMatch(SunExposureTimer::over);
            timers.removeAll(toRemove);
            if (applyEffects) {
                int fireTime = getFireTime(player);
                if (fireTime != 0) player.setRemainingFireTicks(fireTime);
            }
        }
    }

    public static final class SunExposureTimer {
        private final int time;
        private final ServerPlayerEntity player;
        private final Block block;

        public SunExposureTimer(int time, ServerPlayerEntity player, Block block) {
            this.time = time;
            this.player = player;
            this.block = block;
        }

        public int time() {
            return time;
        }

        public ServerPlayerEntity player() {
            return player;
        }

        public Block block() {
            return block;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SunExposureTimer that = (SunExposureTimer) o;
            return Objects.equals(time, that.time) && Objects.equals(player, that.player) && Objects.equals(block, that.block);
        }

        @Override
        public int hashCode() {
            return Objects.hash(time, player, block);
        }

        @Override
        public String toString() {
            return "SunExposureTimer[time=" + time + ", player=" + player + ", block=" + block + "]";
        }

        public void increment() {
            SunExposureTimer newTimer = new SunExposureTimer(time + 1, player, block);
            timers.remove(this);
            timers.add(newTimer);
        }

        @SuppressWarnings("unused")
        public void reset() {
            timers.remove(this);
        }

        public boolean over() {
            return time >= 60;
        }

        public static SunExposureTimer addOrIncrement(ServerPlayerEntity player, Block block) {
            for (SunExposureTimer timer: timers) {
                if (timer.player == player && timer.block == block) {
                    timer.increment();
                    return timer;
                }
            }
            SunExposureTimer newTimer = new SunExposureTimer(1, player, block);
            timers.add(newTimer);
            return newTimer;
        }
    
    }
}
