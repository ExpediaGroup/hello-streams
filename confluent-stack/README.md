# confluent-stack

## Requirements
- docker
- docker-compose


## To run Confluent stack
```
$ docker-compose up -d
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

Open a browser to [http://localhost:9021](http://localhost:9021)
