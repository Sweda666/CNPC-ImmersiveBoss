package sweda.cnpc_immersiveboss.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;

@Deprecated
public class TextureHelper {
    public static int[] getTextureSize(ResourceLocation location) {
        Minecraft mc = Minecraft.getInstance();
        TextureAtlas atlas = (TextureAtlas) mc.getTextureAtlas(ResourceLocation.withDefaultNamespace("textures/atlas/items.png"));
        TextureAtlasSprite sprite = atlas.getSprite(location);
        SpriteContents contents = sprite.contents();
        if (contents != null) {
            return new int[]{contents.width(), contents.height()};
        }

        System.err.println("无法获取纹理尺寸: " + location);
        return new int[]{0, 0};
    }
}