package org.jpc.emulator.peripheral;

import org.jpc.emulator.AbstractHardwareComponent;
import org.jpc.emulator.HardwareComponent;
import org.jpc.emulator.Timer;
import org.jpc.emulator.TimerResponsive;
import org.jpc.emulator.motherboard.InterruptController;
import org.jpc.emulator.motherboard.IntervalTimer;
import org.jpc.j2se.Option;
import org.jpc.support.Clock;

import java.util.logging.*;

public class Mixer extends AbstractHardwareComponent
{
    private static final Logger Log = Logger.getLogger(Mixer.class.getName());
    private static final boolean LOG_BUFFERS = false;
    private static Clock timeSource;
    private static InterruptController irqDevice;
    private static long nextExpiry;
    private static Timer mix;
    private static Timer mix_nosound;

    static public interface MIXER_MixHandler {
        public void call(/*Bit8u*/short[] sampdate, /*Bit32u*/int len);
    }

    static public interface MIXER_Handler {
        public void call(/*Bitu*/int len);
    }

    static private final class BlahModes {
        static public final int MIXER_8MONO=0;
        static public final int MIXER_8STEREO=1;
        static public final int MIXER_16MONO=2;
        static public final int MIXER_16STEREO=3;
    }

    static private final class MixerModes {
        static public final int M_8M = 0;
        static public final int M_8S = 1;
        static public final int M_16M = 2;
        static public final int M_16S = 3;
    }

    static private final int MIXER_BUFSIZE = (16*1024);
    static private final int MIXER_BUFMASK = (MIXER_BUFSIZE-1);

    static public final int MAX_AUDIO = ((1<<(16-1))-1);
    static public final int MIN_AUDIO = -(1<<(16-1));

    public static class MixerChannel {
        public void SetVolume(float _left,float _right) {
            volmain[0].value=_left;
            volmain[1].value=_right;
            UpdateVolume();
        }
        public void SetScale( float f ){
            scale = f;
            UpdateVolume();
        }
        public void UpdateVolume() {
            volmul[0]=(/*Bits*/int)((1 << MIXER_VOLSHIFT)*scale*volmain[0].value*mixer.mastervol[0].value);
            volmul[1]=(/*Bits*/int)((1 << MIXER_VOLSHIFT)*scale*volmain[1].value*mixer.mastervol[1].value);
        }
        public void SetFreq(/*Bitu*/int _freq) {
            freq_add=(int)((_freq<<MIXER_SHIFT)/mixer.freq);
        }
        public void Mix(/*Bitu*/int _needed) {
            needed=_needed;
            while (enabled && needed>done) {
                /*Bitu*/int todo=needed-done;
                todo *= freq_add;
                todo  = (todo >> MIXER_SHIFT) + ((todo & MIXER_REMAIN)!=0?1:0);
                handler.call(todo);
            }
        }
        //Fill up until needed
        public void AddSilence() {
            if (done<needed) {
                done=needed;
                last[0]=last[1]=0;
                freq_index=MIXER_REMAIN;
            }
        }

