package org.jpc.emulator.peripheral;

public class DbOPL
{
    // :TODO: look for ~ and make sure they generate the mask of the right size

    // Java 1.4 does not have Math.log10
    static private double log10(double d) {
        return Math.log(d) / Math.log(10);
    }
    //Use 8 handlers based on a small logatirmic wavetabe and an exponential table for volume
    static private final int WAVE_HANDLER = 10;
    //Use a logarithmic wavetable with an exponential table for volume
    static private final int WAVE_TABLELOG = 11;
    //Use a linear wavetable with a multiply table for volume
    static private final int WAVE_TABLEMUL = 12;

    //Select the type of wave generator routine
    static private final int DBOPL_WAVE = WAVE_TABLEMUL;

    static private final double OPLRATE = (14318180.0 / 288.0);
    static private final int TREMOLO_TABLE = 52;

    //Try to use most precision for frequencies
    //Else try to keep different waves in synch
    static private final boolean WAVE_PRECISION	= false;

//    #ifndef WAVE_PRECISION
//    //Wave bits available in the top of the 32bit range
//    //Original adlib uses 10.10, we use 10.22
//    static private final int WAVE_BITS	10
//    #else
//    //Need some extra bits at the top to have room for octaves and frequency multiplier
//    //We support to 8 times lower rate
//    //128 * 15 * 8 = 15350, 2^13.9, so need 14 bits
//    static private final int WAVE_BITS	14
//    #endif

    static private final int WAVE_BITS = WAVE_PRECISION?14:10;


    static private final int WAVE_SH = ( 32 - WAVE_BITS );
    static private final int WAVE_MASK = ( ( 1 << WAVE_SH ) - 1 );

    //Use the same accuracy as the waves
    static private final int LFO_SH = ( WAVE_SH - 10 );
    //LFO is controlled by our tremolo 256 sample limit
    static private final int LFO_MAX = ( 256 << ( LFO_SH ) );


    //Maximum amount of attenuation bits
    //Envelope goes to 511, 9 bits
//    #if (DBOPL_WAVE == WAVE_TABLEMUL )
//    //Uses the value directly
//    static private final int ENV_BITS	( 9 )
//    #else
//    //Add 3 bits here for more accuracy and would have to be shifted up either way
//    static private final int ENV_BITS	( 9 )
//    #endif

    static private final int ENV_BITS = 9;

    //Limits of the envelope with those bits and when the envelope goes silent
    static private final int ENV_MIN = 0;
    static private final int ENV_EXTRA = ( ENV_BITS - 9 );
    static private final int ENV_MAX = ( 511 << ENV_EXTRA );
    static private final int ENV_LIMIT = ( ( 12 * 256) >> ( 3 - ENV_EXTRA ) );
    static private boolean ENV_SILENT(int _X_ ) {return ( (_X_) >= ENV_LIMIT ); }

    //Attack/decay/release rate counter shift
    static private final int RATE_SH = 24;
    static private final int RATE_MASK = ( ( 1 << RATE_SH ) - 1 );
    //Has to fit within 16bit lookuptable
    static private final int MUL_SH = 16;

    //Check some ranges
//    #if ENV_EXTRA > 3
//    #error Too many envelope bits
//    #endif


    //How much to substract from the base value for the final attenuation
    static private final /*Bit8u*/byte[] KslCreateTable = {
        //0 will always be be lower than 7 * 8
        64, 32, 24, 19,
        16, 12, 11, 10,
         8,  6,  5,  4,
         3,  2,  1,  0,
    };

    static final private /*Bit8u*/byte[] FreqCreateTable = {
        1, 2, 4, 6, 8, 10, 12, 14,
        16, 18, 20, 20, 24, 24, 30, 30
    };


    //We're not including the highest attack rate, that gets a special value
    static final private /*Bit8u*/byte[] AttackSamplesTable = {
        69, 55, 46, 40,
        35, 29, 23, 20,
        19, 15, 11, 10,
        9
    };
    //On a real opl these values take 8 samples to reach and are based upon larger tables
    static final private /*Bit8u*/byte[] EnvelopeIncreaseTable = {
        4,  5,  6,  7,
        8, 10, 12, 14,
        16, 20, 24, 28,
        32,
    };

    static /*Bit16u*/int[] ExpTable;
    static /*Bit16u*/int[] SinTable;

    static {
        if ( DBOPL_WAVE == WAVE_HANDLER || DBOPL_WAVE == WAVE_TABLELOG)
            ExpTable = new int[256];
        if ( DBOPL_WAVE == WAVE_HANDLER )
            SinTable = new int[512]; //PI table used by WAVEHANDLER
        if ( DBOPL_WAVE > WAVE_HANDLER )
            WaveTable = new short[8*512];
        if ( DBOPL_WAVE == WAVE_TABLEMUL )
            MulTable = new int[384];
    }

//    #if ( DBOPL_WAVE > WAVE_HANDLER )
    //Layout of the waveform table in 512 entry intervals
    //With overlapping waves we reduce the table to half it's size

    //	|    |//\\|____|WAV7|//__|/\  |____|/\/\|
    //	|\\//|    |    |WAV7|    |  \/|    |    |
    //	|06  |0126|17  |7   |3   |4   |4 5 |5   |

    //6 is just 0 shifted and masked

    static private final/*Bit16s*/short[] WaveTable;
    //Distance into WaveTable the wave starts
    static private final /*Bit16u*/short[] WaveBaseTable = {
        0x000, 0x200, 0x200, 0x800,
        0xa00, 0xc00, 0x100, 0x400,

    };
    //Mask the counter with this
    static private final /*Bit16u*/short[] WaveMaskTable = {
        1023, 1023, 511, 511,
        1023, 1023, 512, 1023,
    };

    //Where to start the counter on at keyon
    static private final /*Bit16u*/short[] WaveStartTable = {
        512, 0, 0, 0,
        0, 512, 512, 256,
    };
//    #endif

    //#if ( DBOPL_WAVE == WAVE_TABLEMUL )
    private final static /*Bit16u*/int[] MulTable;
    //#endif

    private final static /*Bit8u*/short[] KslTable = new short[8*16];
    private final static /*Bit8u*/short[] TremoloTable = new short[TREMOLO_TABLE];
    //Start of a channel behind the chip struct start
    private final static /*Bit16u*/int[] ChanOffsetTable = new int[32];
    //Start of an operator behind the chip struct start
    private final static /*Bit16u*/int[] OpOffsetTable = new int[64];

    //The lower bits are the shift of the operator vibrato value
    //The highest bit is right shifted to generate -1 or 0 for negation
    //So taking the highest input value of 7 this gives 3, 7, 3, 0, -3, -7, -3, 0
    static private final /*Bit8s*/byte[] VibratoTable = {
        1 - 0x00, 0 - 0x00, 1 - 0x00, 30 - 0x00,
        1 - 0x80, 0 - 0x80, 1 - 0x80, 30 - 0x80
    };

    //Shift strength for the ksl value determined by ksl strength
    static private final /*Bit8u*/byte[] KslShiftTable = {
        31,1,2,0
    };

//    #if (DBOPL_WAVE == WAVE_HANDLER)
//    typedef /*Bits*/int ( DB_FASTCALL *WaveHandler) ( /*Bitu*/long i, /*Bitu*/long volume );
//    #endif
    static private interface WaveHandler {
        public /*Bits*/int call(int i, int volume);
    }

//    typedef /*Bits*/int ( DBOPL::Operator::*VolumeHandler) ( );
//    static private interface VolumeHandler {
//        public /*Bits*/int call();
//    }

//    typedef Channel* ( DBOPL::Channel::*SynthHandler) ( Chip* chip, /*Bit32u*/long samples, /*Bit32s*/int* output );

    //Different synth modes that can generate blocks of data
    static private final int sm2AM = 0;
    static private final int sm2FM = 1;
    static private final int sm3AM = 2;
    static private final int sm3FM = 3;
    static private final int sm4Start = 4;
    static private final int sm3FMFM = 5;
    static private final int sm3AMFM = 6;
    static private final int sm3FMAM = 7;
    static private final int sm3AMAM = 8;
    static private final int sm6Start = 9;
    static private final int sm2Percussion = 10;
    static private final int sm3Percussion = 11;

    static private int SynthMode;

    //Shifts for the values contained in chandata variable
    static private final int SHIFT_KSLBASE = 16;
    static private final int SHIFT_KEYCODE = 24;

