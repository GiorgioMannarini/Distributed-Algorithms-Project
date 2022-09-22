package cs451.models;

public class SenderMessagePair {
    short originalSender;
    int messageID;
    public SenderMessagePair(short originalSender, int messageID) {
        this.originalSender = originalSender;
        this.messageID = messageID;
    }

    // Unique deterministic way to map two integers into one. Credits:
    // https://stackoverflow.com/questions/919612/mapping-two-integers-to-one-in-a-unique-and-deterministic-way
    @Override
    public int hashCode() {
        return this.originalSender >= this.messageID ? this.originalSender *
                this.originalSender + this.originalSender + this.messageID : this.originalSender + this.messageID * this.messageID;
    }

    @Override
    public boolean equals(Object obj) {
        SenderMessagePair myobj = (SenderMessagePair) obj;
        return this.originalSender == myobj.originalSender && this.messageID == myobj.messageID;
    }
}
