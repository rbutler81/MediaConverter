package converter;

import udp.RecvObjectUdp;

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

		RecvObjectUdp<FolderEvent> folderEventReceiver = new RecvObjectUdp<>();
		FolderEvent fe = folderEventReceiver.receive(6000);
		System.out.println();
	}
}
