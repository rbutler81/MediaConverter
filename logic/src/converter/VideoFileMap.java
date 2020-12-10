package converter;

import java.io.Serializable;
import java.util.HashMap;

public class VideoFileMap implements Serializable {

    private static final long serialversionUID = 1293489128L;

    private HashMap<String, VideoFile> dictionary = null;

    public VideoFileMap() {
        this.dictionary = new HashMap<>();
    }

    public VideoFileMap(HashMap<String, VideoFile> dictionary) {
        this.dictionary = dictionary;
    }

    public HashMap<String, VideoFile> getDictionary() {
        return dictionary;
    }

    public VideoFileMap setDictionary(HashMap<String, VideoFile> dictionary) {
        this.dictionary = dictionary;
        return this;
    }
}
