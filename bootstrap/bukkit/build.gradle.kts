import xyz.jpenilla.resourcefactory.bukkit.Permission

plugins {
    alias(libs.plugins.conventions.bootstrap)
    alias(libs.plugins.conventions.bukkit)
    alias(libs.plugins.resourcefactory.bukkit)
    alias(libs.plugins.shadow)
}

dependencies {
 shade(project(":api:bukkit-api")) { isTransitive = false }
 shade(project(":scheduler:standard")) { isTransitive = false }
 shade(project(":nms:v26_R2")) { isTransitive = false }
 shade(libs.kotlinStdlib)
}

bukkitPluginYaml {
    main = "$group.bootstrap.bukkit.BukkitBootstrapImpl"
    version = project.version.toString()
    name = rootProject.name
    apiVersion = "26.3"
    author = "pingpong"
    description = "에고버스 플러그인"
    foliaSupported = false
    website = "https://wiki.egoverse.kr"
    depend = listOf("Kokonut-Core")
    permissions {
        create("betterhud.help") {
            description = "Accesses to help command."
            default = Permission.Default.OP
        }
        create("betterhud.reload") {
            description = "Accesses to reload command."
            default = Permission.Default.OP
        }
        create("betterhud.parse") {
            description = "Accesses to parse command."
            default = Permission.Default.OP
        }
        create("betterhud.hud") {
            description = "Accesses to hud command."
            default = Permission.Default.OP
            children = mapOf(
                "betterhud.hud.add" to true,
                "betterhud.hud.remove" to true
            )
        }
        create("betterhud.compass") {
            description = "Accesses to compass command."
            default = Permission.Default.OP
            children = mapOf(
                "betterhud.compass.add" to true,
                "betterhud.compass.remove" to true
            )
        }
        create("betterhud.turn") {
            description = "Accesses to turn command."
            default = Permission.Default.OP
            children = mapOf(
                "betterhud.turn.on" to true,
                "betterhud.turn.off" to true,
                "betterhud.turn.on.admin" to true,
                "betterhud.turn.off.admin" to true
            )
        }
        create("betterhud.pointer") {
            description = "Accesses to pointer command."
            default = Permission.Default.OP
            children = mapOf(
                "betterhud.pointer.set" to true,
                "betterhud.pointer.clear" to true,
                "betterhud.pointer.remove" to true
            )
        }
        create("betterhud.popup") {
            description = "Accesses to popup command."
            default = Permission.Default.OP
            children = mapOf(
                "betterhud.popup.add" to true,
                "betterhud.popup.remove" to true,
                "betterhud.popup.show" to true,
                "betterhud.popup.hide" to true
            )
        }
    }
}

val shade = configurations.getByName("shade")
val targetAttribute = manifestAttribute + mapOf("paperweight-mappings-namespace" to "mojang")
val groupString = group.toString()

tasks {
    jar {
        finalizedBy(shadowJar)
    }
    shadowJar {
        configurations = listOf(shade)
        archiveBaseName = "${rootProject.name}-bukkit"
        archiveClassifier = ""
        destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
        manifest {
            attributes(targetAttribute)
        }
        dependencies {
            exclude(dependency("org.jetbrains:annotations:13.0"))
        }
        fun prefix(pattern: String) {
            relocate(pattern, "$groupString.shaded.$pattern")
        }
        prefix("kotlin")
        prefix("kr.toxicity.command.impl")
    }
}
