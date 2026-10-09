package cool.furry.mc.forge.projectexpansion.events;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.BlockCompactSun;
import cool.furry.mc.forge.projectexpansion.registries.Advancements;
import cool.furry.mc.forge.projectexpansion.registries.DamageSources;
import cool.furry.mc.forge.projectexpansion.registries.Items;
import cool.furry.mc.forge.projectexpansion.util.SunExposureHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public class ServerEvents {
    static final ArrayList<SunExposureTimer> timers = new ArrayList<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (ServerPlayer player: event.getServer().getPlayerList().getPlayers()) {
            if (player.isCreative() || player.gameMode.getGameModeForPlayer().equals(GameType.SPECTATOR)) continue;
            handleSunExposure(event, player);
            handleWeighedDown(event, player);
        }
    }

    private static void handleWeighedDown(@SuppressWarnings("unused") TickEvent.ServerTickEvent event, ServerPlayer player) {
        int protectionAmount = SunExposureHelper.getProtectionAmount(player);
        int protectionLevel = SunExposureHelper.getProtectionLevel(player);

        boolean hasCompactSun = player.isHolding(Items.COMPACT_SUN.get());

        if (protectionAmount >= 100) return;

        if(!hasCompactSun) {
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() == Items.COMPACT_SUN.get()) {
                    hasCompactSun = true;
                    break;
                }
            }
        }

        if (hasCompactSun) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 3 - protectionLevel, false, false));
        }
    }

    private static int getFireTime(ServerPlayer player) {
        int time = 10;
        int protectionLevel = SunExposureHelper.getProtectionLevel(player);
        if (protectionLevel > 0) {
            time -= 2 + (2 * protectionLevel);
        }

        return time;
    }

    private static void handleSunExposure(TickEvent.ServerTickEvent event, ServerPlayer player) {
        int protectionAmount = SunExposureHelper.getProtectionAmount(player);
        Set<SunExposureTimer> toRemove = new HashSet<>();
        HitResult result = player.pick(10.0f, 0.0f, false);
        DamageSources damage = DamageSources.fromServer(event.getServer());
        if(result instanceof BlockHitResult hit) {
            Block block = player.level.getBlockState(hit.getBlockPos()).getBlock();
            if (block instanceof BlockCompactSun) {
                SunExposureTimer timer = SunExposureTimer.addOrIncrement(player, block);
                if (timer.over()) {
                    if(protectionAmount < 100) {
                        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, false, false));
                        // we don't want them taking fire damage while looking
                        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2, 0, false, false));
                        player.setRemainingFireTicks(2);
                        if(timer.time() % 15 == 0) {
                            float playerMaxHealth = player.getMaxHealth();
                            // deal 40% of the player's max health (4 hearts for 20) in damage every 15 ticks
                            player.hurt(damage.stareAtSun(), playerMaxHealth * 0.4f);
                        }
                        Advancement advancement = event.getServer().getAdvancements().getAdvancement(Advancements.BLINDED_BY_THE_LIGHT);
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

    public record SunExposureTimer(int time, ServerPlayer player, Block block) {
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

        public static SunExposureTimer addOrIncrement(ServerPlayer player, Block block) {
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
