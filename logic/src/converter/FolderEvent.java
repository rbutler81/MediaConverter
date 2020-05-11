package converter;

import udp.SendUdp;

import java.io.Serializable;

public class FolderEvent extends SendUdp implements Serializable {

    private static final long serialVersionUID = 4714006100374071177L;

    TimeInMillis time;
    String path;

    FolderEvent(String s) {
        this.time = new TimeInMillis();
        this.path = s;
    }

    public TimeInMillis getTime() {
        return time;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "FolderEvent{" +
                "time=" + time.getTime() +
                ", path='" + path + '\'' +
                '}';
    }
}
