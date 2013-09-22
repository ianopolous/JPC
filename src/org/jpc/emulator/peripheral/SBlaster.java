package org.jpc.emulator.peripheral;

import org.jpc.emulator.AbstractHardwareComponent;
import org.jpc.emulator.HardwareComponent;
import org.jpc.emulator.motherboard.*;
import org.jpc.j2se.Option;
import org.jpc.support.Clock;

import java.util.logging.*;

public class SBlaster extends AbstractHardwareComponent implements IODevice
{
    private static final Logger Log = Logger.getLogger(SBlaster.class.getName());
    private static InterruptController irqDevice;
    private static Clock timeSource;
    private static DMAController dma;
    private static boolean ioportRegistered = false;
    public static final String BLASTER = "SET BLASTER=A220 I7 D1 T3";
    public static final int BASE = 0x220;
    public static final int IRQ = 7;
    public static final int DMA = 1;
    public static final int HDMA = 5;
    public static final int OPL_RATE = 44100;
    public static final String OPLEMU = "default";
    public static int oplmode;

    private static final boolean DEBUG = false;

    static final private int SB_PIC_EVENTS = 0;

    static final private int DSP_MAJOR = 3;
    static final private int DSP_MINOR = 1;

    static final private int MIXER_INDEX = 0x04;
    static final private int MIXER_DATA = 0x05;

    static final private int DSP_RESET = 0x06;
    static final private int DSP_READ_DATA = 0x0A;
    static final private int DSP_WRITE_DATA = 0x0C;
    static final private int DSP_WRITE_STATUS = 0x0C;
    static final private int DSP_READ_STATUS = 0x0E;
    static final private int DSP_ACK_16BIT = 0x0f;

    static final private int DSP_NO_COMMAND = 0;

    static final private int DMA_BUFSIZE = 1024;
    static final private int DSP_BUFSIZE = 64;
    static final private int DSP_DACSIZE = 512;

//Should be enough for sound generated in millisecond blocks
    static final private int SB_BUF_SIZE = 8096;
    static final private int SB_SH	= 14;
    static final private int SB_SH_MASK	= ((1 << SB_SH)-1);

    static final private int DSP_S_RESET = 0;
    static final private int DSP_S_RESET_WAIT = 1;
    static final private int DSP_S_NORMAL = 2;
    static final private int DSP_S_HIGHSPEED = 3;

    // SB_TYPES
    static final private int SBT_NONE = 0;
    static final private int SBT_1 = 1;
    static final private int SBT_PRO1 = 2;
    static final private int SBT_2 = 3;
    static final private int SBT_PRO2 = 4;
    static final private int SBT_16 = 6;
    static final private int SBT_GB = 7;

    // SB_IRQS
    static final private int SB_IRQ_8 = 0;
    static final private int SB_IRQ_16 = 1;
    static final private int SB_IRQ_MPU = 2;

    // DSP_MODES
    static final private int MODE_NONE = 0;
    static final private int MODE_DAC = 1;
    static final private int MODE_DMA = 2;
    static final private int MODE_DMA_PAUSE = 3;
    static final private int MODE_DMA_MASKED = 4;

    // DMA_MODES
    static final private int DSP_DMA_NONE = 0;
    static final private int DSP_DMA_2 = 1;
    static final private int DSP_DMA_3 = 2;
    static final private int DSP_DMA_4 = 3;
    static final private int DSP_DMA_8 = 4;
    static final private int DSP_DMA_16 = 5;
    static final private int DSP_DMA_16_ALIASED = 6;

    static final private int PLAY_MONO = 0;
    static final private int PLAY_STEREO = 1;

    static private class SB_INFO {
        /*Bitu*/int freq;
        static private class Dma {
            boolean stereo,sign,autoinit;
            /*DMA_MODES*/int mode;
            /*Bitu*/int rate,mul;
            /*Bitu*/int total,left,min;
            /*Bit64u*/long start;
            static private class Buf {
                /*Bit8u*/ byte[] b8 = new byte[DMA_BUFSIZE];
                /*Bit16s*/ short[] b16 = new short[DMA_BUFSIZE];
                byte[] b16tmp = new byte[2*DMA_BUFSIZE];
            }
            Buf buf = new Buf();
            /*Bitu*/int bits;
            DMAController.DMAChannel chan;
            int position;
            /*Bitu*/int remain_size;
        }
        Dma dma = new Dma();
        boolean speaker;
        boolean midi;
        /*Bit8u*/short time_constant;
        /*DSP_MODES*/int mode;
        /*SB_TYPES*/int type;
        static private class Irq {
            boolean pending_8bit;
            boolean pending_16bit;
        }
        Irq irq = new Irq();
        static private class Dsp {
            /*Bit8u*/short state;
            /*Bit8u*/short cmd;
            /*Bit8u*/short cmd_len;
            /*Bit8u*/short cmd_in_pos;
            /*Bit8u*/short[] cmd_in = new short[DSP_BUFSIZE];
            static private class Data {
                /*Bit8u*/short lastval;
                /*Bit8u*/short[] data = new short[DSP_BUFSIZE];
                /*Bitu*/int pos,used;
            }
            Data in = new Data();
            Data out = new Data();
            /*Bit8u*/short test_register;
            /*Bitu*/int write_busy;
        }
        Dsp dsp = new Dsp();
        private static class Dac {
            /*Bit16s*/short[] data = new short[DSP_DACSIZE+1];
            /*Bitu*/int used;
            /*Bit16s*/short last;
        }
        Dac dac = new Dac();
        private static class _Mixer {
            /*Bit8u*/short index;
            /*Bit8u*/short[] dac = new short[2],fm = new short[2],cda =new short[2],master = new short[2],lin = new short[2];
            /*Bit8u*/short mic;
            boolean stereo;
            boolean enabled;
            boolean filtered;
            /*Bit8u*/short[] unhandled = new short[0x48];
        }
        _Mixer mixer = new _Mixer();
        private static class Adpcm {
            /*Bit8u*/ShortRef reference = new ShortRef();
            /*Bits*/IntRef stepsize = new IntRef(0);
            boolean haveref;
        }
        Adpcm adpcm = new Adpcm();
        private static class Hw {
            /*Bitu*/int base;
            /*Bitu*/int irq;
            /*Bit8u*/short dma8,dma16;
        }
        Hw hw = new Hw();
        private static class E2 {
            /*Bits*/int value;
            /*Bitu*/int count;
        }
        E2 e2 = new E2();
        Mixer.MixerChannel chan;
    }

    private static SB_INFO sb;

    private final static String copyright_string="COPYRIGHT (C) CREATIVE TECHNOLOGY LTD, 1992.";

// number of bytes in input for commands (sb/sbpro)
    private final static /*Bit8u*/byte[] DSP_cmd_len_sb = new byte[] {
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x00
//  1,0,0,0, 2,0,2,2, 0,0,0,0, 0,0,0,0,  // 0x10
      1,0,0,0, 2,2,2,2, 0,0,0,0, 0,0,0,0,  // 0x10 Wari hack
      0,0,0,0, 2,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x20
      0,0,0,0, 0,0,0,0, 1,0,0,0, 0,0,0,0,  // 0x30

      1,2,2,0, 0,0,0,0, 2,0,0,0, 0,0,0,0,  // 0x40
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x50
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x60
      0,0,0,0, 2,2,2,2, 0,0,0,0, 0,0,0,0,  // 0x70

      2,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x80
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x90
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0xa0
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0xb0

      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0xc0
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0xd0
      1,0,1,0, 1,0,0,0, 0,0,0,0, 0,0,0,0,  // 0xe0
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0   // 0xf0
    };

// number of bytes in input for commands (sb16)
    private final static /*Bit8u*/byte[] DSP_cmd_len_sb16 = new byte[] {
      0,0,0,0, 1,2,0,0, 1,0,0,0, 0,0,2,1,  // 0x00
//  1,0,0,0, 2,0,2,2, 0,0,0,0, 0,0,0,0,  // 0x10
      1,0,0,0, 2,2,2,2, 0,0,0,0, 0,0,0,0,  // 0x10 Wari hack
      0,0,0,0, 2,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x20
      0,0,0,0, 0,0,0,0, 1,0,0,0, 0,0,0,0,  // 0x30

      1,2,2,0, 0,0,0,0, 2,0,0,0, 0,0,0,0,  // 0x40
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x50
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x60
      0,0,0,0, 2,2,2,2, 0,0,0,0, 0,0,0,0,  // 0x70

      2,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x80
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0x90
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0xa0
      3,3,3,3, 3,3,3,3, 3,3,3,3, 3,3,3,3,  // 0xb0

      3,3,3,3, 3,3,3,3, 3,3,3,3, 3,3,3,3,  // 0xc0
      0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0,  // 0xd0
      1,0,1,0, 1,0,0,0, 0,0,0,0, 0,0,0,0,  // 0xe0
      0,0,0,0, 0,0,0,0, 0,1,0,0, 0,0,0,0   // 0xf0
    };

    private static /*Bit8u*/short[] ASP_regs = new short[256];
    private static boolean ASP_init_in_progress = false;

    private final static int[][] E2_incr_table = new int[][] {
      {  0x01, -0x02, -0x04,  0x08, -0x10,  0x20,  0x40, -0x80, -106 },
      { -0x01,  0x02, -0x04,  0x08,  0x10, -0x20,  0x40, -0x80,  165 },
      { -0x01,  0x02,  0x04, -0x08,  0x10, -0x20, -0x40,  0x80, -151 },
      {  0x01, -0x02,  0x04, -0x08, -0x10,  0x20, -0x40,  0x80,   90 }
    };


    private static void DSP_SetSpeaker(boolean how) {
        if (sb.speaker==how) return;
        sb.speaker=how;
        if (sb.type==SBT_16) return;
        sb.chan.Enable(how);
        if (sb.speaker) {
            //Pic.PIC_RemoveEvents(DMA_Silent_Event);
            CheckDMAEnd();
        } else {

        }
    }

    private static void SB_RaiseIRQ(/*SB_IRQS*/int type) {
        Log.log(Level.FINE,"Raising IRQ");
        switch (type) {
        case SB_IRQ_8:
            if (sb.irq.pending_8bit) {
//			LOG_MSG("SB: 8bit irq pending");
                return;
            }
            sb.irq.pending_8bit=true;
            //Pic.PIC_ActivateIRQ(sb.hw.irq);
            irqDevice.setIRQ(sb.hw.irq, 1);
            break;
        case SB_IRQ_16:
            if (sb.irq.pending_16bit) {
//			LOG_MSG("SB: 16bit irq pending");
                return;
            }
            sb.irq.pending_16bit=true;
            //Pic.PIC_ActivateIRQ(sb.hw.irq);
            irqDevice.setIRQ(sb.hw.irq, 1);
            break;
        default:
            break;
        }
    }

