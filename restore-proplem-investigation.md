# Goal

With kafka streams apps we have observed that state store changelogs are not restored correctly:

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

## Step 3
And I added this configurations:
kafka-streams.acceptable.recovery.lag=10
-> problem persists.

## Step 4
And I added this configurations:
```
kafka-streams.processing.guarantee=exactly_once_v2
# kafka-streams.consumer.isolation.level=read_committed
```

Then:
- Start a kafka streams app with state store
- Stop it and delete state directory
- restart redpanda broker(s)
- Start a kafka streams app with state store
- Restoration is stuck


## Step 5
I added this configurations:
```
quarkus.kafka-streams.application-id=aggregator-5
kafka-streams.processing.guarantee=at_least_once
kafka-streams.consumer.isolation.level=read_committed
```

Then:
- Start a kafka streams app with state store
- Stop it and delete state directory
- restart redpanda broker(s)
- Start a kafka streams app with state store
- Restoration is DONE!


## Step 6
I added this configurations:
```
quarkus.kafka-streams.application-id=aggregator-6
kafka-streams.processing.guarantee=exactly_once_v2
kafka-streams.consumer.isolation.level=read_committed
```

Then:
- Start a kafka streams app

```bash
bash kafka-transactions.sh --bootstrap-server localhost:1121 list

# prints
TransactionalId                                    	Coordinator	ProducerId	TransactionState
aggregator-6-40095974-41c1-4d5f-b45a-22c8715ed5b7-1	1          	6002      	Unknown
```

- Stop app and delete state directory
- restart redpanda broker(s)
- Start a kafka streams app
- Restoration is stuck

```text
2023-09-07 14:18:49,453 INFO  [org.apa.kaf.str.pro.int.StoreChangelogReader] 
    (aggregator-6-fdf3792a-91e9-4fb8-8cee-ed1c00040ea4-StreamThread-1) stream-thread 
    [aggregator-6-fdf3792a-91e9-4fb8-8cee-ed1c00040ea4-StreamThread-1] Restoration in progress for 1 partitions. 
    {aggregator-6-weather-stations-store-changelog-0: position=10, end=11, totalRestored=9}
```

List transactions
```bash
bash kafka-transactions.sh --bootstrap-server localhost:1121 list

# prints
TransactionalId                                    	Coordinator	ProducerId	TransactionState
aggregator-6-40095974-41c1-4d5f-b45a-22c8715ed5b7-1	1          	6002      	Unknown
aggregator-6-fdf3792a-91e9-4fb8-8cee-ed1c00040ea4-1	1          	7001      	Unknown
```

Tried to abort transaction(s)

```bash 
bash kafka-transactions.sh --bootstrap-server localhost:1121 abort --partition 0 \
     --topic aggregator-6-weather-stations-store-changelog --start-offset 0
     
# prints
Could not find any open transactions starting at offset 0 on partition aggregator-6-weather-stations-store-changelog-0
```

After some time transaction of producer 6002 disappeared. Problem persisted.
