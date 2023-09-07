# Goal

With toolbox streams apps we have observed that state store changelogs are not restored correctly:

- Start a kafka streams app with state store
- Stop it and delete state directory
- restart redpanda broker(s)
- Start a kafka streams app with state store
- Restoration is stuck

The goal is to find out, why this happens. Therefore we try to reproduce the behavior with the aggregator app.

## Step 1
commit feba7c8
With default settings, the problem does not appear.


## Step 2

I changed the app id:

quarkus.kafka-streams.application-id=aggregator-2

And I added these configurations:

kafka-streams.processing.guarantee=exactly_once_v2
kafka-streams.consumer.isolation.level=read_committed


Then:
- Start a kafka streams app with state store
- Stop it and delete state directory
- restart redpanda broker(s)
- Start a kafka streams app with state store
- Restoration is stuck

```text
2023-09-07 13:12:14,974 INFO  [org.apa.kaf.str.pro.int.StoreChangelogReader] 
    (aggregator-2-07e80750-06ec-475c-8210-08580ce1bba0-StreamThread-1) stream-thread 
    [aggregator-2-07e80750-06ec-475c-8210-08580ce1bba0-StreamThread-1] Restoration in progress for 1 partitions.
    {aggregator-2-weather-stations-store-changelog-0: position=25, end=26, totalRestored=9}
```



