package org.jpc.emulator.peripheral;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/*
 * Handle the Music output, i.e. Midi Sound
 */
public class AudioLayer
{
    static private byte[] audioBuffer;
    static public SourceDataLine line;
    static private boolean audioThreadExit = false;


    static private Thread audioThread;

    public static boolean open(int bufferSize, int freq) {
        AudioFormat format = new AudioFormat(freq, 16, 2, true, false);
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, bufferSize);
            line.start();
            audioThreadExit = false;
            audioThread = new Thread() {
                public void run() {
                    while (!audioThreadExit) {
                        boolean result;
                        synchronized (Mixer.audioMutex) {
                            result = Mixer.MIXER_CallBack(audioBuffer, audioBuffer.length);
                        }
                        if (result)
                            line.write(audioBuffer, 0, audioBuffer.length);
                        else {
                            try {Thread.sleep(20);} catch (Exception e){}
                        }
                    }
                }
            };
            audioBuffer = new byte[512]; // this needs to be smaller than buffer size passed into open other line.write will block
            audioThread.start();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void stop() {
        audioThreadExit = true;
        try {audioThread.join(2000);} catch (Exception e){}
        line.drain();
        line.stop();
    }
}
