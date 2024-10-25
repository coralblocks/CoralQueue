# CoralQueue
CoralQueue is a ultra-low-latency, lock-free and garbage-free concurrent circular data structures for inter-thread communication. It uses memory barriers instead of locks to allow Java threads (producers and consumers) to exchange messages as fast as possible. For some performance numbers you can check [this link](https://www.coralblocks.com/index.php/coralqueue-performance-numbers/).

It comes in many flavors for you to choose: [Queue](#queue) (one-producer-to-one-consumer), [Multiplexer](#multiplexer) (many-producers-to-one-consumer), [Demultiplexer](#demultiplexer) (one-producer-to-many-consumers), [MpMc](#mpmc) (many-producers-to-many-consumers), etc.

## Queue

<img src="images/Queue.png" alt="Queue" width="50%" height="50%" />

The Queue allows a single producer thread sending messages to the queue and a single consumer thread receiving messages from the queue, both running inside the same JVM. The consumer reads the messages in the same order that they were sent by the producer.

## Multiplexer

<img src="images/Mux.png" alt="Multiplexer" width="50%" height="50%" />

The Multiplexer allows multiple producer threads sending messages to the multiplexer and a single consumer thread receiving messages from the multiplexer, all running inside the same JVM. A multiplexer can be static (with a fixed number of producers) or dynamic (new producers can join at any time). Dynamic multiplexers are slow because they require locks, in order words, dynamic multiplexers are not lock-free.

## Demultiplexer

<img src="images/Demux.png" alt="Demultiplexer" width="50%" height="50%" />

The Demultiplexer allows a single producer thread sending messages to the demultiplexer and multiple consumer threads receiving messages from the demultiplexer, all running inside the same JVM. `Note that messages are not duplicated by the demultiplexer.` They are distributed among the consumer threads, in other words, a message is processed only once by one of the consumers. `Also note that the order that the consumers will process the messages is undetermined.` A demultiplexer can be static (with a fixed number of consumers) or dynamic (new consumers can join at any time). Dynamic demultiplexers are slow because they require locks, in other words, dynamic demultiplexer are not lock-free.

## MpMc

<img src="images/MpMc.png" alt="MpMc" width="50%" height="50%" />

The MpMc (i.e. Multiple Producers / Multiple Consumers) allows multiple producer threads sending messages to the mpmc and multiple consumer threads receiving messages from the mpmc, all running inside the same JVM. `Note that messages are not duplicated by the mpmc.` They are distributed among the consumer threads, in other words, a message is processed only once by one of the consumers. `Also note that the order that the consumers will process the messages is undetermined.` A MpMc can be static (with a fixed number of consumers and producers) or dynamic (new consumers and producers can join at any time). Dynamic mpmcs are slow because they require locks, in other words, dynamic mpmcs are not lock-free.
