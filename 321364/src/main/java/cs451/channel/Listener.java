package cs451.channel;

import cs451.Host;
import cs451.models.MessageModel;
import cs451.perfectlinks.PP2PDeliver;
import cs451.utilities.BaseConfig;
import cs451.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Listener extends Thread {
    // A thread listening for new packets on the port specified in the config file.
    // When a packet arrives, the Deliver corresponding to the current protocol is called in another (child) thread.
    private final int listeningPort;
    private final PP2PDeliver pp2pdeliver;
    private int numberOfHosts;

    public Listener(int listeningPort, PP2PDeliver pp2PDeliver, int numberOfHosts) {
        this.pp2pdeliver = pp2PDeliver;
        this.listeningPort = listeningPort;
        this.numberOfHosts = numberOfHosts;
    }

    @Override
    public void run() {
        try {
            DatagramSocket listeningSocket = new DatagramSocket(this.listeningPort);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Creating the byte array that will store the packet
                    byte[] messagePacket = new byte[Constants.MAX_PACKET_LEN];

                    // Receiving the packet
                    DatagramPacket receivedPacket = new DatagramPacket(messagePacket, messagePacket.length);
                    listeningSocket.receive(receivedPacket);

                    // Getting thr sender ID, sender Inet address and sender port
                    ByteBuffer packetBuffer = ByteBuffer.wrap(messagePacket);
                    short senderID = packetBuffer.getShort();
                    InetAddress sourceAddress = receivedPacket.getAddress();
                    int sourcePort = receivedPacket.getPort();

                    // Parsing all the messages in the packet
                    ArrayList<MessageModel> receivedMessages = this.parseBucket(packetBuffer);

                    if (!BaseConfig.deliverThreads.isShutdown()) {
                        BaseConfig.deliverThreads.execute(() -> {
                            pp2pdeliver.deliver(receivedMessages, sourceAddress, sourcePort, senderID);
                        });
                    }
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
        } catch (SocketException e) {
            //e.printStackTrace();
        }
    }

    public ArrayList<MessageModel> parseBucket(ByteBuffer packetBuffer) {
        ArrayList<MessageModel> messagesInBucket = new ArrayList<>();
        int nextPacketLen = packetBuffer.getInt();
        while (nextPacketLen > 0){
            //System.out.println("NEXT PACKET LEN BEGINNING: "+nextPacketLen);
            byte[] messageBytes = new byte[nextPacketLen];
            packetBuffer.get(messageBytes, 0, nextPacketLen);
            ByteBuffer messageBuffer = ByteBuffer.wrap(messageBytes);
            short originalSender = messageBuffer.getShort();
            //System.out.println("Original sender: "+originalSender);
            int messageID = messageBuffer.getInt();
            //System.out.println("Message ID: "+messageID);
            MessageModel parsed = new MessageModel(originalSender, messageID, "");
            int[] vectorClock = new int[this.numberOfHosts];
            for (int i = 0; i < this.numberOfHosts; i++){
                vectorClock[i] = messageBuffer.getInt();
            }
            parsed.setVectorClock(vectorClock);
            parsed.setPayload(new String(messageBuffer.array(), StandardCharsets.UTF_8));
            messagesInBucket.add(parsed);
            nextPacketLen = packetBuffer.remaining() >= 4 ? packetBuffer.getInt() : 0;
            //System.out.println("NEXT PACKET LEN: "+nextPacketLen);
        }
        //System.out.println("RETURNED");
        return messagesInBucket;
    }
}
