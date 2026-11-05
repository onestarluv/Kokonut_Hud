plugins {
    alias(libs.plugins.conventions.standard)
}

dependencies {
    implementation(project(":api"))
    compileOnly(libs.bundles.adventure)
    compileOnly(libs.bundles.library)

    testImplementation(libs.bundles.library)

}
sourceSets.test { resources.srcDir(rootProject.file("common-resources")) }
