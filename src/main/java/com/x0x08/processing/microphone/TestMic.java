package com.x0x08.processing.microphone;

import java.io.FileOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

/**
 *
 * @author alex
 */
public class TestMic {

public static void main(String[] args) throws Exception {
		AudioFormat format = new AudioFormat(44100, 16, 2, true, true);

		DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format);
		DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, format);

                
                // checks if system supports the data line
			if (!AudioSystem.isLineSupported(targetInfo)) {
				System.out.println("microphone Line not supported");
				System.exit(0);
			}
                
		try (  FileOutputStream fout=  new FileOutputStream("./test.wav");
                        ) {
			TargetDataLine targetLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
			targetLine.open(format);
			targetLine.start();
			
			SourceDataLine sourceLine = (SourceDataLine) AudioSystem.getLine(sourceInfo);
			sourceLine.open(format);
			sourceLine.start();

			int numBytesRead;
			byte[] targetData = new byte[targetLine.getBufferSize() / 5];

			while (true) {
				numBytesRead = targetLine.read(targetData, 0, targetData.length);

				if (numBytesRead == -1)	break;

				sourceLine.write(targetData, 0, numBytesRead);
                                
                            //    AudioSystem.write(sound, fileType, soundFile);
                                
                                fout.write(targetData,0,numBytesRead);
                                fout.flush();
			}
		}
		catch (Exception e) {
			System.err.println(e);
		}
	}

    
}
