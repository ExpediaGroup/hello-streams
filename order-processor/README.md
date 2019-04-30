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
$ cd stream-models
$ make build
```

## Build

```
$ make build
```

## Run
```
$ make run
```

- Open a browser to http://localhost:5000/graphiql
