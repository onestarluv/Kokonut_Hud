pluginManagement { repositories { mavenCentral(); gradlePluginPortal() } }
plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }
dependencyResolutionManagement { repositories { mavenCentral(); maven("https://repo.papermc.io/repository/maven-public/") } }
rootProject.name = "Kokonut_HUD"
include("api:bukkit-api", "dist", "nms:v26_R2", "scheduler:standard", "bootstrap:bukkit")
includeBuild("..")
