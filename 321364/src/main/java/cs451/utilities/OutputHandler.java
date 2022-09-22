package cs451.utilities;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class OutputHandler {
    // I need a set (no duplicate elements) that keeps the order of insertion -> LinkedHashSet.
    // To have it concurrent:
    // https://stackoverflow.com/questions/5290790/is-there-any-concurrent-linkedhashset-in-jdk6-0-or-other-libraries
    private static final Set<String> bufferOutput = Collections.synchronizedSet(new LinkedHashSet<>());
    // I keep track of each message I broadcast, to add it to the buffer only once. This is because I empty the buffer
    // from time to time
    //private static final Set<Integer> myMessagesBroadcasted = Collections.synchronizedSet(new LinkedHashSet<>());
    private static String outputPath;

    public static Set<String> getBuffer() { return bufferOutput; }

    /*public static Set<Integer> getMyMessagesBroadcasted() {
        return myMessagesBroadcasted;
    }*/

    public static void setOutputPath (String path) {
        outputPath = path;
    }

    public static void writeInAppend() {
        try {
            synchronized (bufferOutput){
                Files.write(Paths.get(outputPath), bufferOutput, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                bufferOutput.clear();
            }
        } catch (IOException e){
            //e.printStackTrace();
        }
    }



}
