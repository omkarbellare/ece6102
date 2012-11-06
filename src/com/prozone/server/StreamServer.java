package com.prozone.server;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import com.prozone.driver.ProxyDriver;
import com.prozone.http.NanoHTTPD;
import com.prozone.http.NanoHTTPD.Response;

import uk.co.caprica.vlcj.player.DeinterlaceMode;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class StreamServer {

	private DatagramSocket socket;
	private static final int STREAMIN_PORT = 9998;
	private static final int STREAMOUT_PORT = 5555;

	private String repo = System.getProperty("user.home")+"/stream_files";
	private int writeIndex  = 0;
	private int readIndex = 0;
	
	private static List streamingDevicesList;
	private static volatile String primaryDevice;


	private String[] mediaOptions = {null,
			":no-sout-rtp-sap", 
			":no-sout-standard-sap", 
			":sout-all", 
	":sout-keep"};

	private String url;

	private InetAddress inetAddress;
	private StreamWriter streamWriter;
	private StreamReader streamReader;
	private StreamingServer streamingServer;

	private void constructURL(){
//		url = ":sout=#rtp{sdp=rtsp://@192.168.1.113:5555/demo}";
//		url = ":sout=#rtp{sdp=rtsp://@" + this.inetAddress.getHostAddress() + ":" + StreamServer.STREAMOUT_PORT+ "/demo}";
		url = ":sout=#duplicate{dst=std{access=http,mux=ts,dst=192.168.1.47:5555}}";
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
					String inFileUrl = repo + "movie"+ i+ ".mp4";
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

		private MediaPlayerFactory factory;
		private EmbeddedMediaPlayer player;

		public StreamReader(){
			factory = new MediaPlayerFactory();
		    player = factory.newEmbeddedMediaPlayer();			
		}

		@Override
		public void run() {
			
			while(true){
				
				if(!primaryDevice.equals("")) {
					
					String outFileUrl = repo + "movie"+ readIndex + ".mp4";
					System.out.println("Writing new file: " + outFileUrl);
					player.prepareMedia(
							"http://"+primaryDevice+":8080/videofeed",
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
						//	mediaPlayer.release();
					} catch (InterruptedException e) {
						System.out.println(e.getMessage());
					}
					synchronized(StreamServer.class){
						readIndex ++;
						/*if(readIndex - writeIndex >= 2)
							StreamServer.class.notify();*/
					}
				}
			}
		}
	}
	
	public class StreamingServer extends NanoHTTPD implements Runnable {
		
		public StreamingServer(int port) throws IOException {
			super(port, new File("."));
		}

		public Response serve( String uri, String method, Properties header, Properties parms, Properties files )
		{
			if(uri.equals("/heartbeat")) {
				if(primaryDevice.equals("") && streamingDevicesList!=null && streamingDevicesList.size()>0) {
					primaryDevice=(String) streamingDevicesList.get(0);
//					System.out.println("New primary device is:"+primaryDevice);
				}
				return new NanoHTTPD.Response( HTTP_OK, MIME_PLAINTEXT, "Ok" );
			}
			else if(uri.equals("/deviceList")) {
				
				streamingDevicesList.clear();
				for(Object key:parms.keySet()) {
					streamingDevicesList.add(parms.get(key));
				}
				//If the primary device was deregistered, elect a new primary
//				System.out.println("Primary device:"+primaryDevice);
//				System.out.println("Devices list size:"+streamingDevicesList.size());
				if(!streamingDevicesList.contains(primaryDevice) || primaryDevice.equals("")) {
					if(streamingDevicesList.size()>0) {
						primaryDevice=(String) streamingDevicesList.get(0);
						System.out.println("New primary device is:"+primaryDevice);
					}
					else
						primaryDevice="";
				}
				return new NanoHTTPD.Response( HTTP_OK, MIME_PLAINTEXT, "Ok" );
			}
			else {
				System.out.println("URI="+uri);
				return new NanoHTTPD.Response( HTTP_BADREQUEST, MIME_PLAINTEXT, "Bad Request" );
			}
		}

		@Override
		public void run() {
			//This is to make this thread run forever
			try {
				System.in.read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}

	public StreamServer(InetAddress inetAddress) throws IOException
	{
		try {
			streamingDevicesList=new ArrayList<String>();
			primaryDevice="";
			this.inetAddress = inetAddress;
			socket = new DatagramSocket(STREAMIN_PORT, this.inetAddress);
			socket.setSoTimeout(0); // blocking read
			this.constructURL();
			mediaOptions[0] = url;
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void startServer() throws IOException{
		streamWriter  = this.new StreamWriter(mediaOptions);
		streamReader = this.new StreamReader();
		Properties prop=new Properties();
		prop.load(ProxyDriver.class.getResourceAsStream("/config/config.properties"));
		int server_port=Integer.parseInt(prop.getProperty("server_port"));
		String proxy_port=prop.getProperty("proxy_port");
		String proxy_ip=prop.getProperty("proxy_ip");
		streamingServer = this.new StreamingServer(server_port);
		URL connectURL=new URL("http://"+proxy_ip+":"+proxy_port+"/registerServer?ip="+this.inetAddress.getHostAddress());
		URLConnection conn=connectURL.openConnection();
		
		BufferedReader rd=new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuffer sb=new StringBuffer();
		String line;
		while((line=rd.readLine())!=null) {
			sb.append(line);
		}
		rd.close();
				
		new Thread(streamReader).start();
		//new Thread(streamWriter).start();
		new Thread(streamingServer).start();
	}
}
