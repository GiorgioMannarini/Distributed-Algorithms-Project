package cs451.fifobroadcast;

import cs451.Constants;
import cs451.models.LastDeliveredPlusToDeliver;
import cs451.models.MessageModel;
import cs451.utilities.OutputHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class FIFOdeliver {
    // The idea is that for every host (key of the hash map) I have a SORTED list of messages I need to deliver.
    // I also keep track of the last msg id I delivered for that host, so that I know at what point I am.
    private final ConcurrentHashMap<Integer, LastDeliveredPlusToDeliver> messagesToDeliver;

    public FIFOdeliver(int numberOfHosts){
        messagesToDeliver = new ConcurrentHashMap<>();
        for (int i = 1; i < numberOfHosts+1; i++){
            messagesToDeliver.put(i, new LastDeliveredPlusToDeliver());
        }
    }

    public synchronized void deliver(MessageModel message){
        // Here I get a message from URB. I check if I can deliver it, and if yes, if I can deliver other
        // messages from that source. Otherwise I put it in the queue
        int originalSender = message.getOriginalSenderID();
        LastDeliveredPlusToDeliver lastFromSender = messagesToDeliver.get(originalSender);

        // Safety check. I think this could never happen.
        if (message.getMessageID() < lastFromSender.getLastDelivered().get()){
            return;
        }
        int lastMessageDelivered = lastFromSender.getLastDelivered().get();
        if (lastMessageDelivered+1 == message.getMessageID()){
            // It is safe to deliver the message? Ok, then I deliver it
            OutputHandler.getBuffer().add("d "+originalSender+" "+message.getMessageID());
            //System.out.println("FIFO Delivered message "+message.getMessageID()+"from "+message.getOriginalSenderID());
            lastFromSender.incrementLastDelivered();

            // Now I loop on the queue to check if I can deliver someone else
            boolean otherMessagesToDeliver = true;
            while(otherMessagesToDeliver){
                MessageModel nextMessage = lastFromSender.getToDeliver().poll();
                if (nextMessage == null){
                    otherMessagesToDeliver = false;
                }
                else if (nextMessage.getMessageID() != lastFromSender.getLastDelivered().get()+1){
                    otherMessagesToDeliver = false;
                    lastFromSender.getToDeliver().put(nextMessage);
                }
                else {
                    OutputHandler.getBuffer().add("d "+originalSender+" "+nextMessage.getMessageID());
                    //System.out.println("FIFO Delivered in ELSE message "+nextMessage.getMessageID()+"from "+nextMessage.getOriginalSenderID());
                    lastFromSender.incrementLastDelivered();

                }
            }
        }
        else {
            // Otherwise I put the message in the related queue
            lastFromSender.getToDeliver().put(message);
        }
        if (OutputHandler.getBuffer().size() >= Constants.MAX_OUTPUT_SIZE){
            OutputHandler.writeInAppend();
        }

    }

}
