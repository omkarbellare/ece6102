package com.prozone.vlc;

/**
 * This class is meant to be used as an "Other" class in Greenfoot.  It will
 * allow you to create a WavPlayer object so that you can start and stop
 * background music.  At this moment pausing is not possible.
 *
 * @author Jeremiah Davis
 * @version .4
 *
 */

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;

public class WavPlayer
{
    // Variable for our audio stream
    private AudioInputStream stream;
    
    // Variable for our mixer line 
    private SourceDataLine line;

    // Number of times to loop the sound.  Give a negative number
    // for continuous looping.  Default is 1
    private int loops;

    // Current loop count, this will reset to 0 when the play method
    // is invoked.
    private int loopCount;

    // Is playing, will be true if a sound is playing.
    private boolean isPlaying;
    
    // The file name
    private String fileName;
    
    /** 
     * Variable to store the total number of bytes read in PlayWav.  
     * This is in order to keep this info over
     * multiple invocations of the PlayWav thread. 
     */
    private long totalRead = 0;

    //*************************************************************
    // Constructors                                              
    //*************************************************************

    /**
     * Default Constructor.  Initializes some varibles.  Does not load
     * a sound file.
     */
    public WavPlayer()
    {
        // Initialize loops and loopCount and isPlaying.
        loops = 1;
        loopCount = 0;
        isPlaying = false;
    } // End default constructor.


    //*************************************************************
    // Mutators
    //*************************************************************

    /**
     * setLoops() - Set the number of times to loop a file.
     *
     * @param n - Integer number of time to loop.  If this is not positive,
     *  (n less than or equal to 0) the class will assume continuous looping.
     *  The default is to play one time.
     */
    public void setLoops(int n)
    {
        loops = n;
    } // End setLoops
   
    //************************************************************
    // Accessors
    //************************************************************
    
    /**
     * getLoops() - Returns the number of times to loop the sound.
     *  Remember, a value less than or equal to 0 means continuous looping.
     */
    public int getLoops()
    {
        return loops;
    }

    /**
     * getLoopCount() - Returns the current loop count.  This is reset whenever 
     * the play method is invoked.
     */
    public int getLoopCount()
    {
        return loopCount;
    }
    
    /**
     * getIsPlaying() - Returns true if a file is playing, false otherwise.
     */
    public boolean getIsPlaying()
    {
        return isPlaying;
    }
    
    //************************************************************
    // Player implementation
    //************************************************************

    /**
     * open() - Opens a sound file for playing.  If io exceptions are 
     * thrown, this will print error messages to the console.  
     * Returns true on success, false otherwise.  The audio file needs to
     * be in the audio directory under your greenfoot scenario.
     *
     * @param name - A string, the name of the sound file.
     */
    public boolean open(String name)
    {
        // Store the file name in fileName
        fileName = name;
        
        // First check to see if we already have an open stream.  If so
        // close it.
        close();

        // Variable to keep track of success
        boolean success = false;

        // Need to try and catch for io exceptions.
        try {
            //Get the audio file (resource) as a URL
            URL url = new URL(name);
            
                        
            // Create an audio stream from the file, using the
            // AudioSystem.
            stream = AudioSystem.getAudioInputStream(url);

            // Apparently, ALAW and ULAW encodings need to be converted to
            // PCM_SIGNED before it can be played.  The following should
            // do this.  I got this code from a java sound tutorial.

            // Get the audio format of the stream.
            AudioFormat format = stream.getFormat();
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                format = new AudioFormat(
                     AudioFormat.Encoding.PCM_SIGNED,
                     format.getSampleRate(),
                     format.getSampleSizeInBits()*2,
                     format.getChannels(),
                     format.getFrameSize()*2,
                     format.getFrameRate(),
                     true);        // big endian
                     stream = AudioSystem.getAudioInputStream(format, stream);
            }
            
            // Set a mark at the beginning of the stream so that we can 
            // reset to it.
            // Doesn't work in applet I'll reopen it later instead.
            // stream.mark(Integer.MAX_VALUE);
        
            // Print to the console the file name being played and the
            // file format
            System.out.println(name + " format = " + format);

            // Now get a line to the mixer.  The DataLine.Info class will
            // help the system choose an appropriate line to the mixer.
            DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                           format);
            
            // Now get our SourceDataLine
            line = (SourceDataLine) AudioSystem.getLine(info);

            // Now open the the line
            line.open();

            success = true;
            
