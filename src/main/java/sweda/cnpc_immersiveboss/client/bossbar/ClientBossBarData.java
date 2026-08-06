package sweda.cnpc_immersiveboss.client.bossbar;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端 BossBar 数据存储
 * 接收服务端同步的数据并供渲染器使用
 */
public class ClientBossBarData {

    private static final int COMBAT_TIMEOUT_TICKS = 100; // 5 seconds at 20 TPS

    /**
     * 单个 BossBar 数据
     */
    public static class BossBarEntry {
        public final String textureUrl;
        public final int color;
        public final int bossBarMode;  // 3 = always show, 4 = show only in combat
        public int xShift;
        public double xScale;
        public double yScale;
        public int width;
        public int height;
        public int entityId;
        /** Cached ResourceLocation — parsed once to avoid per-frame string parsing. */
        public final ResourceLocation resourceLocation;

        public BossBarEntry(String textureUrl, int color, int bossBarMode, int xShift, double xScale, double yScale, int width, int height, int entityId) {
            this.textureUrl = textureUrl;
            this.color = color;
            this.bossBarMode = bossBarMode;
            this.xShift = xShift;
            this.xScale = xScale;
            this.yScale = yScale;
            this.width = width;
            this.height = height;
            this.entityId = entityId;
            this.resourceLocation = parseResourceLocation(textureUrl);
        }

        private static ResourceLocation parseResourceLocation(String textureUrl) {
            if (textureUrl == null || textureUrl.isEmpty()) return null;
            try {
                if (textureUrl.contains(":")) {
                    String[] parts = textureUrl.split(":", 2);
                    return ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
                }
                return ResourceLocation.fromNamespaceAndPath("cnpc_immersiveboss", textureUrl);
            } catch (Exception e) {
                return null;
            }
        }
    }

    // 存储所有 BossBar 数据，key 为 entityId
    private static final Map<Integer, BossBarEntry> bossBars = new ConcurrentHashMap<>();

    // 战斗计时器：entityId → 最近一次进入战斗的客户端 tick
    private static final Map<Integer, Integer> lastCombatTick = new ConcurrentHashMap<>();

    /**
     * 更新或添加 BossBar 数据（由网络包调用）
     */
    public static void updateBossBar(int entityId, String textureUrl, int color, int xShift, int bossBarMode, double xScale, double yScale, int width, int height) {
        if (bossBarMode < 3) {
            bossBars.remove(entityId);
            lastCombatTick.remove(entityId);
            return;
        }
        bossBars.put(entityId, new BossBarEntry(textureUrl, color, bossBarMode, xShift, xScale, yScale, width, height, entityId));
    }

    /**
     * 标记实体进入战斗（由网络包中的 combatActive 标志触发）
     */
    public static void markCombat(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            lastCombatTick.put(entityId, (int) mc.level.getGameTime());
        }
    }

    /**
     * 检查指定实体是否在战斗中（计时未过期）
     */
    public static boolean isInCombat(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        Integer last = lastCombatTick.get(entityId);
        if (last == null) return false;
        return (int) mc.level.getGameTime() - last < COMBAT_TIMEOUT_TICKS;
    }

    /**
     * 获取所有可见的 BossBar 数据（直接迭代，避免每帧拷贝）。
     * ConcurrentHashMap 的迭代器是弱一致的，偶尔丢失一帧的更新是可接受的。
     */
    public static Map<Integer, BossBarEntry> getAllBossBars() {
        return bossBars;
    }

    /**
     * 清空所有数据（切换维度或断开连接时调用）
     */
    public static void clear() {
        bossBars.clear();
        lastCombatTick.clear();
    }

    /**
     * 移除指定实体的 BossBar
     */
    public static void remove(int entityId) {
        bossBars.remove(entityId);
        lastCombatTick.remove(entityId);
    }
}
