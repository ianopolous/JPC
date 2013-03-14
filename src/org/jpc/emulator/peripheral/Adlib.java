package org.jpc.emulator.peripheral;

import org.jpc.emulator.AbstractHardwareComponent;
import org.jpc.emulator.HardwareComponent;
import org.jpc.emulator.motherboard.IODevice;
import org.jpc.emulator.motherboard.IOPortHandler;
import org.jpc.j2se.Option;
import org.jpc.support.Clock;

public class Adlib extends AbstractHardwareComponent implements IODevice
{
    static private final int HW_OPL2 = 0;
    static private final int HW_DUALOPL2 = 1;
    static private final int HW_OPL3 = 2;

    private boolean ioportRegistered;
    private boolean single;
    private Clock timeSource;

    static private class RawHeader {
        /*Bit8u*/byte[] id=new byte[8];				/* 0x00, "DBRAWOPL" */
        /*Bit16u*/int versionHigh;			/* 0x08, size of the data following the m */
        /*Bit16u*/int versionLow;			/* 0x0a, size of the data following the m */
        /*Bit32u*/long commands;			/* 0x0c, Bit32u amount of command/data pairs */
        /*Bit32u*/long milliseconds;		/* 0x10, Bit32u Total milliseconds of data in this chunk */
        /*Bit8u*/short hardware;				/* 0x14, Bit8u Hardware Type 0=opl2,1=dual-opl2,2=opl3 */
        /*Bit8u*/short format;				/* 0x15, Bit8u Format 0=cmd/data interleaved, 1 maybe all cdms, followed by all data */
        /*Bit8u*/short compression;			/* 0x16, Bit8u Compression Type, 0 = No Compression */
        /*Bit8u*/short delay256;				/* 0x17, Bit8u Delay 1-256 msec command */
        /*Bit8u*/short delayShift8;			/* 0x18, Bit8u (delay + 1)*256 */
        /*Bit8u*/short conversionTableSize;	/* 0x191, Bit8u Raw Conversion Table size */
    }

