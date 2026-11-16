package com.cosmiclogic.rangesight;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Shared entry point for every loader. This copy: 1.16.5 (Log4j, no SLF4J in that version). */
public final class RangeSight {

    public static final String MOD_ID = "rangesight";
    public static final String MOD_NAME = "Range Sight";
    public static final Logger LOG = LogManager.getLogger(MOD_NAME);

    private RangeSight() {
    }

    public static void init(String loader) {
        LOG.info("Range Sight loaded on {}", loader);
    }
}
