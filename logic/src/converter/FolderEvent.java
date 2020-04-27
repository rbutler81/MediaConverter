package converter;

import java.nio.file.WatchEvent;

public class FolderEvent {

    TimeInMillis time;
    String path;

    FolderEvent(String s, WatchEvent.Kind<?> k) {
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
                "time=" + time +
                ", path='" + path + '\'' +
                '}';
    }
}
