val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    testImplementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
    testImplementation(project(":domain"))
    testImplementation(project(":application"))
    testImplementation(project(":adapters"))
    testImplementation(project(":app"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.2")
}