    private static class Operator {
        //Masks for operator 20 values
        static public final int MASK_KSR = 0x10;
        static public final int MASK_SUSTAIN = 0x20;
        static public final int MASK_VIBRATO = 0x40;
        static public final int MASK_TREMOLO = 0x80;

        static public final int OFF = 0;
        static public final int RELEASE = 1;
        static public final int SUSTAIN = 2;
        static public final int DECAY = 3;
        static public final int ATTACK = 4;

        public int State;

        int volHandlerParam;

//    #if (DBOPL_WAVE == WAVE_HANDLER)
        WaveHandler waveHandler;	//Routine that generate a wave
//    #else
        /*Bit16s*/short[] waveBase;
        int waveBaseOff;
        /*Bit32u*/int waveMask;
        /*Bit32u*/long waveStart;
//    #endif
        /*Bit32u*/long waveIndex;			//WAVE_BITS shifted counter of the frequency index
        /*Bit32u*/int waveAdd;				//The base frequency without vibrato
        /*Bit32u*/int waveCurrent;			//waveAdd + vibratao

        /*Bit32u*/int chanData;			//Frequency/octave and derived data coming from whatever channel controls this
        /*Bit32u*/int freqMul;				//Scale channel frequency with this, TODO maybe remove?
        /*Bit32u*/int vibrato;				//Scaled up vibrato strength
        /*Bit32s*/int sustainLevel;		//When stopping at sustain level stop here
        /*Bit32s*/int totalLevel;			//totalLevel is added to every generated volume
        /*Bit32u*/int currentLevel;		//totalLevel + tremolo
        /*Bit32s*/int volume;				//The currently active volume

        /*Bit32u*/long attackAdd;			//Timers for the different states of the envelope
        /*Bit32u*/long decayAdd;
        /*Bit32u*/long releaseAdd;
        /*Bit32u*/long rateIndex;			//Current position of the evenlope

        /*Bit8u*/short rateZero;				///*Bits*/int for the different states of the envelope having no changes
        /*Bit8u*/short keyOn;				//Bitmask of different values that can generate keyon
        //Registers, also used to check for changes
        /*Bit8u*/short reg20, reg40, reg60, reg80, regE0;
        //Active part of the envelope we're in
        /*Bit8u*/short state;
        //0xff when tremolo is enabled
        /*Bit8u*/byte tremoloMask;
        //Strength of the vibrato
        /*Bit8u*/short vibStrength;
        //Keep track of the calculated KSR so we can check for changes
        /*Bit8u*/short ksr;

        private void SetState(/*Bit8u*/int s) {
            state = (short)s;
            volHandlerParam = s;
        }
        //We zero out when rate == 0
        private void UpdateAttack(Chip chip) {
            /*Bit8u*/int rate = reg60 >> 4;
            if (rate!=0) {
                /*Bit8u*/int val = (rate << 2) + ksr;
                attackAdd = chip.attackRates[val];
                rateZero &= ~(1 << ATTACK);
            } else {
                attackAdd = 0;
                rateZero |= (1 << ATTACK);
            }
        }

        private void UpdateRelease(Chip chip ) {
            /*Bit8u*/int rate = reg80 & 0xf;
            if (rate!=0) {
                /*Bit8u*/int val = (rate << 2) + ksr;
                releaseAdd = chip.linearRates[val];
                rateZero &= ~(1 << RELEASE);
                if ((reg20 & MASK_SUSTAIN)==0) {
                    rateZero &= ~( 1 << SUSTAIN );
                }
            } else {
                rateZero |= (1 << RELEASE);
                releaseAdd = 0;
                if ((reg20 & MASK_SUSTAIN)==0) {
                    rateZero |= ( 1 << SUSTAIN );
                }
            }
        }

        private void UpdateDecay(Chip chip) {
            /*Bit8u*/int rate = reg60 & 0xf;
            if (rate!=0) {
                /*Bit8u*/int val = (rate << 2) + ksr;
                decayAdd = chip.linearRates[val];
                rateZero &= ~(1 << DECAY);
            } else {
                decayAdd = 0;
                rateZero |= (1 << DECAY);
            }
        }

        public void UpdateAttenuation() {
            /*Bit8u*/short kslBase = (/*Bit8u*/short)((chanData >> SHIFT_KSLBASE) & 0xff);
            /*Bit32u*/int tl = reg40 & 0x3f;
            /*Bit8u*/short kslShift = KslShiftTable[ reg40 >> 6 ];
            //Make sure the attenuation goes to the right bits
            totalLevel = tl << ( ENV_BITS - 7 );	//Total level goes 2 bits below max
            totalLevel += ( kslBase << ENV_EXTRA ) >> kslShift;
        }
        public void UpdateRates(Chip chip) {
            //Mame seems to reverse this where enabling ksr actually lowers
            //the rate, but pdf manuals says otherwise?
            /*Bit8u*/short newKsr = (/*Bit8u*/short)((chanData >> SHIFT_KEYCODE) & 0xff);
            if ((reg20 & MASK_KSR)==0) {
                newKsr >>= 2;
            }
            if ( ksr == newKsr )
                return;
            ksr = newKsr;
            UpdateAttack(chip);
            UpdateDecay(chip);
            UpdateRelease(chip);
        }
        public void UpdateFrequency() {
            /*Bit32u*/int freq = chanData & (( 1 << 10 ) - 1);
            /*Bit32u*/long block = (chanData >> 10) & 0xff;
            if (WAVE_PRECISION) {
                block = 7 - block;
                waveAdd = ( freq * freqMul ) >> block;
            } else {
                waveAdd = ( freq << block ) * freqMul;
            }
            if ((reg20 & MASK_VIBRATO)!=0) {
                vibStrength = (/*Bit8u*/short)(freq >> 7);

                if (WAVE_PRECISION)
                    vibrato = ( vibStrength * freqMul ) >> block;
                else
                    vibrato = ( vibStrength << block ) * freqMul;

            } else {
                vibStrength = 0;
                vibrato = 0;
            }
        }

        public void Write20(Chip chip, /*Bit8u*/short val) {
            /*Bit8u*/int change = (reg20 ^ val );
            if (change==0)
                return;
            reg20 = val;
            //Shift the tremolo bit over the entire register, saved a branch, YES!
            tremoloMask = (byte)((/*Bit8s*/byte)(val) >> 7);
            tremoloMask &= ~(( 1 << ENV_EXTRA ) -1);
            //Update specific features based on changes
            if ((change & MASK_KSR)!=0) {
                UpdateRates( chip );
            }
            //With sustain enable the volume doesn't change
            if ((reg20 & MASK_SUSTAIN)!=0 || releaseAdd==0) {
                rateZero |= ( 1 << SUSTAIN );
            } else {
                rateZero &= ~( 1 << SUSTAIN );
            }
            //Frequency multiplier or vibrato changed
            if ((change & (0xf | MASK_VIBRATO))!=0) {
                freqMul = chip.freqMul[ val & 0xf ];
                UpdateFrequency();
            }
        }
        public void Write40(Chip chip, /*Bit8u*/short val) {
            if ((reg40 ^ val )==0)
                return;
            reg40 = val;
            UpdateAttenuation( );
        }
        public void Write60(Chip chip, /*Bit8u*/short val) {
            /*Bit8u*/int change = reg60 ^ val;
            reg60 = val;
            if ((change & 0x0f)!=0) {
                UpdateDecay( chip );
            }
            if ((change & 0xf0)!=0) {
                UpdateAttack( chip );
            }
        }
        public void Write80(Chip chip, /*Bit8u*/short val) {
            /*Bit8u*/int change = (reg80 ^ val );
            if (change==0)
                return;
            reg80 = val;
            /*Bit8u*/int sustain = val >> 4;
            //Turn 0xf into 0x1f
            sustain |= ( sustain + 1) & 0x10;
            sustainLevel = sustain << ( ENV_BITS - 5 );
            if ((change & 0x0f)!=0) {
                UpdateRelease( chip );
            }
        }
        public void WriteE0(Chip chip, /*Bit8u*/short val) {
             if ((regE0 ^ val)==0)
                return;
            //in opl3 mode you can always selet 7 waveforms regardless of waveformselect
            /*Bit8u*/int waveForm = val & ( ( 0x3 & chip.waveFormMask ) | (0x7 & chip.opl3Active ) );
            regE0 = val;
            if ( DBOPL_WAVE == WAVE_HANDLER ) {
                waveHandler = WaveHandlerTable[ waveForm ];
            } else {
                waveBase = WaveTable;
                waveBaseOff = WaveBaseTable[ waveForm ];
                waveStart = WaveStartTable[ waveForm ] << WAVE_SH;
                waveMask = WaveMaskTable[ waveForm ];
            }
        }

