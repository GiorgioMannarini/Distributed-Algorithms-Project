package cs451.perfectlinks;

import cs451.Constants;
import cs451.Host;
import cs451.Parser;
import cs451.models.Bucket;
import cs451.models.MessageModel;
import cs451.channel.Sender;
import cs451.utilities.BaseConfig;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class PP2PSend extends Thread {
    private final ConcurrentHashMap<Short, LinkedBlockingDeque<Bucket>> bucketsToSend;
    // To ensure some form of balance, instead of taking the first destination address every time.
    private final Random randomGetter;

    // I try to limit the number of messages that can be added (and thus sent) because if a process receives too many messages
    // the sender ack will often time out.
    private final Lock lock;
    private final Condition ownMessageCondition;
    private final Condition rebroadcastCondition;
    private final ConcurrentHashMap<Short, AtomicInteger> messagesInQueueForProcess;
    private final ConcurrentHashMap<Short, AtomicInteger> ownMessagesInQueue;
    private final AtomicInteger timeout;

    public PP2PSend(Parser parser) {
        this.bucketsToSend = new ConcurrentHashMap<>();
        this.randomGetter = new Random();
        this.lock = new ReentrantLock();
        this.ownMessageCondition = this.lock.newCondition();
        this.rebroadcastCondition = this.lock.newCondition();

        // I keep track, for each processes, of how many messages I need to send him.
        // I also check how many of these are mine (not rebroadcast) as, as explained below,
        // I give more importance to messages that I need to rebroadcast so that the delivery is faster
        this.messagesInQueueForProcess = new ConcurrentHashMap<>();
        this.ownMessagesInQueue = new ConcurrentHashMap<>();
        this.timeout = new AtomicInteger(50);

        for (Host host : parser.hosts()) {
            this.bucketsToSend.put((short) host.getId(), new LinkedBlockingDeque<>());
            this.messagesInQueueForProcess.put((short)host.getId(), new AtomicInteger(0));
            this.ownMessagesInQueue.put((short)host.getId(), new AtomicInteger(0));
        }
    }

    private boolean canAdd(boolean selfMessages) {
        int freeProcesses = 0;
        if (selfMessages) {
            for (AtomicInteger numMsg : this.ownMessagesInQueue.values()) {
                if (numMsg.get() < Constants.MAX_MESSAGES_SELF) {
                    freeProcesses += 1;
                }
            }
        } else {
            for (AtomicInteger numMsg : this.messagesInQueueForProcess.values()) {
                if (numMsg.get() < Constants.MAX_MESSAGES_PER_PROCESS) {
                    freeProcesses += 1;
                }
            }
        }

        return freeProcesses > this.messagesInQueueForProcess.size() / 2;
    }

    // The idea is that I check if the number of messages is greater than a threshold for more than N/2 processes
    // I wait, as the system is slowing down and I need to deliver the previous messages before
    public void addMessageFromExternalSource(MessageModel message) {
        lock.lock();
        try {
            // It's one of my messages
            if (message.getOriginalSenderID() == message.getSenderID()) {
                if (!this.canAdd(true)) {
                    this.ownMessageCondition.await();
                }
            } else {
                // It's a rebroadcast message
                if (!this.canAdd(false)) {
                    this.rebroadcastCondition.await();
                }
            }
            this.addMessage(message);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void addMessage(MessageModel message) {
        this.messagesInQueueForProcess.get(message.getDestID()).incrementAndGet();
        if (message.getSenderID() == message.getOriginalSenderID()) {
            this.ownMessagesInQueue.get(message.getDestID()).incrementAndGet();
        }

        short mySenderID = message.getSenderID();
        short destinationID = message.getDestID();
        // Here I add the message to the queue. Optimization for URBroadcast
        if (this.bucketsToSend.containsKey(destinationID)) {
            Bucket lastBucket = this.bucketsToSend.get(destinationID).pollLast();
            if (lastBucket != null) {
                if (!lastBucket.addToDatagram(message)) {
                    //System.out.println("Message added to bucket with first message: "+lastBucket.getMessagesInDatagram().get(0).getMessageID());
                    Bucket newBucket = new Bucket(mySenderID, message.getDestAddress(), message.getDestPort());
                    // Of course I suppose one single message can't be bigger than the size of the entire bucket
                    newBucket.addToDatagram(message);
                    this.bucketsToSend.get(destinationID).add(lastBucket);
                    this.bucketsToSend.get(destinationID).add(newBucket);
                } else {
                    this.bucketsToSend.get(destinationID).add(lastBucket);
                }
            } else {
                Bucket newBucket = new Bucket(mySenderID, message.getDestAddress(), message.getDestPort());
                newBucket.addToDatagram(message);
                this.bucketsToSend.get(destinationID).add(newBucket);
            }


        } else {
            Bucket newBucket = new Bucket(mySenderID, message.getDestAddress(), message.getDestPort());
            newBucket.addToDatagram(message);
            LinkedBlockingDeque<Bucket> newList = new LinkedBlockingDeque<>();
            newList.add(newBucket);
            this.bucketsToSend.put(destinationID, newList);
        }
    }

    public void setTimeout(boolean receivedAck) {
        if (!receivedAck) {
            this.timeout.set(Math.min(this.timeout.get() + 10, Constants.MAX_TIMEOUT));
        } else {
            this.timeout.set(Math.max(this.timeout.get() - 10, Constants.MIN_TIMEOUT));
        }
    }

    public void waitForAcks(int numMessagesDelivered, int ownMessagesDelivered, short processID) {
        // The idea is the following: basically I do not send all the messages regardless of what's going on in
        // the other processes. I limit the number of messages I can add to the queue with respect to how many
        // messages I already have to send. I give more importance to the messages to rebroadcast so that
        // deliveries are faster.
        this.messagesInQueueForProcess.get(processID).addAndGet(-numMessagesDelivered);
        this.ownMessagesInQueue.get(processID).addAndGet(-ownMessagesDelivered);
        lock.lock();
        try {
            if (this.canAdd(true)) {
                this.ownMessageCondition.signal();
            }
            if (this.canAdd(false)) {
                this.rebroadcastCondition.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        // An external thread (senderMainThread) is responsible of spawning a thread to send messages
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ArrayList<Short> currentKeys = new ArrayList<>(this.bucketsToSend.keySet());
                if (!(currentKeys.size() == 0)) {
                    short destID = currentKeys.get(this.randomGetter.nextInt(currentKeys.size()));
                    Bucket bucketToSend = this.bucketsToSend.get(destID).pollFirst();
                    if (bucketToSend != null && !BaseConfig.senderChildThread.isShutdown()) {
                        BaseConfig.senderChildThread.execute(new Sender(bucketToSend, this.bucketsToSend.get(destID), this.timeout.get(), new ackListener() {
                            @Override
                            public void onAck(int numMessagesDelivered, int ownMessagesDelivered, short processID) {
                                PP2PSend.this.waitForAcks(numMessagesDelivered, ownMessagesDelivered, processID);
                            }

                            @Override
                            public void onTimeout(boolean received) {
                                PP2PSend.this.setTimeout(received);
                            }
                        }));
                    }
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }

        }
    }

    public interface ackListener {
        // Callbacks: when I receive an ack I need to know from who, and I also need to know if I didn't receive it
        // Because of time out so that I can increase it.
        void onAck(int numMessagesDelivered, int ownMessagesDelivered, short processID);

        void onTimeout(boolean received);
    }

}