    static final private class Timer {
        double start;
        double delay;
        boolean enabled, overflow, masked;
        /*Bit8u*/short counter;
        Timer() {
            masked = false;
            overflow = false;
            enabled = false;
            counter = 0;
            delay = 0;
        }
        //Call update before making any further changes
        void Update( double time ) {
            if ( !enabled || delay==0 )
                return;
            double deltaStart = time - start;
            //Only set the overflow flag when not masked
            if ( deltaStart >= 0 && !masked ) {
                overflow = true;
            }
        }
        //On a reset make sure the start is in sync with the next cycle
        void Reset(double time ) {
            overflow = false;
            if ( delay==0 || !enabled )
                return;
            double delta = (time - start);
            double rem = delta % delay;
            double next = delay - rem;
            start = time + next;
        }
        void Stop( ) {
            enabled = false;
        }
        void Start(double time, /*Bits*/int scale ) {
            //Don't enable again
            if ( enabled ) {
                return;
            }
            enabled = true;
            delay = 0.001 * (256 - counter ) * scale;
            start = time + delay;
        }

    }

//    static private final class Capture {
//        //127 entries to go from raw data to registers
//        /*Bit8u*/short ToReg[127];
//        //How many entries in the ToPort are used
//        /*Bit8u*/short RawUsed;
//        //256 entries to go from port index to raw data
//        /*Bit8u*/short ToRaw[256];
//        /*Bit8u*/short delay256;
//        /*Bit8u*/short delayShift8;
//        RawHeader header;
//
//        FILE*	handle;				//File used for writing
//        /*Bit32u*/long	startTicks;			//Start used to check total raw length on end
//        /*Bit32u*/long	lastTicks;			//Last ticks when last last cmd was added
//        /*Bit8u*/short	buf[1024];	//16 added for delay commands and what not
//        /*Bit32u*/long	bufUsed;
//        /*Bit8u*/short	cmd[2];				//Last cmd's sent to either ports
//        boolean	doneOpl3;
//        boolean	doneDualOpl2;
//
//        RegisterCache* cache;
//
//        void MakeEntry( /*Bit8u*/short reg, /*Bit8u*/short& raw ) {
//            ToReg[ raw ] = reg;
//            ToRaw[ reg ] = raw;
//            raw++;
//        }
//        void MakeTables( void ) {
//            /*Bit8u*/short index = 0;
//            memset( ToReg, 0xff, sizeof ( ToReg ) );
//            memset( ToRaw, 0xff, sizeof ( ToRaw ) );
//            //Select the entries that are valid and the index is the mapping to the index entry
//            MakeEntry( 0x01, index );					//0x01: Waveform select
//            MakeEntry( 0x04, index );					//104: Four-Operator Enable
//            MakeEntry( 0x05, index );					//105: OPL3 Mode Enable
//            MakeEntry( 0x08, index );					//08: CSW / NOTE-SEL
//            MakeEntry( 0xbd, index );					//BD: Tremolo Depth / Vibrato Depth / Percussion Mode / BD/SD/TT/CY/HH On
//            //Add the 32 byte range that hold the 18 operators
//            for ( int i = 0 ; i < 24; i++ ) {
//                if ( (i & 7) < 6 ) {
//                    MakeEntry(0x20 + i, index );		//20-35: Tremolo / Vibrato / Sustain / KSR / Frequency Multiplication Facto
//                    MakeEntry(0x40 + i, index );		//40-55: Key Scale Level / Output Level
//                    MakeEntry(0x60 + i, index );		//60-75: Attack Rate / Decay Rate
//                    MakeEntry(0x80 + i, index );		//80-95: Sustain Level / Release Rate
//                    MakeEntry(0xe0 + i, index );		//E0-F5: Waveform Select
//                }
//            }
//            //Add the 9 byte range that hold the 9 channels
//            for ( int i = 0 ; i < 9; i++ ) {
//                MakeEntry(0xa0 + i, index );			//A0-A8: Frequency Number
//                MakeEntry(0xb0 + i, index );			//B0-B8: Key On / Block Number / F-Number(hi /*Bits*/int)
//                MakeEntry(0xc0 + i, index );			//C0-C8: FeedBack Modulation Factor / Synthesis Type
//            }
//            //Store the amount of bytes the table contains
//            RawUsed = index;
//    //		assert( RawUsed <= 127 );
//            delay256 = RawUsed;
//            delayShift8 = RawUsed+1;
//        }
//
//        void ClearBuf( void ) {
//            fwrite( buf, 1, bufUsed, handle );
//            header.commands += bufUsed / 2;
//            bufUsed = 0;
//        }
//        void AddBuf( /*Bit8u*/short raw, /*Bit8u*/short val ) {
//            buf[bufUsed++] = raw;
//            buf[bufUsed++] = val;
//            if ( bufUsed >= sizeof( buf ) ) {
//                ClearBuf();
//            }
//        }
//        void AddWrite( /*Bit32u*/long regFull, /*Bit8u*/short val ) {
//            /*Bit8u*/short regMask = regFull & 0xff;
//            /*
//                Do some special checks if we're doing opl3 or dualopl2 commands
//                Although you could pretty much just stick to always doing opl3 on the player side
//            */
//            //Enabling opl3 4op modes will make us go into opl3 mode
//            if ( header.hardware != HW_OPL3 && regFull == 0x104 && val && (*cache)[0x105] ) {
//                header.hardware = HW_OPL3;
//            }
//            //Writing a keyon to a 2nd address enables dual opl2 otherwise
//            //Maybe also check for rhythm
//            if ( header.hardware == HW_OPL2 && regFull >= 0x1b0 && regFull <=0x1b8 && val ) {
//                header.hardware = HW_DUALOPL2;
//            }
//            /*Bit8u*/short raw = ToRaw[ regMask ];
//            if ( raw == 0xff )
//                return;
//            if ( regFull & 0x100 )
//                raw |= 128;
//            AddBuf( raw, val );
//        }
//        void WriteCache( void  ) {
//            /*Bitu*/int i, val;
//            /* Check the registers to add */
//            for (i=0;i<256;i++) {
//                //Skip the note on entries
//                if (i>=0xb0 && i<=0xb8)
//                    continue;
//                val = (*cache)[ i ];
//                if (val) {
//                    AddWrite( i, val );
//                }
//                val = (*cache)[ 0x100 + i ];
//                if (val) {
//                    AddWrite( 0x100 + i, val );
//                }
//            }
//        }
//        void InitHeader( void ) {
//            memset( &header, 0, sizeof( header ) );
//            memcpy( header.id, "DBRAWOPL", 8 );
//            header.versionLow = 0;
//            header.versionHigh = 2;
//            header.delay256 = delay256;
//            header.delayShift8 = delayShift8;
//            header.conversionTableSize = RawUsed;
//        }
//        void CloseFile( void ) {
//            if ( handle ) {
//                ClearBuf();
//                /* Endianize the header and write it to beginning of the file */
//                var_write( &header.versionHigh, header.versionHigh );
//                var_write( &header.versionLow, header.versionLow );
//                var_write( &header.commands, header.commands );
//                var_write( &header.milliseconds, header.milliseconds );
//                fseek( handle, 0, SEEK_SET );
//                fwrite( &header, 1, sizeof( header ), handle );
//                fclose( handle );
//                handle = 0;
//            }
//        }
//    public:
//        boolean DoWrite( /*Bit32u*/long regFull, /*Bit8u*/short val ) {
//            /*Bit8u*/short regMask = regFull & 0xff;
//            //Check the raw index for this register if we actually have to save it
//            if ( handle ) {
//                /*
//                    Check if we actually care for this to be logged, else just ignore it
//                */
//                /*Bit8u*/short raw = ToRaw[ regMask ];
//                if ( raw == 0xff ) {
//                    return true;
//                }
//                /* Check if this command will not just replace the same value
//                   in a reg that doesn't do anything with it
//                */
//                if ( (*cache)[ regFull ] == val )
//                    return true;
//                /* Check how much time has passed */
//                /*Bitu*/int passed = PIC_Ticks - lastTicks;
//                lastTicks = PIC_Ticks;
//                header.milliseconds += passed;
//
//                //if ( passed > 0 ) LOG_MSG( "Delay %d", passed ) ;
//
//                // If we passed more than 30 seconds since the last command, we'll restart the the capture
//                if ( passed > 30000 ) {
//                    CloseFile();
//                    goto skipWrite;
//                }
//                while (passed > 0) {
//                    if (passed < 257) {			//1-256 millisecond delay
//                        AddBuf( delay256, passed - 1 );
//                        passed = 0;
//                    } else {
//                        /*Bitu*/int shift = (passed >> 8);
//                        passed -= shift << 8;
//                        AddBuf( delayShift8, shift - 1 );
//                    }
//                }
//                AddWrite( regFull, val );
//                return true;
//            }
//    skipWrite:
//            //Not yet capturing to a file here
//            //Check for commands that would start capturing, if it's not one of them return
//            if ( !(
//                //note on in any channel
//                ( regMask>=0xb0 && regMask<=0xb8 && (val&0x020) ) ||
//                //Percussion mode enabled and a note on in any percussion instrument
//                ( regMask == 0xbd && ( (val&0x3f) > 0x20 ) )
//            )) {
//                return true;
//            }
//            handle = OpenCaptureFile("Raw Opl",".dro");
//            if (!handle)
//                return false;
//            InitHeader();
//            //Prepare space at start of the file for the header
//            fwrite( &header, 1, sizeof(header), handle );
//            /* write the Raw To Reg table */
//            fwrite( &ToReg, 1, RawUsed, handle );
//            /* Write the cache of last commands */
//            WriteCache( );
//            /* Write the command that triggered this */
//            AddWrite( regFull, val );
//            //Init the timing information for the next commands
//            lastTicks = PIC_Ticks;
//            startTicks = PIC_Ticks;
//            return true;
//        }
//        Capture( RegisterCache* _cache ) {
//            cache = _cache;
//            handle = 0;
//            bufUsed = 0;
//            MakeTables();
//        }
//        ~Capture() {
//            CloseFile();
//        }
//
//    };