        static public class Type {}
        public void AddSamples(/*Bitu*/int len, Ptr data, boolean stereo,boolean signeddata,boolean nativeorder) {
            /*Bits*/int[] diff=new int[2];
            /*Bitu*/int mixpos=mixer.pos+done;
            freq_index&=MIXER_REMAIN;
            /*Bitu*/int pos=0;/*Bitu*/int new_pos=0;

            boolean starting = true;
            for (;;) {
                if (!starting)
                    new_pos=freq_index >> MIXER_SHIFT;
                if (starting || pos<new_pos) {
                    if (!starting) {
                        last[0]+=diff[0];
                        if (stereo) last[1]+=diff[1];
                        pos=new_pos;
                    } else {
                        starting = false;
                    }
                    if (pos>=len) return;
                    if ( data.dataWidth() == 1) {
                        if (!signeddata) {
                            if (stereo) {
                                diff[0]=(((/*Bit8s*/byte)(data.get(pos*2+0) ^ 0x80)) << 8)-last[0];
                                diff[1]=(((/*Bit8s*/byte)(data.get(pos*2+1) ^ 0x80)) << 8)-last[1];
                            } else {
                                diff[0]=(((/*Bit8s*/byte)(data.get(pos) ^ 0x80)) << 8)-last[0];
                            }
                        } else {
                            if (stereo) {
                                diff[0]=(data.get(pos*2+0) << 8)-last[0];
                                diff[1]=(data.get(pos*2+1) << 8)-last[1];
                            } else {
                                diff[0]=(data.get(pos) << 8)-last[0];
                            }
                        }
                    //16bit and 32bit both contain 16bit data internally
                    } else  {
                        if (signeddata) {
                            if (stereo) {
                                if (nativeorder) {
                                    diff[0]=(short)data.get(pos*2+0)-last[0];
                                    diff[1]=(short)data.get(pos*2+1)-last[1];
                                } else {
                                    // :TODO: ?
                                    diff[0] = data.get(pos*2)-last[0];
                                    diff[1] = data.get(pos*2+1)-last[1];
                                }
                            } else {
                                if (nativeorder) {
                                    diff[0]=(short)data.get(pos)-last[0];
                                } else {
                                    // :TODO: ?
                                    diff[0]=data.get(pos)-last[0];
                                }
                            }
                        } else {
                            if (stereo) {
                                if (nativeorder) {
                                    diff[0]=data.get(pos*2+0)-32768-last[0];
                                    diff[1]=data.get(pos*2+1)-32768-last[1];
                                } else {
                                    // :TODO: ?
                                    diff[0] = data.get(pos*2)-32768-last[0];
                                    diff[1] = data.get(pos*2+1)-32768-last[1];
                                }
                            } else {
                                if (nativeorder) {
                                    diff[0]=data.get(pos)-32768-last[0];
                                } else {
                                    // :TODO: ?
                                    diff[0]=data.get(pos)-32768-last[0];
                                }
                            }
                        }
                    }
                }
                /*Bits*/int diff_mul=freq_index & MIXER_REMAIN;
                freq_index+=freq_add;
                mixpos&=MIXER_BUFMASK;
                /*Bits*/int sample=last[0]+((diff[0]*diff_mul) >> MIXER_SHIFT);
                mixer.work[mixpos][0]+=sample*volmul[0];
                if (stereo) sample=last[1]+((diff[1]*diff_mul) >> MIXER_SHIFT);
                mixer.work[mixpos][1]+=sample*volmul[1];
                mixpos++;done++;
            }
        }

        static private interface getSample {
            public int call(int pos);

        }

        public void AddSamples(/*Bitu*/int len, short[] data, boolean stereo,boolean signeddata) {
            /*Bits*/int[] diff=new int[2];
            /*Bitu*/int mixpos=mixer.pos+done;
            freq_index&=MIXER_REMAIN;
            /*Bitu*/int pos=0;/*Bitu*/int new_pos=0;

            boolean starting = true;
            for (;;) {
                if (!starting)
                    new_pos=freq_index >> MIXER_SHIFT;
                if (starting || pos<new_pos) {
                    if (!starting) {
                        last[0]+=diff[0];
                        if (stereo) last[1]+=diff[1];
                        pos=new_pos;
                    } else {
                        starting = false;
                    }
                    if (pos>=len) return;
                    if (signeddata) {
                        if (stereo) {
                            diff[0]=data[pos*2+0]-last[0];
                            diff[1]=data[pos*2+1]-last[1];
                        } else {
                            diff[0]=data[pos]-last[0];
                        }
                    } else {
                        if (stereo) {
                            diff[0]=data[pos*2+0]-32768-last[0];
                            diff[1]=data[pos*2+1]-32768-last[1];
                        } else {
                            diff[0]=data[pos]-32768-last[0];
                        }
                    }
                }
                /*Bits*/int diff_mul=freq_index & MIXER_REMAIN;
                freq_index+=freq_add;
                mixpos&=MIXER_BUFMASK;
                /*Bits*/int sample=last[0]+((diff[0]*diff_mul) >> MIXER_SHIFT);
                mixer.work[mixpos][0]+=sample*volmul[0];
                if (stereo) sample=last[1]+((diff[1]*diff_mul) >> MIXER_SHIFT);
                mixer.work[mixpos][1]+=sample*volmul[1];
                mixpos++;done++;
            }
        }

