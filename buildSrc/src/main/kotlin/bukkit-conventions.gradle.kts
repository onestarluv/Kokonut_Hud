plugins {
    id("standard-conventions")
}
dependencies {
 compileOnly("io.papermc.paper:paper-api:26.3.build.+")
 compileOnly("com.mojang:brigadier:1.3.10")
 api(libs.bundles.adventure)
}
