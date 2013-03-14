package org.jpc.emulator.peripheral;

import org.jpc.emulator.AbstractHardwareComponent;
import org.jpc.emulator.HardwareComponent;
import org.jpc.emulator.Timer;
import org.jpc.emulator.TimerResponsive;
import org.jpc.emulator.motherboard.IODevice;
import org.jpc.emulator.motherboard.IOPortHandler;
import org.jpc.emulator.motherboard.InterruptController;
import org.jpc.j2se.Option;
import org.jpc.support.Clock;

import java.util.logging.*;

public class MPU401 extends AbstractHardwareComponent implements IODevice
{
    private static final Logger Log = Logger.getLogger(MPU401.class.getName());
    private static InterruptController irqDevice;
    private static Clock timeSource;
    private static Timer event;
    private static boolean ioportRegistered = false;

    static final private int MPU401_VERSION = 0x15;
    static final private int MPU401_REVISION = 0x01;
    static final private int MPU401_QUEUE = 32;
    static final private float MPU401_TIMECONSTANT = (60000000/1000.0f);
    static final private float MPU401_RESETBUSY = 27.0f;

    static final private int M_UART = 0;
    static final private int M_INTELLIGENT = 1;

    static final private int T_OVERFLOW = 0;
    static final private int T_MARK = 1;
    static final private int T_MIDI_SYS = 2;
    static final private int T_MIDI_NORM = 3;
    static final private int T_COMMAND = 4;

    /* Messages sent to MPU-401 from host */
    static final private int MSG_EOX = 0xf7;
    static final private int MSG_OVERFLOW = 0xf8;
    static final private int MSG_MARK = 0xfc;

    /* Messages sent to host from MPU-401 */
    static final private int MSG_MPU_OVERFLOW = 0xf8;
    static final private int MSG_MPU_COMMAND_REQ = 0xf9;
    static final private int MSG_MPU_END = 0xfc;
    static final private int MSG_MPU_CLOCK = 0xfd;
    static final private int MSG_MPU_ACK = 0xfe;

    static final private class MPU {
        public MPU() {
            for (int i=0;i<playbuf.length;i++)
                playbuf[i] = new track();
        }
        public boolean intelligent;
        public int mode;
        /*Bitu*/int irq;
        /*Bit8u*/short[] queue = new short[MPU401_QUEUE];
        /*Bitu*/int queue_pos,queue_used;
        public static class track {
            /*Bits*/int counter;
            /*Bit8u*/short[] value = new short[8];
            short sys_val;
            /*Bit8u*/short vlength,length;
            /*MpuDataType*/ int type;
        }
        public final track[] playbuf = new track[8];
        public final track condbuf = new track();

        public static class State {
            boolean conductor,cond_req,cond_set, block_ack;
            boolean playing,reset;
            boolean wsd,wsm,wsd_start;
            boolean run_irq,irq_pending;
            boolean send_now;
            boolean eoi_scheduled;
            /*Bits*/int data_onoff;
            /*Bitu*/int command_byte, cmd_pending;
            /*Bit8u*/short tmask,cmask,amask;
            /*Bit16u*/int midi_mask;
            /*Bit16u*/int req_mask;
            /*Bit8u*/short channel,old_chan;
        }
        public final State state = new State();

        public static class Clock {
            /*Bit8u*/short timebase,old_timebase;
            /*Bit8u*/short tempo,old_tempo;
            /*Bit8u*/short tempo_rel,old_tempo_rel;
            /*Bit8u*/short tempo_grad;
            /*Bit8u*/short cth_rate,cth_counter;
            boolean clock_to_host,cth_active;
        }
        public final Clock clock = new Clock();
    }
    private static final MPU mpu = new MPU();

    private static void QueueByte(/*Bit8u*/int data) {
        if (mpu.state.block_ack) {mpu.state.block_ack=false;return;}
        if (mpu.queue_used==0 && mpu.intelligent) {
            mpu.state.irq_pending=true;
            //Pic.PIC_ActivateIRQ(mpu.irq);
            irqDevice.setIRQ(mpu.irq, 1);
        }
        if (mpu.queue_used<MPU401_QUEUE) {
            /*Bitu*/int pos=mpu.queue_used+mpu.queue_pos;
            if (mpu.queue_pos>=MPU401_QUEUE) mpu.queue_pos-=MPU401_QUEUE;
            if (pos>=MPU401_QUEUE) pos-=MPU401_QUEUE;
            mpu.queue_used++;
            mpu.queue[pos]=(short)data;
        } else Log.log(Level.FINE,"MPU401:Data queue full");
    }

