# CoralQueue
CoralQueue is a ultra-low-latency, lock-free and garbage-free concurrent circular queue for inter-thread communication. It uses memory barriers instead of locks to allow Java Threads to exchange messages.

It comes in many flavors for you to choose: Queue (one-producer-to-one-consumer), Multiplexer (many-producers-to-one-consumer), Demultiplexer (one-producer-to-many-consumers), MpMc (many-producers-to-many-consumers), etc.

## Queue


