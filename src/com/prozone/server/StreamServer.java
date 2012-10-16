package com.prozone.server;


import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;



public class StreamServer {

	private DatagramSocket socket;
	private static final int PORT = 9998;
	private DatagramPacket dataPacket;
	private byte[] buf = new byte[2780800];
	private Dimension screenBounds;
	private IMediaWriter writer ;
	
	public StreamServer()
	{
		try {
			socket = new DatagramSocket(PORT, InetAddress.getByName("192.168.1.12"));
			socket.setSoTimeout(0); // blocking read
			dataPacket = new DatagramPacket(buf, buf.length);
			writer = ToolFactory.makeWriter("/home/neer/movie.mp4");
			
			screenBounds = Toolkit.getDefaultToolkit().getScreenSize();

			writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4,
					                   screenBounds.width/2, screenBounds.height/2);

		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void receiveData(){
		int i = 0;
		long startTime = System.nanoTime();

		while(i < 300){
			
			try {
				System.out.println("Waiting for packet data");
				socket.receive(dataPacket);
				ByteArrayInputStream stream = new ByteArrayInputStream(dataPacket.getData());
				BufferedImage image = ImageIO.read(stream);
				i++;
				
				writer.encodeVideo(0, image, System.nanoTime() - startTime,
		                   TimeUnit.NANOSECONDS);

			    
				System.out.println("Packet length"+ dataPacket.getLength());
				System.out.println("Sender:"+ dataPacket.getAddress().getHostAddress());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		writer.close();
		
	}
	
	public static void main(String[] args){
		StreamServer ss = new StreamServer();
		ss.receiveData();
	}

}