    private static void DSP_FlushData() {
        sb.dsp.out.used=0;
        sb.dsp.out.pos=0;
    }

    private static DMAEventHandler DSP_DMA_CallBack = new DMAEventHandler() {
        public void handleDMAEvent(DMAEvent event) {
            if (event == DMAEvent.DMA_REACHED_TC) return;
            else if (event == DMAEvent.DMA_MASKED) {
                if (sb.mode==MODE_DMA) {
                    GenerateDMASound(sb.dma.min);
                    sb.mode=MODE_DMA_MASKED;
    //			DSP_ChangeMode(MODE_DMA_MASKED);
                    Log.log(Level.INFO,"DMA masked, stopping output. ");
                }
            } else if (event == DMAEvent.DMA_UNMASKED) {
                if (sb.mode==MODE_DMA_MASKED && sb.dma.mode!=DSP_DMA_NONE) {
                    DSP_ChangeMode(MODE_DMA);
    //			sb.mode=MODE_DMA;
                    CheckDMAEnd();
                    Log.log(Level.INFO,"DMA unmasked, starting output. ");
                }
            }
        }
    };

    static final private int MIN_ADAPTIVE_STEP_SIZE = 0;
    static final private int MAX_ADAPTIVE_STEP_SIZE = 32767;
    static final private int DC_OFFSET_FADE = 254;

    final private static /*Bit8s*/byte[] scaleMap1 = new byte[]  {
        0,  1,  2,  3,  4,  5,  6,  7,  0,  -1,  -2,  -3,  -4,  -5,  -6,  -7,
        1,  3,  5,  7,  9, 11, 13, 15, -1,  -3,  -5,  -7,  -9, -11, -13, -15,
        2,  6, 10, 14, 18, 22, 26, 30, -2,  -6, -10, -14, -18, -22, -26, -30,
        4, 12, 20, 28, 36, 44, 52, 60, -4, -12, -20, -28, -36, -44, -52, -60
    };
    static final private /*Bit8u*/short[] adjustMap1 = new short[] {
          0, 0, 0, 0, 0, 16, 16, 16,
          0, 0, 0, 0, 0, 16, 16, 16,
        240, 0, 0, 0, 0, 16, 16, 16,
        240, 0, 0, 0, 0, 16, 16, 16,
        240, 0, 0, 0, 0, 16, 16, 16,
        240, 0, 0, 0, 0, 16, 16, 16,
        240, 0, 0, 0, 0,  0,  0,  0,
        240, 0, 0, 0, 0,  0,  0,  0
    };
    private static /*Bit8u*/byte decode_ADPCM_4_sample(/*Bit8u*/int sample,/*Bit8u*/ShortRef reference,/*Bits*/IntRef scale) {
        /*Bits*/int samp = sample + scale.value;

        if ((samp < 0) || (samp > 63)) {
            Log.log(Level.SEVERE,"Bad ADPCM-4 sample");
            if(samp < 0 ) samp =  0;
            if(samp > 63) samp = 63;
        }

        /*Bits*/int ref = reference.value + scaleMap1[samp];
        if (ref > 0xff) reference.value = 0xff;
        else if (ref < 0x00) reference.value = 0x00;
        else reference.value = (/*Bit8u*/short)(ref&0xff);
        scale.value = (scale.value + adjustMap1[samp]) & 0xff;

        return (byte)reference.value;
    }

    static final private /*Bit8s*/byte[] scaleMap2 = new byte[]  {
        0,  1,  0,  -1, 1,  3,  -1,  -3,
        2,  6, -2,  -6, 4, 12,  -4, -12,
        8, 24, -8, -24, 6, 48, -16, -48
    };
    static final private /*Bit8u*/short[] adjustMap2 = new short[]  {
          0, 4,   0, 4,
        252, 4, 252, 4, 252, 4, 252, 4,
        252, 4, 252, 4, 252, 4, 252, 4,
        252, 0, 252, 0
    };

    private static /*Bit8u*/byte decode_ADPCM_2_sample(/*Bit8u*/int sample,/*Bit8u*/ShortRef reference,/*Bits*/IntRef scale) {
        /*Bits*/int samp = sample + scale.value;
        if ((samp < 0) || (samp > 23)) {
            Log.log(Level.SEVERE,"Bad ADPCM-2 sample");
            if(samp < 0 ) samp =  0;
            if(samp > 23) samp = 23;
        }

        /*Bits*/int ref = reference.value + scaleMap2[samp];
        if (ref > 0xff) reference.value = 0xff;
        else if (ref < 0x00) reference.value = 0x00;
        else reference.value = (/*Bit8u*/short)(ref&0xff);
        scale.value = (scale.value + adjustMap2[samp]) & 0xff;

        return (byte)reference.value;
    }

    static final private /*Bit8s*/byte[] scaleMap3 = new byte[]  {
        0,  1,  2,  3,  0,  -1,  -2,  -3,
        1,  3,  5,  7, -1,  -3,  -5,  -7,
        2,  6, 10, 14, -2,  -6, -10, -14,
        4, 12, 20, 28, -4, -12, -20, -28,
        5, 15, 25, 35, -5, -15, -25, -35
    };
    static final private /*Bit8u*/short[] adjustMap3 = new short[] {
          0, 0, 0, 8,   0, 0, 0, 8,
        248, 0, 0, 8, 248, 0, 0, 8,
        248, 0, 0, 8, 248, 0, 0, 8,
        248, 0, 0, 8, 248, 0, 0, 8,
        248, 0, 0, 0, 248, 0, 0, 0
    };

    static private /*Bit8u*/byte decode_ADPCM_3_sample(/*Bit8u*/int sample,/*Bit8u*/ShortRef reference,/*Bits*/IntRef scale) {
        /*Bits*/int samp = sample + scale.value;
        if ((samp < 0) || (samp > 39)) {
            Log.log(Level.SEVERE,"Bad ADPCM-3 sample");
            if(samp < 0 ) samp =  0;
            if(samp > 39) samp = 39;
        }

        /*Bits*/int ref = reference.value + scaleMap3[samp];
        if (ref > 0xff) reference.value = 0xff;
        else if (ref < 0x00) reference.value = 0x00;
        else reference.value = (/*Bit8u*/short)(ref&0xff);
        scale.value = (scale.value + adjustMap3[samp]) & 0xff;

        return (byte)reference.value;
    }

