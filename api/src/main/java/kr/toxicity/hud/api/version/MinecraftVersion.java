package kr.toxicity.hud.api.version;

import org.jetbrains.annotations.NotNull;
import org.semver4j.Semver;

import java.util.Comparator;
import java.util.Objects;

/**
 * Minecraft version.
 * @param first title
 * @param second main update
 * @param third minor update
 */
public record MinecraftVersion(int first, int second, int third) implements Comparable<MinecraftVersion> {
    /**
     * Comparator
     */
    private static final Comparator<MinecraftVersion> COMPARATOR = Comparator.comparing(MinecraftVersion::first)
            .thenComparing(MinecraftVersion::second)
            .thenComparing(MinecraftVersion::third);

    @Override
    public int compareTo(@NotNull MinecraftVersion o) {
        return COMPARATOR.compare(this, o);
    }

    /**
     * 26.3
     */
    public static final MinecraftVersion V26_3 = new MinecraftVersion(26, 3, 0);
    /**
     * Latest
     */
    public static final MinecraftVersion LATEST = V26_3;

    /**
     * Parses version from string
     * @param version version like "26.1.2"
     */
    public static @NotNull MinecraftVersion of(@NotNull String version) {
        var ver = Objects.requireNonNull(Semver.coerce(version));
        return new MinecraftVersion(ver.getMajor(), ver.getMinor(), ver.getPatch());
    }

    @Override
    public @NotNull String toString() {
        return first + "." + second + "." + third;
    }
}