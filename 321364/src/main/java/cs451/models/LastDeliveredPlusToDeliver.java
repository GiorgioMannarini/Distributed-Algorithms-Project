package cs451.models;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

// I use this structure to keep track of the queue for each process PLUS the last message I delivered.
// I use it in FIFO and LCB
public class LastDeliveredPlusToDeliver {
        private final AtomicInteger lastDelivered;
        private final PriorityBlockingQueue<MessageModel> toDeliver;

        public LastDeliveredPlusToDeliver(){
            this.lastDelivered = new AtomicInteger(0);
            this.toDeliver = new PriorityBlockingQueue<>();
        }

        public AtomicInteger getLastDelivered() {
            return lastDelivered;
        }

        public void incrementLastDelivered() {
            this.lastDelivered.incrementAndGet();
        }

        public PriorityBlockingQueue<MessageModel> getToDeliver() {
            return toDeliver;
        }
    }