    private static void GenerateDMASound(/*Bitu*/int size) {
        /*Bitu*/int read=0;/*Bitu*/int done=0;/*Bitu*/int i=0;

        if(sb.dma.autoinit) {
            if (sb.dma.left <= size) size = sb.dma.left;
        } else if (sb.dma.left <= sb.dma.min) size = sb.dma.left;

        switch (sb.dma.mode) {
        case DSP_DMA_2:
            sb.dma.chan.readMemory(sb.dma.buf.b8, 0, sb.dma.position, size);//.Read(size,sb.dma.buf.b8, 0);
            read = size;
            if (read!=0 && sb.adpcm.haveref) {
                sb.adpcm.haveref=false;
                sb.adpcm.reference.value=(short)(sb.dma.buf.b8[0] & 0xFF);
                sb.adpcm.stepsize.value=MIN_ADAPTIVE_STEP_SIZE;
                i++;
            }
            for (;i<read;i++) {
                Mixer.MixTemp8[done++]=decode_ADPCM_2_sample((sb.dma.buf.b8[i] >> 6) & 0x3,sb.adpcm.reference,sb.adpcm.stepsize);
                Mixer.MixTemp8[done++]=decode_ADPCM_2_sample((sb.dma.buf.b8[i] >> 4) & 0x3,sb.adpcm.reference,sb.adpcm.stepsize);
                Mixer.MixTemp8[done++]=decode_ADPCM_2_sample((sb.dma.buf.b8[i] >> 2) & 0x3,sb.adpcm.reference,sb.adpcm.stepsize);
                Mixer.MixTemp8[done++]=decode_ADPCM_2_sample((sb.dma.buf.b8[i] >> 0) & 0x3,sb.adpcm.reference,sb.adpcm.stepsize);
            }
            sb.chan.AddSamples_m8(done,Mixer.MixTemp8);
            break;
        case DSP_DMA_3:
            sb.dma.chan.readMemory(sb.dma.buf.b8, 0, sb.dma.position, size);//read = chan.Read(size,sb.dma.buf.b8, 0);
            read = size;
            if (read!=0 && sb.adpcm.haveref) {
                sb.adpcm.haveref=false;
                sb.adpcm.reference.value=(short)(sb.dma.buf.b8[0] & 0xFF);
                sb.adpcm.stepsize.value=MIN_ADAPTIVE_STEP_SIZE;
                i++;
            }
            for (;i<read;i++) {
                Mixer.MixTemp8[done++]=decode_ADPCM_3_sample((sb.dma.buf.b8[i] >>> 5) & 0x7,sb.adpcm.reference,sb.adpcm.stepsize);
                Mixer.MixTemp8[done++]=decode_ADPCM_3_sample((sb.dma.buf.b8[i] >> 2) & 0x7,sb.adpcm.reference,sb.adpcm.stepsize);
                Mixer.MixTemp8[done++]=decode_ADPCM_3_sample((sb.dma.buf.b8[i] & 0x3) << 1,sb.adpcm.reference,sb.adpcm.stepsize);
            }
            sb.chan.AddSamples_m8(done, Mixer.MixTemp8);
            break;
        case DSP_DMA_4:
            sb.dma.chan.readMemory(sb.dma.buf.b8, 0, sb.dma.position, size);//read = chan.Read(size,sb.dma.buf.b8, 0);
            read = size;
            if (read!=0 && sb.adpcm.haveref) {
                sb.adpcm.haveref=false;
                sb.adpcm.reference.value=(short)(sb.dma.buf.b8[0] & 0xFF);
                sb.adpcm.stepsize.value=MIN_ADAPTIVE_STEP_SIZE;
                i++;
            }
            for (;i<read;i++) {
                Mixer.MixTemp8[done++]=decode_ADPCM_4_sample(sb.dma.buf.b8[i] >>> 4,sb.adpcm.reference,sb.adpcm.stepsize);
                Mixer.MixTemp8[done++]=decode_ADPCM_4_sample(sb.dma.buf.b8[i] & 0xf,sb.adpcm.reference,sb.adpcm.stepsize);
            }
            sb.chan.AddSamples_m8(done,Mixer.MixTemp8);
            break;
        case DSP_DMA_8:
            if (sb.dma.stereo) {
                sb.dma.chan.readMemory(sb.dma.buf.b8, sb.dma.remain_size, sb.dma.position, size);//read=sb.dma.chan.Read(size, sb.dma.buf.b8, sb.dma.remain_size);
                read = size;
                //System.out.printf("DMA Read 4 : %d\n", read);
                /*Bitu*/int total=read+sb.dma.remain_size;
                if (!sb.dma.sign)  sb.chan.AddSamples_s8(total>>1,sb.dma.buf.b8);
                else sb.chan.AddSamples_s8s(total>>>1,sb.dma.buf.b8);
                if ((total&1)!=0) {
                    sb.dma.remain_size=1;
                    sb.dma.buf.b8[0]=sb.dma.buf.b8[total-1];
                } else sb.dma.remain_size=0;
            } else {
                sb.dma.chan.readMemory(sb.dma.buf.b8, 0, sb.dma.position, size);//read=sb.dma.chan.Read(size,sb.dma.buf.b8,0);
                read = size;
                if (!sb.dma.sign) sb.chan.AddSamples_m8(read,sb.dma.buf.b8);
                else sb.chan.AddSamples_m8s(read,sb.dma.buf.b8);
            }
            break;
        case DSP_DMA_16:
        case DSP_DMA_16_ALIASED:
            if (sb.dma.stereo) {
                /* In DSP_DMA_16_ALIASED mode temporarily divide by 2 to get number of 16-bit
                   samples, because 8-bit DMA Read returns byte size, while in DSP_DMA_16 mode
                   16-bit DMA Read returns word size */
                sb.dma.chan.readMemory(sb.dma.buf.b16tmp, sb.dma.remain_size, sb.dma.position, 2*size);//read=sb.dma.chan.Read(size,sb.dma.buf.b16, sb.dma.remain_size)
                for (int j=0; j< size; j++)
                    sb.dma.buf.b16[j+sb.dma.remain_size] = (short)((sb.dma.buf.b16tmp[sb.dma.remain_size+2*j] & 0xff) | (sb.dma.buf.b16tmp[sb.dma.remain_size+2*j+1] << 8));
                read = size >> (sb.dma.mode==DSP_DMA_16_ALIASED ? 1:0);
                /*Bitu*/int total=read+sb.dma.remain_size;
//    #if defined (WORDS_BIGENDIAN)
//                if (sb.dma.sign) sb.chan.AddSamples_s16_nonnative(total>>1,sb.dma.buf.b16);
//                else sb.chan.AddSamples_s16u_nonnative(total>>1,(Bit16u *)sb.dma.buf.b16);
//    #else
                if (sb.dma.sign) sb.chan.AddSamples_s16(total>>>1,sb.dma.buf.b16);
                else sb.chan.AddSamples_s16u(total>>>1,sb.dma.buf.b16);
    //#endif
                if ((total&1)!=0) {
                    sb.dma.remain_size=1;
                    sb.dma.buf.b16[0]=sb.dma.buf.b16[total-1];
                } else sb.dma.remain_size=0;
            } else {
                //read=sb.dma.chan.Read(size,sb.dma.buf.b16, 0)
                sb.dma.chan.readMemory(sb.dma.buf.b16tmp, 0, sb.dma.position, 2*size);
                for (int j=0; j< size; j++)
                    sb.dma.buf.b16[j] = (short)((sb.dma.buf.b16tmp[2*j] & 0xff) | (sb.dma.buf.b16tmp[2*j+1] << 8));
                read = size >> (sb.dma.mode==DSP_DMA_16_ALIASED ? 1:0);
//    #if static final private intd(WORDS_BIGENDIAN)
//                if (sb.dma.sign) sb.chan.AddSamples_m16_nonnative(read,sb.dma.buf.b16);
//                else sb.chan.AddSamples_m16u_nonnative(read,(Bit16u *)sb.dma.buf.b16);
//    #else
                if (sb.dma.sign) sb.chan.AddSamples_m16(read,sb.dma.buf.b16);
                else sb.chan.AddSamples_m16u(read,sb.dma.buf.b16);
//    #endif
            }
            //restore buffer length value to byte size in aliased mode
            if (sb.dma.mode==DSP_DMA_16_ALIASED) read=read<<1;
            break;
        default:
            Log.log(Level.SEVERE, "Unhandled dma mode "+sb.dma.mode);
            sb.mode=MODE_NONE;
            return;
        }
        sb.dma.left-=read;
        if (sb.dma.left==0) {
            //Pic.PIC_RemoveEvents(END_DMA_Event);
            if (sb.dma.mode >= DSP_DMA_16) SB_RaiseIRQ(SB_IRQ_16);
            else SB_RaiseIRQ(SB_IRQ_8);
            if (!sb.dma.autoinit) {
                Log.log(Level.INFO,"Single cycle transfer ended");
                sb.mode=MODE_NONE;
                sb.dma.mode=DSP_DMA_NONE;
            } else {
                sb.dma.left=sb.dma.total;
                if (sb.dma.left==0) {
                    Log.log(Level.INFO,"Auto-init transfer with 0 size");
                    sb.mode=MODE_NONE;
                }
            }
        }
    }

    private static void DMA_Silent_Event(/*Bitu*/int val) {
            if (sb.dma.left<val) val=sb.dma.left;
            sb.dma.chan.readMemory(sb.dma.buf.b8, 0, sb.dma.position, val);//Read(val,sb.dma.buf.b8, 0);
            int read = val;
            sb.dma.left-=read;
            if (sb.dma.left==0) {
                if (sb.dma.mode >= DSP_DMA_16) SB_RaiseIRQ(SB_IRQ_16);
                else SB_RaiseIRQ(SB_IRQ_8);
                if (sb.dma.autoinit) sb.dma.left=sb.dma.total;
                else {
                    sb.mode=MODE_NONE;
                    sb.dma.mode=DSP_DMA_NONE;
                }
            }
            if (sb.dma.left!=0) {
                /*Bitu*/int bigger=(sb.dma.left > sb.dma.min) ? sb.dma.min : sb.dma.left;
                float delay=(bigger*1000.0f)/sb.dma.rate;
                //Pic.PIC_AddEvent(DMA_Silent_Event,delay,bigger);
            }
        }

    private static void END_DMA_Event(/*Bitu*/int val) {
            GenerateDMASound(val);
        }

    private static void CheckDMAEnd() {
        if (sb.dma.left==0) return;
        if (!sb.speaker && sb.type!=SBT_16) {
            /*Bitu*/int bigger=(sb.dma.left > sb.dma.min) ? sb.dma.min : sb.dma.left;
            float delay=(bigger*1000.0f)/sb.dma.rate;
            //Pic.PIC_AddEvent(DMA_Silent_Event,delay,bigger);
            Log.log(Level.INFO,"Silent DMA Transfer scheduling IRQ in "+ String.format("%3f", delay)+" milliseconds");
        } else if (sb.dma.left<sb.dma.min) {
            float delay=(sb.dma.left*1000.0f)/sb.dma.rate;
            Log.log(Level.INFO,"Short transfer scheduling IRQ in "+ String.format("%3f", delay)+" milliseconds");
            //Pic.PIC_AddEvent(END_DMA_Event,delay,sb.dma.left);
            END_DMA_Event(sb.dma.left);
        }
    }

    private static void DSP_ChangeMode(/*DSP_MODES*/int mode) {
        if (sb.mode==mode) return;
        else sb.chan.FillUp();
        sb.mode=mode;
    }

//    private static Pic.PIC_EventHandler DSP_RaiseIRQEvent = new Pic.PIC_EventHandler() {
//        public void call(/*Bitu*/int val) {
//            SB_RaiseIRQ(SB_IRQ_8);
//        }
//    };

    static void DSP_DoDMATransfer(/*DMA_MODES*/int mode,/*Bitu*/int freq,boolean stereo) {
        String type;
        sb.mode=MODE_DMA_MASKED;
        sb.chan.FillUp();
        sb.dma.left=sb.dma.total;
        sb.dma.mode=mode;
        sb.dma.stereo=stereo;
        sb.irq.pending_8bit=false;
        sb.irq.pending_16bit=false;
        switch (mode) {
        case DSP_DMA_2:
            type="2-bits ADPCM";
            sb.dma.mul=(1 << SB_SH)/4;
            break;
        case DSP_DMA_3:
            type="3-bits ADPCM";
            sb.dma.mul=(1 << SB_SH)/3;
            break;
        case DSP_DMA_4:
            type="4-bits ADPCM";
            sb.dma.mul=(1 << SB_SH)/2;
            break;
        case DSP_DMA_8:
            type="8-bits PCM";
            sb.dma.mul=(1 << SB_SH);
            break;
        case DSP_DMA_16_ALIASED:
            type="16-bits(aliased) PCM";
            sb.dma.mul=(1 << SB_SH)*2;
            break;
        case DSP_DMA_16:
            type="16-bits PCM";
            sb.dma.mul=(1 << SB_SH);
            break;
        default:
            Log.log(Level.SEVERE,"DSP:Illegal transfer mode "+mode);
            return;
        }
        if (sb.dma.stereo) sb.dma.mul*=2;
        sb.dma.rate=(sb.freq*sb.dma.mul) >> SB_SH;
        sb.dma.min=(sb.dma.rate*3)/1000;
        sb.chan.SetFreq(freq);
        sb.dma.mode=mode;
        //Pic.PIC_RemoveEvents(END_DMA_Event);
        //sb.dma.chan.Register_Callback(DSP_DMA_CallBack);
        sb.dma.chan.registerEventHandler(DSP_DMA_CallBack);
        if (DEBUG) {
            Log.log(Level.INFO,"DMA Transfer:"+type+" "+(sb.dma.stereo ? "Stereo" : "Mono")+" "+(sb.dma.autoinit ? "Auto-Init" : "Single-Cycle")+" freq "+freq+" rate "+sb.dma.rate+" size "+sb.dma.total);
        }
    }

