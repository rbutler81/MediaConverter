package converter;

import logger.Log;

import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

public class FolderWatcher implements Runnable {

    private Message msg;
    private WatchService watcher;
    private Log logger;
    private Path folderPath;
    private WatchKey key;

    public FolderWatcher(String folderPath, Message msg, Log logger) {
        this.msg = msg;
        this.logger = logger;
        this.folderPath = Paths.get(folderPath);
        try {
            this.watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        while (true) {

            try {
                key = folderPath.register(watcher,
                        ENTRY_CREATE,
                        ENTRY_DELETE,
                        ENTRY_MODIFY);
            } catch (IOException x) {
                logger.appendLine("FolderWatcher{" + x + "}");
            }

            while (true) {

                try {
                    key = watcher.take();
                } catch (InterruptedException x)  {
                    logger.appendLine("FolderWatcher{" + x + "}");
                }

                for (WatchEvent<?> event : key.pollEvents()) {

                    WatchEvent.Kind<?> kind = event.kind();
                    WatchEvent<Path> ev = (WatchEvent<Path>)event;
                    Path filename = ev.context();
                    String test = filename.toString();
                    System.out.println();

                }

            }
        }
    }
}
