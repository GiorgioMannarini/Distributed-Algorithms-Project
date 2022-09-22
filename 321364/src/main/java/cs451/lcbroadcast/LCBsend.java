package cs451.lcbroadcast;

import cs451.Host;
import cs451.Parser;
import cs451.models.MessageModel;
import cs451.urbroadcast.URBsend;
import cs451.utilities.OutputHandler;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LCBsend extends Thread{
    Parser parser;
    URBsend urbSend;
    private ConcurrentHashMap<Short, AtomicInteger> vectorClock;
    int messagesSent;
    private int[] dependencies;
    public LCBsend(Parser parser, int[] dependencies){
        this.parser = parser;
        this.urbSend = new URBsend(this.parser);
        this.vectorClock = new ConcurrentHashMap<>();
        this.dependencies = dependencies;
        for (Host host: parser.hosts()) {
            this.vectorClock.put((short) host.getId(), new AtomicInteger(0));
        }
        this.messagesSent = 0;
        LCBdeliver.setVectorClock(this.vectorClock);
    }

    public void broadcastMessage(MessageModel message){
        //Synchronized because I don't want delivery lines to be written while I am creating the VC
        //Otherwise the LCB might be violated
        synchronized (this.vectorClock) {
            int[] vectorClockToSend = new int[dependencies.length];
            //vectorClock.get(parser.myId()).incrementAndGet();
            for (int i = 0; i < vectorClockToSend.length; i++){
                vectorClockToSend[i] = vectorClock.get((short) (i+1)).get() * dependencies[i];
            }
            vectorClockToSend[parser.myId()-1] = this.messagesSent;
            messagesSent += 1;
            message.setVectorClock(vectorClockToSend);
            OutputHandler.getBuffer().add("b " + message.getMessageID());
        }
        this.urbSend.broadcastMessage(message);
    }

    @Override
    public void run() {
        this.urbSend.run();
    }
}