    private static void ClrQueue() {
        mpu.queue_used=0;
        mpu.queue_pos=0;
    }

    private static final int MPU401_ReadStatus(/*Bitu*/int port, /*Bitu*/int iolen) {
            /*Bit8u*/short ret=0x3f;	/* Bits 6 and 7 clear */
            if (mpu.state.cmd_pending!=0) ret|=0x40;
            if (mpu.queue_used==0) ret|=0x80;
            return ret;
        }

    private static final void MPU401_WriteCommand(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            if (mpu.state.reset) {mpu.state.cmd_pending=val+1;return;}
            if (val<=0x2f) {
                switch (val&3) { /* MIDI stop, start, continue */
                    case 1: {Midi.MIDI_RawOutByte(0xfc);break;}
                    case 2: {Midi.MIDI_RawOutByte(0xfa);break;}
                    case 3: {Midi.MIDI_RawOutByte(0xfb);break;}
                }
                if ((val&0x20)!=0) Log.log(Level.SEVERE,"MPU-401:Unhandled Recording Command "+Integer.toString(val,16));
                switch (val&0xc) {
                    case  0x4:	/* Stop */
                        //Pic.PIC_RemoveEvents(MPU401_Event);
                        mpu.state.playing=false;
                        for (/*Bitu*/int i=0xb0;i<0xbf;i++) {	/* All notes off */
                            Midi.MIDI_RawOutByte(i);
                            Midi.MIDI_RawOutByte(0x7b);
                            Midi.MIDI_RawOutByte(0);
                        }
                        break;
                    case 0x8:	/* Play */
                        Log.log(Level.INFO,"MPU-401:Intelligent mode playback started");
                        mpu.state.playing=true;
                        //Pic.PIC_RemoveEvents(MPU401_Event);
                        //Pic.PIC_AddEvent(MPU401_Event,MPU401_TIMECONSTANT/(mpu.clock.tempo*mpu.clock.timebase));
                        event.setExpiry((long)(timeSource.getRealMillis()+MPU401_TIMECONSTANT/(mpu.clock.tempo*mpu.clock.timebase)));
                        ClrQueue();
                        break;
                }
            }
            else if (val>=0xa0 && val<=0xa7) {	/* Request play counter */
                if ((mpu.state.cmask & (1<<(val&7)))!=0) QueueByte(mpu.playbuf[val&7].counter);
            }
            else if (val>=0xd0 && val<=0xd7) {	/* Send data */
                mpu.state.old_chan=mpu.state.channel;
                mpu.state.channel=(short)(val&7);
                mpu.state.wsd=true;
                mpu.state.wsm=false;
                mpu.state.wsd_start=true;
            }
            else
            switch (val) {
                case 0xdf:	/* Send system message */
                    mpu.state.wsd=false;
                    mpu.state.wsm=true;
                    mpu.state.wsd_start=true;
                    break;
                case 0x8e:	/* Conductor */
                    mpu.state.cond_set=false;
                    break;
                case 0x8f:
                    mpu.state.cond_set=true;
                    break;
                case 0x94: /* Clock to host */
                    mpu.clock.clock_to_host=false;
                    break;
                case 0x95:
                    mpu.clock.clock_to_host=true;
                    break;
                case 0xc2: /* Internal timebase */
                    mpu.clock.timebase=48;
                    break;
                case 0xc3:
                    mpu.clock.timebase=72;
                    break;
                case 0xc4:
                    mpu.clock.timebase=96;
                    break;
                case 0xc5:
                    mpu.clock.timebase=120;
                    break;
                case 0xc6:
                    mpu.clock.timebase=144;
                    break;
                case 0xc7:
                    mpu.clock.timebase=168;
                    break;
                case 0xc8:
                    mpu.clock.timebase=192;
                    break;
                /* Commands with data byte */
                case 0xe0: case 0xe1: case 0xe2: case 0xe4: case 0xe6:
                case 0xe7: case 0xec: case 0xed: case 0xee: case 0xef:
                    mpu.state.command_byte=val;
                    break;
                /* Commands 0xa# returning data */
                case 0xab:	/* Request and clear recording counter */
                    QueueByte(MSG_MPU_ACK);
                    QueueByte(0);
                    return;
                case 0xac:	/* Request version */
                    QueueByte(MSG_MPU_ACK);
                    QueueByte(MPU401_VERSION);
                    return;
                case 0xad:	/* Request revision */
                    QueueByte(MSG_MPU_ACK);
                    QueueByte(MPU401_REVISION);
                    return;
                case 0xaf:	/* Request tempo */
                    QueueByte(MSG_MPU_ACK);
                    QueueByte(mpu.clock.tempo);
                    return;
                case 0xb1:	/* Reset relative tempo */
                    mpu.clock.tempo_rel=40;
                    break;
                case 0xb9:	/* Clear play map */
                case 0xb8:	/* Clear play counters */
                    for (/*Bitu*/int i=0xb0;i<0xbf;i++) {	/* All notes off */
                        Midi.MIDI_RawOutByte(i);
                        Midi.MIDI_RawOutByte(0x7b);
                        Midi.MIDI_RawOutByte(0);
                    }
                    for (/*Bitu*/int i=0;i<8;i++) {
                        mpu.playbuf[i].counter=0;
                        mpu.playbuf[i].type=T_OVERFLOW;
                    }
                    mpu.condbuf.counter=0;
                    mpu.condbuf.type=T_OVERFLOW;
                    if (!(mpu.state.conductor=mpu.state.cond_set)) mpu.state.cond_req=false;
                    mpu.state.amask=mpu.state.tmask;
                    mpu.state.req_mask=0;
                    mpu.state.irq_pending=true;
                    break;
                case 0xff:	/* Reset MPU-401 */
                    Log.log(Level.INFO,"MPU-401:Reset "+Integer.toString(val,16));
                    //Pic.PIC_AddEvent(MPU401_ResetDone,MPU401_RESETBUSY);
                    MPU401_ResetDone.callback();
			        mpu.state.reset=true;
                    MPU401_Reset();
                    if (mpu.mode==M_UART) return;//do not send ack in UART mode
                    break;
                case 0x3f:	/* UART mode */
                    Log.log(Level.INFO,"MPU-401:Set UART mode "+Integer.toString(val,16));
                    mpu.mode=M_UART;
                    break;
                default:
                    //Log.log(LogType.LOG_MISC, LogSeverity.LOG_NORMALS,"MPU-401:Unhandled command %X",val);
            }
            QueueByte(MSG_MPU_ACK);
        }

