package com.prozone.vlc;

import javax.sound.sampled.*;
import javax.swing.JApplet;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.sun.jna.NativeLibrary;

import java.io.File;
import java.io.IOException;

import java.net.URL;

import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;

public class VideoPlayer extends JApplet{
	private static final long serialVersionUID = 1L;

	private JDialog playingDialog;
	
	private Clip clip;
	
	private final JFrame frame; 

	private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
	
	public static void main(String[] args) {
		if(args.length!=1){
			System.err.println("Specify an MRL to play");
			System.exit(1);
		}
		
		final String mrl = args[0];
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new VideoPlayer().run(mrl);	
			}
		});
	}
	
	public VideoPlayer(){
		frame = new JFrame("Prozone Video Streaming");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocation(100, 100);
		frame.setSize(1200, 800);
		
		NativeLibrary.addSearchPath("libvlc", "C:\\Program Files\\VideoLAN\\VLC");
		
		mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
		frame.setContentPane(mediaPlayerComponent);
		
		frame.setVisible(true);
	}

	private void run(String mrl){		
		AudioInputStream ais;
		
		String[] options = {":sout=#standard{mux=ts,access=file,dst=C:\\capture.avi}"};
			    
		mediaPlayerComponent.getMediaPlayer().playMedia(mrl, options);
		
		try{
			URL AudioURL = new URL("http://192.168.1.102:8080/audio.wav");
			
		    clip = AudioSystem.getClip();
		    
//		    float gainAmount = 20;
//		    
//		    FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER	_GAIN);
//		    volume.setValue(gainAmount);
//			
//			ais = AudioSystem.getAudioInputStream(AudioURL);
//			
//			clip.open(ais);
//			
//			clip.loop(Clip.LOOP_CONTINUOUSLY); 
		}
//		catch(UnsupportedAudioFileException e){
//			System.out.println("Problem playing audio from URL");
//			System.exit(1);
//		}
		catch(IOException e){
			System.out.println("Audio stream not working correctly");
			System.exit(1);
		} catch (LineUnavailableException e) {
			System.out.println("Audio line is unavailable");
			System.exit(1);
		}
	}
}
