package converter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class VideoFile implements Serializable {

    private static final long serialversionUID = 129348938L;

    private boolean isHevc = false;
    private String path = "";
    private String outputPath = "";
    private String fileName = "";
    private String format = "";
    private boolean noAudio = true;
    private File inputFile = null;
    private File outputFile = null;
    private boolean encodingDone = false;
    private long fileSize = 0;

    public VideoFile(String path, String inputPath, String outputPath) {
        this.path = path;
        this.inputFile = new File(this.path);
        try {
            this.fileSize = Files.size(Paths.get(inputFile.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> lines = new ArrayList<>();
        try {
            ProcessBuilder ps = new ProcessBuilder("ffprobe.exe","\"" + this.path + "\"");
            ps.redirectErrorStream(true);
            Process p = ps.start();
            //p.waitFor();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            int x = 0;
            while((line = br.readLine()) != null){
                lines.add(line);
                checkLineForVidStream(line, x);
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }

        if (this.format.equals("")) {
            for (int x = 1; x < 10; x++) {
                for (String l : lines) {
                    checkLineForVidStream(l, x);
                    if (!this.format.equals("")) break;
                }
                if (!this.format.equals("")) break;
            }
        }
        if (this.format.equalsIgnoreCase("hevc")) {
            this.isHevc = true;
        }

        if (!this.format.equals("")) {
            int i = this.path.lastIndexOf("\\");
            this.fileName = this.path.substring(i+1);
            this.fileName = this.fileName.substring(0, this.fileName.lastIndexOf(".")) + ".mkv";
            String mediaFolder = this.path.substring(inputPath.length());
            this.outputPath = outputPath + mediaFolder;
            i = this.outputPath.lastIndexOf("\\");
            this.outputPath = this.outputPath.substring(0,i);
        }
        this.outputFile = new File(this.getFullOutputPath());
        String t = this.getInputPath();
    }

    public boolean isEncodingDone() {
        return encodingDone;
    }

    public VideoFile setEncodingDone(boolean encodingDone) {
        this.encodingDone = encodingDone;
        return this;
    }

    public String getFullOutputPath() {
        return outputPath + "\\" + fileName;
    }

    public String getInputPath() {
        return this.path.substring(0,this.path.lastIndexOf("\\"));
    }

    public File getInputFile() {
        return inputFile;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isNoAudio() {
        return noAudio;
    }

    public boolean isFormatCorrect() {

        return !this.format.equals("");
    }

    public long getFileSize() {
        return fileSize;
    }

    public VideoFile setFileSize(long fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    private void checkLineForVidStream(String line, int x) {
        if (line.contains("Stream #0:" + x) && line.contains("Video:")) {
            int i = line.indexOf("Video:") + 7;
            int j = line.indexOf(" ",i);
            this.format = line.substring(i,j);
        } else if (line.contains("Stream #0:") && line.contains("Audio:")) {
            this.noAudio = false;
        }
    }

    public String getFormat() {
        return format;
    }

    public boolean isHevc() {
        return isHevc;
    }

    public String getPath() {
        return path;
    }

    public static boolean isCorrectFormat(String s, List<String> t) {
        boolean r = false;
        for (String str : t) {
            String ext = s.substring(s.lastIndexOf(".") + 1);
            if (ext.equalsIgnoreCase(str)) {
                r = true;
            }
            if (r) break;
        }
        return r;
    }

}
