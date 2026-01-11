


plugins {
    id("idea")
    id("java-library")
    id("maven-publish")
    id("net.thebugmc.gradle.sonatype-central-portal-publisher") version "1.2.4"
    id("org.jetbrains.dokka") version "2.0.0"
}

val javaVersion = JavaVersion.VERSION_17

group = "io.github.breninsul"
version = "1.0.0"



java {
    sourceCompatibility = javaVersion
}
java {
    withJavadocJar()
    withSourcesJar()
}

sourceSets {
    main {
        java {
            srcDirs("src/java")
        }
        resources {
            srcDirs("src/resource")
        }
    }
    test {
        java {
            srcDirs("src/test/java")
        }
    }
}

tasks.compileJava {
    dependsOn.add(tasks.processResources)
}


repositories {
    mavenCentral()
}



dependencies {
    implementation("org.apache.logging.log4j:log4j-core:2.25.3")
    compileOnly("org.apache.logging.log4j:log4j-core:2.25.3")
    annotationProcessor("org.apache.logging.log4j:log4j-core:2.25.3")

    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("com.google.re2j:re2j:1.7")
}






val javadocJar =
    tasks.named<Jar>("javadocJar") {
        from(tasks.named("dokkaJavadoc"))
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
tasks.getByName<Jar>("jar") {
    enabled = true
    archiveClassifier = ""
}





signing {
    useGpgCmd()
}


val repoName = "log4j2-masking-factory"
centralPortal {
    pom {
        packaging = "jar"
        name.set("Log4j2 Masking Factory")
        url.set("https://github.com/BreninSul/$repoName")
        description.set("Log4j2 extension for masking sensitive data using RE2/J")
        licenses {
            license {
                name.set("MIT License")
                url.set("http://opensource.org/licenses/MIT")
            }
        }
        scm {
            connection.set("scm:https://github.com/BreninSul/$repoName.git")
            developerConnection.set("scm:git@github.com:BreninSul/$repoName.git")
            url.set("https://github.com/BreninSul/$repoName")
        }
        developers {
            developer {
                id.set("BreninSul")
                name.set("BreninSul")
                email.set("brenimnsul@gmail.com")
                url.set("breninsul.github.io")
            }
        }
    }
}



tasks.withType<Test> {
    useJUnitPlatform()
}