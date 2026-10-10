package cool.furry.mc.projectexpansion.util;

import cool.furry.mc.projectexpansion.config.Config;
import cool.furry.mc.projectexpansion.registries.SoundEvents;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SoundCategory;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;

public class AlchemicalCollectionCollector {
    public static final HashMap<UUID, Collected> saved = new HashMap<>();
    public static final class Collected {
        private final UUID player;
        private final BigInteger emc;
        private final List<ItemInfo> items;
        private final long lastUpdatedAt;

        public Collected(UUID player, BigInteger emc, List<ItemInfo> items, long lastUpdatedAt) {
            this.player = player;
            this.emc = emc;
            this.items = items;
            this.lastUpdatedAt = lastUpdatedAt;
        }

        public UUID player() {
            return player;
        }

        public BigInteger emc() {
            return emc;
        }

        public List<ItemInfo> items() {
            return items;
        }

        public long lastUpdatedAt() {
            return lastUpdatedAt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Collected that = (Collected) o;
            return Objects.equals(player, that.player) && Objects.equals(emc, that.emc) && Objects.equals(items, that.items) && Objects.equals(lastUpdatedAt, that.lastUpdatedAt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(player, emc, items, lastUpdatedAt);
        }

        @Override
        public String toString() {
            return "Collected[player=" + player + ", emc=" + emc + ", items=" + items + ", lastUpdatedAt=" + lastUpdatedAt + "]";
        }

        public boolean inCooldown() {
            return System.currentTimeMillis() < lastUpdatedAt + 250;
        }

        public boolean process() {
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
            @Nullable ServerPlayerEntity serverPlayer = Util.getPlayer(player);
            if (provider == null || serverPlayer == null) return false;
            for (ItemInfo item : items) {
                if(provider.addKnowledge(item)) {
                    provider.syncKnowledgeChange(serverPlayer, item, true);
                }
            }
            provider.setEmc(provider.getEmc().add(emc));
            provider.syncEmc(serverPlayer);
            if (Config.client.alchemicalCollectionSound.get()) {
                serverPlayer.level.playSound(null, serverPlayer.blockPosition(), SoundEvents.ALCHEMICAL_COLLECTION_COLLECT.get(), SoundCategory.BLOCKS, 1.0F, 0.85F);
            }
            return true;
        }
    
    }

    public static void add(UUID player, BigInteger emc, List<ItemInfo> items) {
        @Nullable Collected existing = get(player);
        if (existing != null) {
            items.addAll(existing.items);
            saved.put(player, new Collected(player, existing.emc.add(emc), items, System.currentTimeMillis()));
        } else {
            saved.put(player, new Collected(player, emc, items, System.currentTimeMillis()));
        }
    }

    public static @Nullable Collected get(UUID player) {
        return saved.get(player);
    }

    public static void process() {
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, Collected> entry : saved.entrySet()) {
            if (!entry.getValue().inCooldown() && entry.getValue().process()) {
                toRemove.add(entry.getKey());
            }
        }
        for (UUID uuid : toRemove) {
            saved.remove(uuid);
        }
    }
}
