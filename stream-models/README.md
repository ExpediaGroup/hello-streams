# hello streams :: Stream Modeling Pipeline

This project models the command events and domain events for hello streams.
 
It builds the schema into avro `avsc` files and Java POJOs as well as registering
the proper subject naming strategy with the schema registry. 
 
To add or change the definition of an OrderCommandEvent or Order domain event,
modify the `src/main/avro/OrderCommandEvents.avdl` file and re-run the maven build.

To add or change the definition of a BeanSupplyCommandEvent or BeanSupply domain event,
modify the `src/main/avro/BeanSupplyCommandEvents.avdl` file and re-run the maven build.

## To Build

Ensure [confluent stack is up](../confluent-stack/README.md)!!.
```bash
$ make build
```
