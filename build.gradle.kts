plugins { alias(libs.plugins.conventions.standard) }
tasks.register("pluginJar") { dependsOn(":bootstrap:bukkit:shadowJar") }
tasks.build { dependsOn(":bootstrap:bukkit:build") }