    private final class Chip {
        //Last selected register
        Timer[] timer = new Timer[2];
        public Chip() {
            for (int i=0;i<timer.length;i++) {
                timer[i] = new Timer();
            }
        }
        //Check for it being a write to the timer
        boolean Write(/*Bit32u*/int addr, /*Bit8u*/short val) {
            switch ( addr ) {
            case 0x02:
                timer[0].counter = val;
                return true;
            case 0x03:
                timer[1].counter = val;
                return true;
            case 0x04:
                double time = timeSource.getEmulatedNanos();//Pic.PIC_FullIndex();
                if ((val & 0x80)!=0) {
                    timer[0].Reset( time );
                    timer[1].Reset( time );
                } else {
                    timer[0].Update( time );
                    timer[1].Update( time );
                    if ((val & 0x1)!=0) {
                        timer[0].Start( time, 80 );
                    } else {
                        timer[0].Stop( );
                    }
                    timer[0].masked = (val & 0x40) > 0;
                    if ( timer[0].masked )
                        timer[0].overflow = false;
                    if ((val & 0x2)!=0) {
                        timer[1].Start( time, 320 );
                    } else {
                        timer[1].Stop( );
                    }
                    timer[1].masked = (val & 0x20) > 0;
                    if ( timer[1].masked )
                        timer[1].overflow = false;

                }
                return true;
            }
            return false;
        }
        //Read the current timer state, will use current double
        /*Bit8u*/short Read() {
            double time = timeSource.getEmulatedNanos();//Pic.PIC_FullIndex();
            timer[0].Update( time );
            timer[1].Update( time );
            /*Bit8u*/short ret = 0;
            //Overflow won't be set if a channel is masked
            if ( timer[0].overflow ) {
                ret |= 0x40;
                ret |= 0x80;
            }
            if ( timer[1].overflow ) {
                ret |= 0x20;
                ret |= 0x80;
            }
            return ret;
        }
    }

//The type of handler this is
    static final private int MODE_OPL2 = 0;
    static final private int MODE_DUALOPL2 = 1;
    static final private int MODE_OPL3 = 2;