    static private final int MPU401_ReadData(/*Bitu*/int port, /*Bitu*/int iolen) {
            /*Bit8u*/short ret=MSG_MPU_ACK;
            if (mpu.queue_used!=0) {
                if (mpu.queue_pos>=MPU401_QUEUE) mpu.queue_pos-=MPU401_QUEUE;
                ret=mpu.queue[mpu.queue_pos];
                mpu.queue_pos++;mpu.queue_used--;
            }
            if (!mpu.intelligent) return ret;

            if (mpu.queue_used == 0) irqDevice.setIRQ(mpu.irq, 0);//Pic.PIC_DeActivateIRQ(mpu.irq);

            if (ret>=0xf0 && ret<=0xf7) { /* MIDI data request */
                mpu.state.channel=(short)(ret&7);
                mpu.state.data_onoff=0;
                mpu.state.cond_req=false;
            }
            if (ret==MSG_MPU_COMMAND_REQ) {
                mpu.state.data_onoff=0;
                mpu.state.cond_req=true;
                if (mpu.condbuf.type!=T_OVERFLOW) {
                    mpu.state.block_ack=true;
                    MPU401_WriteCommand(0x331,mpu.condbuf.value[0],1);
                    if (mpu.state.command_byte!=0) MPU401_WriteData(0x330,mpu.condbuf.value[1],1);
                }
            mpu.condbuf.type=T_OVERFLOW;
            }
            if (ret==MSG_MPU_END || ret==MSG_MPU_CLOCK || ret==MSG_MPU_ACK) {
                mpu.state.data_onoff=-1;
                MPU401_EOIHandlerDispatch();
            }
            return ret;
        }

