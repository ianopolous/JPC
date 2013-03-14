package org.jpc.emulator.peripheral;

import org.jpc.j2se.Option;

import javax.sound.midi.*;
import java.io.InputStream;
import java.util.logging.*;

public class Midi
{
    private static final Logger Log = Logger.getLogger(Midi.class.getName());
    static final private int SYSEX_SIZE = 1024;
    static final private int RAWBUF	= 1024;

    static final private byte[] MIDI_evt_len = new byte[] {
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x00
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x10
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x20
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x30
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x40
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x50
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x60
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x70

      3,3,3,3, 3,3,3,3, 3,3,3,3, 3,3,3,3,  // 0x80
      3,3,3,3, 3,3,3,3, 3,3,3,3, 3,3,3,3,  // 0x90
      3,3,3,3, 3,3,3,3, 3,3,3,3, 3,3,3,3,  // 0xa0
      3,3,3,3, 3,3,3,3, 3,3,3,3, 3,3,3,3,  // 0xb0

      2,2,2,2, 2,2,2,2, 2,2,2,2, 2,2,2,2,  // 0xc0
      2,2,2,2, 2,2,2,2, 2,2,2,2, 2,2,2,2,  // 0xd0

      3,3,3,3, 3,3,3,3, 3,3,3,3, 3,3,3,3,  // 0xe0

      0,2,3,2, 0,0,1,0, 1,0,1,1, 1,0,1,0   // 0xf0
    };

    static private class _midi {
        int status;
        int cmd_len;
        int cmd_pos;
        byte[] cmd_buf = new byte[8];
        byte[] rt_buf = new byte[8];
        public static class Sysex {
            byte[] buf = new byte[SYSEX_SIZE];
            int used;
            int delay;
		    long start;
        }
        public Sysex sysex = new Sysex();
        Receiver handler;
        MidiDevice device;
    }
    static private _midi midi;
    static private ShortMessage msg = new ShortMessage();
    static private SysexMessage sysex_msg = new SysexMessage();

    static public void MIDI_RawOutByte(/*Bit8u*/int data)
    {
        if (midi.sysex.start!=0) {
            /*Bit32u*/long passed_ticks = System.currentTimeMillis() - midi.sysex.start;
            if (passed_ticks < midi.sysex.delay) try {Thread.sleep(midi.sysex.delay - passed_ticks);} catch (InterruptedException e){}
        }
        /* Test for a realtime MIDI message */
        if (data>=0xf8) {
            try {msg.setMessage(data);} catch (Exception e) {}
            midi.handler.send(msg, -1);
            return;
        }
        /* Test for a active sysex tranfer */
        if (midi.status==0xf0) {
            if ((data&0x80)==0) {
                if (midi.sysex.used<(SYSEX_SIZE-1)) midi.sysex.buf[midi.sysex.used++]=(byte)data;
                return;
            } else {
                midi.sysex.buf[midi.sysex.used++]=(byte)0xf7;

                if ((midi.sysex.start!=0) && (midi.sysex.used >= 4) && (midi.sysex.used <= 9) && (midi.sysex.buf[1] == 0x41) && (midi.sysex.buf[3] == 0x16)) {
                    Log.log(Level.WARNING,"MIDI:Skipping invalid MT-32 SysEx midi message (too short to contain a checksum)");
                } else {
    //				LOG(LOG_ALL,LOG_NORMAL)("Play sysex; address:%02X %02X %02X, length:%4d, delay:%3d", midi.sysex.buf[5], midi.sysex.buf[6], midi.sysex.buf[7], midi.sysex.used, midi.sysex.delay);
                    try {sysex_msg.setMessage(midi.sysex.buf, midi.sysex.used);} catch (Exception e) {}
                    midi.handler.send(sysex_msg, -1);
                    if (midi.sysex.start!=0) {
                        if (midi.sysex.buf[5] == 0x7F) {
                            midi.sysex.delay = 290; // All Parameters reset
                        } else if (midi.sysex.buf[5] == 0x10 && midi.sysex.buf[6] == 0x00 && midi.sysex.buf[7] == 0x04) {
                            midi.sysex.delay = 145; // Viking Child
                        } else if (midi.sysex.buf[5] == 0x10 && midi.sysex.buf[6] == 0x00 && midi.sysex.buf[7] == 0x01) {
                            midi.sysex.delay = 30; // Dark Sun 1
                        } else midi.sysex.delay = (/*Bitu*/int)(((float)(midi.sysex.used) * 1.25f) * 1000.0f / 3125.0f) + 2;
                        midi.sysex.start = System.currentTimeMillis();
                    }
                }

                Log.log(Level.INFO,"Sysex message size "+midi.sysex.used);
//                if (CaptureState & CAPTURE_MIDI) {
//                    CAPTURE_AddMidi( true, midi.sysex.used-1, &midi.sysex.buf[1]);
//                }
            }
        }
        if ((data&0x80)!=0) {
            midi.status=data;
            midi.cmd_pos=0;
            midi.cmd_len=MIDI_evt_len[data];
            if (midi.status==0xf0) {
                midi.sysex.buf[0]=(byte)0xf0;
                midi.sysex.used=1;
            }
        }
        if (midi.cmd_len!=0) {
            midi.cmd_buf[midi.cmd_pos++]=(byte)data;
            if (midi.cmd_pos >= midi.cmd_len) {
//                if (CaptureState & CAPTURE_MIDI) {
//                    CAPTURE_AddMidi(false, midi.cmd_len, midi.cmd_buf);
//                }
                try {msg.setMessage(midi.cmd_buf[0], midi.cmd_buf[1], midi.cmd_buf[2]);} catch (Exception e) {}
                midi.handler.send(msg, -1);
                midi.cmd_pos=1;		//Use Running status
            }
        }
    }

