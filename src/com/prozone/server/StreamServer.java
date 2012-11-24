package com.prozone.server;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;

import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.FileUtils;

import com.prozone.driver.ProxyDriver;
import com.prozone.http.NanoHTTPD;
import com.prozone.http.NanoHTTPD.Response;
import com.prozone.proxy.StreamingProxyServer;

import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.player.DeinterlaceMode;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class StreamServer {

	private DatagramSocket socket;
	private static final int STREAMIN_PORT = 9998;
	private static final int STREAMOUT_PORT = 5555;

	private String repo = System.getProperty("user.home")+"/stream_files/";
	private int writeIndex  = 0;
	private int readIndex = 0;
	
	private static List streamingDevicesList;
	private static List streamingServersList;
	private static List<String> proxyList;
	private static volatile String primaryDevice;
	private static volatile String proxy_ip;
	private static volatile String  proxy_port;
	private static volatile long lastHeartbeatTime = 0;

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
	
	private void switchToNewProxy() {
		
		// First we need to find a working alternate proxy server
		for (String proxy : proxyList) {
			if (!proxy.equals(proxy_ip)) {
				// Register the servers and devices currently under this proxy to the new
				// proxy
				boolean allComponentsRegistered = true;
				HttpURLConnection conn;
				
				for (Object server : streamingServersList) {
					System.out.println("Sending "+server.toString());
					try {
						URL url = new URL("http://" + proxy + ":"
								+ proxy_port
								+ "/registerDevice?ip=" + server.toString());
						try {
							conn = (HttpURLConnection) url.openConnection();
							conn.setRequestMethod("GET");
							BufferedReader in = new BufferedReader(
									new InputStreamReader(conn.getInputStream()));
							String response = in.readLine();
							if (!response.equals("Ok")) {
								allComponentsRegistered = false;
								break;
							}
						} catch (IOException e) {
							System.out
									.println("Could not connect with proxy : "
											+ proxy);
							allComponentsRegistered = false;
							break;
						}
					} catch (MalformedURLException e) {
						System.out.println("Malformed URL");
						e.printStackTrace();
					}
				}
				if(allComponentsRegistered)
				for (Object device : streamingDevicesList) {
					System.out.println("Sending "+device.toString());
					try {
						URL url = new URL("http://" + proxy + ":"
								+ proxy_port
								+ "/registerDevice?ip=" + device.toString());
						try {
							conn = (HttpURLConnection) url.openConnection();
							conn.setRequestMethod("GET");
							BufferedReader in = new BufferedReader(
									new InputStreamReader(conn.getInputStream()));
							String response = in.readLine();
							if (!response.equals("Ok")) {
								allComponentsRegistered = false;
								break;
							}
						} catch (IOException e) {
							System.out
									.println("Could not connect with proxy : "
											+ proxy);
							allComponentsRegistered = false;
							break;
						}
					} catch (MalformedURLException e) {
						System.out.println("Malformed URL");
						e.printStackTrace();
					}
				}
				if (allComponentsRegistered) {
					break;
				}
			}
		}
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
		private boolean primaryDeviceDown = false;

		public StreamReader(){
			factory = new MediaPlayerFactory();
		    player = factory.newEmbeddedMediaPlayer();
		    player.addMediaPlayerEventListener(new MediaPlayerEventListener() {
				
				@Override
				public void videoOutput(MediaPlayer arg0, int arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void titleChanged(MediaPlayer arg0, int arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void timeChanged(MediaPlayer arg0, long arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void subItemPlayed(MediaPlayer arg0, int arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void subItemFinished(MediaPlayer arg0, int arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void stopped(MediaPlayer arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void snapshotTaken(MediaPlayer arg0, String arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void seekableChanged(MediaPlayer arg0, int arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void positionChanged(MediaPlayer arg0, float arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void playing(MediaPlayer arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void paused(MediaPlayer arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void pausableChanged(MediaPlayer arg0, int arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void opening(MediaPlayer arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void newMedia(MediaPlayer arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void mediaSubItemAdded(MediaPlayer arg0, libvlc_media_t arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void mediaStateChanged(MediaPlayer arg0, int arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void mediaParsedChanged(MediaPlayer arg0, int arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void mediaMetaChanged(MediaPlayer arg0, int arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void mediaFreed(MediaPlayer arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void mediaDurationChanged(MediaPlayer arg0, long arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void mediaChanged(MediaPlayer arg0, libvlc_media_t arg1, String arg2) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void lengthChanged(MediaPlayer arg0, long arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void forward(MediaPlayer arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void finished(MediaPlayer arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void error(MediaPlayer arg0) {
					arg0.stop();
					System.out.println("error");
					primaryDeviceDown = true;
					
					URL connectURL;
					try {
						
						if(!proxy_ip.equals("")){
						connectURL = new URL("http://"+proxy_ip+":8082/deregisterDevice?ip=" + primaryDevice);
						URLConnection conn = connectURL.openConnection();
						// Get the response
						BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
						StringBuffer sb = new StringBuffer();
						String line;
						while ((line = rd.readLine()) != null)
						{
							sb.append(line);
						}
						rd.close();
						String result = sb.toString();
						}
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					primaryDevice = "";
					
					try {
						synchronized(StreamServer.class){
							FileUtils.copyFile(new File(repo + "movie"+ (readIndex-2) + ".mp4"), new File(repo + "movie"+ (readIndex-1) + ".mp4"));
							//FileUtils.copyFile(new File(repo + "movie"+ (readIndex-1) + ".mp4"), new File(repo + "movie"+ readIndex + ".mp4"));
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
				@Override
				public void endOfSubItems(MediaPlayer arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void buffering(MediaPlayer arg0, float arg1) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void backward(MediaPlayer arg0) {
					// TODO Auto-generated method stub
					
				}
			});
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
	
	public class HeartbeatMonitor extends TimerTask {

		int durationInSeconds;
		
		public HeartbeatMonitor(int durationInSeconds) {
			this.durationInSeconds=durationInSeconds;
			
		}
		public void run() {
			//Only the primary server needs to do this
			if(lastHeartbeatTime!=0) {
				System.out.println("Checking for heartbeat...");
				if(Calendar.getInstance().getTimeInMillis()-lastHeartbeatTime>durationInSeconds*1000) {
					switchToNewProxy();
					//If this transfer was successful, then we mark this server as non-primary
					//by setting lastHeartbeatTime=0.
					lastHeartbeatTime=0;
				}
			}
		}
	}
	
	public class StreamingServer extends NanoHTTPD implements Runnable {
		
		
		public StreamingServer(int port) throws IOException {
			super(port, new File("."));
			Properties prop=new Properties();
			prop.load(ProxyDriver.class.getResourceAsStream("/config/config.properties"));
			int heartbeat_interval=Integer.parseInt(prop.getProperty("heartbeat_interval"));
			Timer t=new Timer();
			HeartbeatMonitor hbm=new HeartbeatMonitor(10);
			t.scheduleAtFixedRate(hbm, 0, heartbeat_interval*1000);
		}

		public Response serve( String uri, String method, Properties header, Properties parms, Properties files )
		{
			if(uri.equals("/heartbeat")) {
				lastHeartbeatTime=Calendar.getInstance().getTimeInMillis();
				if(primaryDevice.equals("") && streamingDevicesList!=null && streamingDevicesList.size()>0) {
					primaryDevice=(String) streamingDevicesList.get(0);
				}
				return new NanoHTTPD.Response( HTTP_OK, MIME_PLAINTEXT, "Ok" );
			}
			else if(uri.equals("/deviceList")) {
				
				streamingDevicesList.clear();
				for(Object key:parms.keySet()) {
					streamingDevicesList.add(parms.get(key));
				}
				//If the primary device was deregistered, elect a new primary
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
			else if(uri.equals("/serverList")) {
				
				System.out.println("Received servers list");
				streamingServersList.clear();
				for(Object key:parms.keySet()) {
					streamingServersList.add(parms.get(key));
					System.out.println(parms.get(key).toString());
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
			streamingServersList=new ArrayList<String>();
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
		proxy_port=prop.getProperty("proxy_port");
		String listProxies[]=prop.getProperty("proxy_ips").split(",");
		proxyList=Arrays.asList(listProxies);
		//TODO: Make this Random
		proxy_ip=proxyList.get(0);
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