        public boolean Silent() {
            if ( !ENV_SILENT( totalLevel + volume ) )
                return false;
            if ((rateZero & ( 1 << state ))==0)
                return false;
            return true;
        }
        public void Prepare(Chip chip) {
            currentLevel = totalLevel + (chip.tremoloValue & tremoloMask);
            waveCurrent = waveAdd;
            if ((vibStrength >> chip.vibratoShift)!=0) {
                /*Bit32s*/int add = vibrato >> chip.vibratoShift;
                //Sign extend over the shift value
                /*Bit32s*/int neg = chip.vibratoSign;
                //Negate the add with -1 or 0
                add = ( add ^ neg ) - neg;
                waveCurrent += add;
            }
        }

        public void KeyOn( /*Bit8u*/int mask) {
            if (keyOn==0) {
                //Restart the frequency generator
                if ( DBOPL_WAVE > WAVE_HANDLER )
                    waveIndex = waveStart;
                else
                    waveIndex = 0;

                rateIndex = 0;
                SetState( ATTACK );
            }
            keyOn |= mask;
        }
        public void KeyOff( /*Bit8u*/int mask) {
            keyOn &= ~mask;
            if (keyOn==0) {
                if (state != OFF) {
                    SetState(RELEASE);
                }
            }
        }

        //template< State state>
        public /*Bits*/int TemplateVolume(int yes) {
            /*Bit32s*/int vol = volume;
            /*Bit32s*/int change;
            switch ( yes ) {
            case OFF:
                return ENV_MAX;
            case ATTACK:
                change = RateForward( attackAdd );
                if (change==0)
                    return vol;
                vol += ( (~vol) * change ) >> 3;
                if ( vol < ENV_MIN ) {
                    volume = ENV_MIN;
                    rateIndex = 0;
                    SetState( DECAY );
                    return ENV_MIN;
                }
                break;
            case DECAY:
                vol += RateForward( decayAdd );
                if (vol >= sustainLevel) {
                    //Check if we didn't overshoot max attenuation, then just go off
                    if (vol >= ENV_MAX) {
                        volume = ENV_MAX;
                        SetState( OFF );
                        return ENV_MAX;
                    }
                    //Continue as sustain
                    rateIndex = 0;
                    SetState( SUSTAIN );
                }
                break;
            case SUSTAIN:
                if ((reg20 & MASK_SUSTAIN)!=0) {
                    return vol;
                }
                //In sustain phase, but not sustaining, do regular release
            case RELEASE:
                vol += RateForward( releaseAdd );;
                if (vol >= ENV_MAX) {
                    volume = ENV_MAX;
                    SetState( OFF );
                    return ENV_MAX;
                }
                break;
            }
            volume = vol;
            return vol;
        }

        public /*Bit32s*/int RateForward( /*Bit32u*/long add ) {
            rateIndex += add;
            /*Bit32s*/int ret = (int)(rateIndex >> RATE_SH);
            rateIndex = rateIndex & RATE_MASK;
            return ret;
        }
        public /*Bitu*/int ForwardWave() {
            waveIndex += waveCurrent;
            return (int)(waveIndex >> WAVE_SH);
        }
        public /*Bitu*/int ForwardVolume() {
            return currentLevel + TemplateVolume(volHandlerParam);
        }

        public /*Bits*/int GetSample( /*Bits*/int modulation ) {
            /*Bitu*/int vol = ForwardVolume();
            if ( ENV_SILENT( vol ) ) {
                //Simply forward the wave
                waveIndex += waveCurrent;
                return 0;
            } else {
                /*Bitu*/int index= ForwardWave();
                index += modulation;
                return GetWave( index, vol );
            }
        }
        public /*Bits*/int GetWave(/*Bitu*/int index, /*Bitu*/int vol) {
            if ( DBOPL_WAVE == WAVE_HANDLER )
                return waveHandler.call( index, vol << ( 3 - ENV_EXTRA ) );
            else if ( DBOPL_WAVE == WAVE_TABLEMUL )
                return (waveBase[waveBaseOff + (index & waveMask) ] * MulTable[ vol >> ENV_EXTRA ]) >> MUL_SH;
            else if ( DBOPL_WAVE == WAVE_TABLELOG ) {
                /*Bit32s*/int wave = waveBase[ waveBaseOff + (index & waveMask) ];
                /*Bit32u*/int total = ( wave & 0x7fff ) + vol << ( 3 - ENV_EXTRA );
                /*Bit32s*/int sig = ExpTable[ total & 0xff ];
                /*Bit32u*/long exp = total >> 8;
                /*Bit32s*/int neg = wave >> 16;
                return ((sig ^ neg) - neg) >> exp;
            } else {
                throw new RuntimeException("No valid wave routine");
            }
        }

        public Operator() {
            chanData = 0;
            freqMul = 0;
            waveIndex = 0;
            waveAdd = 0;
            waveCurrent = 0;
            keyOn = 0;
            ksr = 0;
            reg20 = 0;
            reg40 = 0;
            reg60 = 0;
            reg80 = 0;
            regE0 = 0;
            SetState( OFF );
            rateZero = (1 << OFF);
            sustainLevel = ENV_MAX;
            currentLevel = ENV_MAX;
            totalLevel = ENV_MAX;
            volume = ENV_MAX;
            releaseAdd = 0;
        }
    }

    private static class Channel {
        int index;
        Chip chip;

        Channel(Chip chip, int index) {
            this.chip = chip;
            this.index = index;
            old[0] = old[1] = 0;
            chanData = 0;
            regB0 = 0;
            regC0 = 0;
            maskLeft = -1;
            maskRight = -1;
            feedback = 31;
            fourMask = 0;
            synthHandlerMode = sm2FM;
            for (int i=0;i<op.length;i++) {
                op[i] = new Operator();
            }
        }

        Operator[] op = new Operator[2];
        Operator Op(/*Bitu*/int index) {
            return chip.chan[this.index+ (index >> 1) ].op[index & 1];
        }
        int synthHandlerMode;
        /*Bit32u*/int chanData;		//Frequency/octave and derived values
        /*Bit32s*/int[] old = new int[2];			//Old data for feedback

        /*Bit8u*/int feedback;			//Feedback shift
        /*Bit8u*/short regB0;			//Register values to check for changes
        /*Bit8u*/short regC0;
        //This should correspond with reg104, bit 6 indicates a Percussion channel, bit 7 indicates a silent channel
        /*Bit8u*/short fourMask;
        /*Bit8s*/byte maskLeft;		//Sign extended values for both channel's panning
        /*Bit8s*/byte maskRight;

