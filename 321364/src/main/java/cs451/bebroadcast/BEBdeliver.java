package cs451.bebroadcast;

import cs451.Host;
import cs451.models.MessageModel;
import cs451.urbroadcast.URBdeliver;

public class BEBdeliver {

    private final URBdeliver urbdeliver;
    public BEBdeliver(Host myInstance, int numberOfHosts){
        this.urbdeliver = new URBdeliver(myInstance, numberOfHosts);
    }

    public void deliver(MessageModel message){
        this.urbdeliver.deliver(message);
    }
}