    private static void DSP_PrepareDMA_Old(/*DMA_MODES*/int mode,boolean autoinit,boolean sign) {
        sb.dma.autoinit=autoinit;
        sb.dma.sign=sign;
        if (!autoinit) sb.dma.total=1+sb.dsp.in.data[0]+(sb.dsp.in.data[1] << 8);
        //sb.dma.chan=DMA.GetDMAChannel(sb.hw.dma8);
        sb.dma.chan = dma.getChannel(sb.hw.dma8);
        DSP_DoDMATransfer(mode,sb.freq / (sb.mixer.stereo ? 2 : 1),sb.mixer.stereo);
    }

    private static void DSP_PrepareDMA_New(/*DMA_MODES*/int mode,/*Bitu*/int length,boolean autoinit,boolean stereo) {
        /*Bitu*/int freq=sb.freq;
        //equal length if data format and dma channel are both 16-bit or 8-bit
        sb.dma.total=length;
        sb.dma.autoinit=autoinit;
        if (mode==DSP_DMA_16) {
            if (sb.hw.dma16!=0xff) {
                //sb.dma.chan=DMA.GetDMAChannel(sb.hw.dma16);
                sb.dma.chan = dma.getChannel(sb.hw.dma16);
//                if (sb.dma.chan==null) {
//                    sb.dma.chan=DMA.GetDMAChannel(sb.hw.dma8);
//                    mode=DSP_DMA_16_ALIASED;
//                    sb.dma.total<<=1;
//                }
            } else {
                //sb.dma.chan=DMA.GetDMAChannel(sb.hw.dma8);
                sb.dma.chan = dma.getChannel(sb.hw.dma8);
                mode=DSP_DMA_16_ALIASED;
                //UNDOCUMENTED:
                //In aliased mode sample length is written to DSP as number of
                //16-bit samples so we need double 8-bit DMA buffer length
                sb.dma.total<<=1;
            }
        } else
        {
            //sb.dma.chan=DMA.GetDMAChannel(sb.hw.dma8);
            sb.dma.chan = dma.getChannel(sb.hw.dma8);
        }
        DSP_DoDMATransfer(mode,freq,stereo);
    }


    private static void DSP_AddData(/*Bit8u*/int val) {
        if (sb.dsp.out.used<DSP_BUFSIZE) {
            /*Bitu*/int start=sb.dsp.out.used+sb.dsp.out.pos;
            if (start>=DSP_BUFSIZE) start-=DSP_BUFSIZE;
            sb.dsp.out.data[start]=(short)val;
            sb.dsp.out.used++;
        } else {
            Log.log(Level.SEVERE,"DSP:Data Output buffer full");
        }
    }

    private static void DSP_FinishReset() {
        DSP_FlushData();
        DSP_AddData((short)0xaa);
        sb.dsp.state=DSP_S_NORMAL;
    }

    private static void DSP_Reset() {
        Log.log(Level.FINE,"DSP:Reset");
        //Pic.PIC_DeActivateIRQ(sb.hw.irq);
        irqDevice.setIRQ(sb.hw.irq, 0);

        DSP_ChangeMode(MODE_NONE);
        DSP_FlushData();
        sb.dsp.cmd_len=0;
        sb.dsp.in.pos=0;
        sb.dsp.write_busy=0;
        //Pic.PIC_RemoveEvents(DSP_FinishReset);

        sb.dma.left=0;
        sb.dma.total=0;
        sb.dma.stereo=false;
        sb.dma.sign=false;
        sb.dma.autoinit=false;
        sb.dma.mode=DSP_DMA_NONE;
        sb.dma.remain_size=0;
        //if (sb.dma.chan!=null) sb.dma.chan.Clear_Request();

        sb.freq=22050;
        sb.time_constant=45;
        sb.dac.used=0;
        sb.dac.last=0;
        sb.e2.value=0xaa;
        sb.e2.count=0;
        sb.irq.pending_8bit=false;
        sb.irq.pending_16bit=false;
        sb.chan.SetFreq(22050);
//	DSP_SetSpeaker(false);
        //Pic.PIC_RemoveEvents(END_DMA_Event);
    }

    private static void DSP_DoReset(/*Bit8u*/short val) {
        if (((val&1)!=0) && (sb.dsp.state!=DSP_S_RESET)) {
//TODO Get out of highspeed mode
            DSP_Reset();
            sb.dsp.state=DSP_S_RESET;
        } else if (((val&1)==0) && (sb.dsp.state==DSP_S_RESET)) {	// reset off
            sb.dsp.state=DSP_S_RESET_WAIT;
            //Pic.PIC_RemoveEvents(DSP_FinishReset);
            //Pic.PIC_AddEvent(DSP_FinishReset,20.0f/1000.0f,0);	// 20 microseconds
            DSP_FinishReset();
        }
    }

    private static DMATransferCapable DSP_E2_DMA_CallBack = new DMATransferCapable() {
        public int handleTransfer(DMAController.DMAChannel c, int position, int size) {
            sb.dma.chan = c;
            sb.dma.position = position;
            throw new IllegalStateException("Figure this out bitch...");
//            if (event==DMA.DMAEvent.DMA_UNMASKED) {
//                /*Bit8u*/byte[] val=new byte[] {(/*Bit8u*/byte)(sb.e2.value&0xff)};
//                DMA.DmaChannel chan=DMA.GetDMAChannel(sb.hw.dma8);
//                chan.Register_Callback(null);
//                chan.Write(1, val, 0);
//                return 1;
//            }
//            return 0;
        }
    };

    private static DMATransferCapable DSP_ADC_CallBack = new DMATransferCapable() {
        public int handleTransfer(DMAController.DMAChannel chan, int position, int size) {
            sb.dma.chan = chan;
            sb.dma.position = position;
            int total = sb.dma.left;
            throw new IllegalStateException("Figure this out too, bitch...");
//            if (event!=DMA.DMAEvent.DMA_UNMASKED) return 0;
//            /*Bit8u*/byte[] val=new byte[] {(byte)128};
//            DMA.DmaChannel ch=DMA.GetDMAChannel(sb.hw.dma8);
//            while ((sb.dma.left--)!=0) {
//                ch.Write(1,val, 0);
//            }
//            SB_RaiseIRQ(SB_IRQ_8);
//            //ch.Register_Callback(null);
//            dma.releaseDmaRequest(sb.hw.dma8);
//            return total;
        }
    };

    static private boolean DSP_SB16_ONLY() {
        if (sb.type != SBT_16) {
            Log.log(Level.SEVERE,"DSP:Command "+Integer.toString(sb.dsp.cmd, 16)+" requires SB16");
            return true;
        }
        return false;
    }

    static private boolean DSP_SB2_ABOVE() {
        if (sb.type <= SBT_1) {
            Log.log(Level.SEVERE,"DSP:Command "+Integer.toString(sb.dsp.cmd, 16)+" requires SB2 or above");
            return true;
        }
        return false;
    }

