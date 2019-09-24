.PHONY: start-confluent register-streams build stop-confluent reset-processors delete-topics reset

start-confluent:
	echo "starting confluent stack..."
	(cd confluent-stack && make start)

register-streams:
	echo "Registering streams and schemas..."
	(cd stream-models && make build)

build:
	echo "Building order-processor..."
	(cd order-processor && make build)
	echo "Building bean-processor..."
	(cd bean-processor && make build)
	echo "Building barista-processor..."
	(cd barista-processor && make build)
	echo "Building order-cleaner..."
	(cd order-cleaner && make build)

stop-confluent:
	echo "stopping confluent stack..."
	(cd confluent-stack && make stop)

reset-processors:
	echo "Resetting order-processor"
	kafka-streams-application-reset --application-id order-processor --bootstrap-servers localhost:9092 --to-earliest --execute
	echo "Resetting bean-processor"
	kafka-streams-application-reset --application-id bean-processor --bootstrap-servers localhost:9092 --to-earliest --execute
	echo "Resetting barista-processor"
	kafka-streams-application-reset --application-id barista-processor --bootstrap-servers localhost:9092 --to-earliest --execute
	echo "Resetting order-cleaner"
	kafka-streams-application-reset --application-id order-cleaner --bootstrap-servers localhost:9092 --to-earliest --execute
	echo "Resetting local rocksDB"
	rm -rf /tmp/kafka-streams/

delete-topics:
	echo "Deleting beans topic"
	kafka-topics --bootstrap-server localhost:9092 --topic beans --delete
	echo "Deleting bean-command-events topic"
	kafka-topics --bootstrap-server localhost:9092 --topic bean-command-events --delete
	echo "Deleting orders topic"
	kafka-topics --bootstrap-server localhost:9092 --topic orders --delete
	echo "Deleting order-command-events topic"
	kafka-topics --bootstrap-server localhost:9092 --topic order-command-events --delete

reset: reset-processors delete-topics register-streams
