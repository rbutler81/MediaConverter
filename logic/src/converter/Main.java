package converter;

import configFileUtil.Config;
import logger.ConfigException;
import logger.Log;
import logger.LogConfig;
import udp.RecvObjectUdp;

import java.io.*;
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
        File iniFile = new File(CONFIG_FILE);
        File ffProbe = new File(PATH + "ffprobe.exe");
        File hbPath = new File(PATH + "HandBrakeCLI.exe");
        if (!hbPath.exists()) {
            throw new FileNotFoundException("HandBreakCLI.exe missing - place in same folder as JAR file");
        }
        if (!ffProbe.exists()) {
            throw new FileNotFoundException("ffprobe.exe missing - place in same folder as JAR file");
        }
        if (!iniFile.exists()) {
            throw new FileNotFoundException("Config file: " + CONFIG_FILE);
        }

        // read config file
        final Config CONFIG_PARAMS = Config.readIniFile(CONFIG_FILE);
        final List<String> MEDIA_FOLDER = CONFIG_PARAMS.getParam("MediaFolder");
        final String OUTPUT_FOLDER = CONFIG_PARAMS.getSingleParamAsString("OutputFolder");
        final List<String> FILE_TYPES = CONFIG_PARAMS.getParam("ProcessFiles");
        final String PRESETS_FILE = CONFIG_PARAMS.getSingleParamAsString("PresetsFile");
        final String PRESET_NAME = CONFIG_PARAMS.getSingleParamAsString("PresetName");
        final Integer SCAN_INTERVAL_TIME_MIN = CONFIG_PARAMS.getSingleParamAsInt("ScanInterval_Minutes");

        // create logger
        LogConfig logConfig = new LogConfig(20000000, 5);
        Log logger = new Log(logConfig, PATH, "hevc_converter.log", "");

        // create synchronized message queue (thread safe)
        Message msg = new Message();

        // create and launch udp server
        UDPServer udpListener = new UDPServer(6000, msg);
        Thread udpServer = new Thread(udpListener, "UDP Server");
        udpServer.start();
        Thread.sleep(5000);

        FolderEvent fe = new FolderEvent("c:\\test\\maybe\\testme\\");
        fe.sendTo("localhost", 6000);
        Thread.sleep(999999999);

        Thread folderWatcher = new Thread(new FolderWatcher(MEDIA_FOLDER.get(0), msg, logger), "Folder Watcher");
        folderWatcher.start();


       while (true) {

            List<String> unknownFilesTypes = new ArrayList<>();

            for (String folder : MEDIA_FOLDER) {
                List<String> files = walkMediaFiles(folder);
                List<VideoFile> vidFiles = scanVideoFiles(files, folder, OUTPUT_FOLDER, FILE_TYPES, unknownFilesTypes);
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
}
