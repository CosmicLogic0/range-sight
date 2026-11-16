package com.cosmiclogic.rangesight;

import com.cosmiclogic.rangesight.client.RangeSightNeoForgeClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(RangeSight.MOD_ID)
public class RangeSightNeoForge {

    public RangeSightNeoForge(IEventBus modBus, Dist dist) {
        RangeSight.init("NeoForge");
        // Range Sight only draws on your screen: on a dedicated server it just loads and does nothing.
        if (dist.isClient()) {
            RangeSightNeoForgeClient.init(modBus);
        }
    }
}