            // Make sure totalRead is zeroed when we open a new file
            totalRead = 0;
            // Now use the play method to play the file.
    
        } catch (IOException e) {
            System.err.println("IO error when opening file " + name + ".");
            System.err.println("IO error:  " + e);
        } catch (LineUnavailableException e) {
            System.err.println("Cannot open audio mixer line.");
            System.err.println("LineUnavailableException:  " + e);
        } catch (UnsupportedAudioFileException e) {
            System.err.println("The audio format of " + name + 
                   " is unsupported.");
            System.err.println("UnsupportedAudioFileException:  " + e);
        }

        return success;
    } // End open()

    /**
     * reset() - Reopens the stream without stopping playback.  Or getting
     * a new line.
     */
    private void reset()
    {
        // Need to try and catch for io exceptions.
        try {
            //Get the audio file (resource) as a URL
            URL url = getClass().getClassLoader().getResource("audio/" + fileName);
            
            // Make sure we actually opened a file
            if ( url == null ) 
                throw new IOException();
            
            // Create an audio stream from the file, using the
            // AudioSystem.
            stream = AudioSystem.getAudioInputStream(url);

            // Apparently, ALAW and ULAW encodings need to be converted to
            // PCM_SIGNED before it can be played.  The following should
            // do this.  I got this code from a java sound tutorial.

            // Get the audio format of the stream.
            AudioFormat format = stream.getFormat();
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                format = new AudioFormat(
                     AudioFormat.Encoding.PCM_SIGNED,
                     format.getSampleRate(),
                     format.getSampleSizeInBits()*2,
                     format.getChannels(),
                     format.getFrameSize()*2,
                     format.getFrameRate(),
                     true);        // big endian
                     stream = AudioSystem.getAudioInputStream(format, stream);
            }
            
            // Make sure totalRead is zeroed when we reset
            totalRead = 0;
    
        } catch (IOException e) {
            System.err.println("IO error when opening file " + fileName + ".");
            System.err.println("IO error:  " + e);
        } catch (UnsupportedAudioFileException e) {
            System.err.println("The audio format of " + fileName + 
                   " is unsupported.");
            System.err.println("UnsupportedAudioFileException:  " + e);
        }
    } // End reset();

    /**
     * close() - If a line or a stream is open, close it.  Returns true
     * is successfull or stream is null, false otherwise.
     */
    public boolean close()
    {
        // Stop playback.
        stop();
        
        // Close the line
        if ( line != null && line.isOpen() )
            line.close();
        
        
        // Close the stream, return true if there is no stream to close
        if ( stream == null )
            return true;
        try {
            stream.close();
            return true;
        } catch (IOException e) {
            System.err.println("Could not close stream, IO Exception: " + e);
        }
        return false;
    }  // End close()

    /**
     * play() - Play the sound file.  This will start a new thread so that
     * control will go back to the main program while the sound is playing.
     * Returns true if it successfully starts the thread, false otherwise.
     */
    public boolean play() 
    {
        loopCount = 0;
        
        // First check that we have an open line, if not, just return.
        // Also, just return if it is already playing
        if ( line == null ) {
            System.err.println("Cannot play.  No line.");
            return false;
        }
        
        if ( !line.isOpen() ) {
            System.err.println("Cannot play.  Line is not open.");
            return false;
        }
    
        if ( line.isActive() ) {
            System.err.println("Cannot play.  Already playing.");
            return false;
        }
    
        // Now start the line, it must be started before you can begin
        // playback.
        line.start();
        isPlaying = true;

        // Create a new thread to play the wav
        PlayWav p = new PlayWav(Thread.currentThread());
        p.start();
        
        return true;
    } // End play()
    
    /**
     * class PlayWav - plays a wav in a new thread
     */
    private class PlayWav extends Thread
    {
        // The parent thread.
        private Thread parent;
        
        // Constructor to set the parent thread
        public PlayWav(Thread parent)
        {
            setParentThread(parent);
        }
        
        // Hide the default constructer to force calling the other.
        private PlayWav(){}
        
        // Set the parent thread, so we can test to see if the parent
        // thread of this thread is alive.  If it is not, we will exit
        // this thread after closing the line and the stream.
        public void setParentThread(Thread parent)
        {
            this.parent = parent;
        }
        
        // Run the thread
        public void run()
        {
            
            // We have to write the sound data to the line in a loop.

            try {
                // Control variables
                
                // A byte array to buffer the stream into
                byte [] buffer = new byte[stream.available()];
        
                // The size of the audio stream in bytes.
                long size = stream.getFrameLength() * stream.getFormat().getFrameSize();
        
                // The number of bytes read this iteration and the number to
                // read this iteration.
                int read = 0, toRead = 0;
        
                // Reset and keep playing if we are still looping.
                while ( (loopCount < loops || loops <= 0) && isPlaying ) {
                    // Start writing data to the line, start playing.  Play until the
                    // stream ends or until isPlaying is fasle.
        
                    while ( totalRead < size && isPlaying ) {
        
                        // Only read as much as we can write to the buffer, or 2^14, which ever is smaller.
                        // This will keep the read method from blocking, and by keepint it in about 1 second 
                        // chunks, will help us to pause and resume playback from the same point.
                        toRead = stream.available() < 16384 ? stream.available() : 16384;
        
                        // Now read it from the stream
                        read = stream.read(buffer, 0, toRead);
        
                        // When no bytes are read, stream.read returns -1
                        // Break out of loop when this happens
                        if ( read == -1 ) 
                            break;
                        
                        // Increment our totalRead
                        totalRead += read;
        
                        // Write it to the line
                        line.write(buffer, 0, read);
                        
                        // Make sure that this terminates if the parent thread
                        // dies
                        if ( !parent.isAlive() ) {
                            close();
                            return;
                        }
                    } // End while
                    
                    // Only update the loop count and reset the stream if stop was not invoked.
                    if ( isPlaying ) {
                        loopCount++;
                        // stream.reset();  Doesn't work in applet
                        
                        // Reopen the file for playback.
                        reset(); 
                    }
                } // End while
                
                // Done playing for now
                isPlaying = false;
                
            } catch (IOException e) {
                System.err.println("IOException: " + e);
            } // End try/catch

        } // End run
    } // End class Playwav
    
    /**
     * stop() - sets the "isPlaying" to false, and stops the line and
     * the music.  
     */
    public void stop()
    {
        isPlaying = false;
        
        // MakeSure we really have a line
        if ( line != null ) {
            line.stop();
        }
    } // End stop()
} // End class WavPlayer