    static public boolean MIDI_Available()  {
        return midi.device != null;
    }
    
    public Midi() {
		String dev = Option.mididevice.value("default");
        String conf = Option.midiconfig.value("");
		/* If device = "default" go for first handler that works */
//		MAPPER_AddHandler(MIDI_SaveRawEvent,MK_f8,MMOD1|MMOD2,"caprawmidi","Cap MIDI");
        midi.sysex.delay = 0;
		midi.sysex.start = 0;
		if (conf.contains("delaysysex")) {
			midi.sysex.start = System.currentTimeMillis();
            conf = conf.replace("delaysysex", "").trim();
			Log.log(Level.INFO, "MIDI:Using delayed SysEx processing");
		}

		midi.status=0x00;
		midi.cmd_pos=0;
		midi.cmd_len=0;
        MidiDevice.Info[] devices = null;

        try {
            devices = MidiSystem.getMidiDeviceInfo();
        } catch (Exception e) {

        }
        boolean def = dev.equalsIgnoreCase("default");

        if (!def) {
            if (devices != null) {
                for (int i=0;i<devices.length;i++) {
                    if (devices[i].getName().equalsIgnoreCase(dev)) {
                        try {
                            MidiDevice device = MidiSystem.getMidiDevice(devices[0]);
                            device.open();
                            midi.handler =  device.getReceiver();
                            midi.device = device;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (midi.handler==null) {
                Log.log(Level.INFO, "MIDI:Can't find device:"+dev+", finding default handler.");
            }
        }
        if (midi.handler == null) {
            Synthesizer	synth = null;
            Soundbank soundbank = null;

            try {
                synth=MidiSystem.getSynthesizer();
            } catch (Exception e) {
            }
            if (synth != null) {
                String fileName = "resources/soundbank-deluxe.gm";
                InputStream is = Midi.class.getResourceAsStream(fileName);
                if (is == null) {
                    fileName = "resources/soundbank-mid.gm";
                    is = Midi.class.getResourceAsStream(fileName);
                }
                if (is == null) {
                    fileName = "resources/soundbank-min.gm";
                    is = Midi.class.getResourceAsStream(fileName);
                }
                if (is != null) {
                    try {
                        soundbank = MidiSystem.getSoundbank(is);
                    } catch (Exception e) {
                    }
                    try {is.close();} catch (Exception e) {}
                }
                if (soundbank == null)
                {
                    fileName = "default";
                    try {
                        soundbank = synth.getDefaultSoundbank();
                    } catch (Exception e) {}
                }
                if (soundbank != null) {
                    try {
                        synth.open();
                        if (synth.isSoundbankSupported(soundbank) && synth.loadAllInstruments(soundbank)) {
                            Log.log(Level.INFO, "MIDI: Using Soundbank: "+fileName);
                            midi.handler = synth.getReceiver();
                            midi.device = synth;
                        } else {
                            synth.close();
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        if (devices != null) {
            for (int i=0;i<devices.length && midi.handler == null;i++) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(devices[i]);
                    device.open();
                    midi.handler =  device.getReceiver();
                    midi.device = device;
                    Log.log(Level.INFO, "MIDI:Opened device:"+devices[i].getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (midi.device == null) {
            Log.log(Level.INFO, "MIDI:Can't find any device");
        }
    }

    private static Midi test;

    public static void MIDI_Destroy() {
        if(midi.device!=null) {
            midi.handler.close();
            midi.device.close();
        }
        test = null;
        midi = null;
    }

    public static void MIDI_Init() {
        midi = new _midi();
        test = new Midi();
    }
}