        //Forward the channel data to the operators of the channel
        void SetChanData(Chip chip, /*Bit32u*/int data ) {
            /*Bit32u*/long change = chanData ^ data;
            chanData = data;
            Op(0).chanData = data;
            Op(1).chanData = data;
            //Since a frequency update triggered this, always update frequency
            Op(0).UpdateFrequency();
            Op(1).UpdateFrequency();
            if ((change & ( 0xff << SHIFT_KSLBASE ))!=0) {
                Op( 0 ).UpdateAttenuation();
                Op( 1 ).UpdateAttenuation();
            }
            if ((change & ( 0xff << SHIFT_KEYCODE))!=0) {
                Op( 0 ).UpdateRates( chip );
                Op( 1 ).UpdateRates( chip );
            }
        }
        //Change in the chandata, check for new values and if we have to forward to operators
        void UpdateFrequency(Chip chip, /*Bit8u*/int fourOp ) {
            //Extrace the frequency bits
            /*Bit32u*/int data = chanData & 0xffff;
            /*Bit32u*/long kslBase = KslTable[ data >> 6 ];
            /*Bit32u*/long keyCode = ( data & 0x1c00) >> 9;
            if ((chip.reg08 & 0x40)!=0) {
                keyCode |= ( data & 0x100)>>8;	/* notesel == 1 */
            } else {
                keyCode |= ( data & 0x200)>>9;	/* notesel == 0 */
            }
            //Add the keycode and ksl into the highest bits of chanData
            data |= (keyCode << SHIFT_KEYCODE) | ( kslBase << SHIFT_KSLBASE );
            SetChanData( chip, data );
            if ((fourOp & 0x3f)!=0) {
                chip.chan[index+1].SetChanData( chip, data );
            }
        }
        void WriteA0(Chip chip, /*Bit8u*/int val) {
            /*Bit8u*/int fourOp = chip.reg104 & chip.opl3Active & fourMask;
            //Don't handle writes to silent fourop channels
            if ( fourOp > 0x80 )
                return;
            /*Bit32u*/long change = (chanData ^ val ) & 0xff;
            if (change!=0) {
                chanData ^= change;
                UpdateFrequency(chip, fourOp);
            }
        }
        void WriteB0(Chip chip, /*Bit8u*/short val) {
            /*Bit8u*/int fourOp = chip.reg104 & chip.opl3Active & fourMask;
            //Don't handle writes to silent fourop channels
            if ( fourOp > 0x80 )
                return;
            /*Bitu*/long change = (chanData ^ ( val << 8 ) ) & 0x1f00;
            if (change!=0) {
                chanData ^= change;
                UpdateFrequency( chip, fourOp );
            }
            //Check for a change in the keyon/off state
            if ((( val ^ regB0) & 0x20)==0)
                return;
            regB0 = val;
            if ((val & 0x20)!=0) {
                Op(0).KeyOn( 0x1 );
                Op(1).KeyOn( 0x1 );
                if ((fourOp & 0x3f)!=0) {
                    chip.chan[index+1].Op(0).KeyOn( 1 );
                    chip.chan[index+1].Op(1).KeyOn( 1 );
                }
            } else {
                Op(0).KeyOff( 0x1 );
                Op(1).KeyOff( 0x1 );
                if ((fourOp & 0x3f)!=0) {
                    chip.chan[index+1].Op(0).KeyOff( 1 );
                    chip.chan[index+1].Op(1).KeyOff( 1 );
                }
            }
        }
        void WriteC0(Chip chip, /*Bit8u*/short val ) {
            /*Bit8u*/int change = val ^ regC0;
            if (change==0)
                return;
            regC0 = val;
            feedback = ( val >> 1 ) & 7;
            if (feedback!=0) {
                //We shift the input to the right 10 bit wave index value
                feedback = 9 - feedback;
            } else {
                feedback = 31;
            }
            //Select the new synth mode
            if (chip.opl3Active!=0) {
                //4-op mode enabled for this channel
                if (((chip.reg104 & fourMask) & 0x3f)!=0) {
                    Channel chan0, chan1;
                    //Check if it's the 2nd channel in a 4-op
                    if ((fourMask & 0x80 )==0) {
                        chan0 = this;
                        chan1 = chip.chan[index+1];
                    } else {
                        chan0 = chip.chan[index-1];
                        chan1 = this;
                    }

                    /*Bit8u*/int synth = ( (chan0.regC0 & 1) << 0 )| (( chan1.regC0 & 1) << 1 );
                    switch ( synth ) {
                    case 0:
                        chan0.synthHandlerMode = sm3FMFM;
                        break;
                    case 1:
                        chan0.synthHandlerMode = sm3AMFM;
                        break;
                    case 2:
                        chan0.synthHandlerMode = sm3FMAM;
                        break;
                    case 3:
                        chan0.synthHandlerMode = sm3AMAM;
                        break;
                    }
                //Disable updating percussion channels
                } else if ((fourMask & 0x40)!=0 && (chip.regBD & 0x20)!=0) {

                //Regular dual op, am or fm
                } else if ((val & 1)!=0) {
                    synthHandlerMode = sm3AM;
                } else {
                    synthHandlerMode = sm3FM;
                }
                maskLeft = ( val & 0x10 )!=0 ? (byte)-1 : 0;
                maskRight = ( val & 0x20 )!=0 ? (byte)-1 : 0;
            //opl2 active
            } else {
                //Disable updating percussion channels
                if ( (fourMask & 0x40)!=0 && (chip.regBD & 0x20)!=0) {

                //Regular dual op, am or fm
                } else if ((val & 1)!=0) {
                    synthHandlerMode = sm2AM;
                } else {
                    synthHandlerMode = sm2FM;
                }
            }
        }
        void ResetC0(Chip chip ) {
            /*Bit8u*/short val = regC0;
            regC0 ^= 0xff;
            WriteC0(chip, val);
        }

        //call this for the first channel
        void GeneratePercussion(boolean opl3Mode, Chip chip, /*Bit32s*/int[] output, int offset ) {
            Channel chan = this;

            //BassDrum
            /*Bit32s*/int mod = (/*Bit32u*/int)((old[0] + old[1])) >> feedback;
            old[0] = old[1];
            old[1] = Op(0).GetSample( mod );

            //When bassdrum is in AM mode first operator is ignoed
            if ((chan.regC0 & 1)!=0) {
                mod = 0;
            } else {
                mod = old[0];
            }
            /*Bit32s*/int sample = Op(1).GetSample(mod);


            //Precalculate stuff used by other outputs
            /*Bit32u*/int noiseBit = chip.ForwardNoise() & 0x1;
            /*Bit32u*/int c2 = Op(2).ForwardWave();
            /*Bit32u*/int c5 = Op(5).ForwardWave();
            /*Bit32u*/int phaseBit = (((c2 & 0x88) ^ ((c2<<5) & 0x80))!=0 | ((c5 ^ (c5<<2)) & 0x20)!=0) ? 0x02 : 0x00;

            //Hi-Hat
            /*Bit32u*/int hhVol = Op(2).ForwardVolume();
            if ( !ENV_SILENT( hhVol ) ) {
                /*Bit32u*/int hhIndex = (phaseBit<<8) | (0x34 << ( phaseBit ^ (noiseBit << 1 )));
                sample += Op(2).GetWave( hhIndex, hhVol );
            }
            //Snare Drum
            /*Bit32u*/int sdVol = Op(3).ForwardVolume();
            if ( !ENV_SILENT( sdVol ) ) {
                /*Bit32u*/int sdIndex = ( 0x100 + (c2 & 0x100) ) ^ ( noiseBit << 8 );
                sample += Op(3).GetWave( sdIndex, sdVol );
            }
            //Tom-tom
            sample += Op(4).GetSample( 0 );

            //Top-Cymbal
            /*Bit32u*/int tcVol = Op(5).ForwardVolume();
            if ( !ENV_SILENT( tcVol ) ) {
                /*Bit32u*/int tcIndex = (1 + phaseBit) << 8;
                sample += Op(5).GetWave( tcIndex, tcVol );
            }
            sample <<= 1;
            if ( opl3Mode ) {
                output[offset] += sample;
                output[offset+1] += sample;
            } else {
                output[offset] += sample;
            }
        }

