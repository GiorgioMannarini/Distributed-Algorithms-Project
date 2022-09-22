package cs451;

import cs451.channel.Listener;
import cs451.lcbroadcast.LCBsend;
import cs451.models.MessageModel;
import cs451.utilities.ConfigLCB;
import cs451.utilities.OutputHandler;
import cs451.perfectlinks.PP2PDeliver;
import cs451.utilities.BaseConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        BaseConfig.shutdownAllThreadsImmediately();

        //write/flush output file if necessary
        System.out.println("Writing output.");
        OutputHandler.writeInAppend();
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

        initSignalHandlers();
        //ConfigPP2P config = new ConfigPP2P(parser.config());
        //InetAddress destAddress = null;
        //int destPort = 0;
        // example
        try {
            Files.deleteIfExists(Paths.get(parser.output()));
        } catch (IOException e) {
            //e.printStackTrace();
        }

        ConfigLCB config = new ConfigLCB(parser.config());
        LCBsend sender = new LCBsend(parser, config.getMask(parser.myId()));
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID: " + pid + "\n");
        System.out.println("From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid + "` to stop processing packets\n");
        System.out.println("My ID: " + parser.myId() + "\n");
        System.out.println("List of resolved hosts is:");
        System.out.println("==========================");
        for (Host host: parser.hosts()) {
            if (parser.myId() == host.getId()){
                int myPort = host.getPort();
                //Start listening in a new thread on that port
                Listener listener = new Listener(myPort, new PP2PDeliver(host, parser.hosts().size()), parser.hosts().size());
                if (!BaseConfig.listenerThread.isShutdown()) {
                    BaseConfig.listenerThread.execute(listener);
                }
            }
            System.out.println(host.getId());
            System.out.println("Human-readable IP: " + host.getIp());
            System.out.println("Human-readable Port: " + host.getPort());
            System.out.println();
        }

        System.out.println();

        System.out.println("Path to output:");
        System.out.println("===============");
        System.out.println(parser.output() + "\n");

        System.out.println("Path to config:");
        System.out.println("===============");
        System.out.println(parser.config() + "\n");
        System.out.println("Broadcasting and delivering messages...\n");

        OutputHandler.setOutputPath(parser.output());
        if (!BaseConfig.senderMainThread.isShutdown()){
            BaseConfig.senderMainThread.execute(sender);
        }
        for (int i = 1; i < config.getMsgNumber()+1; i++){
            final MessageModel message = new MessageModel((short) parser.myId(), i, i+"", (short) parser.myId());
            BaseConfig.ownBroadcastThread.execute(() -> {sender.broadcastMessage(message);});
        }
        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
