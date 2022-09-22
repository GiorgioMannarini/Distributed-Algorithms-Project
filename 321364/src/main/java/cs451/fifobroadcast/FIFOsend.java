package cs451.fifobroadcast;

import cs451.Parser;
import cs451.models.MessageModel;
import cs451.urbroadcast.URBsend;

public class FIFOsend extends Thread{
    Parser parser;
    URBsend urbSend;

    public FIFOsend(Parser parser){
        this.parser = parser;
        this.urbSend = new URBsend(this.parser);
    }

    public void broadcastMessage(MessageModel message){
        this.urbSend.broadcastMessage(message);
    }

    @Override
    public void run() {
        this.urbSend.run();
    }

}