    private static void DSP_DoCommand() {
	    Log.log(Level.INFO, String.format("DSP Command %X",sb.dsp.cmd));
        switch (sb.dsp.cmd) {
        case 0x04:
            if (sb.type == SBT_16) {
                /* SB16 ASP set mode register */
                if ((sb.dsp.in.data[0]&0xf1)==0xf1) ASP_init_in_progress=true;
                else ASP_init_in_progress=false;
                Log.log(Level.INFO,"DSP Unhandled SB16ASP command "+Integer.toString(sb.dsp.cmd, 16)+" (set mode register to "+Integer.toString(sb.dsp.in.data[0],16)+")");
            } else {
                /* DSP Status SB 2.0/pro version. NOT SB16. */
                DSP_FlushData();
                if (sb.type == SBT_2) DSP_AddData(0x88);
                else if ((sb.type == SBT_PRO1) || (sb.type == SBT_PRO2)) DSP_AddData(0x7b);
                else DSP_AddData(0xff);			//Everything enabled
            }
            break;
        case 0x05:	/* SB16 ASP set codec parameter */
            Log.log(Level.SEVERE,"DSP Unhandled SB16ASP command "+Integer.toString(sb.dsp.cmd, 16)+" (set codec parameter)");
            break;
        case 0x08:	/* SB16 ASP get version */
            Log.log(Level.SEVERE,"DSP Unhandled SB16ASP command "+Integer.toString(sb.dsp.cmd, 16)+" sub "+Integer.toString(sb.dsp.in.data[0],16));
            if (sb.type == SBT_16) {
                switch (sb.dsp.in.data[0]) {
                    case 0x03:
                        DSP_AddData(0x18);	// version ID (??)
                        break;
                    default:
                        Log.log(Level.SEVERE,"DSP Unhandled SB16ASP command "+Integer.toString(sb.dsp.cmd, 16)+" sub "+Integer.toString(sb.dsp.in.data[0], 16));
                        break;
                }
            } else {
                Log.log(Level.SEVERE,"DSP Unhandled SB16ASP command "+Integer.toString(sb.dsp.cmd, 16)+" sub "+Integer.toString(sb.dsp.in.data[0],16));
            }
            break;
        case 0x0e:	/* SB16 ASP set register */
            if (sb.type == SBT_16) {
//			Log.log(LogTypes.LOG_SB,LogSeverities.LOG_NORMAL,"SB16 ASP set register %X := %X",sb.dsp.in.data[0],sb.dsp.in.data[1]);
                ASP_regs[sb.dsp.in.data[0]] = sb.dsp.in.data[1];
            } else {
                Log.log(Level.SEVERE,"DSP Unhandled SB16ASP command "+Integer.toString(sb.dsp.cmd, 16)+" (set register)");
            }
            break;
        case 0x0f:	/* SB16 ASP get register */
            if (sb.type == SBT_16) {
                if ((ASP_init_in_progress) && (sb.dsp.in.data[0]==0x83)) {
                    ASP_regs[0x83] = (short)~ASP_regs[0x83];
                }
//			Log.log(LogTypes.LOG_SB,LogSeverities.LOG_NORMAL,"SB16 ASP get register %X == %X",sb.dsp.in.data[0],ASP_regs[sb.dsp.in.data[0]]);
                DSP_AddData(ASP_regs[sb.dsp.in.data[0]]);
            } else {
                Log.log(Level.SEVERE,"DSP Unhandled SB16ASP command "+Integer.toString(sb.dsp.cmd, 16)+" (get register)");
            }
            break;
        case 0x10:	/* Direct DAC */
            DSP_ChangeMode(MODE_DAC);
            if (sb.dac.used<DSP_DACSIZE) {
                sb.dac.data[sb.dac.used++]=(short)((sb.dsp.in.data[0] ^ 0x80) << 8);
                sb.dac.data[sb.dac.used++]=(short)((sb.dsp.in.data[0] ^ 0x80) << 8);
            }
            break;
        case 0x24:	/* Singe Cycle 8-Bit DMA ADC */
            sb.dma.left=sb.dma.total=1+sb.dsp.in.data[0]+(sb.dsp.in.data[1] << 8);
            sb.dma.sign=false;
            Log.log(Level.SEVERE,"DSP:Faked ADC for "+sb.dma.total+" bytes");
            //DMA.GetDMAChannel(sb.hw.dma8).Register_Callback(DSP_ADC_CallBack);
            dma.registerChannel(sb.hw.dma8, DSP_ADC_CallBack);
            break;
        case 0x14:	/* Singe Cycle 8-Bit DMA DAC */
        case 0x15:	/* Wari hack. Waru uses this one instead of 0x14, but some weird stuff going on there anyway */
        case 0x91:	/* Singe Cycle 8-Bit DMA High speed DAC */
            /* Note: 0x91 is documented only for DSP ver.2.x and 3.x, not 4.x */
            DSP_PrepareDMA_Old(DSP_DMA_8,false,false);
            break;
        case 0x90:	/* Auto Init 8-bit DMA High Speed */
        case 0x1c:	/* Auto Init 8-bit DMA */
            if (DSP_SB2_ABOVE()) break; /* Note: 0x90 is documented only for DSP ver.2.x and 3.x, not 4.x */
            DSP_PrepareDMA_Old(DSP_DMA_8,true,false);
            break;
        case 0x38:  /* Write to SB MIDI Output */
            if (sb.midi) Midi.MIDI_RawOutByte(sb.dsp.in.data[0]);
            break;
        case 0x40:	/* Set Timeconstant */
            sb.freq=(1000000 / (256 - sb.dsp.in.data[0]));
            /* Nasty kind of hack to allow runtime changing of frequency */
            if (sb.dma.mode != DSP_DMA_NONE && sb.dma.autoinit) {
                DSP_PrepareDMA_Old(sb.dma.mode,sb.dma.autoinit,sb.dma.sign);
            }
            break;
        case 0x41:	/* Set Output Samplerate */
        case 0x42:	/* Set Input Samplerate */
            if (DSP_SB16_ONLY()) break;
            sb.freq=(sb.dsp.in.data[0] << 8)  | sb.dsp.in.data[1];
            break;
        case 0x48:	/* Set DMA Block Size */
            if (DSP_SB2_ABOVE()) break;
            //TODO Maybe check limit for new irq?
            sb.dma.total=1+sb.dsp.in.data[0]+(sb.dsp.in.data[1] << 8);
            break;
        case 0x75:	/* 075h : Single Cycle 4-bit ADPCM Reference */
            sb.adpcm.haveref=true;
        case 0x74:	/* 074h : Single Cycle 4-bit ADPCM */
            DSP_PrepareDMA_Old(DSP_DMA_4,false,false);
            break;
        case 0x77:	/* 077h : Single Cycle 3-bit(2.6bit) ADPCM Reference*/
            sb.adpcm.haveref=true;
        case 0x76:  /* 074h : Single Cycle 3-bit(2.6bit) ADPCM */
            DSP_PrepareDMA_Old(DSP_DMA_3,false,false);
            break;
        case 0x7d:	/* Auto Init 4-bit ADPCM Reference */
		    if (DSP_SB2_ABOVE()) break;
		    sb.adpcm.haveref=true;
		    DSP_PrepareDMA_Old(DSP_DMA_4,true,false);
		    break;
        case 0x17:	/* 017h : Single Cycle 2-bit ADPCM Reference*/
            sb.adpcm.haveref=true;
        case 0x16:  /* 074h : Single Cycle 2-bit ADPCM */
            DSP_PrepareDMA_Old(DSP_DMA_2,false,false);
            break;
        case 0x80:	/* Silence DAC */
            //Pic.PIC_AddEvent(DSP_RaiseIRQEvent,
            //    (1000.0f*(1+sb.dsp.in.data[0]+(sb.dsp.in.data[1] << 8))/sb.freq));
            SB_RaiseIRQ(SB_IRQ_8);
            break;
        case 0xb0:	case 0xb1:	case 0xb2:	case 0xb3:  case 0xb4:	case 0xb5:	case 0xb6:	case 0xb7:
        case 0xb8:	case 0xb9:	case 0xba:	case 0xbb:  case 0xbc:	case 0xbd:	case 0xbe:	case 0xbf:
        case 0xc0:	case 0xc1:	case 0xc2:	case 0xc3:  case 0xc4:	case 0xc5:	case 0xc6:	case 0xc7:
        case 0xc8:	case 0xc9:	case 0xca:	case 0xcb:  case 0xcc:	case 0xcd:	case 0xce:	case 0xcf:
            if (DSP_SB16_ONLY()) break;
            /* Generic 8/16 bit DMA */
//		DSP_SetSpeaker(true);		//SB16 always has speaker enabled
            sb.dma.sign=(sb.dsp.in.data[0] & 0x10) > 0;
            DSP_PrepareDMA_New((sb.dsp.cmd & 0x10)!=0 ? DSP_DMA_16 : DSP_DMA_8,
                1+sb.dsp.in.data[1]+(sb.dsp.in.data[2] << 8),
                (sb.dsp.cmd & 0x4)>0,
                (sb.dsp.in.data[0] & 0x20) > 0
            );
            break;
        case 0xd5:	/* Halt 16-bit DMA */
            if (DSP_SB16_ONLY()) break;
        case 0xd0:	/* Halt 8-bit DMA */
//		DSP_ChangeMode(MODE_NONE);
//		Games sometimes already program a new dma before stopping, gives noise
            if (sb.mode==MODE_NONE) {
			    // possibly different code here that does not switch to MODE_DMA_PAUSE
		    }
            sb.mode=MODE_DMA_PAUSE;
            //Pic.PIC_RemoveEvents(END_DMA_Event);
            break;
        case 0xd1:	/* Enable Speaker */
            DSP_SetSpeaker(true);
            break;
        case 0xd3:	/* Disable Speaker */
            DSP_SetSpeaker(false);
            break;
        case 0xd8:  /* Speaker status */
            if (DSP_SB2_ABOVE()) break;
            DSP_FlushData();
            if (sb.speaker) DSP_AddData(0xff);
            else DSP_AddData(0x00);
            break;
        case 0xd6:	/* Continue DMA 16-bit */
            if (DSP_SB16_ONLY()) break;
        case 0xd4:	/* Continue DMA 8-bit*/
            if (sb.mode==MODE_DMA_PAUSE) {
                sb.mode=MODE_DMA_MASKED;
                if (sb.dma.chan!=null) //sb.dma.chan.Register_Callback(DSP_DMA_CallBack);
                    sb.dma.chan.registerEventHandler(DSP_DMA_CallBack);
            }
            break;
        case 0xd9:  /* Exit Autoinitialize 16-bit */
            if (DSP_SB16_ONLY()) break;
        case 0xda:	/* Exit Autoinitialize 8-bit */
            if (DSP_SB2_ABOVE()) break;
            /* Set mode to single transfer so it ends with current block */
            sb.dma.autoinit=false;		//Should stop itself
            break;
        case 0xe0:	/* DSP Identification - SB2.0+ */
            DSP_FlushData();
            DSP_AddData(~sb.dsp.in.data[0]);
            break;
        case 0xe1:	/* Get DSP Version */
            DSP_FlushData();
            switch (sb.type) {
            case SBT_1:
                DSP_AddData(0x1);DSP_AddData(0x05);break;
            case SBT_2:
                DSP_AddData(0x2);DSP_AddData(0x1);break;
            case SBT_PRO1:
                DSP_AddData(0x3);DSP_AddData(0x0);break;
            case SBT_PRO2:
                DSP_AddData(0x3);DSP_AddData(0x2);break;
            case SBT_16:
                DSP_AddData(0x4);DSP_AddData(0x5);break;
            default:
                break;
            }
            break;
        case 0xe2:	/* Weird DMA identification write routine */
            {
                Log.log(Level.INFO,"DSP Function 0xe2");
                for (/*Bitu*/int i = 0; i < 8; i++)
                    if (((sb.dsp.in.data[0] >> i) & 0x01)!=0) sb.e2.value += E2_incr_table[sb.e2.count % 4][i];
                sb.e2.value += E2_incr_table[sb.e2.count % 4][8];
                sb.e2.count++;
                //DMA.GetDMAChannel(sb.hw.dma8).Register_Callback(DSP_E2_DMA_CallBack);
                dma.registerChannel(sb.hw.dma8, DSP_E2_DMA_CallBack);
            }
            break;
        case 0xe3:	/* DSP Copyright */
            {
                DSP_FlushData();
                byte[] b = copyright_string.getBytes();
                for (int i=0;i<b.length;i++) {
                    DSP_AddData(b[i]);
                }
                DSP_AddData(0);
            }
            break;
        case 0xe4:	/* Write Test Register */
            sb.dsp.test_register=sb.dsp.in.data[0];
            break;
        case 0xe8:	/* Read Test Register */
            DSP_FlushData();
            DSP_AddData(sb.dsp.test_register);
            break;
        case 0xf2:	/* Trigger 8bit IRQ */
            SB_RaiseIRQ(SB_IRQ_8);
            break;
        case 0xf3:   /* Trigger 16bit IRQ */
		    DSP_SB16_ONLY();
		    SB_RaiseIRQ(SB_IRQ_16);
		    break;
        case 0xf8:  /* Undocumented, pre-SB16 only */
            DSP_FlushData();
            DSP_AddData(0);
            break;
        case 0x30: case 0x31:
            Log.log(Level.SEVERE,"DSP:Unimplemented MIDI I/O command "+Integer.toString(sb.dsp.cmd,16));
            break;
        case 0x34: case 0x35: case 0x36: case 0x37:
            if (DSP_SB2_ABOVE()) break;
            Log.log(Level.SEVERE,"DSP:Unimplemented MIDI UART command "+Integer.toString(sb.dsp.cmd,16));
            break;
        case 0x7f: case 0x1f:
            if (DSP_SB2_ABOVE()) break;
            Log.log(Level.SEVERE,"DSP:Unimplemented auto-init DMA ADPCM command "+Integer.toString(sb.dsp.cmd,16));
            break;
        case 0x20:
            DSP_AddData(0x7f);   // fake silent input for Creative parrot
            break;
        case 0x2c:
        case 0x98: case 0x99: /* Documented only for DSP 2.x and 3.x */
        case 0xa0: case 0xa8: /* Documented only for DSP 3.x */
            Log.log(Level.SEVERE,"DSP:Unimplemented input command "+Integer.toString(sb.dsp.cmd,16));
            break;
        case 0xf9:	/* SB16 ASP ??? */
            if (sb.type == SBT_16) {
                Log.log(Level.SEVERE,"SB16 ASP unknown function "+Integer.toString(sb.dsp.in.data[0],16));
                // just feed it what it expects
                switch (sb.dsp.in.data[0]) {
                case 0x0b:
                    DSP_AddData(0x00);
                    break;
                case 0x0e:
                    DSP_AddData(0xff);
                    break;
                case 0x0f:
                    DSP_AddData(0x07);
                    break;
                case 0x23:
                    DSP_AddData(0x00);
                    break;
                case 0x24:
                    DSP_AddData(0x00);
                    break;
                case 0x2b:
                    DSP_AddData(0x00);
                    break;
                case 0x2c:
                    DSP_AddData(0x00);
                    break;
                case 0x2d:
                    DSP_AddData(0x00);
                    break;
                case 0x37:
                    DSP_AddData(0x38);
                    break;
                default:
                    DSP_AddData(0x00);
                    break;
                }
            } else {
                Log.log(Level.SEVERE,"SB16 ASP unknown function "+Integer.toString(sb.dsp.cmd,16));
            }
            break;
        default:
            Log.log(Level.SEVERE,"DSP:Unhandled (undocumented) command "+Integer.toString(sb.dsp.cmd,16));
            break;
        }
        sb.dsp.cmd=DSP_NO_COMMAND;
        sb.dsp.cmd_len=0;
        sb.dsp.in.pos=0;
    }