        public void AddSamples(/*Bitu*/int len, int[] data, boolean stereo,boolean signeddata) {
            /*Bits*/int[] diff=new int[2];
            /*Bitu*/int mixpos=mixer.pos+done;
            freq_index&=MIXER_REMAIN;
            /*Bitu*/int pos=0;/*Bitu*/int new_pos=0;

            boolean starting = true;
            for (;;) {
                if (!starting)
                    new_pos=freq_index >> MIXER_SHIFT;
                if (starting || pos<new_pos) {
                    if (!starting) {
                        last[0]+=diff[0];
                        if (stereo) last[1]+=diff[1];
                        pos=new_pos;
                    } else {
                        starting = false;
                    }
                    if (pos>=len) return;
                    if (signeddata) {
                        if (stereo) {
                            diff[0]=data[pos*2+0]-last[0];
                            diff[1]=data[pos*2+1]-last[1];
                        } else {
                            diff[0]=data[pos]-last[0];
                        }
                    } else {
                        if (stereo) {
                            diff[0]=data[pos*2+0]-32768-last[0];
                            diff[1]=data[pos*2+1]-32768-last[1];
                        } else {
                            diff[0]=data[pos]-32768-last[0];
                        }
                    }
                }
                /*Bits*/int diff_mul=freq_index & MIXER_REMAIN;
                freq_index+=freq_add;
                mixpos&=MIXER_BUFMASK;
                /*Bits*/int sample=last[0]+((diff[0]*diff_mul) >> MIXER_SHIFT);
                mixer.work[mixpos][0]+=sample*volmul[0];
                if (stereo) sample=last[1]+((diff[1]*diff_mul) >> MIXER_SHIFT);
                mixer.work[mixpos][1]+=sample*volmul[1];
                mixpos++;done++;
            }
        }

        public void AddSamples(/*Bitu*/int len, byte[] data, boolean stereo,boolean signeddata) {
            /*Bits*/int[] diff=new int[2];
            /*Bitu*/int mixpos=mixer.pos+done;
            freq_index&=MIXER_REMAIN;
            /*Bitu*/int pos=0;/*Bitu*/int new_pos=0;

            boolean starting = true;
            for (;;) {
                if (!starting)
                    new_pos=freq_index >> MIXER_SHIFT;
                if (starting || pos<new_pos) {
                    if (!starting) {
                        last[0]+=diff[0];
                        if (stereo) last[1]+=diff[1];
                        pos=new_pos;
                    } else {
                        starting = false;
                    }
                    if (pos>=len) return;
                     if (!signeddata) {
                        if (stereo) {
                            diff[0]=(((/*Bit8s*/byte)(data[pos*2+0] ^ 0x80)) << 8)-last[0];
                            diff[1]=(((/*Bit8s*/byte)(data[pos*2+1] ^ 0x80)) << 8)-last[1];
                        } else {
                            diff[0]=(((/*Bit8s*/byte)(data[pos] ^ 0x80)) << 8)-last[0];
                        }
                    } else {
                        if (stereo) {
                            diff[0]=(data[pos*2+0] << 8)-last[0];
                            diff[1]=(data[pos*2+1] << 8)-last[1];
                        } else {
                            diff[0]=(data[pos] << 8)-last[0];
                        }
                    }
                }
                /*Bits*/int diff_mul=freq_index & MIXER_REMAIN;
                freq_index+=freq_add;
                mixpos&=MIXER_BUFMASK;
                /*Bits*/int sample=last[0]+((diff[0]*diff_mul) >> MIXER_SHIFT);
                if (mixpos < 10)
                {
                    System.out.printf("last[0]=%d diff[0]=%d diff_mull=%d \n",last[0], diff[0], diff_mul);
                System.out.printf("mixpos=%d 0=%d sample=%d volmul=%d\n", mixpos, mixer.work[mixpos][0], sample, volmul[0]);
                }
                mixer.work[mixpos][0]+=sample*volmul[0];
                if (stereo) sample=last[1]+((diff[1]*diff_mul) >> MIXER_SHIFT);
                mixer.work[mixpos][1]+=sample*volmul[1];
                mixpos++;done++;
            }
        }

