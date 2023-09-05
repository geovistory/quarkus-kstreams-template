# Start

Download dev-stack

```text
git clone https://github.com/geovistory/dev-stack.git

cd dev-stack

bash ./scripts/build

# wait until stack up and running
```

Terminal a

```bash
cd producer
quarkus dev
```

Terminal b

```bash
cd aggregator
quarkus dev
```

Open redpanda console from dev-stack and see temperatures-aggregated topic:

http://localhost:1120/topics/temperatures-aggregated

At the same time, continuous testing is enabled. Hit 'd' in the terminal and navigate to Continuous Testing.
If you change (test-)code, the test will re-run. 