        //Generate blocks of data in specific modes
        Channel BlockTemplate(int mode, Chip chip, /*Bit32u*/long samples, /*Bit32s*/int[] output, int offset) {
         switch( mode ) {
            case sm2AM:
            case sm3AM:
                if ( Op(0).Silent() && Op(1).Silent() ) {
                    old[0] = old[1] = 0;
                    return chip.chan[index+1];
                }
                break;
            case sm2FM:
            case sm3FM:
                if ( Op(1).Silent() ) {
                    old[0] = old[1] = 0;
                    return chip.chan[index+1];
                }
                break;
            case sm3FMFM:
                if ( Op(3).Silent() ) {
                    old[0] = old[1] = 0;
                    return chip.chan[index+2];
                }
                break;
            case sm3AMFM:
                if ( Op(0).Silent() && Op(3).Silent() ) {
                    old[0] = old[1] = 0;
                    return chip.chan[index+2];
                }
                break;
            case sm3FMAM:
                if ( Op(1).Silent() && Op(3).Silent() ) {
                    old[0] = old[1] = 0;
                    return chip.chan[index+2];
                }
                break;
            case sm3AMAM:
                if ( Op(0).Silent() && Op(2).Silent() && Op(3).Silent() ) {
                    old[0] = old[1] = 0;
                    return chip.chan[index+2];
                }
                break;
            }
            //Init the operators with the the current vibrato and tremolo values
            Op( 0 ).Prepare( chip );
            Op( 1 ).Prepare( chip );
            if ( mode > sm4Start ) {
                Op( 2 ).Prepare( chip );
                Op( 3 ).Prepare( chip );
            }
            if ( mode > sm6Start ) {
                Op( 4 ).Prepare( chip );
                Op( 5 ).Prepare( chip );
            }
            for ( /*Bitu*/int i = 0; i < samples; i++ ) {
                //Early out for percussion handlers
                if ( mode == sm2Percussion ) {
                    GeneratePercussion(false, chip, output, offset+i );
                    continue;	//Prevent some unitialized value bitching
                } else if ( mode == sm3Percussion ) {
                    GeneratePercussion(true, chip, output, offset+i * 2 );
                    continue;	//Prevent some unitialized value bitching
                }

                //Do unsigned shift so we can shift out all bits but still stay in 10 bit range otherwise
                /*Bit32s*/int mod = (/*Bit32u*/int)((old[0] + old[1])) >>> feedback;
                old[0] = old[1];
                old[1] = Op(0).GetSample( mod );
                /*Bit32s*/int sample=0;
                /*Bit32s*/int out0 = old[0];
                if ( mode == sm2AM || mode == sm3AM ) {
                    sample = out0 + Op(1).GetSample( 0 );
                } else if ( mode == sm2FM || mode == sm3FM ) {
                    sample = Op(1).GetSample( out0 );
                } else if ( mode == sm3FMFM ) {
                    /*Bits*/int next = Op(1).GetSample( out0 );
                    next = Op(2).GetSample( next );
                    sample = Op(3).GetSample( next );
                } else if ( mode == sm3AMFM ) {
                    sample = out0;
                    /*Bits*/int next = Op(1).GetSample( 0 );
                    next = Op(2).GetSample( next );
                    sample += Op(3).GetSample( next );
                } else if ( mode == sm3FMAM ) {
                    sample = Op(1).GetSample( out0 );
                    /*Bits*/int next = Op(2).GetSample( 0 );
                    sample += Op(3).GetSample( next );
                } else if ( mode == sm3AMAM ) {
                    sample = out0;
                    /*Bits*/int next = Op(1).GetSample( 0 );
                    sample += Op(2).GetSample( next );
                    sample += Op(3).GetSample( 0 );
                }
                switch( mode ) {
                case sm2AM:
                case sm2FM:
                    output[ offset+i ] += sample;
                    break;
                case sm3AM:
                case sm3FM:
                case sm3FMFM:
                case sm3AMFM:
                case sm3FMAM:
                case sm3AMAM:
                    output[ offset+i * 2 + 0 ] += sample & maskLeft;
                    output[ offset+i * 2 + 1 ] += sample & maskRight;
                    break;
                }
            }
            switch( mode ) {
            case sm2AM:
            case sm2FM:
            case sm3AM:
            case sm3FM:
                return chip.chan[index+1];
            case sm3FMFM:
            case sm3AMFM:
            case sm3FMAM:
            case sm3AMAM:
                return chip.chan[index+2];
            case sm2Percussion:
            case sm3Percussion:
                return chip.chan[index+3];
            }
            return null;
        }
    }

    static private class Chip {
        Chip() {
            reg08 = 0;
            reg04 = 0;
            regBD = 0;
            reg104 = 0;
            opl3Active = 0;
            for (int i=0;i<chan.length;i++) {
                chan[i] = new Channel(this, i);
            }
        }

        //This is used as the base counter for vibrato and tremolo
        /*Bit32u*/int lfoCounter;
        /*Bit32u*/int lfoAdd;


        /*Bit32u*/long noiseCounter;
        /*Bit32u*/long noiseAdd;
        /*Bit32u*/int noiseValue;

        //Frequency scales for the different multiplications
        /*Bit32u*/int[] freqMul = new int[16];
        //Rates for decay and release for rate of this chip
        /*Bit32u*/int[] linearRates = new int[76];
        //Best match attack rates for the rate of this chip
        /*Bit32u*/int[] attackRates = new int[76];

        //18 channels with 2 operators each
        Channel[] chan = new Channel[19]; // last one is null

        /*Bit8u*/short reg104;
        /*Bit8u*/short reg08;
        /*Bit8u*/short reg04;
        /*Bit8u*/short regBD;
        /*Bit8u*/short vibratoIndex;
        /*Bit8u*/short tremoloIndex;
        /*Bit8s*/byte vibratoSign;
        /*Bit8u*/short vibratoShift;
        /*Bit8u*/short tremoloValue;
        /*Bit8u*/short vibratoStrength;
        /*Bit8u*/short tremoloStrength;
        //Mask for allowed wave forms
        /*Bit8u*/short waveFormMask;
        //0 or -1 when enabled
        /*Bit8s*/byte opl3Active;

        //Return the maximum amount of samples before and LFO change
        /*Bit32u*/int ForwardLFO( /*Bit32u*/int samples ) {
            //Current vibrato value, runs 4x slower than tremolo
            vibratoSign = (byte)(( VibratoTable[ vibratoIndex >> 2] ) >> 7);
            vibratoShift = (short)(( VibratoTable[ vibratoIndex >> 2] & 7) + vibratoStrength);
            tremoloValue = (short)(TremoloTable[ tremoloIndex ] >> tremoloStrength);

            //Check hom many samples there can be done before the value changes
            /*Bit32u*/int todo = LFO_MAX - lfoCounter;
            /*Bit32u*/int count = (todo + lfoAdd - 1) / lfoAdd;
            if ( count > samples ) {
                count = samples;
                lfoCounter += count * lfoAdd;
            } else {
                lfoCounter += count * lfoAdd;
                lfoCounter &= (LFO_MAX - 1);
                //Maximum of 7 vibrato value * 4
                vibratoIndex = (short)(( vibratoIndex + 1 ) & 31);
                //Clip tremolo to the the table size
                if ( tremoloIndex + 1 < TREMOLO_TABLE  )
                    ++tremoloIndex;
                else
                    tremoloIndex = 0;
            }
            return count;
        }
        /*Bit32u*/int ForwardNoise() {
            noiseCounter += noiseAdd;
            /*Bitu*/long count = noiseCounter >> LFO_SH;
            noiseCounter &= WAVE_MASK;
            for ( ; count > 0; --count ) {
                //Noise calculation from mame
                noiseValue ^= ( 0x800302 ) & ( 0 - (noiseValue & 1 ) );
                noiseValue >>= 1;
            }
            return noiseValue;
        }

        void WriteBD(/*Bit8u*/short val) {
            /*Bit8u*/int change = regBD ^ val;
            if (change==0)
                return;
            regBD = val;
            //TODO could do this with shift and xor?
            vibratoStrength = (val & 0x40)!=0 ? (short)0x00 : (short)0x01;
            tremoloStrength = (val & 0x80)!=0 ? (short)0x00 : (short)0x02;
            if ((val & 0x20)!=0) {
                //Drum was just enabled, make sure channel 6 has the right synth
                if ((change & 0x20)!=0) {
                    if ( opl3Active!=0 ) {
                        chan[6].synthHandlerMode = sm3Percussion;
                    } else {
                        chan[6].synthHandlerMode = sm2Percussion;
                    }
                }
                //Bass Drum
                if ((val & 0x10)!=0) {
                    chan[6].op[0].KeyOn( 0x2 );
                    chan[6].op[1].KeyOn( 0x2 );
                } else {
                    chan[6].op[0].KeyOff( 0x2 );
                    chan[6].op[1].KeyOff( 0x2 );
                }
                //Hi-Hat
                if ((val & 0x1)!=0) {
                    chan[7].op[0].KeyOn( 0x2 );
                } else {
                    chan[7].op[0].KeyOff( 0x2 );
                }
                //Snare
                if ((val & 0x8)!=0) {
                    chan[7].op[1].KeyOn( 0x2 );
                } else {
                    chan[7].op[1].KeyOff( 0x2 );
                }
                //Tom-Tom
                if ((val & 0x4)!=0) {
                    chan[8].op[0].KeyOn( 0x2 );
                } else {
                    chan[8].op[0].KeyOff( 0x2 );
                }
                //Top Cymbal
                if ((val & 0x2)!=0) {
                    chan[8].op[1].KeyOn( 0x2 );
                } else {
                    chan[8].op[1].KeyOff( 0x2 );
                }
            //Toggle keyoffs when we turn off the percussion
            } else if ((change & 0x20)!=0) {
                //Trigger a reset to setup the original synth handler
                chan[6].ResetC0( this );
                chan[6].op[0].KeyOff( 0x2 );
                chan[6].op[1].KeyOff( 0x2 );
                chan[7].op[0].KeyOff( 0x2 );
                chan[7].op[1].KeyOff( 0x2 );
                chan[8].op[0].KeyOff( 0x2 );
                chan[8].op[1].KeyOff( 0x2 );
            }
        }
//        static private int REGOP( _FUNC_ ) {
//            index = ( ( reg >> 3) & 0x20 ) | ( reg & 0x1f );
//            if ( OpOffsetTable[ index ] ) {
//                Operator* regOp = (Operator*)( ((char *)this ) + OpOffsetTable[ index ] );
//                regOp._FUNC_( this, val );
//            }
//        }

//        static private final int REGCHAN( _FUNC_ )
//            index = ( ( reg >> 4) & 0x10 ) | ( reg & 0xf );
//            if ( ChanOffsetTable[ index ] ) {
//                Channel* regChan = (Channel*)( ((char *)this ) + ChanOffsetTable[ index ] );
//                regChan._FUNC_( this, val );
//            }

