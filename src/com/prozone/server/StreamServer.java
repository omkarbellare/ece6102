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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
<<<<<<< HEAD
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
=======
>>>>>>> 84cd36d5203d92e7c12d39b53c81026d39e398b5
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

import sun.net.dns.ResolverConfiguration.Options;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;

import com.xuggle.ferry.IBuffer;
<<<<<<< HEAD
import com.xuggle.mediatool.IMediaCoder;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.Converter;
=======
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
>>>>>>> 84cd36d5203d92e7c12d39b53c81026d39e398b5
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IStreamCoder.Direction;
import com.xuggle.xuggler.IVideoPicture.PictType;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.video.ArgbConverter;
import com.xuggle.xuggler.video.BgrConverter;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import com.xuggle.xuggler.IVideoPicture;

public class StreamServer {

	private DatagramSocket socket;
	private static final int STREAMIN_PORT = 9998;
	private static final int STREAMOUT_PORT = 5555;
<<<<<<< HEAD
	
	private byte[] buf = new byte[500000];
	
=======

	private byte[] buf = new byte[2780800];

>>>>>>> 84cd36d5203d92e7c12d39b53c81026d39e398b5
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
<<<<<<< HEAD
	
	private boolean isData = false;
	private ServerSocket tcpSocket;
	private Socket client;
	
=======

	//private boolean isData = false;

>>>>>>> 84cd36d5203d92e7c12d39b53c81026d39e398b5
	private void constructURL(){
		//{sdp=rtsp://@192.168.1.12:5555/demo}
//		url = ":sout=#duplicate{dst=std{access=http,mux=ts,dst=" + this.inetAddress.getHostAddress() + ":" + StreamServer.STREAMOUT_PORT+ "/}}";
		url = ":sout=#rtp{sdp=rtsp://@" + this.inetAddress.getHostAddress() + ":" + StreamServer.STREAMOUT_PORT+ "/demo}";
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
<<<<<<< HEAD
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
				
=======
				for(; i < read;i++)
					player.playMedia(repo + "movie"+ i+ ".mp4",this.mediaOptions);

>>>>>>> 84cd36d5203d92e7c12d39b53c81026d39e398b5
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
<<<<<<< HEAD
		private IMediaReader audioReader;
		
		private MediaPlayerFactory factory;
		private EmbeddedMediaPlayer player;

		
=======
		private TargetDataLine line;
		AudioFormat audioFormat;
>>>>>>> 84cd36d5203d92e7c12d39b53c81026d39e398b5

		public StreamReader(){
			screenBounds = Toolkit.getDefaultToolkit().getScreenSize();
			dataPacket = new DatagramPacket(buf, buf.length);
<<<<<<< HEAD
		    factory = new MediaPlayerFactory();
		    
		    player = factory.newEmbeddedMediaPlayer();
			
=======

			//Adding lines for getting Audio line
			audioFormat = getAudioFormat(); 
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, 
					audioFormat); 
			try { 
				this.line = (TargetDataLine) AudioSystem.getLine(info); 
				this.line.open(audioFormat); 
			} 
			catch (LineUnavailableException e) 
			{ 
				System.out.println("unable to get a recording line"); 
				e.printStackTrace(); 
				System.exit(1); 
			} 
		}

		//Audio format gets returned. Need to modify this if audio stream not working correctly
		private AudioFormat getAudioFormat(){ 
			float sampleRate = 8000.0F; //8000,11025,16000,22050,44100 
			int sampleSizeInBits = 16; 	//8,16 
			int channels = 1; 			//1,2 
			boolean signed = true;		//true,false 
			boolean bigEndian = false;	//true,false
			
			return new AudioFormat(sampleRate, 
					sampleSizeInBits, 
					channels, 
					signed, 
					bigEndian); 
>>>>>>> 84cd36d5203d92e7c12d39b53c81026d39e398b5
		}

		@Override
		public void run() {
<<<<<<< HEAD
			
		
			
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
=======
			//short[] audio = {1};
			while(true){
				long startTime = System.nanoTime();
				writer = ToolFactory.makeWriter(repo + "movie"+ readIndex + ".mp4");
				writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4,
						screenBounds.width/2, screenBounds.height/2);
				writer.addAudioStream(1, 0, 1, (int)audioFormat.getSampleRate());

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
						//writer.encodeAudio(0, audio);
						
						System.out.println("Packet length"+ dataPacket.getLength());
						System.out.println("Sender:"+ dataPacket.getAddress().getHostAddress());
						
						// Add audio stream 
		                writer.addAudioStream(1, 0,1,(int) audioFormat.getSampleRate()); 
		                
		                // audio buffer 
		                int numBytesToRead=192000; 
		                byte[] audioBuf = new byte[numBytesToRead]; 
		                
		                int nBytesRead = this.line.read(audioBuf, 0, this.line.available()); 
		                
		                System.out.println("Read " + nBytesRead+"  bytes of sound"); 
                        if(nBytesRead==0) 
                                continue; 
                        
                        // encode audio to stream #1 
                        IBuffer iBuf = IBuffer.make(null, audioBuf, 0, nBytesRead); 
                        
                        IAudioSamples smp = IAudioSamples.make(iBuf, 1,IAudioSamples.Format.FMT_S16); 
                        
                        if(smp==null) 
                            continue; 
                        
                        long numSample =nBytesRead/smp.getSampleSize();
                        
                        System.out.println("NUM SAMPLE "+numSample+" SMP size "+smp.getSampleSize()); 
                        
                        smp.setComplete(true, numSample,(int) audioFormat.getSampleRate(), audioFormat.getChannels(), IAudioSamples.Format.FMT_S16, (System.nanoTime() - startTime)/1000);
                        
                        writer.encodeAudio(1, smp); 
					} catch (IOException e) {
						e.printStackTrace();
					}
>>>>>>> 84cd36d5203d92e7c12d39b53c81026d39e398b5
				}
				
				synchronized(StreamServer.class){
					readIndex ++;
					if(readIndex - writeIndex >=2)
						StreamServer.class.notify();
				}

			}
		}

	}

	public StreamServer(InetAddress inetAddress)
	{
		try {
<<<<<<< HEAD
			this.inetAddress = inetAddress;
			socket = new DatagramSocket(STREAMIN_PORT, this.inetAddress);
			socket.setSoTimeout(0); // blocking read
			tcpSocket = new ServerSocket(9999);
=======
			//			this.inet4Address = (Inet4Address)inetAddress.;
			this.inetAddress = inetAddress;
			socket = new DatagramSocket(STREAMIN_PORT, this.inetAddress);
			socket.setSoTimeout(0); // blocking read

>>>>>>> 84cd36d5203d92e7c12d39b53c81026d39e398b5
			this.constructURL();
			mediaOptions[0] = url;

		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void startServer(){
		streamWriter  = this.new StreamWriter(mediaOptions);
		streamReader = this.new StreamReader();
		/*try {
			client = tcpSocket.accept();
			if(client!=null)
				System.out.println("connection received");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
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
