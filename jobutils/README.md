# Batch jobs

This library offers small utility functions for generating batch jobs in code. See  the `BatchJob` class for details.

To use this library add the following on root level in `nais.yaml:`

```$yaml
leaderElection:true
```

Both the `run` and `runAsync` methods in this library wil utilize leader election to ensure only one instance runs the job.

The jobs will add a `jobId` to the MDC-context which can be used for searching and filtering in Kibana.