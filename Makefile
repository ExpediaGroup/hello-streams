.PHONY: build start-confluent

start-confluent:
	echo "building confluent connect container"
	(cd confluent-stack && make build)
	echo "starting confluent stack"
	(cd confluent-stack && make run)

build:
	echo "Registering streams and schemas..."
	(cd stream-models && make build)
	echo "Building order-processor..."
	(cd order-processor && make build)
	echo "Building bean-processor..."
	(cd bean-processor && make build)
	echo "Building barista-processor..."
	(cd barista-processor && make build)
	echo "Building order-cleaner..."
	(cd order-cleaner && make build)