    private static /*Bitu*/int length,cnt,posd;
    private static final void MPU401_WriteData(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            if (mpu.mode==M_UART) {Midi.MIDI_RawOutByte(val);return;}
            switch (mpu.state.command_byte) {	/* 0xe# command data */
                case 0x00:
                    break;
                case 0xe0:	/* Set tempo */
                    mpu.state.command_byte=0;
                    mpu.clock.tempo=(short)val;
                    return;
                case 0xe1:	/* Set relative tempo */
                    mpu.state.command_byte=0;
                    if (val!=0x40) //default value
                        Log.log(Level.SEVERE,"MPU-401:Relative tempo change not implemented");
                    return;
                case 0xe7:	/* Set internal clock to host interval */
                    mpu.state.command_byte=0;
                    mpu.clock.cth_rate=(short)(val>>2);
                    return;
                case 0xec:	/* Set active track mask */
                    mpu.state.command_byte=0;
                    mpu.state.tmask=(short)val;
                    return;
                case 0xed: /* Set play counter mask */
                    mpu.state.command_byte=0;
                    mpu.state.cmask=(short)val;
                    return;
                case 0xee: /* Set 1-8 MIDI channel mask */
                    mpu.state.command_byte=0;
                    mpu.state.midi_mask&=0xff00;
                    mpu.state.midi_mask|=val;
                    return;
                case 0xef: /* Set 9-16 MIDI channel mask */
                    mpu.state.command_byte=0;
                    mpu.state.midi_mask&=0x00ff;
                    mpu.state.midi_mask|=((/*Bit16u*/int)val)<<8;
                    return;
                //case 0xe2:	/* Set graduation for relative tempo */
                //case 0xe4:	/* Set metronome */
                //case 0xe6:	/* Set metronome measure length */
                default:
                    mpu.state.command_byte=0;
                    return;
            }
            if (mpu.state.wsd) {	/* Directly send MIDI message */
                if (mpu.state.wsd_start) {
                    mpu.state.wsd_start=false;
                    cnt=0;
                        switch (val&0xf0) {
                            case 0xc0:case 0xd0:
                                mpu.playbuf[mpu.state.channel].value[0]=(short)val;
                                length=2;
                                break;
                            case 0x80:case 0x90:case 0xa0:case 0xb0:case 0xe0:
                                mpu.playbuf[mpu.state.channel].value[0]=(short)val;
                                length=3;
                                break;
                            case 0xf0:
                                Log.log(Level.SEVERE,"MPU-401:Illegal WSD byte");
                                mpu.state.wsd=false;
                                mpu.state.channel=mpu.state.old_chan;
                                return;
                            default: /* MIDI with running status */
                                cnt++;
                                Midi.MIDI_RawOutByte(mpu.playbuf[mpu.state.channel].value[0]);
                        }
                }
                if (cnt<length) {Midi.MIDI_RawOutByte(val);cnt++;}
                if (cnt==length) {
                    mpu.state.wsd=false;
                    mpu.state.channel=mpu.state.old_chan;
                }
                return;
            }
            if (mpu.state.wsm) {	/* Directly send system message */
                if (val==MSG_EOX) {Midi.MIDI_RawOutByte(MSG_EOX);mpu.state.wsm=false;return;}
                if (mpu.state.wsd_start) {
                    mpu.state.wsd_start=false;
                    cnt=0;
                    switch (val) {
                        case 0xf2:{ length=3; break;}
                        case 0xf3:{ length=2; break;}
                        case 0xf6:{ length=1; break;}
                        case 0xf0:{ length=0; break;}
                        default:
                            length=0;
                    }
                }
                if (length==0 || cnt<length) {Midi.MIDI_RawOutByte(val);cnt++;}
                if (cnt==length) mpu.state.wsm=false;
                return;
            }
            if (mpu.state.cond_req) { /* Command */
                switch (mpu.state.data_onoff) {
                    case -1:
                        return;
                    case  0: /* Timing byte */
                        mpu.condbuf.vlength=0;
                        if (val<0xf0) mpu.state.data_onoff++;
                        else {
                            mpu.state.data_onoff=-1;
                            MPU401_EOIHandlerDispatch();
                            return;
                        }
                        if (val==0) mpu.state.send_now=true;
                        else mpu.state.send_now=false;
                        mpu.condbuf.counter=val;
                        break;
                    case  1: /* Command byte #1 */
                        mpu.condbuf.type=T_COMMAND;
                        if (val==0xf8 || val==0xf9) mpu.condbuf.type=T_OVERFLOW;
                        mpu.condbuf.value[mpu.condbuf.vlength]=(short)val;
                        mpu.condbuf.vlength++;
                        if ((val&0xf0)!=0xe0) MPU401_EOIHandlerDispatch();
                        else mpu.state.data_onoff++;
                        break;
                    case  2:/* Command byte #2 */
                        mpu.condbuf.value[mpu.condbuf.vlength]=(short)val;
                        mpu.condbuf.vlength++;
                        MPU401_EOIHandlerDispatch();
                        break;
                }
                return;
            }
            switch (mpu.state.data_onoff) { /* Data */
                case   -1:
                    return;
                case    0: /* Timing byte */
                    if (val<0xf0) mpu.state.data_onoff=1;
                    else {
                        mpu.state.data_onoff=-1;
                        MPU401_EOIHandlerDispatch();
                        return;
                    }
                    if (val==0) mpu.state.send_now=true;
                    else mpu.state.send_now=false;
                    mpu.playbuf[mpu.state.channel].counter=val;
                    break;
                case    1: /* MIDI */
                    mpu.playbuf[mpu.state.channel].vlength++;
                    posd=mpu.playbuf[mpu.state.channel].vlength;
                    if (posd==1) {
                        switch (val&0xf0) {
                            case 0xf0: /* System message or mark */
                                if (val>0xf7) {
                                    mpu.playbuf[mpu.state.channel].type=T_MARK;
                                    mpu.playbuf[mpu.state.channel].sys_val=(short)val;
                                    length=1;
                                } else {
                                    Log.log(Level.SEVERE,"MPU-401:Illegal message");
                                    mpu.playbuf[mpu.state.channel].type=T_MIDI_SYS;
                                    mpu.playbuf[mpu.state.channel].sys_val=(short)val;
                                    length=1;
                                }
                                break;
                            case 0xc0: case 0xd0: /* MIDI Message */
                                mpu.playbuf[mpu.state.channel].type=T_MIDI_NORM;
                                length=mpu.playbuf[mpu.state.channel].length=2;
                                break;
                            case 0x80: case 0x90: case 0xa0:  case 0xb0: case 0xe0:
                                mpu.playbuf[mpu.state.channel].type=T_MIDI_NORM;
                                length=mpu.playbuf[mpu.state.channel].length=3;
                                break;
                            default: /* MIDI data with running status */
                                posd++;
                                mpu.playbuf[mpu.state.channel].vlength++;
                                mpu.playbuf[mpu.state.channel].type=T_MIDI_NORM;
                                length=mpu.playbuf[mpu.state.channel].length;
                                break;
                        }
                    }
                    if (!(posd==1 && val>=0xf0)) mpu.playbuf[mpu.state.channel].value[posd-1]=(short)val;
                    if (posd==length) MPU401_EOIHandlerDispatch();
            }
        }