        public void AddSamples_m8(/*Bitu*/int len, /*Bit8u*/byte[] data) {
            AddSamples(len,data,false,false);
        }
        public void AddSamples_s8(/*Bitu*/int len, /*Bit8u*/byte[] data) {
            AddSamples(len,data,true,false);
        }
        public void AddSamples_m8s(/*Bitu*/int len, /*/*Bit8s*/byte[] data) {
            AddSamples(len,data,false,true);
        }
        public void AddSamples_s8s(/*Bitu*/int len, /*/*Bit8s*/byte[] data) {
            AddSamples(len,data,true,true);
        }
        public void AddSamples_m16(/*Bitu*/int len, /*Bit16s*/short[] data) {
            AddSamples(len,data,false,true);
        }
        public void AddSamples_s16(/*Bitu*/int len, /*Bit16s*/short[] data) {
            AddSamples(len,data,true,true);
        }
        public void AddSamples_m16u(/*Bitu*/int len, /*Bit16u*/short[] data) {
            AddSamples(len,data,false,false);
        }
        public void AddSamples_s16u(/*Bitu*/int len, /*Bit16u*/short[] data) {
            AddSamples(len,data,true,false);
        }
        public void AddSamples_m32(/*Bitu*/int len, /*Bit32s*/int[] data) {
            AddSamples(len,data,false,true);
        }
        public void AddSamples_s32(/*Bitu*/int len, /*Bit32s*/int[] data) {
            AddSamples(len,data,true,true);
        }
//        public void AddSamples_m16_nonnative(/*Bitu*/int len, /*Bit16s*/ShortPtr data) {
//            AddSamples(len,data,false,true,false);
//        }
//        public void AddSamples_s16_nonnative(/*Bitu*/int len, /*Bit16s*/ShortPtr data) {
//            AddSamples(len,data,true,true,false);
//        }
//        public void AddSamples_m16u_nonnative(/*Bitu*/int len, /*Bit16u*/ShortPtr data) {
//            AddSamples(len,data,false,false,false);
//        }
//        public void AddSamples_s16u_nonnative(/*Bitu*/int len, /*Bit16u*/ShortPtr data) {
//            AddSamples(len,data,true,false,false);
//        }
//        public void AddSamples_m32_nonnative(/*Bitu*/int len, /*Bit32s*/IntPtr data) {
//            AddSamples(len,data,false,true,false);
//        }
//        public void AddSamples_s32_nonnative(/*Bitu*/int len, /*Bit32s*/IntPtr data) {
//            AddSamples(len,data,true,true,false);
//        }

        //Strech block up into needed data
        public void AddStretched(/*Bitu*/int len, /*Bit16s*/short[] data) {
            if (done>=needed) {
                Log.log(Level.SEVERE, "Can't add, buffer full");
                return;
            }
            /*Bitu*/int outlen=needed-done;/*Bits*/int diff;
            freq_index=0;
            /*Bitu*/int temp_add=(len << MIXER_SHIFT)/outlen;
            /*Bitu*/int mixpos=mixer.pos+done;done=needed;
            /*Bitu*/int pos=0;
            diff=data[0]-last[0];
            while ((outlen--)!=0) {
                /*Bitu*/int new_pos=freq_index >> MIXER_SHIFT;
                if (pos<new_pos) {
                    pos=new_pos;
                    last[0]+=diff;
                    diff=data[pos]-last[0];
                }
                /*Bits*/int diff_mul=freq_index & MIXER_REMAIN;
                freq_index+=temp_add;
                mixpos&=MIXER_BUFMASK;
                /*Bits*/int sample=last[0]+((diff*diff_mul) >> MIXER_SHIFT);
                mixer.work[mixpos][0]+=sample*volmul[0];
                mixer.work[mixpos][1]+=sample*volmul[1];
                mixpos++;
            }
        }
        public void FillUp() {
            synchronized (audioMutex) {
                if (!enabled || done<mixer.done) {
                    return;
                }
                // scale the sound so it is the right speed
                float index = 1.0f;//set this to not scale for now. Pic.PIC_TickIndex();
                Mix((/*Bitu*/int)(index*mixer.needed));
            }
        }
        public void Enable(boolean _yesno) {
            if (_yesno==enabled) return;
            enabled=_yesno;
            if (enabled) {
                freq_index=MIXER_REMAIN;
                synchronized (audioMutex) {
                    if (done<mixer.done) done=mixer.done;
                }
            }
        }
        public MIXER_Handler handler;
        public FloatRef[] volmain = new FloatRef[2];
        public float scale;
        public /*Bit32s*/int[] volmul = new int[2];
        public /*Bitu*/int freq_add,freq_index;
        public /*Bitu*/int done,needed;
        public /*Bits*/int[] last = new int[2];
        public String name;
        public boolean enabled;
        public MixerChannel next;

