package cs451.channel;
import cs451.models.Bucket;
import cs451.models.MessageModel;
import cs451.perfectlinks.PP2PSend;
import cs451.utilities.OutputHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

public class Sender extends Thread{

    private final Bucket bucketToSend;
    private final LinkedBlockingDeque<Bucket> buckets;
    private final PP2PSend.ackListener ackListener;
    private final int timeout;

    public Sender(Bucket bucketToSend, LinkedBlockingDeque<Bucket> buckets, int timeout, PP2PSend.ackListener ackListener) {
        this.ackListener = ackListener;
        this.bucketToSend = bucketToSend;
        this.buckets = buckets;
        this.timeout = timeout;
    }


    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {

            // I don't need to wait forever if I don't receive the ack -> set time out.
            // The time out is variable: I try to have a short timeout at first and then if the network is concested
            // I increase it
            socket.setSoTimeout(this.timeout);

            DatagramPacket packet = this.bucketToSend.createDatagram();

            // Here I send the bucket for the first time, so I log the action on the output file
            /*this.bucketToSend.getMessagesInDatagram().forEach(message -> {
                if (message.getSenderID() == message.getOriginalSenderID() && OutputHandler.getMyMessagesBroadcasted().add(message.getMessageID())){
                    OutputHandler.getBuffer().add("b " + message.getMessageID());
                }
            });*/
            socket.send(packet);

            // Now for the ack: I receive the ack on the same random port I sent the message from.
            // I expect the ack to have a fixed length of 8 bytes (two integers, the ID of the sender of the ack and
            // the ID of the message for which the ack is sent).
            byte[] ack = new byte[12];
            DatagramPacket ackPacket = new DatagramPacket(ack, ack.length);
            socket.receive(ackPacket);
            if (!checkAck(ack)) {
                // If for some reason the ack I receive is not correct, I will send again the bucket
                buckets.addFirst(bucketToSend);
            }

        } catch (IOException e) {
            // if I was not able to send or I did not receive the ack I put the message back in the queue
            // and this thread dies
            this.ackListener.onTimeout(false);
            buckets.addFirst(bucketToSend);
        }
    }


    private boolean checkAck(byte[] receivedAck) {
        ByteBuffer ackBuffer = ByteBuffer.wrap(receivedAck);
        short ackSenderID = ackBuffer.getShort();
        int ackMsgID = ackBuffer.getInt();
        short originalSenderID = ackBuffer.getShort();

        MessageModel firstMessageOfBucket = bucketToSend.getMessagesInDatagram().get(0);

        // Checking ack
        if (ackSenderID == firstMessageOfBucket.getSenderID() &&
                ackMsgID == firstMessageOfBucket.getMessageID() && originalSenderID == firstMessageOfBucket.getOriginalSenderID()) {
                this.ackListener.onAck(this.bucketToSend.getNumberOfMessages(), this.bucketToSend.numberOfOwnPackets(), this.bucketToSend.getLastMessage().getDestID());
                this.ackListener.onTimeout(true);
            return true;

        }
        return false;
    }
}
