package cs451.utilities;

import cs451.Constants;
import cs451.Host;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

// Base configurations for the project that do not depend on the protocol used
public abstract class BaseConfig {
    private static final String SPACES_REGEX = "\\s+";
    private final String filename;
    protected ArrayList<String[]> fileLines = new ArrayList<>();

    //I Will use some threads to deliver, but only one thread to broadcast MY OWN messages and one to listen.
    // I use only one thread to send because of this answer on the forum:
    // https://moodle.epfl.ch/mod/forum/discuss.php?d=65799#p132111
    public static ThreadPoolExecutor deliverThreads = (ThreadPoolExecutor) Executors.newFixedThreadPool(Constants.NUM_DELIVER_THREADS);
    public static ThreadPoolExecutor listenerThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    public static ThreadPoolExecutor senderMainThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    public static ThreadPoolExecutor senderChildThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

    // Needed to create the queue of messages. I broadcast my own messages in a different thread than the one
    // that is used to rebroadcast.
    public static ThreadPoolExecutor ownBroadcastThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    public static ThreadPoolExecutor reBroadcastThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);


    public BaseConfig(String filename){
        this.filename = filename;
        this.populate();
    }
    public void populate() {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            for (String line; (line = br.readLine()) != null;) {
                if (line.isBlank()) {
                    continue;
                }
                String[] splits = line.split(SPACES_REGEX);
                fileLines.add(splits);
            }
        } catch (IOException e) {
            System.err.println("Problem with the hosts file!");
        }
    }

    public int getMsgNumber(){
        return Integer.parseInt(fileLines.get(0)[0]);
    }

    public static Host getHostFromID(int ID, List<Host> hosts){
        for (Host host : hosts){
            if (host.getId() == ID) {
                return host;
            }
        }
        return null;
    }

    public static void shutdownAllThreadsImmediately(){
        if (deliverThreads != null) {
            deliverThreads.shutdownNow();
        }
        if (listenerThread != null) {
            listenerThread.shutdownNow();
        }
        if (senderMainThread != null) {
            senderMainThread.shutdownNow();
        }
        if (senderChildThread != null) {
            senderChildThread.shutdownNow();
        }
        if (ownBroadcastThread != null){
            ownBroadcastThread.shutdownNow();
        }
        if (reBroadcastThread != null){
            reBroadcastThread.shutdownNow();
        }

    }

}