    private static void DSP_DoWrite(/*Bit8u*/short val) {
        switch (sb.dsp.cmd) {
        case DSP_NO_COMMAND:
            sb.dsp.cmd=val;
            if (sb.type == SBT_16) sb.dsp.cmd_len=DSP_cmd_len_sb16[val];
            else sb.dsp.cmd_len=DSP_cmd_len_sb[val];
            sb.dsp.in.pos=0;
            if (sb.dsp.cmd_len==0) DSP_DoCommand();
            break;
        default:
            sb.dsp.in.data[sb.dsp.in.pos]=val;
            sb.dsp.in.pos++;
            if (sb.dsp.in.pos>=sb.dsp.cmd_len)
                DSP_DoCommand();
        }
    }

    private static /*Bit8u*/short DSP_ReadData() {
/* Static so it repeats the last value on succesive reads (JANGLE DEMO) */
        if (sb.dsp.out.used!=0) {
            sb.dsp.out.lastval=sb.dsp.out.data[sb.dsp.out.pos];
            sb.dsp.out.pos++;
            if (sb.dsp.out.pos>=DSP_BUFSIZE) sb.dsp.out.pos-=DSP_BUFSIZE;
            sb.dsp.out.used--;
        }
        return sb.dsp.out.lastval;
    }

//The soundblaster manual says 2.0 Db steps but we'll go for a bit less
    static private float CALCVOL(float _VAL) { return (float)Math.pow(10.0f,((float)(31-_VAL)*-1.3f)/20);}
    private static void CTMIXER_UpdateVolumes() {
        if (!sb.mixer.enabled) return;
        Mixer.MixerChannel chan;
        //adjust to get linear master volume slider in trackers
        chan=Mixer.MIXER_FindChannel("SB");
        if (chan!=null) chan.SetVolume((float)(sb.mixer.master[0])/31.0f*CALCVOL(sb.mixer.dac[0]),
                                  (float)(sb.mixer.master[1])/31.0f*CALCVOL(sb.mixer.dac[1]));
        chan=Mixer.MIXER_FindChannel("FM");
        if (chan!=null) chan.SetVolume((float)(sb.mixer.master[0])/31.0f*CALCVOL(sb.mixer.fm[0]),
                                  (float)(sb.mixer.master[1])/31.0f*CALCVOL(sb.mixer.fm[1]));
        chan=Mixer.MIXER_FindChannel("CDAUDIO");
	    if (chan!=null) chan.SetVolume((float)(sb.mixer.master[0])/31.0f*CALCVOL(sb.mixer.cda[0]),
							  (float)(sb.mixer.master[1])/31.0f*CALCVOL(sb.mixer.cda[1]));
    }

    private static void CTMIXER_Reset() {
        sb.mixer.fm[0]=
        sb.mixer.fm[1]=
        sb.mixer.cda[0]=
	    sb.mixer.cda[1]=
        sb.mixer.dac[0]=
        sb.mixer.dac[1]=31;
        sb.mixer.master[0]=
        sb.mixer.master[1]=31;
        CTMIXER_UpdateVolumes();
    }

    static private void SETPROVOL(short[] _WHICH_,int _VAL_) {
        _WHICH_[0]=  (short) ((((_VAL_) & 0xf0) >> 3)|(sb.type==SBT_16 ? 1:3));
        _WHICH_[1]=  (short) ((((_VAL_) & 0x0f) << 1)|(sb.type==SBT_16 ? 1:3));
    }

    static private int MAKEPROVOL(short[] _WHICH_) {
        return ((((_WHICH_[0] & 0x1e) << 3) | ((_WHICH_[1] & 0x1e) >> 1)) & (sb.type==SBT_16 ? 0xff:0xee));
    }

    private static void DSP_ChangeStereo(boolean stereo) {
        if (!sb.dma.stereo && stereo) {
            sb.chan.SetFreq(sb.freq/2);
            sb.dma.mul*=2;
            sb.dma.rate=(sb.freq*sb.dma.mul) >> SB_SH;
            sb.dma.min=(sb.dma.rate*3)/1000;
        } else if (sb.dma.stereo && !stereo) {
            sb.chan.SetFreq(sb.freq);
            sb.dma.mul/=2;
            sb.dma.rate=(sb.freq*sb.dma.mul) >> SB_SH;
            sb.dma.min=(sb.dma.rate*3)/1000;
        }
        sb.dma.stereo=stereo;
    }

    private static void CTMIXER_Write(/*Bit8u*/short val) {
        switch (sb.mixer.index) {
        case 0x00:		/* Reset */
            CTMIXER_Reset();
            Log.log(Level.WARNING,"Mixer reset value "+Integer.toString(val,16));
            break;
        case 0x02:		/* Master Volume (SB2 Only) */
            SETPROVOL(sb.mixer.master,(val&0xf)|(val<<4));
            CTMIXER_UpdateVolumes();
            break;
        case 0x04:		/* DAC Volume (SBPRO) */
            SETPROVOL(sb.mixer.dac,val);
            CTMIXER_UpdateVolumes();
            break;
        case 0x06:		/* FM output selection, Somewhat obsolete with dual OPL SBpro + FM volume (SB2 Only) */
            //volume controls both channels
            SETPROVOL(sb.mixer.fm,(val&0xf)|(val<<4));
            CTMIXER_UpdateVolumes();
            if((val&0x60)!=0) Log.log(Level.WARNING,"Turned FM one channel off. not implemented "+Integer.toString(val,16));
            //TODO Change FM Mode if only 1 fm channel is selected
            break;
        case 0x08:		/* CDA Volume (SB2 Only) */
            SETPROVOL(sb.mixer.cda,(val&0xf)|(val<<4));
            CTMIXER_UpdateVolumes();
            break;
        case 0x0a:		/* Mic Level (SBPRO) or DAC Volume (SB2): 2-bit, 3-bit on SB16 */
            if (sb.type==SBT_2) {
                sb.mixer.dac[0]=sb.mixer.dac[1]=(short)(((val & 0x6) << 2)|3);
                CTMIXER_UpdateVolumes();
            } else {
                sb.mixer.mic=(short)(((val & 0x7) << 2)|(sb.type==SBT_16?1:3));
            }
            break;
        case 0x0e:		/* Output/Stereo Select */
            sb.mixer.stereo=(val & 0x2) > 0;
            sb.mixer.filtered=(val & 0x20) > 0;
            DSP_ChangeStereo(sb.mixer.stereo);
            Log.log(Level.WARNING,"Mixer set to "+(sb.dma.stereo ? "STEREO" : "MONO"));
            break;
        case 0x22:		/* Master Volume (SBPRO) */
            SETPROVOL(sb.mixer.master,val);
            CTMIXER_UpdateVolumes();
            break;
        case 0x26:		/* FM Volume (SBPRO) */
            SETPROVOL(sb.mixer.fm,val);
            CTMIXER_UpdateVolumes();
            break;
        case 0x28:		/* CD Audio Volume (SBPRO) */
            SETPROVOL(sb.mixer.cda,val);
            CTMIXER_UpdateVolumes();
            break;
        case 0x2e:		/* Line-in Volume (SBPRO) */
            SETPROVOL(sb.mixer.lin,val);
            break;
        //case 0x20:		/* Master Volume Left (SBPRO) ? */
        case 0x30:		/* Master Volume Left (SB16) */
            if (sb.type==SBT_16) {
                sb.mixer.master[0]=(short)(val>>>3);
                CTMIXER_UpdateVolumes();
            }
            break;
        //case 0x21:		/* Master Volume Right (SBPRO) ? */
        case 0x31:		/* Master Volume Right (SB16) */
            if (sb.type==SBT_16) {
                sb.mixer.master[1]=(short)(val>>>3);
                CTMIXER_UpdateVolumes();
            }
            break;
        case 0x32:		/* DAC Volume Left (SB16) */
            if (sb.type==SBT_16) {
                sb.mixer.dac[0]=(short)(val>>>3);
                CTMIXER_UpdateVolumes();
            }
            break;
        case 0x33:		/* DAC Volume Right (SB16) */
            if (sb.type==SBT_16) {
                sb.mixer.dac[1]=(short)(val>>>3);
                CTMIXER_UpdateVolumes();
            }
            break;
        case 0x34:		/* FM Volume Left (SB16) */
            if (sb.type==SBT_16) {
                sb.mixer.fm[0]=(short)(val>>>3);
                CTMIXER_UpdateVolumes();
            }
                    break;
        case 0x35:		/* FM Volume Right (SB16) */
            if (sb.type==SBT_16) {
                sb.mixer.fm[1]=(short)(val>>>3);
                CTMIXER_UpdateVolumes();
            }
            break;
        case 0x36:		/* CD Volume Left (SB16) */
            if (sb.type==SBT_16) {
                sb.mixer.cda[0]=(short)(val>>>3);
                CTMIXER_UpdateVolumes();
            }
            break;
        case 0x37:		/* CD Volume Right (SB16) */
            if (sb.type==SBT_16) {
                sb.mixer.cda[1]=(short)(val>>>3);
                CTMIXER_UpdateVolumes();
            }
            break;
        case 0x38:		/* Line-in Volume Left (SB16) */
            if (sb.type==SBT_16) sb.mixer.lin[0]=(short)(val>>>3);
            break;
        case 0x39:		/* Line-in Volume Right (SB16) */
            if (sb.type==SBT_16) sb.mixer.lin[1]=(short)(val>>>3);
            break;
        case 0x3a:
            if (sb.type==SBT_16) sb.mixer.mic=(short)(val>>>3);
            break;
        case 0x80:		/* IRQ Select */
            sb.hw.irq=0xff;
            if ((val & 0x1)!=0) sb.hw.irq=2;
            else if ((val & 0x2)!=0) sb.hw.irq=5;
            else if ((val & 0x4)!=0) sb.hw.irq=7;
            else if ((val & 0x8)!=0) sb.hw.irq=10;
            break;
        case 0x81:		/* DMA Select */
            sb.hw.dma8=0xff;
            sb.hw.dma16=0xff;
            if ((val & 0x1)!=0) sb.hw.dma8=0;
            else if ((val & 0x2)!=0) sb.hw.dma8=1;
            else if ((val & 0x8)!=0) sb.hw.dma8=3;
            if ((val & 0x20)!=0) sb.hw.dma16=5;
            else if ((val & 0x40)!=0) sb.hw.dma16=6;
            else if ((val & 0x80)!=0) sb.hw.dma16=7;
            Log.log(Level.INFO,"Mixer select dma8:"+Integer.toString(sb.hw.dma8,16)+" dma16:"+Integer.toString(sb.hw.dma16,16));
            break;
        default:

            if(	((sb.type == SBT_PRO1 || sb.type == SBT_PRO2) && sb.mixer.index==0x0c) || /* Input control on SBPro */
                 (sb.type == SBT_16 && sb.mixer.index >= 0x3b && sb.mixer.index <= 0x47)) /* New SB16 registers */
                sb.mixer.unhandled[sb.mixer.index] = val;
            Log.log(Level.WARNING,"MIXER:Write "+Integer.toString(val, 16)+" to unhandled index "+Integer.toString(sb.mixer.index,16));
        }
    }

