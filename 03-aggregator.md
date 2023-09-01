# How the aggregator app was created

I followed a tutorial

From here https://quarkus.io/guides/kafka-streams#the-pipeline-implementation until "Building and Running the
Applications".

## Use redpanda from dev stack

When you remove the following line from application.properties,
the Dev Services will automatically start a redpanda docker container:

`kafka.bootstrap.servers=localhost:9092`

Instead, I want to connect to redpanda of our dev stack, so that we can benefit
of the integration with other services, like the redpanda console:

# Dev profile

`%dev.kafka.bootstrap.servers=localhost:1121,localhost:1122,localhost:1123`


