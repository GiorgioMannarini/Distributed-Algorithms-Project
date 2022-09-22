package cs451.utilities;

public class ConfigLCB extends BaseConfig{
    int numberOfHosts;
    public ConfigLCB(String filename){
        super(filename);
        this.numberOfHosts = this.fileLines.size() -1;
    }
    public int[] getMask(int hostID){
        int[] mask = new int[this.numberOfHosts];
        String[] hostLine = this.fileLines.get(hostID);
        for (String id : hostLine){
            mask[Integer.parseInt(id)-1] = 1;
        }
        return mask;
    }
}
