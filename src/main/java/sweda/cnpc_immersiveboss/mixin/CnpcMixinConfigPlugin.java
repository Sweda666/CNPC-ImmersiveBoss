package sweda.cnpc_immersiveboss.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class CnpcMixinConfigPlugin implements IMixinConfigPlugin {
    private static final String TACZ_MIXIN_PREFIX =
        "sweda.cnpc_immersiveboss.mixin.compat.MixinTacz";
    private static final String BETTER_COMBAT_MIXIN_PREFIX =
        "sweda.cnpc_immersiveboss.mixin.compat.MixinBetterCombat";
    private static final String IRONS_SPELLBOOKS_MIXIN_PREFIX =
        "sweda.cnpc_immersiveboss.mixin.compat.MixinIrons";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(TACZ_MIXIN_PREFIX)) {
            return isModLoaded("tacz");
        }
        if (mixinClassName.startsWith(BETTER_COMBAT_MIXIN_PREFIX)) {
            return isModLoaded("bettercombat");
        }
        if (mixinClassName.startsWith(IRONS_SPELLBOOKS_MIXIN_PREFIX)) {
            return isModLoaded("irons_spellbooks");
        }
        return true;
    }

    private static boolean isModLoaded(String modId) {
        return FMLLoader.getLoadingModList() != null
            && FMLLoader.getLoadingModList().getModFileById(modId) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {
    }
}