    private static void MPU401_IntelligentOut(/*Bit8u*/int chan) {
        /*Bitu*/int val;
        switch (mpu.playbuf[chan].type) {
            case T_OVERFLOW:
                break;
            case T_MARK:
                val=mpu.playbuf[chan].sys_val;
                if (val==0xfc) {
                    Midi.MIDI_RawOutByte(val);
                    mpu.state.amask&=~(1<<chan);
                    mpu.state.req_mask&=~(1<<chan);
                }
                break;
            case T_MIDI_NORM:
                for (/*Bitu*/int i=0;i<mpu.playbuf[chan].vlength;i++)
                    Midi.MIDI_RawOutByte(mpu.playbuf[chan].value[i]);
                break;
            default:
                break;
        }
    }

    private static void UpdateTrack(/*Bit8u*/int chan) {
        MPU401_IntelligentOut(chan);
        if ((mpu.state.amask & (1<<chan))!=0) {
            mpu.playbuf[chan].vlength=0;
            mpu.playbuf[chan].type=T_OVERFLOW;
            mpu.playbuf[chan].counter=0xf0;
            mpu.state.req_mask|=(1<<chan);
        } else {
            if (mpu.state.amask==0 && !mpu.state.conductor) mpu.state.req_mask|=(1<<12);
        }
    }

    private static void UpdateConductor() {
        if (mpu.condbuf.value[0]==0xfc) {
            mpu.condbuf.value[0]=0;
            mpu.state.conductor=false;
            mpu.state.req_mask&=~(1<<9);
            if (mpu.state.amask==0) mpu.state.req_mask|=(1<<12);
            return;
        }
        mpu.condbuf.vlength=0;
        mpu.condbuf.counter=0xf0;
        mpu.state.req_mask|=(1<<9);
    }

