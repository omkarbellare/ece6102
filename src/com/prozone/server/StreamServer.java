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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;



public class StreamServer {

	private DatagramSocket socket;
	private static final int STREAMIN_PORT = 9998;
	private static final int STREAMOUT_PORT = 5555;
	
	private byte[] buf = new byte[2780800];
	
	private String repo = "/home/neer/stream_files/";
	private int writeIndex  = 0;
	private int readIndex = 0;
	
	
	private String[] mediaOptions = {null,
			  ":no-sout-rtp-sap", 
			  ":no-sout-standard-sap", 
			  ":sout-all", 
			  ":sout-keep"};
	
	private String url;
	
	private InetAddress inetAddress;
	private StreamWriter streamWriter;
	private StreamReader streamReader;
	
	private boolean isData = false;
	
	private void constructURL(){
		//{sdp=rtsp://@192.168.1.12:5555/demo}
		url = ":sout=#rtp{sdp=rtsp://@" + this.inetAddress.getHostAddress() + ":" + StreamServer.STREAMOUT_PORT+ "/demo,select=noaudio}";
		System.out.println(url);
	}
	
	/*
	 * Writing RTSP Streams for VLC Audience
	 */
	private class StreamWriter implements Runnable{

		private MediaPlayerFactory factory;
		private HeadlessMediaPlayer player;
		private String[] mediaOptions;
		
		public StreamWriter(String[] mediaOptions){
			factory= new MediaPlayerFactory();
			player = factory.newHeadlessMediaPlayer();
			this.mediaOptions = mediaOptions;
		}
		
		@Override
		public void run() {
			int read = 0;
			int i;
			while(true){
				System.out.println(StreamServer.class.toString());
				synchronized (StreamServer.class) {
					read= readIndex;
					i = writeIndex;
					if(read - i < 2){
						try {
							System.out.println("Writer waiting for more media");
							StreamServer.class.wait();
						} catch (InterruptedException e) {
							continue;
						}
					}
					
				}
				for(; i < read;i++)
					player.playMedia(repo + "movie"+ i+ ".mp4",this.mediaOptions);
				
				synchronized (StreamServer.class) {
					writeIndex = i;
				}
			}
			
		}
		
	}
	
	/*
	 * Thread for streaming in live JPEG feed and writing it to mp4 files
	 */
	private class StreamReader implements Runnable{

		private IMediaWriter writer;
		private Dimension screenBounds;
		private DatagramPacket dataPacket;

		public StreamReader(){
			screenBounds = Toolkit.getDefaultToolkit().getScreenSize();
			dataPacket = new DatagramPacket(buf, buf.length);
		}
		@Override
		public void run() {
			short[] audio = {1};
			while(true){
				long startTime = System.nanoTime();
				writer = ToolFactory.makeWriter(repo + "movie"+ readIndex + ".mp4");
				writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4,
		                   screenBounds.width/2, screenBounds.height/2);
			
				int i=0;
				while(i < 100){
					
					try {
						System.out.println("StreamReader waiting for packet data");
						socket.receive(dataPacket);
						ByteArrayInputStream stream = new ByteArrayInputStream(dataPacket.getData());
						BufferedImage image = ImageIO.read(stream);
						i++;
						
						writer.encodeVideo(0, image, System.nanoTime() - startTime,
				                   TimeUnit.NANOSECONDS);
					    writer.encodeAudio(0, audio);
					    
						System.out.println("Packet length"+ dataPacket.getLength());
						System.out.println("Sender:"+ dataPacket.getAddress().getHostAddress());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				writer.close();
				System.out.println();
				synchronized(StreamServer.class){
					readIndex ++;
					if(readIndex - writeIndex > 1)
						StreamServer.class.notify();
				}
			
			}
		}
		
	}
	
	public StreamServer(InetAddress inetAddress)
	{
		try {
//			this.inet4Address = (Inet4Address)inetAddress.;
			this.inetAddress = inetAddress;
			socket = new DatagramSocket(STREAMIN_PORT, this.inetAddress);
			socket.setSoTimeout(0); // blocking read
			
			this.constructURL();
			mediaOptions[0] = url;
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startServer(){
		streamWriter  = this.new StreamWriter(mediaOptions);
		streamReader = this.new StreamReader();
		new Thread(streamReader).start();
		new Thread(streamWriter).start();
	}
	
	public static void main(String[] args){
		try {
			NetworkInterface network = null;
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			Collection<NetworkInterface> netsCollection = Collections.list(nets);
			for(NetworkInterface net : netsCollection){
				if(net.getDisplayName().equals("wlan0")){
					network = net;
					break;
				}
			}
			InetAddress inetAddress = null;
			Enumeration<InetAddress> inets = network.getInetAddresses();
			while (inets.hasMoreElements()) {
				 inetAddress= (InetAddress) inets.nextElement();
				if(inetAddress instanceof Inet4Address)
					break;
				
			}
			
			System.out.println(inetAddress.getHostAddress());
			StreamServer ss = new StreamServer(inetAddress);
			ss.startServer();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}