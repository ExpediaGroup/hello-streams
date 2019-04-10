# hello-streams :: orderprocessor

## Start Confluent Stack
```
# from top level directory
$ cd confluent-stack
$ docker-compose up -d
```

## Register Streams (requires confluent stack)
```
# from top level directory
$ cd order-schemas
$ make build
```

## Build

```
# from top level directory
$ cd orderprocessor
$ make build
```

## Run
```
# from top level directory
$ cd orderprocessor
$ make run
```

- Open a browser to http://localhost:5000/graphiql


## Test
```
# from top level directory
$ cd orderprocessor
$ ./mvnw clean test
```
