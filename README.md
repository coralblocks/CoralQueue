# CoralQueue
CoralQueue is a ultra-low-latency, lock-free, garbage-free, batching and concurrent collection of circular data structures for inter-thread communication in Java. It uses memory barriers instead of locks to allow Java threads (producers and consumers) to exchange messages as fast as possible. For some performance numbers you can check [this link](https://www.coralblocks.com/index.php/coralqueue-performance-numbers/).

The data structures are: [Queue](#queue) (one-producer-to-one-consumer), [Multiplexer](#multiplexer) (many-producers-to-one-consumer), [Demultiplexer](#demultiplexer) (one-producer-to-many-consumers), [MpMc](#mpmc) (many-producers-to-many-consumers), etc.

## Queue

<img src="images/Queue.png" alt="Queue" width="50%" height="50%" />

The Queue allows a single producer thread sending messages to the queue and a single consumer thread receiving messages from the queue, both running inside the same JVM. The consumer reads the messages in the same order that they were sent by the producer.

## Multiplexer

<img src="images/Mux.png" alt="Multiplexer" width="50%" height="50%" />

The Multiplexer allows multiple producer threads sending messages to the multiplexer and a single consumer thread receiving messages from the multiplexer, all running inside the same JVM.

## Demultiplexer

<img src="images/Demux.png" alt="Demultiplexer" width="50%" height="50%" />

The Demultiplexer allows a single producer thread sending messages to the demultiplexer and multiple consumer threads receiving messages from the demultiplexer, all running inside the same JVM. `Note that messages are not duplicated by the demultiplexer.` They are distributed among the consumer threads, in other words, a message is processed only once by one of the consumers. `Also note that the order that the consumers will process the messages is undetermined.`

## MpMc

<img src="images/MpMc.png" alt="MpMc" width="50%" height="50%" />

The MpMc (i.e. Multiple Producers / Multiple Consumers) allows multiple producer threads sending messages to the mpmc and multiple consumer threads receiving messages from the mpmc, all running inside the same JVM. `Note that messages are not duplicated by the mpmc.` They are distributed among the consumer threads, in other words, a message is processed only once by one of the consumers. `Also note that the order that the consumers will process the messages is of course undetermined as is the order that the producers will send the messages.`
