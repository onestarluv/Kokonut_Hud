plugins {
    id("standard-conventions")
}
val shade = configurations.create("shade")
configurations.implementation { extendsFrom(shade) }
sourceSets.main { resources.srcDirs(rootDir.resolve("common-resources")) }
dependencies {
 compileOnly(libs.bundles.library)
 shade(project(":dist")) { isTransitive = false }
 shade(project(":api")) { isTransitive = false }
 shade(libs.betterCommand) { isTransitive = false }
 shade(libs.exp4j)
 shade(libs.expiringmap)
 shade(libs.semver4j)
}