    static public interface Handler {
        //Write an address to a chip, returns the address the chip sets
        public /*Bit32u*/long WriteAddr( /*Bit32u*/int port, /*Bit8u*/short val );
        //Write to a specific register in the chip
        public void WriteReg( /*Bit32u*/int addr, /*Bit8u*/short val );
        //Generate a certain amount of samples
        public void Generate( Mixer.MixerChannel chan, /*Bitu*/int samples );
        //Initialize at a specific sample rate and mode
        public void Init( /*Bitu*/long rate );
    }

//The cache for 2 chips or an opl3
//    typedef /*Bit8u*/short RegisterCache[512];

    private class Module
    {
//        private IoHandler.IO_ReadHandleObject[] ReadHandler = new IoHandler.IO_ReadHandleObject[3];
//        private IoHandler.IO_WriteHandleObject[] WriteHandler = new IoHandler.IO_WriteHandleObject[3];
        private Mixer.MixerObject mixerObject = new Mixer.MixerObject();

        public Module()
        {
//            for (int i=0;i<ReadHandler.length;i++)
//                ReadHandler[i] = new IoHandler.IO_ReadHandleObject();
//            for (int i=0;i<WriteHandler.length;i++)
//                WriteHandler[i] = new IoHandler.IO_WriteHandleObject();
            for (int i=0;i<chip.length;i++) {
                chip[i] = new Chip();
            }
            reg.normal = 0;
//            capture = null;

            /*Bitu*/int base = Option.sbbase.intValue(SBlaster.BASE, 16);
            /*Bitu*/int rate = Option.oplrate.intValue(SBlaster.OPL_RATE);
            //Make sure we can't select lower than 8000 to prevent fixed point issues
            if ( rate < 8000 )
                rate = 8000;
            String oplemu = Option.oplemu.value(SBlaster.OPLEMU);

            mixerChan = mixerObject.Install(OPL_CallBack,rate,"FM");
            mixerChan.SetScale(2.0f);
            if (oplemu.equals("fast")) {
                handler = new DbOPL.Handler();
            } else if (oplemu.equals("compat")) {
                System.out.println("OPLEMU compat not implemented");
//                if ( oplmode == OPL_opl2 ) {
//                    handler = new OPL2::Handler();
//                } else {
//                    handler = new OPL3::Handler();
//                }
                handler = new DbOPL.Handler();
            } else {
                handler = new DbOPL.Handler();
            }
            handler.Init( rate );
            switch (SBlaster.oplmode) {
                case 2:
                    single = true;
                    Init(MODE_OPL2);
                    break;
                case 3:
                    Init(MODE_DUALOPL2);
                    single = false;
                    break;
                case 4:
                    Init(MODE_OPL3);
                    single = false;
                    break;
                default:
                    single = false;
            }
            //0x388 range
//            WriteHandler[0].Install(0x388,OPL_Write,IoHandler.IO_MB, 4 );
//            ReadHandler[0].Install(0x388,OPL_Read,IoHandler.IO_MB, 4 );
//            //0x220 range
//            if ( !single ) {
//                WriteHandler[1].Install(base,OPL_Write,IoHandler.IO_MB, 4 );
//                ReadHandler[1].Install(base,OPL_Read,IoHandler.IO_MB, 4 );
//            }
//            //0x228 range
//            WriteHandler[2].Install(base+8,OPL_Write,IoHandler.IO_MB, 2);
//            ReadHandler[2].Install(base+8,OPL_Read,IoHandler.IO_MB, 1);

//            MAPPER_AddHandler(OPL_SaveRawEvent,MK_f7,MMOD1|MMOD2,"caprawopl","Cap OPL");
        }

