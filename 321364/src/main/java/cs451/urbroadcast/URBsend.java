package cs451.urbroadcast;

import cs451.Constants;
import cs451.Parser;
import cs451.bebroadcast.BEBsend;
import cs451.models.MessageModel;

public class URBsend extends Thread {
    BEBsend bebsend;
    public URBsend(Parser parser) {
        this.bebsend = new BEBsend(parser);
        URBdeliver.setBebsend(this.bebsend);
    }

    public void broadcastMessage(MessageModel message){
        this.bebsend.broadcastMessages(message);
    }

    @Override
    public void run() {
        bebsend.run();
    }
}
