package converter;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPServer implements Runnable{

	private Message msg;
	private int port;
	
	public UDPServer(int port, Message msg) {
		this.port = port;
		this.msg = msg;
	}
	
	@Override
	public void run() {
		
		DatagramSocket serverSocket = null;
		
		try {
			serverSocket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		System.out.println("Listening on port: " + port);
        
        while (true) {

        	try {

        		byte[] buffer = new byte[10000];
        		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        		serverSocket.receive(packet);

				System.out.println(System.currentTimeMillis());
				byte[] recvData = new byte[packet.getLength()];
				for (int i = 0; i < packet.getLength(); i++) {
					recvData[i] = buffer[i];
				}

				System.out.println(System.currentTimeMillis());
				ByteArrayInputStream bais = new ByteArrayInputStream(recvData);
				ObjectInputStream ois = new ObjectInputStream(bais);
				FolderEvent fe = (FolderEvent) ois.readObject();

				System.out.println(System.currentTimeMillis());
				System.out.println(fe);
				fe.getTime().waitForSecondsPast(10);
				System.out.println(System.currentTimeMillis());

			} catch (Exception e) {
				e.printStackTrace();
			}

			msg.addMsg(new FolderEvent(""));
			synchronized (msg){
				msg.notify();
			}
		}
		
	}
}
