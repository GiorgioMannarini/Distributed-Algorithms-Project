package cs451.utilities;

public class ConfigPP2P extends BaseConfig {
    public ConfigPP2P(String filename) {
        super(filename);
    }

    public int getReceiver(){
        return Integer.parseInt(fileLines.get(0)[1]);
    }

}