                //Mode we're running in
        private int mode;
        //Last selected address in the chip for the different modes
        private class Reg {
            /*Bit32u*/int normal;
            /*Bit8u*/short dual(int index) {
                if (index == 0)
                    return (short)(normal & 0xFF);
                else
                    return (short)((normal >> 8) & 0xFF);
            }
            /*Bit8u*/void dual(int index, int value) {
                if (index == 0) {
                    normal &= 0xFFFFFF00;
                    normal |= value & 0xFF;
                } else {
                    normal &= 0xFFFF00FF;
                    normal |= (value << 8) & 0xFF;
                }
            }
        }

        private Reg reg = new Reg();

        private void CacheWrite( /*Bit32u*/int reg, /*Bit8u*/short val )
        {
            //Store it into the cache
            cache[ reg ] = val;
        }

        private void DualWrite( /*Bit8u*/short index, /*Bit8u*/short reg, /*Bit8u*/short val )
        {
            //Make sure you don't use opl3 features
            //Don't allow write to disable opl3		
            if ( reg == 5 ) {
                return;
            }
            //Only allow 4 waveforms
            if ( reg >= 0xE0 ) {
                val &= 3;
            }
            //Write to the timer?
            if ( chip[index].Write( reg, val ) )
                return;
            //Enabling panning
            if ( reg >= 0xc0 && reg <=0xc8 ) {
                val &= 0x0f;
                val |= index!=0 ? 0xA0 : 0x50;
            }
            /*Bit32u*/int fullReg = reg + (index!=0 ? 0x100 : 0);
            handler.WriteReg( fullReg, val );
            CacheWrite( fullReg, val );
        }
        public Mixer.MixerChannel mixerChan;
        public /*Bit32u*/long lastUsed;				//Ticks when adlib was last used to turn of mixing after a few second

        public Handler handler;				//Handler that will generate the sound
//        public RegisterCache cache;
        public short[] cache = new short[512];
//        public Capture capture;
        public Chip[] chip = new Chip[2];

