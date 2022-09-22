package cs451;

public class Constants {
    public static final int ARG_LIMIT_CONFIG = 7;

    // indexes for id
    public static final int ID_KEY = 0;
    public static final int ID_VALUE = 1;

    // indexes for hosts
    public static final int HOSTS_KEY = 2;
    public static final int HOSTS_VALUE = 3;

    // indexes for output
    public static final int OUTPUT_KEY = 4;
    public static final int OUTPUT_VALUE = 5;

    // indexes for config
    public static final int CONFIG_VALUE = 6;

    public static final int NUM_DELIVER_THREADS = 3;
    public static final int MAX_PACKET_LEN = 65000; //65000
    public static final int MAX_MESSAGES_PER_PROCESS = 5000;
    public static final int MAX_MESSAGES_SELF = 1000;
    public static final int MIN_TIMEOUT = 50;
    public static final int MAX_TIMEOUT = 3000;
    // Maximum output size after which I write the output in append, so that I don't waste too much memory.
    public static final int MAX_OUTPUT_SIZE = 10000;
}
