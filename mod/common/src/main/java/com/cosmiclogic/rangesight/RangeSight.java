package com.cosmiclogic.rangesight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared entry point for every loader. */
public final class RangeSight {

    public static final String MOD_ID = "rangesight";
    public static final String MOD_NAME = "Range Sight";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private RangeSight() {
    }

    /** Called by each loader's main class (client and dedicated server). */
    public static void init(String loader) {
        LOG.info("Range Sight loaded on {}", loader);
    }
}
