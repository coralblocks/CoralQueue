## All about using the Queue

The queue is a circular data structure with pre-allocated <i> data transfer mutable objects</i>. You should see these data transfer mutable objects as <i>carriers of data</i>, in other words, they are there to allow
you to transfer data (and not object references) from producers to consumers. The steps are:

- A producer fetches an available data transfer mutable object from the queue
- The producer populates the mutable object with the data it wants to transfer (i.e. send) to the consumer(s)
- The producer flushes to notify the consumer(s)
- A consumer fetches an available data transfer mutable object from the queue
- The consumer reads the data from the mutable object
- The consumer calls <code>donePolling</code> to notify the producer(s)