    static private final TimerResponsive MPU401_Event = new TimerResponsive() {
        public void callback() {
            if (mpu.mode==M_UART) return;
            if (!mpu.state.irq_pending) {
                for (/*Bitu*/int i=0;i<8;i++) { /* Decrease counters */
                    if ((mpu.state.amask & (1<<i))!=0) {
                        mpu.playbuf[i].counter--;
                        if (mpu.playbuf[i].counter<=0) UpdateTrack(i);
                    }
                }
                if (mpu.state.conductor) {
                    mpu.condbuf.counter--;
                    if (mpu.condbuf.counter<=0) UpdateConductor();
                }
                if (mpu.clock.clock_to_host) {
                    mpu.clock.cth_counter++;
                    if (mpu.clock.cth_counter >= mpu.clock.cth_rate) {
                        mpu.clock.cth_counter=0;
                        mpu.state.req_mask|=(1<<13);
                    }
                }
                if (!mpu.state.irq_pending && mpu.state.req_mask!=0) MPU401_EOIHandler.callback();
            }
            //Pic.PIC_RemoveEvents(MPU401_Event);
            /*Bitu*/int new_time;
            if ((new_time=mpu.clock.tempo*mpu.clock.timebase)==0) return;
            //Pic.PIC_AddEvent(MPU401_Event,MPU401_TIMECONSTANT/new_time);
            event.setExpiry((long)(timeSource.getEmulatedNanos()+MPU401_TIMECONSTANT/new_time));
        }
        public int getType() {return -1;}
    };

    private static void MPU401_EOIHandlerDispatch() {
        if (mpu.state.send_now) {
            mpu.state.eoi_scheduled=true;
            //Pic.PIC_AddEvent(MPU401_EOIHandler,0.06f); //Possible a bit longer
            MPU401_EOIHandler.callback();
        }
        else if (!mpu.state.eoi_scheduled) MPU401_EOIHandler.callback();
    }

    //Updates counters and requests new data on "End of Input"
    static private final TimerResponsive MPU401_EOIHandler = new TimerResponsive() {
        public void callback() {
            mpu.state.eoi_scheduled=false;
            if (mpu.state.send_now) {
                mpu.state.send_now=false;
                if (mpu.state.cond_req) UpdateConductor();
                else UpdateTrack(mpu.state.channel);
            }
            mpu.state.irq_pending=false;
            if (!mpu.state.playing || mpu.state.req_mask==0) return;
            /*Bitu*/int i=0;
            do {
                if ((mpu.state.req_mask & (1<<i))!=0) {
                    QueueByte(0xf0+i);
                    mpu.state.req_mask&=~(1<<i);
                    break;
                }
            } while ((i++)<16);
        }
        public int getType() {return -1;}
    };

    static private final TimerResponsive MPU401_ResetDone = new TimerResponsive() {
        public void callback() {
            mpu.state.reset=false;
            if (mpu.state.cmd_pending!=0) {
                MPU401_WriteCommand(0x331,mpu.state.cmd_pending-1,1);
                mpu.state.cmd_pending=0;
            }
        }
        public int getType() {return -1;}
    };

    private static void MPU401_Reset() {
        //Pic.PIC_DeActivateIRQ(mpu.irq);
        irqDevice.setIRQ(mpu.irq, 0);
        mpu.mode=(mpu.intelligent ? M_INTELLIGENT : M_UART);
        //Pic.PIC_RemoveEvents(MPU401_EOIHandler);
	    mpu.state.eoi_scheduled=false;
        mpu.state.wsd=false;
        mpu.state.wsm=false;
        mpu.state.conductor=false;
        mpu.state.cond_req=false;
        mpu.state.cond_set=false;
        mpu.state.playing=false;
        mpu.state.run_irq=false;
        mpu.state.irq_pending=false;
        mpu.state.cmask=0xff;
        mpu.state.amask=mpu.state.tmask=0;
        mpu.state.midi_mask=0xffff;
        mpu.state.data_onoff=0;
        mpu.state.command_byte=0;
        mpu.state.block_ack=false;
        mpu.clock.tempo=mpu.clock.old_tempo=100;
        mpu.clock.timebase=mpu.clock.old_timebase=120;
        mpu.clock.tempo_rel=mpu.clock.old_tempo_rel=40;
        mpu.clock.tempo_grad=0;
        mpu.clock.clock_to_host=false;
        mpu.clock.cth_rate=60;
        mpu.clock.cth_counter=0;
        ClrQueue();
        mpu.state.req_mask=0;
        mpu.condbuf.counter=0;
        mpu.condbuf.type=T_OVERFLOW;
        for (/*Bitu*/int i=0;i<8;i++) {mpu.playbuf[i].type=T_OVERFLOW;mpu.playbuf[i].counter=0;}
    }

//    private IoHandler.IO_ReadHandleObject[] ReadHandler=new IoHandler.IO_ReadHandleObject[2];
//    private IoHandler.IO_WriteHandleObject[] WriteHandler=new IoHandler.IO_WriteHandleObject[2];
    private boolean installed; /*as it can fail to install by 2 ways (config and no midi)*/