        public MixerChannel() {
            for (int i=0;i<volmain.length;i++) {
                volmain[i] = new FloatRef();
            }
        }
    }

    /* Object to maintain a mixerchannel; As all objects it registers itself with create
    * and removes itself when destroyed. */
    public static class MixerObject{
        private boolean installed = false;
        private String m_name;
        public MixerChannel Install(MIXER_Handler handler,/*Bitu*/int freq,String name) {
            if(!installed) {
                if(name.length() > 31) throw new IllegalStateException("Too long mixer channel name");
                m_name=name;
                installed = true;
                return MIXER_AddChannel(handler,freq,name);
            } else {
                throw new IllegalStateException("already added mixer channel.");
                //return null; //Compiler happy
            }
        }
        public void destroy() {
            if(!installed) return;
            MIXER_DelChannel(MIXER_FindChannel(m_name));
        }
    }


    /* PC Speakers functions, tightly related to the timer functions */
    public static void PCSPEAKER_SetCounter(/*Bitu*/int cntr,/*Bitu*/int mode){}
    public static void PCSPEAKER_SetType(/*Bitu*/int mode){}

    private static final int MIXER_SSIZE = 4;
    private static final int MIXER_SHIFT = 14;
    private static final int MIXER_REMAIN = ((1<<MIXER_SHIFT)-1);
    private static final int MIXER_VOLSHIFT = 13;

    static /*Bit16s*/short MIXER_CLIP(/*Bits*/int SAMP) {
        if (SAMP < MAX_AUDIO) {
            if (SAMP > MIN_AUDIO)
                return (short)SAMP;
            else return MIN_AUDIO;
        } else return MAX_AUDIO;
    }

    static class _Mixer {
        public _Mixer() {
            for (int i=0;i<mastervol.length;i++) {
                mastervol[i] = new FloatRef();
            }
        }
        /*Bit32s*/int[][] work=new int[MIXER_BUFSIZE][2];
        /*Bitu*/int pos,done;
        /*Bitu*/int needed, min_needed, max_needed;
        /*Bit32u*/long tick_add,tick_remain;
        FloatRef[] mastervol=new FloatRef[2];
        MixerChannel channels;
        boolean nosound;
        /*Bit32u*/int freq;
        /*Bit32u*/int blocksize;
    }
    static private _Mixer mixer;

    static public /*Bit8u*/byte[] MixTemp8=new byte[MIXER_BUFSIZE];
    static public short[] MixTemp16=new short[MIXER_BUFSIZE>>1];
    static public int[] MixTemp32=new int[MIXER_BUFSIZE>>2];

    public static MixerChannel MIXER_AddChannel(MIXER_Handler handler,/*Bitu*/int freq,String name) {
        MixerChannel chan=new MixerChannel();
        chan.scale = 1.0f;
        chan.handler=handler;
        chan.name=name;
        chan.SetFreq(freq);
        chan.next=mixer.channels;
        chan.SetVolume(1,1);
        chan.enabled=false;
        mixer.channels=chan;
        return chan;
    }

    public static MixerChannel MIXER_FindChannel(String name) {
        MixerChannel chan=mixer.channels;
        while (chan!=null) {
            if (chan.name.equalsIgnoreCase(name)) break;
            chan=chan.next;
        }
        return chan;
    }

    /* Find the device you want to delete with findchannel "delchan gets deleted" */
    public static void MIXER_DelChannel(MixerChannel delchan) {
        MixerChannel chan=mixer.channels;
        MixerChannel where=mixer.channels;
        while (chan!=null) {
            if (chan==delchan) {
                where.next=chan.next;
                return;
            }
            where=chan;
            chan=chan.next;
        }
    }

