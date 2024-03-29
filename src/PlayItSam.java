import java.util.Iterator;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import processing.core.*;
import ddf.minim.analysis.*;
import ddf.minim.*;

/**
 * @created 2011 December 15th
 * @author Samuel Walz <samuel.walz@gmail.com>
 * 
 */
public class PlayItSam extends PApplet {
	 
	
	public class Key {
		double threshold = 0;
		double averageStrength = 0;
		public float lastStrength = 0f;
		public float strength = 0f;
		public int midiKey = -1;
		public boolean pressed = false;
	}
	private Key[] keys;
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// GUI variables
	int lineHeight;
	int freqHistoryHeight; // height of the area to display freq history
	int freqDisplayHeight; 
	int freqDisplayWidth;
	int scaleHeight; // height of the scale Inscription
	float freqGraphFactor = 3f; // how much to amplify the freq graph display
	
	
	
	
	
	
	
	
	Minim minim;
	AudioPlayer myinput;
	FFT fft;

	
	
	
	
	
	
	// moving strength averages
	int numberOfPeaksForMovingAverage = 42;
	float movingStrengthAverage = 10f;
	float decreaseStrengthAverageFactor = 0.5f; // slow down the decreasing in case no peak is detected
	float movingAverageCompareThreshold = 0.2f;
	int mainPeakThreshold;
	int lonelyPeakThreshold;
	float whistlingPeakNeighbourDistance = 1; //logarithmic distance
	//RingBuffer<float[]> ourFrequencyBuffer;
	
	 
	private int numStrongestPitches = 3; // how many keys shall be detected max at the same time frame?
	private Key[] strongestPitches = new Key[numStrongestPitches]; 
	private float[] freqArray;
    private int octaveSteps = 12;
    private int midiOffset = 36; // we skip the first 3 octaves - so it starts at 36
	private float[] frequenciesOfInterest =
			{/*16.35f, //C0
			 17.32f, //C#0/Db0
			 18.35f, //D0 	
			 19.45f, //D#0/Eb0
			 20.60f, //E0 	
			 21.83f, //F0 
			 23.12f, //F#0/Gb0 	
			 24.50f, //G0 	
			 25.96f, //G#0/Ab0
			 27.50f, //A0 	
			 29.14f, //A#0/Bb0 	
			 30.87f, //B0 	
			 32.70f, //C1 	
			 34.65f, //C#1/Db1 	
			 36.71f, //D1
             38.89f, //D#1/Eb1 
			 41.20f, //E1  
			 43.65f, //F1 	
			 46.25f, //F#1/Gb1 	
			 49.00f, //G1 	
			 51.91f, //G#1/Ab1 	
			 55.00f, //A1 	
			 58.27f, //A#1/Bb1 
			 61.74f, //B1 	
			 65.41f, //C2 - begin of human voice range	0
			 69.30f, //C#2/Db2 	
			 73.42f, //D2 	
			 77.78f, //D#2/Eb2 	
			 82.41f, //E2 	     - start of human voice range
			 87.31f, //F2 	
			 92.50f, //F#2/Gb2 	
			 98.00f, //G2 	
			 103.83f, //G#2/Ab2 	
			 110.00f, //A2 	
			 116.54f, //A#2/Bb2 	10
			 123.47f, //B2 */	
			 130.81f, //C3 	      + our start voice range
			 138.59f, //C#3/Db3
			 146.83f, //D3 	
			 155.56f, //D#3/Eb3 	
			 164.81f, //E3 	
			 174.61f, //F3 	
			 185.00f, //F#3/Gb3 	
			 196.00f, //G3 	
			 207.65f, //G#3/Ab3 	20
			 220.00f, //A3 	
			 233.08f, //A#3/Bb3 	
			 246.94f, //B3 	
			 261.63f, //C4 	        - start of human common range
			 277.18f, //C#4/Db4 
			 293.66f, //D4 
			 311.13f, //D#4/Eb4 
			 329.63f, //E4 	        - end of human common range
			 349.23f, //F4 
			 369.99f, //F#4/Gb4 	30
			 392.00f, //G4 
			 415.30f, //G#4/Ab4 
			 440.00f, //A4 
			 466.16f, //A#4/Bb4 
			 493.88f, //B4 	
			 523.25f, //C5 
			 554.37f, //C#5/Db5 
			 587.33f, //D5 
			 622.25f, //D#5/Eb5
			 659.26f, //E5         40     - our end for voice range
			 698.46f, //F5 
			 739.99f, //F#5/Gb5 
			 783.99f, //G5 
			 830.61f, //G#5/Ab5 
			 880.00f, //A5        - end of human voice range  
			 932.33f, //A#5/Bb5 
			 987.77f, //B5 
			 1046.50f, //C6 
			 1108.73f, //C#6/Db6 
			 1174.66f, //D6         50
			 1244.51f, //D#6/Eb6 
			 1318.51f, //E6   - begin of whistling range   52
			 1396.91f, //F6 
			 1479.98f, //F#6/Gb6 
			 1567.98f, //G6 
			 1661.22f, //G#6/Ab6 
			 1760.00f, //A6 
			 1864.66f, //A#6/Bb6 
			 1975.53f, //B6 
			 2093.00f, //C7          60
			 2217.46f, //C#7/Db7 
			 2349.32f, //D7 
			 2489.02f, //D#7/Eb7 
			 2637.02f, //E7 
			 2793.83f, //F7 
			 2959.96f, //F#7/Gb7 
			 3135.96f, //G7 
			 3322.44f, //G#7/Ab7 
			 3520.00f, //A7 
			 3729.31f, //A#7/Bb7     70
			 3951.07f/*, //B7 - end of whistling range      71
			 4186.01f, //C8 
			 4434.92f, //C#8/Db8
			 4698.64f, //D8 
			 4978.03f  //D#8/Eb8*/
			};
	