        //Handle port writes
        public void PortWrite(/*Bitu*/int port, /*Bitu*/short val) {
            //Keep track of last write time
            lastUsed = timeSource.getEmulatedMicros();//Pic.PIC_Ticks;
            //Maybe only enable with a keyon?
            if ( !mixerChan.enabled ) {
                mixerChan.Enable(true);
            }
            if ((port & 1)!=0) {
                switch ( mode ) {
                case MODE_OPL2:
                case MODE_OPL3:
                    if ( !chip[0].Write( reg.normal, val ) ) {
                        handler.WriteReg( reg.normal, val );
                        CacheWrite( reg.normal, val );
                    }
                    break;
                case MODE_DUALOPL2:
                    //Not a 0x??8 port, then write to a specific port
                    if ((port & 0x8)==0) {
                        /*Bit8u*/short index = (short)(( port & 2 ) >> 1);
                        DualWrite( index, reg.dual(index), val );
                    } else {
                        //Write to both ports
                        DualWrite( (short)0, reg.dual(0), val );
                        DualWrite( (short)1, reg.dual(1), val );
                    }
                    break;
                }
            } else {
                //Ask the handler to write the address
                //Make sure to clip them in the right range
                switch ( mode ) {
                case MODE_OPL2:
                    reg.normal = (int)handler.WriteAddr( port, val ) & 0xff;
                    break;
                case MODE_OPL3:
                    reg.normal = (int)handler.WriteAddr( port, val ) & 0x1ff;
                    break;
                case MODE_DUALOPL2:
                    //Not a 0x?88 port, when write to a specific side
                    if ((port & 0x8)==0) {
                        /*Bit8u*/int index = ( port & 2 ) >> 1;
                        reg.dual(index, val & 0xff);
                    } else {
                        reg.dual(0, val & 0xff);
                        reg.dual(1, val & 0xff);
                    }
                    break;
                }
            }
        }

        public /*Bitu*/int PortRead(/*Bitu*/int port)  {
            switch ( mode ) {
            case MODE_OPL2:
                //We allocated 4 ports, so just return -1 for the higher ones
                if ((port & 3)==0) {
                    //Make sure the low /*Bits*/int are 6 on opl2
                    return chip[0].Read() | 0x6;
                } else {
                    return 0xff;
                }
            case MODE_OPL3:
                //We allocated 4 ports, so just return -1 for the higher ones
                if ((port & 3)==0) {
                    return chip[0].Read();
                } else {
                    return 0xff;
                }
            case MODE_DUALOPL2:
                //Only return for the lower ports
                if ((port & 1)!=0) {
                    return 0xff;
                }
                //Make sure the low /*Bits*/int are 6 on opl2
                return chip[ (port >> 1) & 1].Read() | 0x6;
            }
            return 0;
        }
        public void Init(int m) {
            mode = m;
            switch ( mode ) {
            case MODE_OPL3:
            case MODE_OPL2:
                break;
            case MODE_DUALOPL2:
                //Setup opl3 mode in the hander
                handler.WriteReg(0x105, (short)1);
                //Also set it up in the cache so the capturing will start opl3
                CacheWrite(0x105, (short)1);
                break;
            }
        }
    }

    private static Module module = null;

    private final Mixer.MIXER_Handler OPL_CallBack = new Mixer.MIXER_Handler() {
        public void call(/*Bitu*/int len) {
            module.handler.Generate( module.mixerChan, len );
            //Disable the sound generation after 30 seconds of silence
            if ((timeSource.getEmulatedMicros() - module.lastUsed) > 30000000) {
                /*Bitu*/int i;
                for (i=0xb0;i<0xb9;i++) if ((module.cache[i] &0x20)!=0 || (module.cache[i+0x100] & 0x20)!=0) break;
                if (i==0xb9) module.mixerChan.Enable(false);
                else module.lastUsed = timeSource.getEmulatedMicros();//Pic.PIC_Ticks;
            }
        }
    };

    public Adlib()
    {
        // soundblaster must be instantiated first!
        module = new Module();
    }

    public int[] ioPortsRequested()
    {
        int[] ports = new int[6 + (single ? 0:4)];
        int base = Option.sbbase.intValue(SBlaster.BASE, 16);
        int i=0;
        for (;i <4 ; i++)
            ports[i] = 0x388 + i;
        if (!single)
            for (int j=0;j <4 ; j++)
                ports[i++] = base + j;
        ports[i++] = base+8;
        ports[i++] = base+9;
        return ports;
    }

    public void acceptComponent(HardwareComponent component)
    {
        if ((component instanceof Clock) && component.initialised())
            timeSource = (Clock) component;
        if ((component instanceof IOPortHandler) && component.initialised()) {
            ((IOPortHandler) component).registerIOPortCapable(this);
            ioportRegistered = true;
        }
    }

    public boolean initialised()
    {
        return (timeSource != null) && ioportRegistered;
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
        return module.PortRead(port);
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
        module.PortWrite( port, (short)data);
    }
}