    private static /*Bit8u*/int CTMIXER_Read() {
        /*Bit8u*/short ret;
//	if ( sb.mixer.index< 0x80) LOG_MSG("Read mixer %x",sb.mixer.index);
        switch (sb.mixer.index) {
        case 0x00:		/* RESET */
            return 0x00;
        case 0x02:		/* Master Volume (SB2 Only) */
            return ((sb.mixer.master[1]>>>1) & 0xe);
        case 0x22:		/* Master Volume (SBPRO) */
            return	MAKEPROVOL(sb.mixer.master);
        case 0x04:		/* DAC Volume (SBPRO) */
            return MAKEPROVOL(sb.mixer.dac);
        case 0x06:		/* FM Volume (SB2 Only) + FM output selection */
            return ((sb.mixer.fm[1]>>>1) & 0xe);
        case 0x08:		/* CD Volume (SB2 Only) */
            return ((sb.mixer.cda[1]>>>1) & 0xe);
        case 0x0a:		/* Mic Level (SBPRO) or Voice (SB2 Only) */
            if (sb.type==SBT_2) return (sb.mixer.dac[0]>>>2);
            else return ((sb.mixer.mic >>> 2) & (sb.type==SBT_16 ? 7:6));
        case 0x0e:		/* Output/Stereo Select */
            return 0x11|(sb.mixer.stereo ? 0x02 : 0x00)|(sb.mixer.filtered ? 0x20 : 0x00);
        case 0x26:		/* FM Volume (SBPRO) */
            return MAKEPROVOL(sb.mixer.fm);
        case 0x28:		/* CD Audio Volume (SBPRO) */
            return MAKEPROVOL(sb.mixer.cda);
        case 0x2e:		/* Line-IN Volume (SBPRO) */
            return MAKEPROVOL(sb.mixer.lin);
        case 0x30:		/* Master Volume Left (SB16) */
            if (sb.type==SBT_16) return sb.mixer.master[0]<<3;
            ret=0xa;
            break;
        case 0x31:		/* Master Volume Right (S16) */
            if (sb.type==SBT_16) return sb.mixer.master[1]<<3;
            ret=0xa;
            break;
        case 0x32:		/* DAC Volume Left (SB16) */
            if (sb.type==SBT_16) return sb.mixer.dac[0]<<3;
            ret=0xa;
            break;
        case 0x33:		/* DAC Volume Right (SB16) */
            if (sb.type==SBT_16) return sb.mixer.dac[1]<<3;
            ret=0xa;
            break;
        case 0x34:		/* FM Volume Left (SB16) */
            if (sb.type==SBT_16) return sb.mixer.fm[0]<<3;
            ret=0xa;
            break;
        case 0x35:		/* FM Volume Right (SB16) */
            if (sb.type==SBT_16) return sb.mixer.fm[1]<<3;
            ret=0xa;
            break;
        case 0x36:		/* CD Volume Left (SB16) */
            if (sb.type==SBT_16) return sb.mixer.cda[0]<<3;
            ret=0xa;
            break;
        case 0x37:		/* CD Volume Right (SB16) */
            if (sb.type==SBT_16) return sb.mixer.cda[1]<<3;
            ret=0xa;
            break;
        case 0x38:		/* Line-in Volume Left (SB16) */
            if (sb.type==SBT_16) return sb.mixer.lin[0]<<3;
            ret=0xa;
            break;
        case 0x39:		/* Line-in Volume Right (SB16) */
            if (sb.type==SBT_16) return sb.mixer.lin[1]<<3;
            ret=0xa;
            break;
        case 0x3a:		/* Mic Volume (SB16) */
            if (sb.type==SBT_16) return sb.mixer.mic<<3;
            ret=0xa;
            break;
        case 0x80:		/* IRQ Select */
            switch (sb.hw.irq) {
            case 2:  return 0x1;
            case 5:  return 0x2;
            case 7:  return 0x4;
            case 10: return 0x8;
            }
        case 0x81:		/* DMA Select */
            ret=0;
            switch (sb.hw.dma8) {
            case 0:ret|=0x1;break;
            case 1:ret|=0x2;break;
            case 3:ret|=0x8;break;
            }
            switch (sb.hw.dma16) {
            case 5:ret|=0x20;break;
            case 6:ret|=0x40;break;
            case 7:ret|=0x80;break;
            }
            return ret;
        case 0x82:		/* IRQ Status */
            return	(sb.irq.pending_8bit ? 0x1 : 0) |
                    (sb.irq.pending_16bit ? 0x2 : 0) |
                    ((sb.type == SBT_16) ? 0x20 : 0);
        default:
            if (	((sb.type == SBT_PRO1 || sb.type == SBT_PRO2) && sb.mixer.index==0x0c) || /* Input control on SBPro */
                (sb.type == SBT_16 && sb.mixer.index >= 0x3b && sb.mixer.index <= 0x47)) /* New SB16 registers */
                ret = sb.mixer.unhandled[sb.mixer.index];
            else
                ret=0xa;
            Log.log(Level.WARNING,"MIXER:Read from unhandled index "+Integer.toString(sb.mixer.index,16));
        }
        return ret;
    }

//    private static IoHandler.IO_ReadHandler read_sb = new IoHandler.IO_ReadHandler() {
//        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
//            switch (port-sb.hw.base) {
//            case MIXER_INDEX:
//                return sb.mixer.index;
//            case MIXER_DATA:
//                return CTMIXER_Read();
//            case DSP_READ_DATA:
//                return DSP_ReadData();
//            case DSP_READ_STATUS:
//                //TODO See for high speed dma :)
//                if (sb.irq.pending_8bit)  {
//                    sb.irq.pending_8bit=false;
//                    //Pic.PIC_DeActivateIRQ(sb.hw.irq);
//                    irqDevice.setIRQ(sb.hw.irq, 0);
//                }
//                if (sb.dsp.out.used!=0) return 0xff;
//                else return 0x7f;
//            case DSP_ACK_16BIT:
//                sb.irq.pending_16bit=false;
//                break;
//            case DSP_WRITE_STATUS:
//                switch (sb.dsp.state) {
//                case DSP_S_NORMAL:
//                    sb.dsp.write_busy++;
//                    if ((sb.dsp.write_busy & 8)!=0) return 0xff;
//                    return 0x7f;
//                case DSP_S_RESET:
//                case DSP_S_RESET_WAIT:
//                    return 0xff;
//                }
//                return 0xff;
//            case DSP_RESET:
//                return 0xff;
//            default:
//                Log.log(Level.SEVERE,"Unhandled read from SB Port "+Integer.toString(port, 16));
//                break;
//            }
//            return 0xff;
//        }
//    };

//    private static IoHandler.IO_WriteHandler write_sb = new IoHandler.IO_WriteHandler() {
//        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
//            /*Bit8u*/short val8=(/*Bit8u*/short)(val&0xff);
//            switch (port-sb.hw.base) {
//            case DSP_RESET:
//                DSP_DoReset(val8);
//                break;
//            case DSP_WRITE_DATA:
//                DSP_DoWrite(val8);
//                break;
//            case MIXER_INDEX:
//                sb.mixer.index=val8;
//                break;
//            case MIXER_DATA:
//                CTMIXER_Write(val8);
//                break;
//            default:
//                Log.log(Level.SEVERE,"Unhandled write to SB Port "+Integer.toString(port,16));
//                break;
//            }
//        }
//    };

//    private static IoHandler.IO_WriteHandler adlib_gusforward = new IoHandler.IO_WriteHandler() {
//        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
//            //Gus.adlib_commandreg=(/*Bit8u*/short)(val&0xff);
//        }
//    };