	// keyboard control
	private boolean drawAnalysis = true;
	private boolean showAllFrequencies = true;
	private boolean markPeakTypes = false;
	private boolean drawLines = true;
	private boolean hideWeakFrequencies = false;
	private boolean markStrongestPeaks = true;
	 
	// graphics
	PGraphics historyBuffer;
	
	// midi stuff
	Receiver receiver;
	
	@Override
	public void setup()
	{
	  frameRate(24);	
	
	  size(900, 700, JAVA2D);
	  
	  movingAverageCompareThreshold = 0.8f;
	  mainPeakThreshold = 3;
	  lonelyPeakThreshold = 20;
	  lineHeight = 5;
	  scaleHeight = 20;
	  freqDisplayHeight = 200;
	  freqDisplayWidth = 900;
	  freqHistoryHeight = height - scaleHeight - freqDisplayHeight;
	  
	  
	  historyBuffer = this.createGraphics(width, freqHistoryHeight, JAVA2D); 
	 
	  minim = new Minim(this);
	  //myinput = minim.getLineIn(Minim.MONO, 2048, 44100f);
	  myinput = minim.loadFile("elise.mp3", 2048);
	  myinput.loop();
	  myinput.mute();
	  myinput.setVolume(2.0f);
	  
	  
	  // reset framerate
	  float fps = myinput.sampleRate()/myinput.bufferSize();
	  frameRate(fps);
	  
	  lineHeight = (int)(freqHistoryHeight / (fps * 3)); 
	  
	  fft = new FFT(myinput.bufferSize(), myinput.sampleRate());
	  fft.window(FourierTransform.HAMMING);
	  
	  
	  rectMode(CORNERS);
	  
	  
	  
	  
	  
	  freqArray = new float[frequenciesOfInterest.length];
	  keys = new Key[frequenciesOfInterest.length];
	  for (int i = 0; i < keys.length; i++) {
		  keys[i] = new Key();
		  keys[i].midiKey = i + midiOffset;
	  }
	  
	  try {
		  receiver = MidiSystem.getReceiver();
	  } catch (MidiUnavailableException e) {
		  e.printStackTrace();
		  exit();
	  }
	  
	  
		  // keys & how to use them
		  System.out.print("comands\n\np: pause\nf: show all frequncies in the history\nm: mark peak types"
				  +"\nl: draw lines\nh: hide weak frequencies\ns: mark strongest peaks");
	}
	 
	@Override
	public void keyPressed() {
		//System.out.print(key);
		switch (key) {
		case 'p':
			drawAnalysis = !drawAnalysis;
			//System.out.print("P\n");
			break;
		case 'f':
			showAllFrequencies = !showAllFrequencies;
			break;
		case 'm':
			markPeakTypes = !markPeakTypes;
			break;
		case 'l':
			drawLines = !drawLines;
			break;
		case 'h':
			hideWeakFrequencies = !hideWeakFrequencies;
			break;
		case 's':
			markStrongestPeaks = !markStrongestPeaks;
			break;
		}
		
		  
		  
		}
	