    public MPU401() {
        installed = false;
        String s_mpu = Option.mpu401.value("intelligent");
        if(s_mpu.equalsIgnoreCase("none")) return;
        if(s_mpu.equalsIgnoreCase("off")) return;
        if(s_mpu.equalsIgnoreCase("false")) return;
        if (!Midi.MIDI_Available()) return;
        /*Enabled and there is a Midi */
        installed = true;

//        for (int i=0;i<WriteHandler.length;i++) {
//            WriteHandler[i] = new IoHandler.IO_WriteHandleObject();
//        }
//        for (int i=0;i<ReadHandler.length;i++) {
//            ReadHandler[i] = new IoHandler.IO_ReadHandleObject();
//        }
//        WriteHandler[0].Install(0x330,MPU401_WriteData,IoHandler.IO_MB);
//        WriteHandler[1].Install(0x331,MPU401_WriteCommand,IoHandler.IO_MB);
//        ReadHandler[0].Install(0x330,MPU401_ReadData,IoHandler.IO_MB);
//        ReadHandler[1].Install(0x331,MPU401_ReadStatus,IoHandler.IO_MB);

        mpu.queue_used=0;
        mpu.queue_pos=0;
        mpu.mode=M_UART;
        mpu.irq=9;	/* Princess Maker 2 wants it on irq 9 */

        mpu.intelligent = true;	//Default is on
        if(s_mpu.equalsIgnoreCase("uart")) mpu.intelligent = false;
        if (!mpu.intelligent) return;
        /*Set IRQ and unmask it(for timequest/princess maker 2) */
        //Pic.PIC_SetIRQMask(mpu.irq,false);
        //MPU401_Reset();
    }

    public int ioPortRead8(int address)
    {
        return ioPortRead32(address);
    }

    public int ioPortRead16(int address)
    {
        return ioPortRead32(address);
    }

    public int ioPortRead32(int port)
    {
        if (port == 0x330)
        {
            return MPU401_ReadData(port, 32);
        }
        else if (port == 0x331)
        {
            return MPU401_ReadStatus(port, 32);
        }
        throw new IllegalStateException("Unknown port read "+port);
    }

    public void ioPortWrite8(int address, int data)
    {
        ioPortWrite32(address, data);
    }

    public void ioPortWrite16(int address, int data)
    {
        ioPortWrite32(address, data);
    }

    public void ioPortWrite32(int port, int data)
    {
        if (port == 0x330)
        {
            MPU401_WriteData(port, data, 32);
        }
        else if (port == 0x331)
        {
            MPU401_WriteCommand(port, data, 32);
        }
    }

    public int[] ioPortsRequested()
    {
        int[] ports = new int[]{0x330, 0x331};
        return ports;
    }

    public boolean initialised()
    {
        return (irqDevice != null) && (timeSource != null) && ioportRegistered;
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
            timeSource = (Clock) component;
        if ((component instanceof IOPortHandler) && component.initialised()) {
            ((IOPortHandler) component).registerIOPortCapable(this);
            ioportRegistered = true;
        }

        if (this.initialised())
            MPU401_Init();
    }

    static private MPU401 test;

    public static void MPU401_Destroy() {
        if(!test.installed) return;
        if(!Option.mpu401.value("intelligent").equalsIgnoreCase("intelligent")) return;
        //Pic.PIC_SetIRQMask(mpu.irq,true);
        irqDevice.setIRQ(mpu.irq, 0);
    }

    public static void MPU401_Init() {
        test = new MPU401();
        event = timeSource.newTimer(MPU401_Event);
        irqDevice.setIRQ(mpu.irq, 0);
        MPU401_Reset();
    }
}
