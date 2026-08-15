plugins {
    `java-library`
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    api(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
    api(project(":domain"))
    api(project(":application"))

    api("jakarta.ws.rs:jakarta.ws.rs-api")
    api("jakarta.inject:jakarta.inject-api")

    // :app needs Jdbi/PostgresPlugin to wire the CDI producer for the Jdbi bean.
    api("org.jdbi:jdbi3-core:3.54.0")
    api("org.jdbi:jdbi3-postgres:3.54.0")

    implementation("com.github.f4b6a3:uuid-creator:6.1.1")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
