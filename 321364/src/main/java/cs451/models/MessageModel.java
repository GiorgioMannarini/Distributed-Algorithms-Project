package cs451.models;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;

public class MessageModel implements Serializable, Comparable<MessageModel> {
    private short senderID;
    private final short originalSenderID; //For URB and the other abstractions on top of it
    private short destID;
    private InetAddress destAddress;
    private int destPort;
    private final int messageID;
    private String payload;
    private int lenInBytes;
    // FOR LCB
    private int[] vectorClock;

    public MessageModel(short senderID, short destID, InetAddress destAddress, int destPort, int messageID, String payload,
                        short originalSenderID) {
        this.senderID = senderID;
        this.originalSenderID = originalSenderID;
        this.destID = destID;
        this.destAddress = destAddress;
        this.destPort = destPort;
        this.messageID = messageID;
        this.payload = payload;
        this.lenInBytes = 1*Integer.BYTES + 1*Short.BYTES + this.payload.getBytes().length;
    }

    public MessageModel(short senderID, int messageID, String payload, short originalSenderID) {
        this.senderID = senderID;
        this.messageID = messageID;
        this.payload = payload;
        this.originalSenderID = originalSenderID;
        this.lenInBytes = 1*Integer.BYTES + 1*Short.BYTES + this.payload.getBytes().length;
    }

    // Constructor for the listener when sending more than one message at a time
    public MessageModel(short originalSenderID, int messageID, String payload){
        this.originalSenderID = originalSenderID;
        this.messageID = messageID;
        this.payload = payload;
        this.lenInBytes = 1*Integer.BYTES + 1*Short.BYTES + this.payload.getBytes().length;
        // Basically in each packet in a bucket I need its length in bytes to properly serialize it.
    }

    public int getLenInBytes() {
        return lenInBytes;
    }

    public short getSenderID() {
        return senderID;
    }

    public short getDestID() { return destID; }

    public InetAddress getDestAddress() {
        return destAddress;
    }

    public int getDestPort() {
        return destPort;
    }

    public Integer getMessageID() {
        return messageID;
    }

    public String getPayload() {
        return payload;
    }

    public short getOriginalSenderID() {
        return originalSenderID;
    }

    public void setSenderID(short senderID) {
        this.senderID = senderID;
    }

    public int[] getVectorClock() {
        return vectorClock;
    }

    public void setPayload(String payload){
        this.payload = payload;
    }

    public void setVectorClock(int[] vectorClock){
        this.vectorClock = vectorClock;
        this.lenInBytes += vectorClock.length * Integer.BYTES;
    }

    @Override
    public int compareTo(MessageModel o) {
        return this.getMessageID().compareTo(o.messageID);
    }

}
