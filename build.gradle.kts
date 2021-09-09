plugins {
    kotlin("jvm") version "1.5.30"
    `maven-publish`
}


val arrowVersion: String by project
val mockkVersion: String by project
val exposedVersion: String by project
val kodeinVersion: String by project

group "typhoon"
version "3.0.3"



repositories {
    mavenCentral()
}



dependencies {


    testImplementation("io.kotest:kotest-runner-junit5:4.6.1")
    testImplementation("io.kotest:kotest-assertions-core:4.6.1")
    testImplementation("io.kotest:kotest-property:4.6.1")
    testImplementation("io.mockk:mockk:${mockkVersion}")
    testImplementation("junit:junit:4.13.2")


    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")

    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-jodatime:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")


    implementation("org.apache.logging.log4j:log4j-core:2.14.1")

    implementation("org.slf4j:slf4j-log4j12:1.7.32")

    implementation("mysql:mysql-connector-java:8.0.25")
    implementation("com.h2database:h2:1.4.200")

    implementation("org.kodein.di:kodein-di:${kodeinVersion}")
    implementation("io.arrow-kt:arrow-fx-coroutines:${arrowVersion}")


}


tasks {
    test {
        useJUnitPlatform()
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/lukasrieger/ty-backend")
            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("TOKEN")
            }
        }
    }


    publications {
        register("gpr", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}
