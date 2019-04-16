# confluent-stack

## Requirements
- docker
- docker-compose

## Build
```bash
$ make build
```

## To run Confluent stack
```bash
$ make run
```

## To stop Confluent stack
```bash
$ make stop
```

This will start up the following:
```
Creating network "confluent-stack_default" with the default driver
Creating zookeeper ... done
Creating broker    ... done
Creating schema-registry ... done
Creating connect         ... done
Creating ksql-server     ... done
Creating ksql-cli        ... done
Creating control-center  ... done
```

## To verify Confluent stack

Open a browser to Confluent Command Center - [http://localhost:9021](http://localhost:9021)
