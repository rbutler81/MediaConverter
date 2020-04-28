package converter;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.file.WatchEvent;

public class FolderEvent implements Serializable {

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

    public boolean sendTo(String addr, int port) {

        try {
            InetAddress address = InetAddress.getByName(addr);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
            ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(byteStream));
            os.flush();
            os.writeObject(this);
            os.flush();
            //retrieves byte array
            byte[] sendBuf = byteStream.toByteArray();
            DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, address, port);
            int byteCount = packet.getLength();
            //packet.send(packet);
            os.close();
        } catch (IOException e) {
            System.err.println("Exception:  " + e);
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public String toString() {
        return "FolderEvent{" +
                "time=" + time +
                ", path='" + path + '\'' +
                '}';
    }
}
