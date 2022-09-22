package cs451.models;

import cs451.Constants;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Bucket {
    // A bucket is a collection of messages to send to the same host, in order to save on network resources.
    // For each bucket I keep track of the sender ID, the destination address and port and of course of
    // all the messages I need to send.
    // Every message in the bucket is preceded by an int that specifies how long is that message in terms of bytes
    private final short senderID;
    private int lenOfBucket;
    private final ArrayList<MessageModel> messagesInDatagram;
    private final InetAddress destinationAddress;
    private final int destinationPort;

    public Bucket(short senderID, InetAddress destinationAddress, int destinationPort){
        this.senderID = senderID;
        this.messagesInDatagram = new ArrayList<>();
        this.lenOfBucket = Integer.BYTES; //The first 4 bytes represent the sender ID
        this.destinationAddress = destinationAddress;
        this.destinationPort = destinationPort;
    }

    public Boolean addToDatagram(MessageModel message){
        if (this.lenOfBucket + message.getLenInBytes() + Integer.BYTES < Constants.MAX_PACKET_LEN){
            // Integer.BYTES because I will add the packet plus an int indicating how long is that packet
            messagesInDatagram.add(message);
            lenOfBucket += message.getLenInBytes()+Integer.BYTES;
            return true;
        }
        return false;
    }

    public DatagramPacket createDatagram(){
        // Here I add the serialized message model to the bucket.
        // This means that the first 4 bytes will tell who's the sender of the bucket.
        // Then, for each message, I have its length, that I need to know to "cut" the bucket again into packets
        // When receiving it, the original sender ID, the message ID and the payload.
        ByteBuffer packet = ByteBuffer.allocate(this.lenOfBucket);
        packet.putShort(this.senderID);
        messagesInDatagram.forEach(message -> {
            packet.putInt(message.getLenInBytes());
            packet.putShort(message.getOriginalSenderID());
            packet.putInt(message.getMessageID());
            for (int vectorElement : message.getVectorClock()){
                packet.putInt(vectorElement);
            }
            packet.put(message.getPayload().getBytes());
        });

        byte[] serializedContent = packet.array();
        return new DatagramPacket(serializedContent, serializedContent.length,
                this.destinationAddress, this.destinationPort);
    }

    public ArrayList<MessageModel> getMessagesInDatagram() {
        return messagesInDatagram;
    }

    public MessageModel getLastMessage(){
        return this.messagesInDatagram.get(this.messagesInDatagram.size()-1);
    }

    public int numberOfOwnPackets(){
        int ownPackets = 0;
        for (MessageModel message : this.messagesInDatagram){
            if (message.getOriginalSenderID() == message.getSenderID()){
                ownPackets += 1;
            }
        }
        return ownPackets;
    }

    public int getNumberOfMessages() {return messagesInDatagram.size();}

}