    public static boolean SB_Get_Address(/*Bitu*/IntRef sbaddr, /*Bitu*/IntRef sbirq, /*Bitu*/IntRef sbdma) {
        sbaddr.value = 0;
        sbirq.value = 0;
        sbdma.value = 0;
        if (sb.type == SBT_NONE) return false;
        else {
            sbaddr.value = sb.hw.base;
            sbirq.value = sb.hw.irq;
            sbdma.value = sb.hw.dma8;
            return true;
        }
    }

    private static Mixer.MIXER_Handler SBLASTER_CallBack = new Mixer.MIXER_Handler() {
        public void call(/*Bitu*/int len) {
            switch (sb.mode) {
            case MODE_NONE:
            case MODE_DMA_PAUSE:
            case MODE_DMA_MASKED:
                sb.chan.AddSilence();
                break;
            case MODE_DAC:
    //		GenerateDACSound(len);
    //		break;
                if (sb.dac.used==0) {
                    sb.mode=MODE_NONE;
                    return;
                }
                sb.chan.AddStretched(sb.dac.used,sb.dac.data);
                sb.dac.used=0;
                break;
            case MODE_DMA:
                len*=sb.dma.mul;
                if ((len&SB_SH_MASK)!=0) len+=1 << SB_SH;
                len>>=SB_SH;
                if (len>sb.dma.left) len=sb.dma.left;
                GenerateDMASound(len);
                break;
            }
        }
    };

    /* Data */
//    private IoHandler.IO_ReadHandleObject[] ReadHandler = new IoHandler.IO_ReadHandleObject[0x10];
//    private IoHandler.IO_WriteHandleObject[] WriteHandler = new IoHandler.IO_WriteHandleObject[0x10];
    private Mixer.MixerObject MixerChan = new Mixer.MixerObject();

    /* Support Functions */
    private void Find_Type_And_Opl(/*SB_TYPES*/IntRef type, /*OPL_Mode*/IntRef opl_mode){
        String sbtype = Option.sbtype.value("sb16");
        if (sbtype.equalsIgnoreCase("sb1")) type.value=SBT_1;
        else if (sbtype.equalsIgnoreCase("sb2")) type.value=SBT_2;
        else if (sbtype.equalsIgnoreCase("sbpro1")) type.value=SBT_PRO1;
        else if (sbtype.equalsIgnoreCase("sbpro2")) type.value=SBT_PRO2;
        else if (sbtype.equalsIgnoreCase("sb16")) type.value=SBT_16;
        else if (sbtype.equalsIgnoreCase("gb")) type.value=SBT_GB;
        else if (sbtype.equalsIgnoreCase("none")) type.value=SBT_NONE;
        else type.value=SBT_16;

        if (type.value==SBT_16) {
            //if ((!Dosbox.IS_EGAVGA_ARCH()) || !DMA.SecondDMAControllerAvailable()) type.value=SBT_PRO2;

        }

        /* OPL/CMS Init */
        switch (type.value) {
            case SBT_NONE:
                opl_mode.value = 0;
                break;
            case SBT_GB:
                opl_mode.value=1;
                break;
            case SBT_1:
            case SBT_2:
                opl_mode.value=2;
                break;
            case SBT_PRO1:
                opl_mode.value=3;
                break;
            case SBT_PRO2:
            case SBT_16:
                opl_mode.value=4;
                break;
        }
    }

    private void close() {
        switch (oplmode) {
        case 0:
            break;
        case 1:
            //Gameblaster.CMS_ShutDown(m_configuration);
            break;
        case 2:
            //Gameblaster.CMS_ShutDown(m_configuration);
            // fall-through
        case 3:
        case 4:
            //Adlib.OPL_ShutDown(m_configuration);
            break;
        }
        if (sb.type==SBT_NONE || sb.type==SBT_GB) return;
        DSP_Reset(); // Stop everything
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
        switch (port-sb.hw.base) {
            case MIXER_INDEX:
                return sb.mixer.index;
            case MIXER_DATA:
                return CTMIXER_Read();
            case DSP_READ_DATA:
                return DSP_ReadData();
            case DSP_READ_STATUS:
                //TODO See for high speed dma :)
                if (sb.irq.pending_8bit)  {
                    sb.irq.pending_8bit=false;
                    //Pic.PIC_DeActivateIRQ(sb.hw.irq);
                    irqDevice.setIRQ(sb.hw.irq, 0);
                }
                if (sb.dsp.out.used!=0) return 0xff;
                else return 0x7f;
            case DSP_ACK_16BIT:
                sb.irq.pending_16bit=false;
                break;
            case DSP_WRITE_STATUS:
                switch (sb.dsp.state) {
                    case DSP_S_NORMAL:
                        sb.dsp.write_busy++;
                        if ((sb.dsp.write_busy & 8)!=0) return 0xff;
                        return 0x7f;
                    case DSP_S_RESET:
                    case DSP_S_RESET_WAIT:
                        return 0xff;
                }
                return 0xff;
            case DSP_RESET:
                return 0xff;
            default:
                Log.log(Level.SEVERE,"Unhandled read from SB Port "+Integer.toString(port, 16));
                break;
        }
        return 0xff;
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
        /*Bit8u*/short val8=(/*Bit8u*/short)(data & 0xff);
            switch (port-sb.hw.base) {
            case DSP_RESET:
                DSP_DoReset(val8);
                break;
            case DSP_WRITE_DATA:
                DSP_DoWrite(val8);
                break;
            case MIXER_INDEX:
                sb.mixer.index=val8;
                break;
            case MIXER_DATA:
                CTMIXER_Write(val8);
                break;
            default:
                Log.log(Level.SEVERE,"Unhandled write to SB Port "+Integer.toString(port,16));
                break;
            }
    }

    public int[] ioPortsRequested()
    {
        int base = sb.hw.base;
        int[] ports = new int[16-4-2];
        int index = 0;
        for (int i = 4; i < 16; i++)
        {
            if ((i==8) || (i==9))
                continue;
            ports[index++] = base+i;
        }
        return ports;
    }

    private static SBlaster test;

    public static void SBLASTER_ShutDown() {
        test.close();
        test = null;
        sb = null;
    }

    public SBlaster()
    {
        sb = new SB_INFO();
        /*Bitu*/int i;

//        for (i=0;i<WriteHandler.length;i++) {
//            WriteHandler[i] = new IoHandler.IO_WriteHandleObject();
//        }
//        for (i=0;i<ReadHandler.length;i++) {
//            ReadHandler[i] = new IoHandler.IO_ReadHandleObject();
//        }
        sb.hw.base = Option.sbbase.intValue(BASE, 16);
        sb.hw.irq = Option.sb_irq.intValue(IRQ);
        /*Bitu*/int dma8bit = Option.sb_dma.intValue(DMA);
        if (dma8bit>0xff) dma8bit=0xff;
        sb.hw.dma8=(/*Bit8u*/short)(dma8bit&0xff);
        /*Bitu*/int dma16bit = Option.sb_hdma.intValue(HDMA);
        if (dma16bit>0xff) dma16bit=0xff;
        sb.hw.dma16=(/*Bit8u*/short)(dma16bit&0xff);

        sb.mixer.enabled = Option.sbmixer.isSet();
        sb.mixer.stereo=false;

        IntRef t = new IntRef(sb.type);
        IntRef o = new IntRef(oplmode);
        Find_Type_And_Opl(t,o);
        sb.type = t.value;
        oplmode = o.value;

        switch (oplmode) {
        case 0:
            //WriteHandler[0].Install(0x388,adlib_gusforward,IoHandler.IO_MB);
            break;
        case 1:
            //WriteHandler[0].Install(0x388,adlib_gusforward,IoHandler.IO_MB);
            //Gameblaster.CMS_Init(section);
            break;
        case 2:
            //Gameblaster.CMS_Init(section);
            // fall-through
        case 3:
        case 4:
            //Adlib.OPL_Init(oplmode);
            break;
        }
        if (sb.type==SBT_NONE || sb.type==SBT_GB) return;

        sb.chan=MixerChan.Install(SBLASTER_CallBack,22050,"SB");
        sb.dsp.state=DSP_S_NORMAL;
        sb.dsp.out.lastval=0xaa;
        //sb.dma.chan=null;

//        for (i=4;i<=0xf;i++) {
//            if (i==8 || i==9) continue;
//            //Disable mixer ports for lower soundblaster
//            if ((sb.type==SBT_1 || sb.type==SBT_2) && (i==4 || i==5)) continue;
//            ReadHandler[i].Install(sb.hw.base+i,read_sb,IoHandler.IO_MB);
//            WriteHandler[i].Install(sb.hw.base+i,write_sb,IoHandler.IO_MB);
//        }
        for (i=0;i<256;i++) ASP_regs[i] = 0;
        ASP_regs[5] = 0x01;
        ASP_regs[9] = 0xf8;


        CTMIXER_Reset();

        // The documentation does not specify if SB gets initialized with the speaker enabled
        // or disabled. Real SBPro2 has it disabled.
        sb.speaker=false;
        // On SB16 the speaker flag does not affect actual speaker state.
        if (sb.type == SBT_16) sb.chan.Enable(true);
        else sb.chan.Enable(false);

//        String line = String.format("SET BLASTER=A%3x I%d D%d",new Object[]{new Integer(sb.hw.base),new Integer(sb.hw.irq), new Integer(sb.hw.dma8)});
//        if (sb.type==SBT_16) line+= " H"+ sb.hw.dma16;
//        line+=" T"+sb.type;
//        autoexecline.Install(line);

        /* Soundblaster midi interface */
        if (!Midi.MIDI_Available()) sb.midi = false;
        else sb.midi = true;
    }

    public boolean initialised()
    {
        return (irqDevice != null) && (timeSource != null) && (dma != null) && ioportRegistered;
    }

    public boolean updated()
    {
        return irqDevice.updated() && timeSource.updated() && dma.updated();
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

        if ((component instanceof DMAController) && component.initialised())
            if (((DMAController) component).isPrimary())
            {
                dma = (DMAController) component;
                //dma.registerChannel(sb.hw.dma8 & 3, DSP_DMA_CallBack);
                //dma.registerChannel(sb.hw.dma16 & 3, this);
            }

        if (this.initialised())
        {
            DSP_Reset();
        }
    }
}