        void WriteReg(/*Bit32u*/int reg, /*Bit8u*/int val ) {
            /*Bitu*/int index;
            switch ( (reg & 0xf0) >> 4 ) {
            case 0x00 >> 4:
                if ( reg == 0x01 ) {
                    waveFormMask = ( val & 0x20 )!=0 ? (short)0x7 : (short)0x0;
                } else if ( reg == 0x104 ) {
                    //Only detect changes in lowest 6 bits
                    if (((reg104 ^ val) & 0x3f)==0)
                        return;
                    //Always keep the highest bit enabled, for checking > 0x80
                    reg104 = (short)(0x80 | ( val & 0x3f ));
                } else if ( reg == 0x105 ) {
                    //MAME says the real opl3 doesn't reset anything on opl3 disable/enable till the next write in another register
                    if (((opl3Active ^ val) & 1 )==0)
                        return;
                    opl3Active = ( val & 1 )!=0 ? (byte)0xff : (byte)0;
                    //Update the 0xc0 register for all channels to signal the switch to mono/stereo handlers
                    for ( int i = 0; i < 18;i++ ) {
                        chan[i].ResetC0( this );
                    }
                } else if ( reg == 0x08 ) {
                    reg08 = (short)val;
                }
            case 0x10 >> 4:
                break;
            case 0x20 >> 4:
            case 0x30 >> 4:
//                REGOP( Write20 );
                index = ( ( reg >> 3) & 0x20 ) | ( reg & 0x1f );
                if (OpOffsetTable[index]>0) {
                    int offset = OpOffsetTable[ index ];
                    Operator regOp = chan[offset & 0xFFFF].op[offset >>> 16];
                    regOp.Write20( this, (short)val );
                }
                break;
            case 0x40 >> 4:
            case 0x50 >> 4:
//                REGOP( Write40 );
                index = ( ( reg >> 3) & 0x20 ) | ( reg & 0x1f );
                if (OpOffsetTable[index]>0) {
                    int offset = OpOffsetTable[ index ];
                    Operator regOp = chan[offset & 0xFFFF].op[offset >>> 16];
                    regOp.Write40( this, (short)val );
                }
                break;
            case 0x60 >> 4:
            case 0x70 >> 4:
//                REGOP( Write60 );
                index = ( ( reg >> 3) & 0x20 ) | ( reg & 0x1f );
                if (OpOffsetTable[index]>0) {
                    int offset = OpOffsetTable[ index ];
                    Operator regOp = chan[offset & 0xFFFF].op[offset >>> 16];
                    regOp.Write60( this, (short)val );
                }
                break;
            case 0x80 >> 4:
            case 0x90 >> 4:
//                REGOP( Write80 );
                index = ( ( reg >> 3) & 0x20 ) | ( reg & 0x1f );
                if (OpOffsetTable[index]>0) {
                    int offset = OpOffsetTable[ index ];
                    Operator regOp = chan[offset & 0xFFFF].op[offset >>> 16];
                    regOp.Write80( this, (short)val );
                }
                break;
            case 0xa0 >> 4:
//                REGCHAN( WriteA0 );
                index = ( ( reg >> 4) & 0x10 ) | ( reg & 0xf );
                if (ChanOffsetTable[index]>=0) {
                    Channel regChan = this.chan[ChanOffsetTable[ index ]];
                    regChan.WriteA0( this, val );
                }
                break;
            case 0xb0 >> 4:
                if ( reg == 0xbd ) {
                    WriteBD( (short)val );
                } else {
//                    REGCHAN( WriteB0 );
                    index = ( ( reg >> 4) & 0x10 ) | ( reg & 0xf );
                    if (ChanOffsetTable[index]>=0) {
                        Channel regChan = this.chan[ChanOffsetTable[ index ]];
                        regChan.WriteB0( this, (short)val );
                    }
                }
                break;
            case 0xc0 >> 4:
//                REGCHAN( WriteC0 );
                index = ( ( reg >> 4) & 0x10 ) | ( reg & 0xf );
                if (ChanOffsetTable[index]>=0) {
                    Channel regChan = this.chan[ChanOffsetTable[ index ]];
                    regChan.WriteC0( this, (short)val );
                }
            case 0xd0 >> 4:
                break;
            case 0xe0 >> 4:
            case 0xf0 >> 4:
//                REGOP( WriteE0 );
                index = ( ( reg >> 3) & 0x20 ) | ( reg & 0x1f );
                if (OpOffsetTable[index]>0) {
                    int offset = OpOffsetTable[ index ];
                    Operator regOp = chan[offset & 0xFFFF].op[offset >>> 16];
                    regOp.WriteE0( this, (short)val );
                }
                break;
            }
        }

        /*Bit32u*/long WriteAddr( /*Bit32u*/int port, /*Bit8u*/short val ) {
            switch ( port & 3 ) {
            case 0:
                return val;
            case 2:
                if ( opl3Active!=0 || (val == 0x05) )
                    return 0x100 | val;
                else
                    return val;
            }
            return 0;
        }

        void GenerateBlock2( /*Bitu*/int total, /*Bit32s*/int[] output, int offset) {
            while ( total > 0 ) {
                /*Bit32u*/int samples = ForwardLFO( total );
                java.util.Arrays.fill(output, offset, samples+offset, 0);
                int count = 0;
                for(int i=0; i < 9;) {
                    Channel ch = chan[i];
                    count++;
                    i = ch.BlockTemplate(ch.synthHandlerMode, this, samples, output, offset).index;
                }
                total -= samples;
                offset += samples;
            }
        }
        void GenerateBlock3( /*Bitu*/int total, /*Bit32s*/int[] output, int offset) {
            while ( total > 0 ) {
                /*Bit32u*/int samples = ForwardLFO( total );
                java.util.Arrays.fill(output, offset, offset+samples*2, 0);
                int count = 0;
                for(int i=0; i < 18;) {
                    Channel ch = chan[i];
                    count++;
                    i = ch.BlockTemplate(ch.synthHandlerMode, this, samples, output, offset).index;
                }
                total -= samples;
                offset += samples * 2;
            }
        }

