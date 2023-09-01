# Add avro support

## Add project

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

