package cool.furry.mc.neoforge.projectexpansion.events;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.block.BlockCompactSun;
import cool.furry.mc.neoforge.projectexpansion.registries.*;
import cool.furry.mc.neoforge.projectexpansion.util.SunExposureHelper;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = Main.MOD_ID)
public class ServerEvents {
    static ArrayList<SunExposureTimer> timers = new ArrayList<>();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer player: event.getServer().getPlayerList().getPlayers()) {
            if (player.isCreative() || player.gameMode.getGameModeForPlayer().equals(GameType.SPECTATOR)) continue;
            handleSunExposure(event, player);
            handleWeighedDown(event, player);
        }
    }

    private static void handleWeighedDown(ServerTickEvent.Post event, ServerPlayer player) {
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

    private static void handleSunExposure(ServerTickEvent.Post event, ServerPlayer player) {
        int protectionAmount = SunExposureHelper.getProtectionAmount(player);
        Set<SunExposureTimer> toRemove = new HashSet<>();
        HitResult result = player.pick(10.0f, 0.0f, false);
        DamageSources damage = DamageSources.fromServer(event.getServer());
        if(result instanceof BlockHitResult hit) {
            Block block = player.level().getBlockState(hit.getBlockPos()).getBlock();
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
                        AdvancementHolder advancement = event.getServer().getAdvancements().get(Advancements.BLINDED_BY_THE_LIGHT);
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
                player.setRemainingFireTicks(fireTime);
            }
        }
    }

    public record SunExposureTimer(int time, ServerPlayer player, Block block) {
        public void increment() {
            SunExposureTimer newTimer = new SunExposureTimer(time + 1, player, block);
            timers.remove(this);
            timers.add(newTimer);
        }

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
