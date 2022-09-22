package cs451.bebroadcast;
import cs451.Host;
import cs451.Parser;
import cs451.models.MessageModel;
import cs451.perfectlinks.PP2PSend;
import cs451.utilities.BaseConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class BEBsend extends Thread{
    PP2PSend pp2PSend;
    Parser parser;

    // One instance of BEBsend will be created, but it is used by URBsend and also by URBdeliver
    public BEBsend (Parser parser) {
        this.pp2PSend = new PP2PSend(parser);
        this.parser = parser;
    }

    public void broadcastMessages(MessageModel message){
        // I send the message to every host. However, the rebroadcast is done in a different thread
        // Because I could add messages to rebroadcast but not my own, given the two different lock
        // conditions in PP2Psend. Again, this is done because I want to give higher priority to the messages
        // I want to rebroadcast so that I can deliver them faster.
        for (Host host : this.parser.hosts()){
            try {
                if (message.getOriginalSenderID() != message.getSenderID()){
                    BaseConfig.reBroadcastThread.execute(() -> {
                        try {
                            MessageModel messageToSend = new MessageModel(message.getSenderID(), (short) host.getId(),
                                    InetAddress.getByName(host.getIp()), host.getPort(), message.getMessageID(),
                                    message.getPayload(), message.getOriginalSenderID());
                            messageToSend.setVectorClock(message.getVectorClock());
                            this.pp2PSend.addMessageFromExternalSource(messageToSend);
                        } catch (UnknownHostException e) {
                            //e.printStackTrace();
                        }
                    });
                }
                else {
                    // Executed in a different thread
                    MessageModel messageToSend = new MessageModel(message.getSenderID(), (short) host.getId(),
                            InetAddress.getByName(host.getIp()), host.getPort(), message.getMessageID(),
                            message.getPayload(), message.getOriginalSenderID());
                    messageToSend.setVectorClock(message.getVectorClock());
                    this.pp2PSend.addMessageFromExternalSource(messageToSend);
                }

            }
            catch (UnknownHostException e){
                //e.printStackTrace();
            }

        }
    }

    @Override
    public void run(){
        pp2PSend.run();
    }

}