    static private boolean Mixer_irq_important() {
        /* In some states correct timing of the irqs is more important then
         * non stuttering audo */
        return false;
    }

    /* Mix a certain amount of new samples */
    static private void MIXER_MixData(/*Bitu*/int needed) {
        MixerChannel chan=mixer.channels;
        while (chan!=null) {
            chan.Mix(needed);
            chan=chan.next;
        }
        //Reset the the tick_add for constant speed
        if( Mixer_irq_important() )
            mixer.tick_add = ((mixer.freq) << MIXER_SHIFT)/1000;
        mixer.done = needed;
    }

    static private TimerResponsive MIXER_Mix = new TimerResponsive() {
        public void callback() {
            synchronized (audioMutex) {
                MIXER_MixData(mixer.needed);
                mixer.tick_remain+=mixer.tick_add;
                mixer.needed+=(mixer.tick_remain>>MIXER_SHIFT);
                mixer.tick_remain&=MIXER_REMAIN;
                nextExpiry += 1000000;
                mix.setExpiry(nextExpiry);
            }
        }
        public int getType()
        {
            return -1;
        }
    };

    static private TimerResponsive MIXER_Mix_NoSound = new TimerResponsive() {
        public void callback() {
            MIXER_MixData(mixer.needed);
            /* Clear piece we've just generated */
            for (/*Bitu*/int i=0;i<mixer.needed;i++) {
                mixer.work[mixer.pos][0]=0;
                mixer.work[mixer.pos][1]=0;
                mixer.pos=(mixer.pos+1)&MIXER_BUFMASK;
            }
            /* Reduce count in channels */
            for (MixerChannel chan=mixer.channels;chan!=null;chan=chan.next) {
                if (chan.done>mixer.needed) chan.done-=mixer.needed;
                else chan.done=0;
            }
            /* Set values for next tick */
            mixer.tick_remain+=mixer.tick_add;
            mixer.needed=(int)(mixer.tick_remain>>MIXER_SHIFT);
            mixer.tick_remain&=MIXER_REMAIN;
            mixer.done=0;
            nextExpiry += 1000000;
            mix_nosound.setExpiry(nextExpiry);
        }
        public int getType()
        {
            return -1;
        }
    };

