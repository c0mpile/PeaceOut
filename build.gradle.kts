plugins {
    java
}

group = "c0mpile"
version = "1.2.0"

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(26))
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    jar {
        archiveBaseName.set("PeaceOut")
    }
}
