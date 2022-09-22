package cs451.perfectlinks;
import cs451.Host;
import cs451.bebroadcast.BEBdeliver;
import cs451.models.MessageModel;
import cs451.utilities.BaseConfig;
import cs451.utilities.OutputHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PP2PDeliver {
    private final Set<Long> pp2poutput;
    private final BEBdeliver bebdeliver;

    public PP2PDeliver(Host myInstance, int numberOfHosts) {
        this.bebdeliver = new BEBdeliver(myInstance, numberOfHosts);
        this.pp2poutput = Collections.synchronizedSet(new HashSet<>());
    }

    public void deliver(ArrayList<MessageModel> messages, InetAddress sourceAddress, int sourcePort, short senderID) {
        // I always send the ack, even if I already have the packet

        try (DatagramSocket ackSocket = new DatagramSocket()){
            ByteBuffer ack = ByteBuffer.allocate(3*Integer.BYTES);
            ack.putShort(senderID); //ME
            ack.putInt(messages.get(0).getMessageID()); //Message ID of the first message
            ack.putShort(messages.get(0).getOriginalSenderID()); //Original sender of the first message
            // I only need to check this because the same message (same message ID, same sender, same Original sender)
            // can be only in one bucket

            // Creating the actual byte array from the buffer
            byte[] serializedAck = ack.array();

            DatagramPacket ackPacket = new DatagramPacket(serializedAck, serializedAck.length,
                    sourceAddress, sourcePort);
            ackSocket.send(ackPacket);

            if (!BaseConfig.deliverThreads.isShutdown()) {
                messages.forEach(message -> {
                    message.setSenderID(senderID);
                    // I check here if the message has already been delivered by perfect links or not
                    long valueToAdd = Long.parseLong(message.getSenderID()+""+message.getMessageID()+message.getOriginalSenderID());
                    if (this.pp2poutput.add(valueToAdd)){
                        bebdeliver.deliver(message);
                    }
                });
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }
}