    static boolean MIXER_CallBack(byte[] stream, int len) {
        /*Bitu*/int need=(/*Bitu*/int)len/MIXER_SSIZE;
        /*Bit16s*/ShortPtr output=new ShortPtr(stream,0);
        /*Bitu*/int reduce;
        /*Bitu*/int pos, index, index_add;
        /*Bits*/int sample;
        /* Enough room in the buffer ? */
        if (mixer.done < need) {
            if (LOG_BUFFERS)
                Log.log(Level.INFO, String.format("Full underrun need %d, have %d, min %d", need, mixer.done, mixer.min_needed));
            if((need - mixer.done) > (need >>7) ) //Max 1 percent stretch.
                return false;
            reduce = mixer.done;
            index_add = (reduce << MIXER_SHIFT) / need;
            mixer.tick_add = ((mixer.freq+mixer.min_needed) << MIXER_SHIFT)/1000;
        } else if (mixer.done < mixer.max_needed) {
            /*Bitu*/int left = mixer.done - need;
            if (left < mixer.min_needed) {
                if( !Mixer_irq_important() ) {
                    /*Bitu*/int needed = mixer.needed - need;
                    /*Bitu*/int diff = (mixer.min_needed>needed?mixer.min_needed:needed) - left;
                    mixer.tick_add = ((mixer.freq+(diff*3)) << MIXER_SHIFT)/1000;
                    left = 0; //No stretching as we compensate with the tick_add value
                } else {
                    left = (mixer.min_needed - left);
                    left = 1 + (2*left) / mixer.min_needed; //left=1,2,3
                }
                if (LOG_BUFFERS)
                    Log.log(Level.INFO, String.format("needed underrun need %d, have %d, min %d, left %d", need, mixer.done, mixer.min_needed, left));
                reduce = need - left;
                index_add = (reduce << MIXER_SHIFT) / need;
            } else {
                reduce = need;
                index_add = (1 << MIXER_SHIFT);
                if (LOG_BUFFERS)
                    Log.log(Level.INFO, String.format("regular run need %d, have %d, min %d, left %d", need, mixer.done, mixer.min_needed, left));

                /* Mixer tick value being updated:
                 * 3 cases:
                 * 1) A lot too high. >division by 5. but maxed by 2* min to prevent too fast drops.
                 * 2) A little too high > division by 8
                 * 3) A little to nothing above the min_needed buffer > go to default value
                 */
                /*Bitu*/int diff = left - mixer.min_needed;
                if(diff > (mixer.min_needed<<1)) diff = mixer.min_needed<<1;
                if(diff > (mixer.min_needed>>1))
                    mixer.tick_add = ((mixer.freq-(diff/5)) << MIXER_SHIFT)/1000;
                else if (diff > (mixer.min_needed>>4))
                    mixer.tick_add = ((mixer.freq-(diff>>3)) << MIXER_SHIFT)/1000;
                else
                    mixer.tick_add = (mixer.freq<< MIXER_SHIFT)/1000;
            }
        } else {
            /* There is way too much data in the buffer */
            if (LOG_BUFFERS)
                Log.log(Level.INFO, String.format("overflow run need %d, have %d, min %d", need, mixer.done, mixer.min_needed));
            if (mixer.done > MIXER_BUFSIZE)
                index_add = MIXER_BUFSIZE - 2*mixer.min_needed;
            else
                index_add = mixer.done - 2*mixer.min_needed;
            index_add = (index_add << MIXER_SHIFT) / need;
            reduce = mixer.done - 2* mixer.min_needed;
            mixer.tick_add = ((mixer.freq-(mixer.min_needed/5)) << MIXER_SHIFT)/1000;
        }
        /* Reduce done count in all channels */
        for (MixerChannel chan=mixer.channels;chan!=null;chan=chan.next) {
            if (chan.done>reduce) chan.done-=reduce;
            else chan.done=0;
        }

        // Reset mixer.tick_add when irqs are important
        if( Mixer_irq_important() )
            mixer.tick_add=(mixer.freq<< MIXER_SHIFT)/1000;

        mixer.done -= reduce;
        mixer.needed -= reduce;
        pos = mixer.pos;
        mixer.pos = (mixer.pos + reduce) & MIXER_BUFMASK;
        index = 0;
        if(need != reduce) {
            while ((need--)!=0) {
                /*Bitu*/int i = (pos + (index >> MIXER_SHIFT )) & MIXER_BUFMASK;
                index += index_add;
                sample=mixer.work[i][0]>>MIXER_VOLSHIFT;
                output.setInc(MIXER_CLIP(sample));
                sample=mixer.work[i][1]>>MIXER_VOLSHIFT;
                output.setInc(MIXER_CLIP(sample));
            }
            /* Clean the used buffer */
            while ((reduce--)!=0) {
                pos &= MIXER_BUFMASK;
                mixer.work[pos][0]=0;
                mixer.work[pos][1]=0;
                pos++;
            }
        } else {
            while ((reduce--)!=0) {
                pos &= MIXER_BUFMASK;
                sample=mixer.work[pos][0]>>MIXER_VOLSHIFT;
                output.setInc(MIXER_CLIP(sample));
                sample=mixer.work[pos][1]>>MIXER_VOLSHIFT;
                output.setInc(MIXER_CLIP(sample));
                mixer.work[pos][0]=0;
                mixer.work[pos][1]=0;
                pos++;
            }
        }
        return true;
    }

    public void MakeVolume(String scan, FloatRef vol0,FloatRef vol1) {
        /*Bitu*/int w=0;
        boolean db=(scan.toUpperCase().charAt(0)=='D');
        if (db) scan=scan.substring(1);
        while (scan.length()>0) {
            if (scan.charAt(0)==':') {
                scan=scan.substring(0);w=1;
            }
            String before=scan;
            float val=0.0f;
            try {
                int pos = scan.indexOf(' ');
                if (pos>0)
                    val = Float.parseFloat(scan.substring(0, pos));
                else
                    val = Float.parseFloat(scan);
            } catch (Exception e) {
                scan=scan.substring(1);
            }

            if (!db) val/=100;
            else val=(float)Math.pow(10.0f,(float)val/20.0f);
            if (val<0) val=1.0f;
            if (w==0) {
                vol0.value=val;
            } else {
                vol1.value=val;
            }
        }
        if (w==0) vol1.value=vol0.value;
    }

