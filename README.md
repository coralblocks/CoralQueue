# CoralQueue
CoralQueue is a ultra-low-latency, lock-free and garbage-free concurrent circular queue for inter-thread communication. It uses memory barriers instead of locks to allow Java Threads to exchange messages.

It comes in many flavors for you to choose: Queue (one-producer-to-one-consumer), Multiplexer (many-producers-to-one-consumer), Demultiplexer (one-producer-to-many-consumers), MpMc (many-producers-to-many-consumers), etc.

## Queue

<img src="images/Queue.png" alt="Queue" width="50%" height="50%" />

The Queue allows a single producer thread sending messages to the queue and a single consumer thread receiving messages from the queue, both running inside the same JVM.

## Multiplexer

<img src="images/Mux.png" alt="Queue" width="50%" height="50%" />

The Multiplexer allows a multiple producer threads sending messages to the multiplexer and a single consumer thread receiving messages from the multiplexer, all running inside the same JVM.
