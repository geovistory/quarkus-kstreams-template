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