    private void ShowVolume(String name,FloatRef vol0,FloatRef vol1) {
        System.out.printf("%-8s %3.0f:%-3.0f  %+3.2f:%-+3.2f \n",new Object[] {name,
            new Float(vol0.value*100),new Float(vol1.value*100),
            new Float(20*Math.log(vol0.value)/Math.log(10.0f)),new Float(20*Math.log(vol1.value)/Math.log(10.0f))}
        );
    }

    public static void MIXER_Stop() {
        AudioLayer.stop();
        mixer = null;
    }

    final static public Object audioMutex = new Object();

    public static void MIXER_Init() {
        mixer = new _Mixer();

        /* Read out config section */
        mixer.freq = Option.mixer_rate.intValue(SBlaster.OPL_RATE);
        mixer.nosound = Option.mixer_nosound.value();
        mixer.blocksize = Option.mixer_blocksize.intValue(512);

        /* Initialize the internal stuff */
        mixer.channels=null;
        mixer.pos=0;
        mixer.done=0;
        mixer.mastervol[0].value=1.0f;
        mixer.mastervol[1].value=1.0f;

        mixer.tick_remain=0;
        mixer.min_needed = Option.mixer_prebuffer.intValue(20);
        if (mixer.min_needed>100) mixer.min_needed=100;
        mixer.min_needed=(int)(mixer.freq*mixer.min_needed)/1000;
        mixer.max_needed=(int)mixer.blocksize * 2 + 2*mixer.min_needed;
        mixer.needed=mixer.min_needed+1;

        if (mixer.nosound) {
            Log.log(Level.INFO, "MIXER:No Sound Mode Selected.");
            mixer.tick_add=((mixer.freq) << MIXER_SHIFT)/1000;
            //Timer.TIMER_AddTickHandler(MIXER_Mix_NoSound);
            mix_nosound = timeSource.newTimer(MIXER_Mix_NoSound);
            nextExpiry = timeSource.getEmulatedNanos();
            mix_nosound.setExpiry(nextExpiry);
//        }
//        else if (!AudioLayer.open(Option.mixer_javabuffer.intValue(8820), mixer.freq)) {
//            else if (SDL_OpenAudio(&spec, &obtained) <0 ) {
//                mixer.nosound = true;
//                Log.log_msg("MIXER:Can't open audio: %s , running in nosound mode.",SDL_GetError());
//                mixer.tick_add=((mixer.freq) << MIXER_SHIFT)/1000;
//                Timer.TIMER_AddTickHandler(MIXER_Mix_NoSound);
        } else {
//                if((mixer.freq != obtained.freq) || (mixer.blocksize != obtained.samples))
//                    Log.log_msg("MIXER:Got different values from SDL: freq %d, blocksize %d",obtained.freq,obtained.samples);
//                mixer.freq=obtained.freq;
//                mixer.blocksize=obtained.samples;
            mixer.tick_add=(mixer.freq << MIXER_SHIFT)/1000;
            //Timer.TIMER_AddTickHandler(MIXER_Mix);

//                SDL_PauseAudio(0);
        }
    }

    public boolean initialised()
    {
        return (irqDevice != null) && irqDevice.initialised() && (timeSource != null);
    }

    public boolean updated()
    {
        return (irqDevice.updated() && timeSource.updated());
    }

    public void acceptComponent(HardwareComponent component)
    {
        if ((component instanceof InterruptController) && component.initialised())
            irqDevice = (InterruptController) component;
        if ((component instanceof Clock) && component.initialised())
        {
            timeSource = (Clock) component;
            mix = timeSource.newTimer(MIXER_Mix);
            nextExpiry = timeSource.getEmulatedNanos();
            mix.setExpiry(nextExpiry);
        }

        if (this.initialised()) {
        }
    }
}
