package converter;

import configFileUtil.Config;
import logger.*;
import threads.Message;
import udp.heartbeat.HeartBeatStatus;
import udp.heartbeat.HeartBeatWorkerThread;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    // setup static variables ///////////////////////////////////////////////////////////////////
    static final String PATH = Paths.get(".").toAbsolutePath().normalize().toString() + "\\";

    public static void main(String[] args) throws IOException, InterruptedException, ConfigException {

        // check for config file + external executables
        final String CONFIG_FILE = PATH + args[0];
        File dataFile = new File(PATH + "data.ser");
        File iniFile = new File(CONFIG_FILE);
        File ffProbe = new File(PATH + "ffprobe.exe");
        File hbPath = new File(PATH + "HandBrakeCLI.exe");
        HashMap<String, VideoFile> persistedData = null;

        if (!hbPath.exists()) {
            throw new FileNotFoundException("HandBreakCLI.exe missing - place in same folder as JAR file");
        }
        if (!ffProbe.exists()) {
            throw new FileNotFoundException("ffprobe.exe missing - place in same folder as JAR file");
        }
        if (!iniFile.exists()) {
            throw new FileNotFoundException("Config file: " + CONFIG_FILE);
        }
        if (dataFile.exists()) {
            persistedData = readDataFile(dataFile);
        } else {
            persistedData = new HashMap<>();
        }

        // read config file
        final Config CONFIG_PARAMS = Config.readIniFile(CONFIG_FILE);
        final List<String> MEDIA_FOLDER = CONFIG_PARAMS.getParam("MediaFolder");
        final String OUTPUT_FOLDER = CONFIG_PARAMS.getSingleParamAsString("OutputFolder");
        final List<String> FILE_TYPES = CONFIG_PARAMS.getParam("ProcessFiles");
        final String PRESETS_FILE = CONFIG_PARAMS.getSingleParamAsString("PresetsFile");
        final String PRESET_NAME = CONFIG_PARAMS.getSingleParamAsString("PresetName");
        final Integer SCAN_INTERVAL_TIME_MIN = CONFIG_PARAMS.getSingleParamAsInt("ScanInterval_Minutes");


       while (true) {

            List<String> unknownFilesTypes = new ArrayList<>();

            for (String folder : MEDIA_FOLDER) {

                List<String> files = walkMediaFiles(folder);
                List<String> filesToScan = new ArrayList<>();

                for (String s : files) {

                    if (persistedData.containsKey(s)) {
                        if (!persistedData.get(s).isHevc() || (persistedData.get(s).getFileSize() != Files.size(Paths.get(s)))) {
                            filesToScan.add(s);
                        }
                    } else {
                        filesToScan.add(s);
                    }
                }

                List<VideoFile> vidFiles = scanVideoFiles(filesToScan, folder, OUTPUT_FOLDER, FILE_TYPES, unknownFilesTypes);

                // add files to the map, check file size again? serialize

                encode(vidFiles, hbPath, PRESETS_FILE, PRESET_NAME);

            }
            if (SCAN_INTERVAL_TIME_MIN == 0) break;
            clearScreen();
            if (unknownFilesTypes.size() > 0) {
                System.out.println("Unknown file types:");
                for (String s : unknownFilesTypes) {
                    System.out.println(s);
                }
            }

            System.out.println(java.time.LocalTime.now() + ": Sleeping for " + SCAN_INTERVAL_TIME_MIN + " minutes" );
            Thread.sleep(SCAN_INTERVAL_TIME_MIN * 1000 * 60);
        }
    }

    private static void encode(List<VideoFile> vidFiles, File hbPath, String presetsFile, String presetsName) {
        // encode using handbreak
        int filesToEncode = 0;
        for (VideoFile vf : vidFiles) {
            if (!vf.isHevc() && vf.isFormatCorrect()) filesToEncode = filesToEncode + 1;
        }

        int currentFile = 0;
        for (VideoFile vf : vidFiles) {
            if (!vf.isHevc() && vf.isFormatCorrect()) {
                currentFile = currentFile + 1;
                try {
                    File outputFolder = new File(vf.getOutputPath());
                    if (!outputFolder.exists()) {
                        outputFolder.mkdirs();
                    }

                    ProcessBuilder ps = new ProcessBuilder(hbPath.toString()
                            ,"--preset-import-file", presetsFile
                            , "-Z", "\"" + presetsName + "\""
                            , "-i", "\"" + vf.getPath() + "\""
                            , "-o", "\"" + vf.getFullOutputPath() + "\"");
                    ps.redirectErrorStream(true);
                    Process p = ps.start();

                    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line;
                    int x = 0;
                    while ((line = br.readLine()) != null) {
                        clearScreen();
                        System.out.println("Encoding: " + currentFile + " of " + filesToEncode);
                        System.out.println(line);
                        if (line.contains("Encode done!")) {
                            vf.setEncodingDone(true);
                        }
                    }
                } catch (Exception ex) {
                    System.out.println(ex);
                }

                // delete old file and copy new file back to original folder
                if (vf.isEncodingDone()) {
                    if (vf.getInputFile().delete()) {
                        String newFileName = vf.getInputPath() + "\\" + vf.getFileName();
                        vf.getOutputFile().renameTo(new File(newFileName));
                    }
                }
            }
        }
    }

    private static List<VideoFile> scanVideoFiles(List<String> filesToScan, String mediaFolder, String outputFolder, List<String> fileTypes, List<String> unknownFiles) {
        // use ffprobe on all files to find out what they've been encoded with - collect data
        Map<String, Integer> formats = new HashMap<>();
        List<VideoFile> vidFiles = new ArrayList<>();
        int numberOfFiles = filesToScan.size();
        int i = 0;
        for (String f : filesToScan) {
            i++;
            System.out.println("Scanning file " + i + " of " + numberOfFiles + " : " + f);
            if (VideoFile.isCorrectFormat(f, fileTypes)) {
                VideoFile vf = new VideoFile(f, mediaFolder, outputFolder);
                vidFiles.add(vf);
                if (!formats.containsKey(vf.getFormat())) {
                    formats.put(vf.getFormat(),1);
                } else {
                    Integer c = formats.get(vf.getFormat());
                    c = c + 1;
                    formats.replace(vf.getFormat(), c);
                }
            } else {
                unknownFiles.add(f);
            }
        }
        return vidFiles;
    }

    private static List<String> walkMediaFiles(String MEDIA_FOLDER) {
        // scan through media folder to find all files
        FileWalker fw = new FileWalker();
        List<String> files = new ArrayList<>();
        fw.walk(MEDIA_FOLDER, files);
        return files;
    }

    public static void clearScreen() throws IOException, InterruptedException {
        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
    }

    public static HashMap<String, VideoFile> readDataFile(File f) {

        VideoFileMap videoFileMap = null;

        // Reading the object from a file
        try {
            FileInputStream file = new FileInputStream(f.toString());
            ObjectInputStream in = new ObjectInputStream(file);

            // Method for deserialization of object
            videoFileMap = (VideoFileMap) in.readObject();

            in.close();
            file.close();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (videoFileMap != null) {
            return videoFileMap.getDictionary();
        } else {
            return new HashMap<String, VideoFile>();
        }
    }

    private static void saveDataFile(File f, HashMap<String, VideoFile> data) {

        if (f.exists()) {
            f.delete();
        }

        //Saving of object in a file
        FileOutputStream file = null;
        try {
            file = new FileOutputStream(f.toString());
            ObjectOutputStream out = new ObjectOutputStream(file);

            // Method for serialization of object
            out.writeObject(data);

            out.close();
            file.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
}
}
