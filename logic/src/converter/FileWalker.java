package converter;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class FileWalker {

    public void walk(String path, List<String> output) {

        File root = new File( path );
        File[] list = root.listFiles();

        if (list == null) return;

        for ( File f : list ) {
            if ( f.isDirectory() ) {
                walk( f.getAbsolutePath(), output );
                System.out.println( "Dir: " + f.getAbsoluteFile() );
            }
            else {
                System.out.println( "File: " + f.getAbsoluteFile() );
                output.add(f.toString());
            }
        }
    }

}
