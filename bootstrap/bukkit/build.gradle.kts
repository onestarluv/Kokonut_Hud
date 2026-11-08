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
 compileOnly("kr.gomsv:Kokonut-Core:cbt-1.0")
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
        fun op(name: String, text: String, children: Map<String, Boolean> = emptyMap()) = create(name) {
            description = text
            default = Permission.Default.OP
            this.children = children
        }
        op("betterhud.help", "HUD 명령어 도움말")
        op("betterhud.reload", "HUD 다시 불러오기")
        op("betterhud.parse", "플레이스홀더 출력 확인")
        op("betterhud.hud", "플레이어 HUD 추가·제거", mapOf("betterhud.hud.add" to true, "betterhud.hud.remove" to true))
        op("betterhud.turn", "HUD 켜기·끄기", mapOf(
            "betterhud.turn.on" to true, "betterhud.turn.off" to true,
            "betterhud.turn.on.admin" to true, "betterhud.turn.off.admin" to true,
        ))
    }
}

val shade = configurations.getByName("shade")
val targetAttribute = manifestAttribute + mapOf("paperweight-mappings-namespace" to "mojang")
val groupString = group.toString()

val collectPluginArtifact = tasks.register<Copy>("collectPluginArtifact") {
    val artifact = tasks.named<org.gradle.api.tasks.bundling.AbstractArchiveTask>("shadowJar")
    dependsOn(artifact)
    from(artifact.flatMap { it.archiveFile })
    into(rootProject.layout.projectDirectory.dir("../plugin"))
}
tasks.named("shadowJar") { finalizedBy(collectPluginArtifact) }

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
