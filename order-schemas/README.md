# OrderCommandEvent Stream Modeling Pipeline

This project models OrderCommandEvents and builds the schema into avro `avsc` files and Java POJOs.
 
To add or change the definition of an OrderCommandEvent, 
modify the `src/main/avro/OrderCommandEvents.avdl` file and re-run the maven build.

## To Build

Ensure [confluent stack is up](../confluent-stack/README.md).
```bash
$ make build

```