        //void Generate( /*Bit32u*/long samples );
        void Setup(/*Bit32u*/long rate) {
            double d_original = OPLRATE;
        //	double original = rate;
            double scale = d_original / (double)rate;

            //Noise counter is run at the same precision as general waves
            noiseAdd = (/*Bit32u*/long)( 0.5 + scale * ( 1 << LFO_SH ) );
            noiseCounter = 0;
            noiseValue = 1;	//Make sure it triggers the noise xor the first time
            //The low frequency oscillation counter
            //Every time his overflows vibrato and tremoloindex are increased
            lfoAdd = (/*Bit32u*/int)( 0.5 + scale * ( 1 << LFO_SH ) );
            lfoCounter = 0;
            vibratoIndex = 0;
            tremoloIndex = 0;

            //With higher octave this gets shifted up
            //-1 since the freqCreateTable = *2
            if (WAVE_PRECISION) {
                double freqScale = ( 1 << 7 ) * scale * ( 1 << ( WAVE_SH - 1 - 10));
                for ( int i = 0; i < 16; i++ ) {
                    freqMul[i] = (/*Bit32u*/int)( 0.5 + freqScale * FreqCreateTable[ i ] );
                }
            } else {
                /*Bit32u*/int freqScale = (/*Bit32u*/int)( 0.5 + scale * ( 1 << ( WAVE_SH - 1 - 10)));
                for ( int i = 0; i < 16; i++ ) {
                    freqMul[i] = freqScale * FreqCreateTable[ i ];
                }
            }

            //-3 since the real envelope takes 8 steps to reach the single value we supply
            for ( /*Bit8u*/int i = 0; i < 76; i++ ) {
                /*Bit8u*/int index, shift;
                //EnvelopeSelect( i, index, shift );
                if ( i < 13 * 4 ) {				//Rate 0 - 12
                    shift = 12 - ( i >> 2 );
                    index = i & 3;
                } else if ( i < 15 * 4 ) {		//rate 13 - 14
                    shift = 0;
                    index = i - 12 * 4;
                } else {							//rate 15 and up
                    shift = 0;
                    index = 12;
                }
                linearRates[i] = (/*Bit32u*/int)( scale * (EnvelopeIncreaseTable[ index ] << ( RATE_SH + ENV_EXTRA - shift - 3 )));
            }
            //Generate the best matching attack rate
            for ( /*Bit8u*/int i = 0; i < 62; i++ ) {
                /*Bit8u*/int index, shift;
                //EnvelopeSelect( i, index, shift );
                if ( i < 13 * 4 ) {				//Rate 0 - 12
                    shift = 12 - ( i >> 2 );
                    index = i & 3;
                } else if ( i < 15 * 4 ) {		//rate 13 - 14
                    shift = 0;
                    index = i - 12 * 4;
                } else {							//rate 15 and up
                    shift = 0;
                    index = 12;
                }
                //Original amount of samples the attack would take
                /*Bit32s*/int i_original = (/*Bit32u*/int)( (AttackSamplesTable[ index ] << shift) / scale);

                /*Bit32s*/int guessAdd = (/*Bit32u*/int)( scale * (EnvelopeIncreaseTable[ index ] << ( RATE_SH - shift - 3 )));
                /*Bit32s*/int bestAdd = guessAdd;
                /*Bit32u*/long bestDiff = 1 << 30;
                for( /*Bit32u*/long passes = 0; passes < 16; passes ++ ) {
                    /*Bit32s*/int volume = ENV_MAX;
                    /*Bit32s*/int samples = 0;
                    /*Bit32u*/int count = 0;
                    while ( volume > 0 && samples < i_original * 2 ) {
                        count += guessAdd;
                        /*Bit32s*/int change = count >> RATE_SH;
                        count &= RATE_MASK;
                        if (change!=0) { // less than 1 %
                            volume += ( ~volume * change ) >> 3;
                        }
                        samples++;

                    }
                    /*Bit32s*/int diff = i_original - samples;
                    /*Bit32u*/long lDiff = Math.abs(diff);
                    //Init last on first pass
                    if ( lDiff < bestDiff ) {
                        bestDiff = lDiff;
                        bestAdd = guessAdd;
                        if (bestDiff==0)
                            break;
                    }
                    //Below our target
                    if ( diff < 0 ) {
                        //Better than the last time
                        /*Bit32s*/int mul = ((i_original - diff) << 12) / i_original;
                        guessAdd = ((guessAdd * mul) >> 12);
                        guessAdd++;
                    } else if ( diff > 0 ) {
                        /*Bit32s*/int mul = ((i_original - diff) << 12) / i_original;
                        guessAdd = (guessAdd * mul) >> 12;
                        guessAdd--;
                    }
                }
                attackRates[i] = bestAdd;
            }
            for ( /*Bit8u*/short i = 62; i < 76; i++ ) {
                //This should provide instant volume maximizing
                attackRates[i] = 8 << RATE_SH;
            }
            //Setup the channels with the correct four op flags
            //Channels are accessed through a table so they appear linear here
            chan[ 0].fourMask = 0x00 | ( 1 << 0 );
            chan[ 1].fourMask = 0x80 | ( 1 << 0 );
            chan[ 2].fourMask = 0x00 | ( 1 << 1 );
            chan[ 3].fourMask = 0x80 | ( 1 << 1 );
            chan[ 4].fourMask = 0x00 | ( 1 << 2 );
            chan[ 5].fourMask = 0x80 | ( 1 << 2 );

            chan[ 9].fourMask = 0x00 | ( 1 << 3 );
            chan[10].fourMask = 0x80 | ( 1 << 3 );
            chan[11].fourMask = 0x00 | ( 1 << 4 );
            chan[12].fourMask = 0x80 | ( 1 << 4 );
            chan[13].fourMask = 0x00 | ( 1 << 5 );
            chan[14].fourMask = 0x80 | ( 1 << 5 );

            //mark the percussion channels
            chan[ 6].fourMask = 0x40;
            chan[ 7].fourMask = 0x40;
            chan[ 8].fourMask = 0x40;

            //Clear Everything in opl3 mode
            WriteReg( 0x105, 0x1 );
            for ( int i = 0; i < 512; i++ ) {
                if ( i == 0x105 )
                    continue;
                WriteReg( i, 0xff );
                WriteReg( i, 0x0 );
            }
            WriteReg( 0x105, 0x0 );
            //Clear everything in opl2 mode
            for ( int i = 0; i < 255; i++ ) {
                WriteReg( i, 0xff );
                WriteReg( i, 0x0 );
            }
        }
    }

    final public static class Handler implements Adlib.Handler {
        Chip chip = new Chip();
        public /*Bit32u*/long WriteAddr( /*Bit32u*/int port, /*Bit8u*/short val ) {
            return chip.WriteAddr( port, val );

        }
        public void WriteReg( /*Bit32u*/int addr, /*Bit8u*/short val ) {
            chip.WriteReg( addr, val );
        }

        /*Bit32s*/int[] buffer = new int[512*2];
        public void Generate( Mixer.MixerChannel chan, /*Bitu*/int samples ) {
            if (samples > 512)
                samples = 512;
            if (chip.opl3Active==0) {
                chip.GenerateBlock2( samples, buffer, 0);
                chan.AddSamples_m32( samples, buffer);
            } else {
                chip.GenerateBlock3( samples, buffer, 0);
                chan.AddSamples_s32( samples, buffer);
            }
        }

        public void Init( /*Bitu*/long rate ) {
            InitTables();
            chip.Setup( rate );
        }
    }

    static private final double PI = 3.14159265358979323846;


//    #if ( DBOPL_WAVE == WAVE_HANDLER )
    /*
        Generate the different waveforms out of the sine/exponetial table using handlers
    */
    static private /*Bits*/int MakeVolume( /*Bitu*/int wave, /*Bitu*/int volume ) {
        /*Bitu*/int total = wave + volume;
        /*Bitu*/int index = total & 0xff;
        /*Bitu*/int sig = ExpTable[ index ];
        /*Bitu*/int exp = total >> 8;
//    #if 0
//        //Check if we overflow the 31 shift limit
//        if ( exp >= 32 ) {
//            LOG_MSG( "WTF %d %d", total, exp );
//        }
//    #endif
        return (sig >> exp);
    }

    static private final WaveHandler WaveForm0 = new WaveHandler() {
        public /*Bits*/int call(int i, int volume)  {
            /*Bits*/int neg = 0 - (( i >> 9) & 1);//Create ~0 or 0
            /*Bitu*/int wave = SinTable[i & 511];
            return (MakeVolume( wave, volume ) ^ neg) - neg;
        }
    };

    static private final WaveHandler WaveForm1 = new WaveHandler() {
            public /*Bits*/int call(int i, int volume)  {
            /*Bit32u*/int wave = SinTable[i & 511];
            wave |= ( ( (i ^ 512 ) & 512) - 1) >> ( 32 - 12 );
            return MakeVolume( wave, volume );
        }
    };

    static private final WaveHandler WaveForm2 = new WaveHandler() {
        public /*Bits*/int call(int i, int volume)  {
            /*Bitu*/int wave = SinTable[i & 511];
            return MakeVolume( wave, volume );
        }
    };

    static private final WaveHandler WaveForm3 = new WaveHandler() {
        public /*Bits*/int call(int i, int volume)  {
            /*Bitu*/int wave = SinTable[i & 255];
            wave |= ( ( (i ^ 256 ) & 256) - 1) >> ( 32 - 12 );
            return MakeVolume( wave, volume );
        }
    };

    static private final WaveHandler WaveForm4 = new WaveHandler() {
        public /*Bits*/int call(int i, int volume)  {
            //Twice as fast
            i <<= 1;
            /*Bits*/int neg = 0 - (( i >> 9) & 1);//Create ~0 or 0
            /*Bitu*/int wave = SinTable[i & 511];
            wave |= ( ( (i ^ 512 ) & 512) - 1) >> ( 32 - 12 );
            return (MakeVolume( wave, volume ) ^ neg) - neg;
        }
    };

    static private final WaveHandler WaveForm5 = new WaveHandler() {
        public /*Bits*/int call(int i, int volume)  {
            //Twice as fast
            i <<= 1;
            /*Bitu*/int wave = SinTable[i & 511];
            wave |= ( ( (i ^ 512 ) & 512) - 1) >> ( 32 - 12 );
            return MakeVolume( wave, volume );
        }
    };

    static private final WaveHandler WaveForm6 = new WaveHandler() {
        public /*Bits*/int call(int i, int volume)  {
            /*Bits*/int neg = 0 - (( i >> 9) & 1);//Create ~0 or 0
            return (MakeVolume( 0, volume ) ^ neg) - neg;
        }
    };

