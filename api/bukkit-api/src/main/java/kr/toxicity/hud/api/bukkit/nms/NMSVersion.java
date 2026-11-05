package kr.toxicity.hud.api.bukkit.nms;

import lombok.Getter;

/**
 * Represents server's version.
 */
@Getter
public enum NMSVersion {
    /** Paper 26.3 resource pack format. */
    V26_R2(97)
    ;
    /**
     * That client version's resource pack mcmeta version.
     */
    private final int metaVersion;

    NMSVersion(int metaVersion) {
        this.metaVersion = metaVersion;
    }
}