	@Override
	public void draw()
	{
		if (drawAnalysis) {
			strokeWeight(1);
			background(0);
		}
	  
		int w = new Integer(freqDisplayWidth/frequenciesOfInterest.length);
		  
		  
		  
		  
		int currentLine = freqHistoryHeight/lineHeight + 1;
		  
		  
		boolean thereHasBeenAPeak = false;
		  
		if (drawAnalysis) {
			historyBuffer.beginDraw();
			historyBuffer.copy(0, lineHeight, historyBuffer.width, historyBuffer.height-lineHeight, 
					0, 0, historyBuffer.width, historyBuffer.height-lineHeight);
		}
	  
	  
	  
	  
	  
	    for (int i = 0; i < keys.length; i++) {
	    	keys[i].lastStrength = keys[i].strength;
	    }
	  
	  
	  
		// analyse next piece of sound
		fft.forward(myinput.mix);
	  
	 
	 
	  
		float strongestPitch = 0f;
		for(int i = 0; i < frequenciesOfInterest.length; i++)
		{
		
			
		    // draw a rectangle for each average, multiply the value by 5 so we can see it better
			float freqStrength = fft.getFreq(frequenciesOfInterest[i]);
			//System.out.println(freqStrength);
			
			if (drawAnalysis) {
				noStroke();
				fill(100);
				rect(i*w, freqDisplayHeight + freqHistoryHeight, i*w + w, freqDisplayHeight + freqHistoryHeight - freqStrength*freqGraphFactor);
			}
		
			freqArray[i] = freqStrength;
		
			
			
			if (drawAnalysis) {
				historyBuffer.noStroke();
				if (keys[i].pressed) {
					historyBuffer.fill(log(freqArray[i]+1)*42);
				} else {
					historyBuffer.fill(0, 0, log(freqArray[i]+1)*42);
				}
				historyBuffer.rect(i*w, freqHistoryHeight, w, -lineHeight + 1);
			}
		
	    
			//scale inscription
			if (i%octaveSteps == 0 && drawAnalysis)
			{
	  			//size(2);
	  			stroke(100);
	  			line(i*w, freqDisplayHeight + freqHistoryHeight, i*w, height);
	  			stroke(100);
	  			line(i*w, freqHistoryHeight, i*w, freqDisplayHeight + freqHistoryHeight);
	  			
	  			text(frequenciesOfInterest[i],i*w+1, height-2);
			}
	    
	  }
		
		// analyse frequency array
		for (int i = 1; i < freqArray.length-1; i++) { // missing out the 1st and last freqArraypos on purpose
			float freqStrength = freqArray[i];
			// MIDI
			
			float whatIHear = freqStrength - this.movingStrengthAverage * (1.0f - this.movingAverageCompareThreshold);
			if (whatIHear < 0) whatIHear = 0f;
			
			// what do we actually hear?
			if (whatIHear > 0) {
				
				keys[i].strength = freqStrength;
				
				if (keys[i].strength > keys[i].lastStrength*2
						&& keys[i].strength > freqArray[i+1]
								&& keys[i].strength >= freqArray[i-1]) {
					insertPitch(keys[i]);
				}
			} else if (whatIHear == 0 && keys[i].lastStrength > 0
					&& keys[i].pressed) {
				// midi off
				ShortMessage msg = new ShortMessage();
				try {
					msg.setMessage(ShortMessage.NOTE_OFF, 0, i+midiOffset, 0);
				} catch (InvalidMidiDataException e) {
					e.printStackTrace();
				}
				receiver.send(msg, -1);
				keys[i].strength = whatIHear;
				keys[i].pressed = false;
				
				if (drawAnalysis) {
					historyBuffer.noFill();
					historyBuffer.stroke(200, 0, 0);
					historyBuffer.rect(i*w, freqHistoryHeight, w, -lineHeight + 1);
				}
			}
			
			
			if (whatIHear > strongestPitch) strongestPitch = freqStrength;
		}
		
		// create midi Note On signals
		for (int i = 0; i < strongestPitches.length; i++) {
			Key k = strongestPitches[i]; strongestPitches[i] = null;
			if (k != null) {
				// midi on
				ShortMessage msg = new ShortMessage();
				int velocity = (int)k.strength;
				if (velocity > 127) velocity = 127;
				try {
					msg.setMessage(ShortMessage.NOTE_ON, 0, k.midiKey, velocity);
				} catch (InvalidMidiDataException e) {
					e.printStackTrace();
				}
				receiver.send(msg, -1);
				k.pressed = true;
				if (drawAnalysis) {
					historyBuffer.noFill();
					historyBuffer.stroke(0, 200, 0);
					historyBuffer.rect((k.midiKey-midiOffset)*w, freqHistoryHeight, w, -lineHeight + 1);
				}
			}
		}
	  
	  
	  historyBuffer.endDraw();
	  if (drawAnalysis) image(historyBuffer, 0, 0);
	  
	  
	  // calc average
	  this.movingStrengthAverage = 
			  (movingStrengthAverage * (this.numberOfPeaksForMovingAverage  - (1 - (strongestPitch==0?this.decreaseStrengthAverageFactor:0)))
			  + strongestPitch)/this.numberOfPeaksForMovingAverage;
	  
	  	
	  // draw average
	  if (drawAnalysis) {
		  stroke(150, 0, 0);
		  int y = (int)(height - this.scaleHeight - this.movingStrengthAverage*this.freqGraphFactor);
		  line(0, y, width, y);
		  stroke(0, 250, 0);
		  y = (int)(height - this.scaleHeight - this.movingStrengthAverage*this.freqGraphFactor*(1.0f-this.movingAverageCompareThreshold));
		  line(0, y, width, y);
	  }
	    
	}
	
	private void insertPitch(Key k) {
		Key currentKey = k;
		for (int i = 0; i < this.strongestPitches.length; i++) {
			if (strongestPitches[i] == null || strongestPitches[i].strength < currentKey.strength) {
				Key tempStorage = strongestPitches[i];
				strongestPitches[i] = currentKey;
				currentKey = tempStorage;
			}
		}
	}
	
	
	
	@Override
	public void stop()
	{
		receiver.close();
	  // always close Minim audio classes when you are done with them
	  myinput.close();
	  // always stop Minim before exiting
	  minim.stop();
	  
	  super.stop();
	}
	
	
}
