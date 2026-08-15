plugins {
    id("io.quarkus")
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(project(":adapters"))

    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-agroal")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}
