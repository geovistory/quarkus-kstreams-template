# How this repo was set up

gradle init

```bash 
Select type of project to generate:
  1: basic
  2: application
  3: library
  4: Gradle plugin
Enter selection (default: basic) [1..4] 1

Select build script DSL:
  1: Kotlin
  2: Groovy
Enter selection (default: Kotlin) [1..2] 2

Project name (default: kstreams-dev-mode): ENTER
```

create producer project

```bash
quarkus create app org.acme:producer \
    --extension='kafka' \
    --no-code \
    --gradle
```

create aggregator project

```bash
quarkus create app org.acme:aggregator \
    --extension='kafka-streams,resteasy-reactive-jackson' \ 
    --no-code \
    --gradle
```


### Configure Gradle for Multi-Project:

Open the `settings.gradle` file in the quarkus-repo directory and include the subprojects and add the repos and plugin id:

```groovy
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
    plugins {
        id "${quarkusPluginId}" version "${quarkusPluginVersion}"
    }
}
rootProject.name = 'kstreams-dev-mode'
include 'producer', 'aggregator'
```

Move `./producer/gradle.properties` to `./gradle.properties`

Remove `./aggregator/gradle.properties` 


Remove gradlew, gradlew.bat and settings.gradle files from both projects.
