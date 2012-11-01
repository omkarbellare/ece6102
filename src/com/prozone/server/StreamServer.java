package com.prozone.server;


import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;

import com.xuggle.ferry.IBuffer;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.IAudioSamples;
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

	//private boolean isData = false;

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
		private EmbeddedMediaPlayer player;
		private String[] mediaOptions;

		public StreamWriter(String[] mediaOptions){
			factory = new MediaPlayerFactory();
			player = factory.newEmbeddedMediaPlayer();
			this.mediaOptions = mediaOptions;
		}

		@Override
		public void run() {
			int read = 0;
			int i;
			while(true){
				
				
				synchronized (StreamServer.class) {
					read = readIndex;
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

				for(; i < readIndex;i++){
					String inFileUrl = repo + "pandit"+ i+ ".mp4";
					System.out.println("Playing movie:"+inFileUrl);
					
					player.playMedia(inFileUrl,url,":no-sout-rtp-sap", 
							  ":no-sout-standard-sap", 
							  ":sout-all", 
							  ":sout-keep");
					
					try {
						Thread.sleep(18000);
						player.stop();
					    //mediaPlayer.release();
					} catch (InterruptedException e) {
						System.out.println(e.getMessage());
					}
					player.stop();
	
					System.out.println("finish playing movie"+i+".mp4");
				}
				

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

		private TargetDataLine line;
		AudioFormat audioFormat;
		
		private MediaPlayerFactory factory;
		private EmbeddedMediaPlayer player;

		public StreamReader(){
			factory = new MediaPlayerFactory();
		    player = factory.newEmbeddedMediaPlayer();			
		}

		@Override
		public void run() {
			
			while(true){
				String outFileUrl = repo + "pandit"+ readIndex + ".mp4";
				System.out.println("Writing new file");
				player.prepareMedia(
						  "http://192.168.1.109:8080/videofeed",
						  ":sout=#transcode{vcodec=h264,venc=x264{cfr=16},scale=1,acodec=mp4a,ab=160,channels=2,samplerate=44100}:file{dst="+outFileUrl+"}",
						  ":no-sout-rtp-sap", 
						  ":no-sout-standard-sap", 
						  ":sout-all", 
						  ":sout-keep"
						);
				
				player.start();
			    try {
					Thread.sleep(20000);
					player.stop();
				    //mediaPlayer.release();
				} catch (InterruptedException e) {
					System.out.println(e.getMessage());
				}
			    
				synchronized(StreamServer.class){
					readIndex ++;
					if(readIndex - writeIndex >= 2)
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
