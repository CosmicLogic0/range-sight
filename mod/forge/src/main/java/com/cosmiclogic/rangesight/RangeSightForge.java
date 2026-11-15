package com.cosmiclogic.rangesight;

import com.cosmiclogic.rangesight.client.RangeSightForgeClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(RangeSight.MOD_ID)
public class RangeSightForge {

    public RangeSightForge() {
        RangeSight.init("Forge");
        // Range Sight only draws on your screen: on a dedicated server it just loads and does nothing.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RangeSightForgeClient.init(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }
}
