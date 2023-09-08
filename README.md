# Hanging Changelog Restoration

This repo aims to reproduce the problem described below in a minimal a local setup.

It uses:
- docker compose to setup redpanda 
- quarkus to create 
  - a producer app (`./producer`)
  - a kafka streams app (`./aggregator`). 

The quarkus apps are inspired by [kafka-streams-quickstart](https://github.com/quarkusio/quarkus-quickstarts/blob/main/kafka-streams-quickstart/).

## Problem description

The kafka streams app gets stuck when restoring the state store from the changelog topics under these circumstances:

- Kafka streams app was running and changelog topic contains messages
- Redpanda is restarted
- Kafka streams restarts with empty state directory (so that it needs to restore its state)

Logs similar to this are printed:

```text
2023-09-07 13:12:14,974 INFO  [org.apa.kaf.str.pro.int.StoreChangelogReader] 
    (aggregator-2-07e80750-06ec-475c-8210-08580ce1bba0-StreamThread-1) stream-thread 
    [aggregator-2-07e80750-06ec-475c-8210-08580ce1bba0-StreamThread-1] Restoration in progress for 1 partitions.
    {aggregator-2-weather-stations-store-changelog-0: position=25, end=26, totalRestored=9}
```

## Steps to reproduce the problem

**Step 1: Start one redpanda instance**

```bash
docker compose --project-name restore-problem up --build -d
```

Will expose:
- localhost:18080   Console
- localhost:18081   Schema Registry
- localhost:19092   Redpanda

**Step 2: Produce messages**

Build and start a producer (using quarkus).

```bash
cd producer

quarkus dev
```

In the console you should find topics `temperature-values` and `weather-stations`.


**Step 3: Start kafka streams app**

In a new terminal

```bash
cd aggregator

quarkus dev
```

In the console you should find topics `temperatures-aggregated` and `aggregator-weather-stations-store-changelog`.

**Step 4: Stop both apps**

in both terminals stop the processes: `Cmd + c` / `Ctrl + c`

**Step 5: Restart redpanda**

```bash
docker compose --project-name restore-problem down
# wait for completion

docker compose --project-name restore-problem up --build -d 
# wait for completion
```

**Step 6: Delete State Dir**

Remove the folder `aggregator/state_dev_mode`

**Step 7: Start the aggregator**

```bash
cd aggregator

quarkus dev
```

See problem:

```text
# log:
Restoration in progress for 1 partitions. {aggregator-weather-stations-store-changelog-0: position=10, end=11, totalRestored=0}
```

**Cleanup**

```bash
docker compose --project-name restore-problem down
```

```bash
docker volume ls -f "name=restore-problem"

docker volume rm restore-problem_redpanda1
```

