package com.cosmiclogic.rangesight;

import com.cosmiclogic.rangesight.client.RangeSightNeoForgeClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(RangeSight.MOD_ID)
public class RangeSightNeoForge {

    public RangeSightNeoForge(IEventBus modBus, Dist dist) {
        RangeSight.init("NeoForge");
        if (dist.isClient()) {
            RangeSightNeoForgeClient.init(modBus);
        }
    }
}