    static private final WaveHandler WaveForm7 = new WaveHandler() {
            public /*Bits*/int call(int i, int volume)  {
            //Negative is reversed here
            /*Bits*/int neg = (( i >> 9) & 1) - 1;
            /*Bitu*/int wave = (i << 3);
            //When negative the volume also runs backwards
            wave = ((wave ^ neg) - neg) & 4095;
            return (MakeVolume( wave, volume ) ^ neg) - neg;
        }
    };

    static final private WaveHandler[] WaveHandlerTable = {
        WaveForm0, WaveForm1, WaveForm2, WaveForm3,
        WaveForm4, WaveForm5, WaveForm6, WaveForm7
    };

//    #endif


    private static boolean doneTables = false;
    static private void InitTables() {
        if ( doneTables )
            return;
        doneTables = true;
        if ( DBOPL_WAVE == WAVE_HANDLER || DBOPL_WAVE == WAVE_TABLELOG ) {
            //Exponential volume table, same as the real adlib
            for ( int i = 0; i < 256; i++ ) {
                //Save them in reverse
                ExpTable[i] = (int)( 0.5 + ( Math.pow(2.0, ( 255 - i) * ( 1.0 /256 ) )-1) * 1024 );
                ExpTable[i] += 1024; //or remove the -1 oh well :)
                //Preshift to the left once so the final volume can shift to the right
                ExpTable[i] *= 2;
            }
        }
        if ( DBOPL_WAVE == WAVE_HANDLER ) {
            //Add 0.5 for the trunc rounding of the integer cast
            //Do a PI sinetable instead of the original 0.5 PI
            for ( int i = 0; i < 512; i++ ) {
                SinTable[i] = (/*Bit16s*/short)( 0.5 - log10( Math.sin( (i + 0.5) * (PI / 512.0) ) ) / log10(2.0)*256 );
            }
        }
        if ( DBOPL_WAVE == WAVE_TABLEMUL ) {
            //Multiplication based tables
            for ( int i = 0; i < 384; i++ ) {
                int s = i * 8;
                //TODO maybe keep some of the precision errors of the original table?
                double val = ( 0.5 + ( Math.pow(2.0, -1.0 + ( 255 - s) * ( 1.0 /256 ) )) * ( 1 << MUL_SH ));
                MulTable[i] = (/*Bit16u*/int)(val);
            }

            //Sine Wave Base
            for ( int i = 0; i < 512; i++ ) {
                WaveTable[ 0x0200 + i ] = (/*Bit16s*/short)(Math.sin( (i + 0.5) * (PI / 512.0) ) * 4084);
                WaveTable[ 0x0000 + i ] = (short)-WaveTable[ 0x200 + i ];
            }
            //Exponential wave
            for ( int i = 0; i < 256; i++ ) {
                WaveTable[ 0x700 + i ] = (/*Bit16s*/short)( 0.5 + ( Math.pow(2.0, -1.0 + ( 255 - i * 8) * ( 1.0 /256 ) ) ) * 4085 );
                WaveTable[ 0x6ff - i ] = (short)-WaveTable[ 0x700 + i ];
            }
        }
        if ( DBOPL_WAVE == WAVE_TABLELOG ) {
            //Sine Wave Base
            for ( int i = 0; i < 512; i++ ) {
                WaveTable[ 0x0200 + i ] = (/*Bit16s*/short)( 0.5 - log10( Math.sin( (i + 0.5) * (PI / 512.0) ) ) / log10(2.0)*256 );
                WaveTable[ 0x0000 + i ] = (short)(((/*Bit16s*/short)0x8000) | WaveTable[ 0x200 + i]);
            }
            //Exponential wave
            for ( int i = 0; i < 256; i++ ) {
                WaveTable[ 0x700 + i ] = (short)(i * 8);
                WaveTable[ 0x6ff - i ] = (short)(((/*Bit16s*/short)0x8000) | i * 8);
            }
        }

        //	|    |//\\|____|WAV7|//__|/\  |____|/\/\|
        //	|\\//|    |    |WAV7|    |  \/|    |    |
        //	|06  |0126|27  |7   |3   |4   |4 5 |5   |

        if (( DBOPL_WAVE == WAVE_TABLELOG ) || ( DBOPL_WAVE == WAVE_TABLEMUL )) {
            for ( int i = 0; i < 256; i++ ) {
                //Fill silence gaps
                WaveTable[ 0x400 + i ] = WaveTable[0];
                WaveTable[ 0x500 + i ] = WaveTable[0];
                WaveTable[ 0x900 + i ] = WaveTable[0];
                WaveTable[ 0xc00 + i ] = WaveTable[0];
                WaveTable[ 0xd00 + i ] = WaveTable[0];
                //Replicate sines in other pieces
                WaveTable[ 0x800 + i ] = WaveTable[ 0x200 + i ];
                //double speed sines
                WaveTable[ 0xa00 + i ] = WaveTable[ 0x200 + i * 2 ];
                WaveTable[ 0xb00 + i ] = WaveTable[ 0x000 + i * 2 ];
                WaveTable[ 0xe00 + i ] = WaveTable[ 0x200 + i * 2 ];
                WaveTable[ 0xf00 + i ] = WaveTable[ 0x200 + i * 2 ];
            }
        }

        //Create the ksl table
        for ( int oct = 0; oct < 8; oct++ ) {
            int base = oct * 8;
            for ( int i = 0; i < 16; i++ ) {
                int val = base - KslCreateTable[i];
                if ( val < 0 )
                    val = 0;
                //*4 for the final range to match attenuation range
                KslTable[ oct * 16 + i ] = (short)(val * 4);
            }
        }
        //Create the Tremolo table, just increase and decrease a triangle wave
        for ( /*Bit8u*/short i = 0; i < TREMOLO_TABLE / 2; i++ ) {
            /*Bit8u*/short val = (short)(i << ENV_EXTRA);
            TremoloTable[i] = val;
            TremoloTable[TREMOLO_TABLE - 1 - i] = val;
        }
        //Create a table with offsets of the channels from the start of the chip
        Chip chip = null;
        for ( /*Bitu*/int i = 0; i < 32; i++ ) {
            /*Bitu*/int index = i & 0xf;
            if ( index >= 9 ) {
                ChanOffsetTable[i] = -1;
                continue;
            }
            //Make sure the four op channels follow eachother
            if ( index < 6 ) {
                index = (index % 3) * 2 + ( index / 3 );
            }
            //Add back the bits for highest ones
            if ( i >= 16 )
                index += 9;
//            /*Bitu*/int blah = reinterpret_cast</*Bitu*/long>( &(chip.chan[ index ]) );
            ChanOffsetTable[i] = index;
        }
        //Same for operators
        for ( /*Bitu*/int i = 0; i < 64; i++ ) {
            if ( i % 8 >= 6 || ( (i / 8) % 4 == 3 ) ) {
                OpOffsetTable[i] = -1;
                continue;
            }
            /*Bitu*/int chNum = (i / 8) * 3 + (i % 8) % 3;
            //Make sure we use 16 and up for the 2nd range to match the chanoffset gap
            if ( chNum >= 12 )
                chNum += 16 - 12;
            /*Bitu*/int opNum = ( i % 8 ) / 3;
            Channel chan = null;
//            /*Bitu*/int blah = reinterpret_cast</*Bitu*/long>( &(chan.op[opNum]) );
            OpOffsetTable[i] = ChanOffsetTable[ chNum ] | opNum<<16;
        }
//    #if 0
//        //Stupid checks if table's are correct
//        for ( /*Bitu*/long i = 0; i < 18; i++ ) {
//            /*Bit32u*/long find = (/*Bit16u*/int)( &(chip.chan[ i ]) );
//            for ( /*Bitu*/long c = 0; c < 32; c++ ) {
//                if ( ChanOffsetTable[c] == find ) {
//                    find = 0;
//                    break;
//                }
//            }
//            if ( find ) {
//                find = find;
//            }
//        }
//        for ( /*Bitu*/long i = 0; i < 36; i++ ) {
//            /*Bit32u*/long find = (/*Bit16u*/int)( &(chip.chan[ i / 2 ].op[i % 2]) );
//            for ( /*Bitu*/long c = 0; c < 64; c++ ) {
//                if ( OpOffsetTable[c] == find ) {
//                    find = 0;
//                    break;
//                }
//            }
//            if ( find ) {
//                find = find;
//            }
//        }
//    #endif
    }
}
