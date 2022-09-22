package cs451.urbroadcast;

import cs451.Constants;
import cs451.Host;
import cs451.Parser;
import cs451.bebroadcast.BEBsend;
import cs451.fifobroadcast.FIFOdeliver;
import cs451.lcbroadcast.LCBdeliver;
import cs451.models.MessageModel;
import cs451.models.SenderMessagePair;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class URBdeliver {
    // This structure implements the "ack[m]" structure seen in the algorithm plus the "forwarded" queue.
    // The idea is that whenever I receive a message that DOESN'T COME from me, if it's the first time I see it,
    // I forward it. Otherwise I just increment the number of processes from which I've seen that message.
    // As soon as I receive the same message from N/2+1 different processes I deliver it (write it in the output).
     private final ConcurrentHashMap<SenderMessagePair, AtomicInteger> acks;
     private final Host myInstance;
     private static BEBsend bebsend;
     private final int threshold;
     //private final FIFOdeliver fifoDeliver;
    private final LCBdeliver lcbdeliver;

     public URBdeliver(Host myInstance, int numberOfHosts){
         //this.fifoDeliver = new FIFOdeliver(numberOfHosts);
         this.lcbdeliver = new LCBdeliver();
         this.myInstance = myInstance;
         this.acks = new ConcurrentHashMap<>();
         this.threshold = (numberOfHosts / 2) +1;
         //System.out.println("Threshold: "+this.threshold);
     }

    public static void setBebsend (BEBsend bebsend) {URBdeliver.bebsend = bebsend;}

    public synchronized void deliver(MessageModel message){
         SenderMessagePair originalAndMessageIDPair = new SenderMessagePair(message.getOriginalSenderID(), message.getMessageID());
         if (!this.acks.containsKey(originalAndMessageIDPair)){
             if(message.getOriginalSenderID() != (short) this.myInstance.getId()) {
                 // This means that it is the first time I see that message and that I wasn't the one who sent it ->
                 // I need to broadcast it. I am the sender now, not the one from which I received the packet
                 message.setSenderID((short) myInstance.getId());
                 URBdeliver.bebsend.broadcastMessages(message);
             }
             // If I was the one that sent the message I don't need to broadcast it again
             this.acks.put(originalAndMessageIDPair, new AtomicInteger(1));
         }
         else {
             // Otherwise I increment the value
             this.acks.get(originalAndMessageIDPair).incrementAndGet();
         }
         // Now, is it safe to deliver it?
         if (acks.get(originalAndMessageIDPair).get() == threshold) {
             this.lcbdeliver.deliver(message);
         }
     }
}
