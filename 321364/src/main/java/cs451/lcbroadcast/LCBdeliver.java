package cs451.lcbroadcast;

import cs451.Constants;
import cs451.models.LastDeliveredPlusToDeliver;
import cs451.models.MessageModel;
import cs451.utilities.OutputHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LCBdeliver {
    // Idea: share vector clock between sender and receiver
    private static ConcurrentHashMap<Short, AtomicInteger> vectorClock;
    // For each process I have a hashmap of objects LastDeliveredPlusToDeliver.
    // Last delivered = id of the last delivered msg for that process, ToDeliver = list of messages that that process
    // Sent and I still have to deliver.
    private ArrayList<MessageModel> messagesToDeliver;

    public LCBdeliver(){
        this.messagesToDeliver = new ArrayList<>();
    }

    public static void setVectorClock(ConcurrentHashMap<Short, AtomicInteger> vc){
        LCBdeliver.vectorClock = vc;
    }

    public synchronized void deliver(MessageModel message){
        int[] messageVc = message.getVectorClock();

        if (this.canDeliver(messageVc)){
            // Synchronized because I don't want to deliver while I am creating the VC of a message I am sending
            // Otherwise it might be incorrect
            synchronized (LCBdeliver.vectorClock) {
                OutputHandler.getBuffer().add("d "+message.getOriginalSenderID()+" "+message.getMessageID());
                //System.out.println("Delivered message "+message.getMessageID()+ "From "+ message.getOriginalSenderID());
                LCBdeliver.vectorClock.get(message.getOriginalSenderID()).incrementAndGet();
            }

            boolean otherMessages = true;
            while(otherMessages){
                // The idea is to recursively check if I can deliver a new message after I delivered one.
                otherMessages = false;
                Iterator <MessageModel> iterableMessages = this.messagesToDeliver.iterator();
                while (iterableMessages.hasNext()){
                    MessageModel messageQueued = iterableMessages.next();
                    if(this.canDeliver(messageQueued.getVectorClock())){
                        synchronized (LCBdeliver.vectorClock){
                            OutputHandler.getBuffer().add("d "+messageQueued.getOriginalSenderID()+" "+messageQueued.getMessageID());
                            //System.out.println("Delivered message "+messageQueued.getMessageID()+ "From "+ messageQueued.getOriginalSenderID());
                            LCBdeliver.vectorClock.get(messageQueued.getOriginalSenderID()).incrementAndGet();
                        }
                        otherMessages = true;
                        iterableMessages.remove();
                    }
                }
            }
        }
        else {
            messagesToDeliver.add(message);
        }
        if (OutputHandler.getBuffer().size() >= Constants.MAX_OUTPUT_SIZE){
            OutputHandler.writeInAppend();
        }
    }

    private boolean canDeliver(int[] messageVc){
        //Check if I can deliver a specific message
        boolean canDeliver = true;
        int i = 0;
        while (canDeliver && i < messageVc.length){
            if (messageVc[i] > LCBdeliver.vectorClock.get((short) (i+1)).get()){
                canDeliver = false;
            }
            i += 1;
        }
        return canDeliver;
    }


}
