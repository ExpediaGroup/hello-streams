# confluent-stack

## Requirements
- confluent (local installation - See [Confluent installation][confluent-install])
- confluent-cli (See [Confluent CLI installation instructions][confluent-cli])

## To start Confluent stack
```bash
$ make start
```

## To stop Confluent stack
```bash
$ make stop
```

This will start up the following:
```
confluent local start
    The local commands are intended for a single-node development environment
    only, NOT for production usage. https://docs.confluent.io/current/cli/index.html

Using CONFLUENT_CURRENT: {...}/var/confluent.xDahqCZQ
Starting zookeeper
zookeeper is [UP]
Starting kafka
kafka is [UP]
Starting schema-registry
schema-registry is [UP]
Starting kafka-rest
kafka-rest is [UP]
Starting connect
connect is [UP]
Starting ksql-server
ksql-server is [UP]
Starting control-center
control-center is [UP]
```

## To verify Confluent stack

Open a browser to Confluent Command Center - [http://localhost:9021](http://localhost:9021)

[confluent-install]: https://docs.confluent.io/current/installation/installing_cp/zip-tar.html#prod-kafka-cli-install
[confluent-cli]: https://docs.confluent.io/current/cli/index.html
