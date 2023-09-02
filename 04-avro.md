# Add avro support

First, I tried to create a project that generates the avro files, that can be imported by producer and aggregator.
Unfortunately this was not working, see below "Add Project".

Then, I added a folder with avro schemas to a folder `/avro`.
This folder is copied by `buildSrc/src/main/groovy/org.acme.copy-avro.gradle` into the subprojects.
The subprojects use the plugin `org.acme.copy-avro` to ensure, the schemas are always copied before build.

In the producer and aggregator:

## Add avro to Producer

in `producer/build.gradle` add:

```groovy

repositories {
    // ..
    maven {
        url "https://packages.confluent.io/maven"
    }
}

dependencies {
    // ...
    implementation 'io.quarkus:quarkus-confluent-registry-avro'

    implementation("io.confluent:kafka-avro-serializer:7.2.0") {
        exclude group: "jakarta.ws.rs", module: "jakarta.ws.rs-api"
    }
}
```

## Schemas

The schemas are copied into `avro/src/main/avro` as described above.
The folder is in .gitignore.

## Modify the ValuesGenerator

In values generator, use the generated Avro Classes. There is no need to explicitly set the Serde/Serializer: This will
be detected automatically: https://quarkus.io/guides/kafka#serialization-autodetection

## Modify Config
 In `application.properties` add the following lines, to undef

```text
mp.messaging.outgoing.temperature-values.connector=smallrye-kafka

# automatically register the schema with the registry, if not present
mp.messaging.outgoing.temperature-values.key.auto.register.schemas=true
mp.messaging.outgoing.temperature-values.value.auto.register.schemas=true

mp.messaging.outgoing.weather-stations.connector=smallrye-kafka

# automatically register the schema with the registry, if not present
mp.messaging.outgoing.weather-stations.key.auto.register.schemas=true
mp.messaging.outgoing.weather-stations.value.auto.register.schemas=true


# Dev profile
%dev.kafka.bootstrap.servers=localhost:1121

# set the schema.registry.url
%dev.mp.messaging.connector.smallrye-kafka.schema.registry.url=http://localhost:1127
```

---

## NOT WORKING: Add project

```bash
quarkus create cli org.acme:avro \
    --extension='quarkus-confluent-registry-avro' \
    --no-code \
    --gradle

```

This generates a folder called avro.

Integrate it in multi project setup, in `settings.gradle` add avro:

```
include 'producer', 'aggregator', 'avro'
```

Remove  `avro/settings.gradle`  `avro/gradlew` `avro/gradlew.bat`

## Add avro project as dependency to producer and aggregator

in `producer/build.gradle` add:

```groovy

dependencies {
    // ...
    implementation project(':avro')
}
```

## Add schemas

Add the avro .avsc files in `avro/src/main/avro`.

## Build avro Java Classes

`./gradlew avro:build`

See generated classes in `avro/build/classes/java/quarkus-generated-sources/avsc`

## Use avro classes in producer

I had to add this as dependency:

```groovy
repositories {
    // ...
    maven {
        url "https://packages.confluent.io/maven"
    }
}

dependencies {

    // ...
    implementation("io.quarkus:quarkus-confluent-registry-avro")
    implementation("io.confluent:kafka-avro-serializer:7.2.0") {
        exclude group: "jakarta.ws.rs", module: "jakarta.ws.rs-api"
    }

    implementation project(':avro')

}
```

This did not work, as of this issue: https://github.com/quarkusio/quarkus/issues/31650


