package org.jpc.emulator.peripheral;

import org.jpc.emulator.*;
import org.jpc.emulator.motherboard.*;
import org.jpc.support.*;

import java.io.*;

public class BochsFloppyController// implements IODevice, DMATransferCapable, HardwareComponent, TimerResponsive
{
//    private static final boolean DEBUG = false;
//
//    public static enum DriveType {DRIVE_144, DRIVE_288, DRIVE_120, DRIVE_NONE}
//    public static int FROM_FLOPPY = 10;
//    public static int TO_FLOPPY = 11;
//
//    @Override
//    public int handleTransfer(DMAController.DMAChannel channel, int position, int size) {
//        return 0;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public boolean initialised() {
//        return false;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public void acceptComponent(HardwareComponent component) {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public void reset() {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public boolean updated() {
//        return false;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public void updateComponent(HardwareComponent component) {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public void loadState(DataInput input) throws IOException {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public void saveState(DataOutput output) throws IOException {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public void ioPortWrite8(int address, int data) {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public void ioPortWrite16(int address, int data) {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public void ioPortWrite32(int address, int data) {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public int ioPortRead8(int address) {
//        return 0;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public int ioPortRead16(int address) {
//        return 0;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public int ioPortRead32(int address) {
//        return 0;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public int[] ioPortsRequested() {
//        return new int[0];  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public void callback() {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public int getType() {
//        return 0;
//    }
//
//
//    static class FloppyDrive
//    {
//
//        BlockDevice device;
//        int sectors_per_track;
//        int sectors;
//        int tracks;
//        int heads;
//        DriveType type;
//        int write_protected;
//        int status_changed;
//        boolean  vvfat_floppy;
//        BlockDevice vvfat;
//
//        public FloppyDrive(BlockDevice d, int sectors_per_track, int sectors, int tracks, int heads, DriveType type, int write_protected, int status_changed, boolean vvfat_floppy, BlockDevice vvfat)
//        {
//            this.device = d;
//            this.sectors_per_track = sectors_per_track;
//            this.sectors = sectors;
//            this.tracks = tracks;
//            this.heads = heads;
//            this.type = type;
//            this.write_protected = write_protected;
//            this.status_changed = status_changed;
//            this.vvfat_floppy = vvfat_floppy;
//            this.vvfat = vvfat;
//        }
//    }
//
//
//    int   data_rate;
//
//    int[] command = new int[10]; // largest command size ???
//    int   command_index;
//    int   command_size;
//    boolean command_complete;
//    int   pending_command;
//
//    boolean multi_track;
//    boolean pending_irq;
//    int   reset_sensei;
//    int   format_count;
//    int   format_fillbyte;
//
//    int[] result = new int[10];
//    int   result_index;
//    int   result_size;
//
//    int   DOR; // Digital Ouput Register
//    int   TDR; // Tape Drive Register
//    int[] cylinder = new int[4]; // really only using 2 drives
//    int[] head = new int[4];     // really only using 2 drives
//    int[] sector = new int[4];   // really only using 2 drives
//    int[] eot = new int[4];      // really only using 2 drives
//    boolean TC;          // Terminal Count status from DMA controller
//
//    /* MAIN STATUS REGISTER
//     * b7: MRQ: main request 1=data register ready     0=data register not ready
//     * b6: DIO: data input/output:
//     *     1=controller->CPU (ready for data read)
//     *     0=CPU->controller (ready for data write)
//     * b5: NDMA: non-DMA mode: 1=controller not in DMA modes
//     *                         0=controller in DMA mode
//     * b4: BUSY: instruction(device busy) 1=active 0=not active
//     * b3-0: ACTD, ACTC, ACTB, ACTA:
//     *       drive D,C,B,A in positioning mode 1=active 0=not active
//     */
//    int   main_status_reg;
//
//    int   status_reg0;
//    int   status_reg1;
//    int   status_reg2;
//    int   status_reg3;
//
//    // drive field allows up to 4 drives, even though probably only 2 will
//    // ever be used.
//    FloppyDrive[] media = new FloppyDrive[4];
//    int num_supported_floppies;
//    int[] floppy_buffer = new int[512+2]; // 2 extra for good measure
//    int floppy_buffer_index;
//    int      floppy_timer_index;
//    boolean[] media_present = new boolean[4];
//    int[] device_type = new int[4];
//    int[] DIR = new int[4]; // Digital Input Register:
//                  // b7: 0=diskette is present and has not been changed
//                  //     1=diskette missing or changed
//    boolean  lock;      // FDC lock status
//    int    SRT;       // step rate time
//    int    HUT;       // head unload time
//    int    HLT;       // head load time
//    int    config;    // configure byte #1
//    int    pretrk;    // precompensation track
//    int    perp_mode; // perpendicular mode
//
//    int[] statusbar_id = new int[2]; // IDs of the status LEDs
//
//    public static int BX_FLOPPY_NONE = 10; // media not present
//    public static int BX_FLOPPY_1_2 = 11; // 1.2M  5.25"
//    public static int BX_FLOPPY_1_44 = 12; // 1.44M 3.5"
//    public static int BX_FLOPPY_2_88 = 13; // 2.88M 3.5"
//    public static int BX_FLOPPY_720K = 14; // 720K  3.5"
//    public static int BX_FLOPPY_360K = 15; // 360K  5.25"
//    public static int BX_FLOPPY_160K = 16; // 160K  5.25"
//    public static int BX_FLOPPY_180K = 17; // 180K  5.25"
//    public static int BX_FLOPPY_320K = 18; // 320K  5.25"
//    public static int BX_FLOPPY_LAST = 18; // last legal value of floppy type
//
//
////
//// Floppy Disk Controller Docs:
//// Intel 82077A Data sheet
////   ftp://void-core.2y.net/pub/docs/fdc/82077AA_FloppyControllerDatasheet.pdf
//// Intel 82078 Data sheet
////   ftp://download.intel.com/design/periphrl/datashts/29047403.PDF
//// Other FDC references
////   http://debs.future.easyspace.com/Programming/Hardware/FDC/floppy.html
//// And a port list:
////   http://mudlist.eorbit.net/~adam/pickey/ports.html
////
//
//
//// #include "iodev.h"
//// #include "hdimage/hdimage.h"
//// #include "floppy.h"
//// windows.h included by bochs.h
//
//
//
///* for main status register */
//public static int FD_MS_MRQ = 0x80;
//    public static int FD_MS_DIO = 0x40;
//    public static int FD_MS_NDMA = 0x20;
//    public static int FD_MS_BUSY = 0x10;
//    public static int FD_MS_ACTD = 0x08;
//    public static int FD_MS_ACTC = 0x04;
//    public static int FD_MS_ACTB = 0x02;
//    public static int FD_MS_ACTA = 0x01;
//
//    public static int FLOPPY_DMA_CHAN = 2;
//
//    public static int FDRIVE_NONE = 0x00;
//    public static int FDRIVE_525DD = 0x01;
//    public static int FDRIVE_525HD = 0x02;
//    public static int FDRIVE_350DD = 0x04;
//    public static int FDRIVE_350HD = 0x08;
//    public static int FDRIVE_350ED = 0x10;
//
//    static class floppy_type_t
//    {
//
//        int id;
//        int trk;
//        int hd;
//        int spt;
//        int sectors;
//        int drive_mask;
//
//        public floppy_type_t(int id, int trk, int hd, int spt, int sectors, int drive_mask)
//        {
//            this.id = id;
//            this.trk = trk;
//            this.hd = hd;
//            this.spt = spt;
//            this.sectors = sectors;
//            this.drive_mask = drive_mask;
//        }
//    }
//
//    static floppy_type_t[] floppy_type = new floppy_type_t[] {
//            new floppy_type_t(BX_FLOPPY_160K, 40, 1, 8, 320, 0x0),
//            new floppy_type_t(BX_FLOPPY_180K, 40, 1, 9, 360, 0x0),
//            new floppy_type_t(BX_FLOPPY_320K, 40, 2, 8, 640, 0x0),
//            new floppy_type_t(BX_FLOPPY_360K, 40, 2, 9, 720, 0x0),
//            new floppy_type_t(BX_FLOPPY_720K, 80, 2, 9, 1440, 0x1),
//            new floppy_type_t(BX_FLOPPY_1_2, 80, 2, 15, 2400, 0x0),
//            new floppy_type_t(BX_FLOPPY_1_44, 80, 2, 18, 2880, 0x1),
//            new floppy_type_t(BX_FLOPPY_2_88, 80, 2, 36, 5760, 0x10)};
//
//    static int[] drate_in_k = new int[] {500, 300, 250, 1000};
//
//
//
//
//
//
//    public BochsFloppyController()
//    {
//        char pname[10];
//
//        floppy_timer_index = -1;
//        for (int i = 0; i < 2; i++) {
//            close_media(media[i]);
//            sprintf(pname, "floppy.%d", i);
//            bx_list_c *floppy = (bx_list_c*)SIM->get_param(pname);
//            SIM->get_param_string("path", floppy)->set_handler(NULL);
//            SIM->get_param_bool("readonly", floppy)->set_handler(NULL);
//            SIM->get_param_enum("status", floppy)->set_handler(NULL);
//        }
//        SIM->get_bochs_root()->remove("floppy");
//        if (DEBUG) System.out.printf(("Exit"));
//    }
//
//void init()
//{
//  int i, devtype, cmos_value;
//  char pname[10];
//
//  if (DEBUG) System.out.printf(("Init $Id: floppy.cc 11588 2013-01-23 17:56:57Z vruppert $"));
//  DEV_dma_register_8bit_channel(2, dma_read, dma_write, "Floppy Drive");
//  DEV_register_irq(6, "Floppy Drive");
//  for (int addr=0x03F2; addr<=0x03F7; addr++) {
//    DEV_register_ioread_handler(this, read_handler, addr, "Floppy Drive", 1);
//    DEV_register_iowrite_handler(this, write_handler, addr, "Floppy Drive", 1);
//  }
//
//
//  cmos_value = 0x00; /* start out with: no drive 0, no drive 1 */
//
//  this.num_supported_floppies = 0;
//
//  for (i=0; i<4; i++) {
//    this.media[i].type              = BX_FLOPPY_NONE;
//    this.media[i].sectors_per_track = 0;
//    this.media[i].tracks            = 0;
//    this.media[i].heads             = 0;
//    this.media[i].sectors           = 0;
//    this.media[i].fd                = -1;
//    this.media[i].vvfat_floppy      = false;
//    this.media[i].status_changed    = 0;
//    this.media_present[i]           = false;
//    this.device_type[i]             = FDRIVE_NONE;
//  }
//
//  //
//  // Floppy A setup
//  //
//
//  devtype = SIM->get_param_enum(BXPN_FLOPPYA_DEVTYPE)->get();
//  cmos_value = (devtype << 4);
//  if (devtype != BX_FDD_NONE) {
//    this.device_type[0] = 1 << (devtype - 1);
//    this.num_supported_floppies++;
//    this.statusbar_id[0] = bx_gui->register_statusitem(" A: ");
//  } else {
//    this.statusbar_id[0] = -1;
//  }
//
//  if (SIM->get_param_enum(BXPN_FLOPPYA_TYPE)->get() != BX_FLOPPY_NONE) {
//    if (SIM->get_param_enum(BXPN_FLOPPYA_STATUS)->get() == BX_INSERTED) {
//      this.media[0].write_protected = SIM->get_param_bool(BXPN_FLOPPYA_READONLY)->get();
//      if (evaluate_media(this.device_type[0], SIM->get_param_enum(BXPN_FLOPPYA_TYPE)->get(),
//                         SIM->get_param_string(BXPN_FLOPPYA_PATH)->getptr(), & this.media[0])) {
//        this.media_present[0] = 1;
//#define MED (this.media[0])
//        BX_INFO(("fd0: '%s' ro=%d, h=%d,t=%d,spt=%d",
//          SIM->get_param_string(BXPN_FLOPPYA_PATH)->getptr(),
//          MED.write_protected, MED.heads, MED.tracks, MED.sectors_per_track));
//        if (MED.write_protected)
//          SIM->get_param_bool(BXPN_FLOPPYA_READONLY)->set(1);
//#undef MED
//      } else {
//        SIM->get_param_enum(BXPN_FLOPPYA_STATUS)->set(BX_EJECTED);
//      }
//    }
//  }
//
//  //
//  // Floppy B setup
//  //
//
//  devtype = SIM->get_param_enum(BXPN_FLOPPYB_DEVTYPE)->get();
//  cmos_value |= devtype;
//  if (devtype != BX_FDD_NONE) {
//    this.device_type[1] = 1 << (devtype - 1);
//    this.num_supported_floppies++;
//    this.statusbar_id[1] = bx_gui->register_statusitem(" B: ");
//  } else {
//    this.statusbar_id[1] = -1;
//  }
//
//  if (SIM->get_param_enum(BXPN_FLOPPYB_TYPE)->get() != BX_FLOPPY_NONE) {
//    if (SIM->get_param_enum(BXPN_FLOPPYB_STATUS)->get() == BX_INSERTED) {
//      this.media[1].write_protected = SIM->get_param_bool(BXPN_FLOPPYB_READONLY)->get();
//      if (evaluate_media(this.device_type[1], SIM->get_param_enum(BXPN_FLOPPYB_TYPE)->get(),
//                         SIM->get_param_string(BXPN_FLOPPYB_PATH)->getptr(), & this.media[1])) {
//        this.media_present[1] = 1;
//#define MED (this.media[1])
//        BX_INFO(("fd1: '%s' ro=%d, h=%d,t=%d,spt=%d",
//          SIM->get_param_string(BXPN_FLOPPYB_PATH)->getptr(),
//          MED.write_protected, MED.heads, MED.tracks, MED.sectors_per_track));
//        if (MED.write_protected)
//          SIM->get_param_bool(BXPN_FLOPPYB_READONLY)->set(1);
//#undef MED
//      } else {
//        SIM->get_param_enum(BXPN_FLOPPYB_STATUS)->set(BX_EJECTED);
//      }
//    }
//  }
//
//  /* CMOS Floppy Type and Equipment Byte register */
//  DEV_cmos_set_reg(0x10, cmos_value);
//  if (this.num_supported_floppies > 0) {
//    DEV_cmos_set_reg(0x14, (DEV_cmos_get_reg(0x14) & 0x3e) |
//                          ((this.num_supported_floppies-1) << 6) | 1);
//  } else {
//    DEV_cmos_set_reg(0x14, (DEV_cmos_get_reg(0x14) & 0x3e));
//  }
//
//  if (this.floppy_timer_index == BX_NULL_TIMER_HANDLE) {
//    this.floppy_timer_index =
//      bx_pc_system.register_timer(this, timer_handler, 250, 0, 0, "floppy");
//  }
//  /* phase out s.non_dma in favor of using FD_MS_NDMA, more like hardware */
//  this.main_status_reg &= ~FD_MS_NDMA;  // enable DMA from start
//  /* these registers are not cleared by reset */
//  this.SRT = 0;
//  this.HUT = 0;
//  this.HLT = 0;
//
//  // runtime parameters
//  for (i = 0; i < 2; i++) {
//    sprintf(pname, "floppy.%d", i);
//    bx_list_c *floppy = (bx_list_c*)SIM->get_param(pname);
//    SIM->get_param_string("path", floppy)->set_handler(floppy_param_string_handler);
//    SIM->get_param_string("path", floppy)->set_runtime_param(1);
//    SIM->get_param_bool("readonly", floppy)->set_handler(floppy_param_handler);
//    SIM->get_param_bool("readonly", floppy)->set_runtime_param(1);
//    SIM->get_param_enum("status", floppy)->set_handler(floppy_param_handler);
//    SIM->get_param_enum("status", floppy)->set_runtime_param(1);
//  }
//  // register handler for correct floppy parameter handling after runtime config
//  SIM->register_runtime_config_handler(this, runtime_config_handler);
//#if BX_DEBUGGER
//  // register device for the 'info device' command (calls debug_dump())
//  bx_dbg_register_debug_info("floppy", this);
//#endif
//}
//
//void reset(int type)
//{
//  long i;
//
//  this.pending_irq = 0;
//  this.reset_sensei = 0; /* no reset result present */
//
//  this.main_status_reg = 0;
//  this.status_reg0 = 0;
//  this.status_reg1 = 0;
//  this.status_reg2 = 0;
//  this.status_reg3 = 0;
//
//  // software reset (via DOR port 0x3f2 bit 2) does not change DOR
//  if (type == BX_RESET_HARDWARE) {
//    this.DOR = 0x0c;
//    // motor off, drive 3..0
//    // DMA/INT enabled
//    // normal operation
//    // drive select 0
//
//    // DIR and CCR affected only by hard reset
//    for (i=0; i<4; i++) {
//      this.DIR[i] |= 0x80; // disk changed
//      }
//    this.data_rate = 2; /* 250 Kbps */
//    this.lock = 0;
//  } else {
//    BX_INFO(("controller reset in software"));
//  }
//  if (this.lock == 0) {
//    this.config = 0;
//    this.pretrk = 0;
//  }
//  this.perp_mode = 0;
//
//  for (i=0; i<4; i++) {
//    this.cylinder[i] = 0;
//    this.head[i] = 0;
//    this.sector[i] = 0;
//    this.eot[i] = 0;
//  }
//
//  DEV_pic_lower_irq(6);
//  if (!(this.main_status_reg & FD_MS_NDMA)) {
//    DEV_dma_set_drq(FLOPPY_DMA_CHAN, 0);
//  }
//  enter_idle_phase();
//}
//
//void register_state()
//{
//  int i;
//  char name[8];
//  bx_list_c *drive;
//
//  bx_list_c *list = new bx_list_c(SIM->get_bochs_root(), "floppy", "Floppy State");
//  new bx_shadow_num_c(list, "data_rate", &this.data_rate);
//  bx_list_c *command = new bx_list_c(list, "command");
//  for (i=0; i<10; i++) {
//    sprintf(name, "%d", i);
//    new bx_shadow_num_c(command, name, &this.command[i], BASE_HEX);
//  }
//  new bx_shadow_num_c(list, "command_index", &this.command_index);
//  new bx_shadow_num_c(list, "command_size", &this.command_size);
//  new bx_shadow_bool_c(list, "command_complete", &this.command_complete);
//  new bx_shadow_num_c(list, "pending_command", &this.pending_command, BASE_HEX);
//  new bx_shadow_bool_c(list, "multi_track", &this.multi_track);
//  new bx_shadow_bool_c(list, "pending_irq", &this.pending_irq);
//  new bx_shadow_num_c(list, "reset_sensei", &this.reset_sensei);
//  new bx_shadow_num_c(list, "format_count", &this.format_count);
//  new bx_shadow_num_c(list, "format_fillbyte", &this.format_fillbyte, BASE_HEX);
//  bx_list_c *result = new bx_list_c(list, "result");
//  for (i=0; i<10; i++) {
//    sprintf(name, "%d", i);
//    new bx_shadow_num_c(result, name, &this.result[i], BASE_HEX);
//  }
//  new bx_shadow_num_c(list, "result_index", &this.result_index);
//  new bx_shadow_num_c(list, "result_size", &this.result_size);
//  new bx_shadow_num_c(list, "DOR", &this.DOR, BASE_HEX);
//  new bx_shadow_num_c(list, "TDR", &this.TDR, BASE_HEX);
//  new bx_shadow_bool_c(list, "TC", &this.TC);
//  new bx_shadow_num_c(list, "main_status_reg", &this.main_status_reg, BASE_HEX);
//  new bx_shadow_num_c(list, "status_reg0", &this.status_reg0, BASE_HEX);
//  new bx_shadow_num_c(list, "status_reg1", &this.status_reg1, BASE_HEX);
//  new bx_shadow_num_c(list, "status_reg2", &this.status_reg2, BASE_HEX);
//  new bx_shadow_num_c(list, "status_reg3", &this.status_reg3, BASE_HEX);
//  new bx_shadow_num_c(list, "floppy_buffer_index", &this.floppy_buffer_index);
//  new bx_shadow_bool_c(list, "lock", &this.lock);
//  new bx_shadow_num_c(list, "SRT", &this.SRT, BASE_HEX);
//  new bx_shadow_num_c(list, "HUT", &this.HUT, BASE_HEX);
//  new bx_shadow_num_c(list, "HLT", &this.HLT, BASE_HEX);
//  new bx_shadow_num_c(list, "config", &this.config, BASE_HEX);
//  new bx_shadow_num_c(list, "pretrk", &this.pretrk);
//  new bx_shadow_num_c(list, "perp_mode", &this.perp_mode);
//  new bx_shadow_data_c(list, "buffer", this.floppy_buffer, 512);
//  for (i=0; i<4; i++) {
//    sprintf(name, "drive%d", i);
//    drive = new bx_list_c(list, name);
//    new bx_shadow_num_c(drive, "cylinder", &this.cylinder[i]);
//    new bx_shadow_num_c(drive, "head", &this.head[i]);
//    new bx_shadow_num_c(drive, "sector", &this.sector[i]);
//    new bx_shadow_num_c(drive, "eot", &this.eot[i]);
//    new bx_shadow_bool_c(drive, "media_present", &this.media_present[i]);
//    new bx_shadow_num_c(drive, "DIR", &this.DIR[i], BASE_HEX);
//  }
//}
//
//void after_restore_state()
//{
//  if (this.statusbar_id[0] >= 0) {
//    if ((this.DOR & 0x10) > 0)
//      bx_gui->statusbar_setitem(this.statusbar_id[0], 1);
//  }
//  if (this.statusbar_id[1] >= 0) {
//    if ((this.DOR & 0x20) > 0)
//      bx_gui->statusbar_setitem(this.statusbar_id[1], 1);
//  }
//}
//
//void runtime_config_handler(void *this_ptr)
//{
//  bx_floppy_ctrl_c *class_ptr = (bx_floppy_ctrl_c *) this_ptr;
//  class_ptr->runtime_config();
//}
//
//void runtime_config()
//{
//  int drive;
//  boolean status;
//  char pname[16];
//
//  for (drive=0; drive<2; drive++) {
//    if (this.media[drive].status_changed) {
//      sprintf(pname, "floppy.%d.status", drive);
//      status = (SIM->get_param_enum(pname)->get() == BX_INSERTED);
//      if (this.media_present[drive]) {
//        thiset_media_status(drive, 0);
//      }
//      if (status) {
//        thiset_media_status(drive, 1);
//      }
//      this.media[drive].status_changed = 0;
//    }
//  }
//}
//
//// static IO port read callback handler
//// redirects to non-static class handler to avoid virtual functions
//
//long read_handler(void *this_ptr, long address, int io_len)
//{
//#if !BX_USE_FD_SMF
//  bx_floppy_ctrl_c *class_ptr = (bx_floppy_ctrl_c *) this_ptr;
//
//  return class_ptr->read(address, io_len);
//}
//
///* reads from the floppy io ports */
//long read(long address, int io_len)
//{
//#else
//  UNUSED(this_ptr);
//#endif  // !BX_USE_FD_SMF
//  int value = 0, drive;
//
//  int pending_command = this.pending_command;
//  switch (address) {
//#if BX_DMA_FLOPPY_IO
//    case 0x3F2: // diskette controller digital output register
//      value = this.DOR;
//      break;
//
//    case 0x3F4: /* diskette controller main status register */
//      value = this.main_status_reg;
//      break;
//
//    case 0x3F5: /* diskette controller data */
//      if ((this.main_status_reg & FD_MS_NDMA) &&
//          ((this.pending_command & 0x4f) == 0x46)) {
//        dma_write(&value, 1);
//        lower_interrupt();
//        // don't enter idle phase until we've given CPU last data byte
//        if (this.TC) enter_idle_phase();
//      } else if (this.result_size == 0) {
//        BX_ERROR(("port 0x3f5: no results to read"));
//        this.main_status_reg &= FD_MS_NDMA;
//        value = this.result[0];
//      } else {
//        value = this.result[this.result_index++];
//        this.main_status_reg &= 0xF0;
//        BX_FD_THIS lower_interrupt();
//        if (this.result_index >= this.result_size) {
//          enter_idle_phase();
//        }
//      }
//      break;
//#endif  // #if BX_DMA_FLOPPY_IO
//
//    case 0x3F3: // Tape Drive Register
//      drive = this.DOR & 0x03;
//      if (this.media_present[drive]) {
//        switch (this.media[drive].type) {
//          case BX_FLOPPY_160K:
//          case BX_FLOPPY_180K:
//          case BX_FLOPPY_320K:
//          case BX_FLOPPY_360K:
//          case BX_FLOPPY_1_2:
//            value = 0x00;
//            break;
//          case BX_FLOPPY_720K:
//            value = 0xc0;
//            break;
//          case BX_FLOPPY_1_44:
//            value = 0x80;
//            break;
//          case BX_FLOPPY_2_88:
//            value = 0x40;
//            break;
//          default: // BX_FLOPPY_NONE
//            value = 0x20;
//            break;
//        }
//      } else {
//        value = 0x20;
//      }
//      break;
//
//    case 0x3F6: // Reserved for future floppy controllers
//                // This address shared with the hard drive controller
//      value = DEV_hd_read_handler(bx_devices.pluginHardDrive, address, io_len);
//      break;
//
//    case 0x3F7: // diskette controller digital input register
//      // This address shared with the hard drive controller:
//      //   Bit  7   : floppy
//      //   Bits 6..0: hard drive
//      value = DEV_hd_read_handler(bx_devices.pluginHardDrive, address, io_len);
//      value &= 0x7f;
//      // add in diskette change line if motor is on
//      drive = this.DOR & 0x03;
//      if (this.DOR & (1<<(drive+4))) {
//        value |= (this.DIR[drive] & 0x80);
//      }
//      break;
//
//    default:
//      BX_ERROR(("io_read: unsupported address 0x%04x", (int) address));
//      return(0);
//      break;
//  }
//  if (DEBUG) System.out.printf(("read(): during command 0x%02x, port 0x%04x returns 0x%02x",
//            pending_command, address, value));
//  return (value);
//}
//
//// static IO port write callback handler
//// redirects to non-static class handler to avoid virtual functions
//
//void write_handler(void *this_ptr, long address, long value, int io_len)
//{
//#if !BX_USE_FD_SMF
//  bx_floppy_ctrl_c *class_ptr = (bx_floppy_ctrl_c *) this_ptr;
//  class_ptr->write(address, value, io_len);
//}
//
///* writes to the floppy io ports */
//void write(long address, long value, int io_len)
//{
//#else
//  UNUSED(this_ptr);
//#endif  // !BX_USE_FD_SMF
//  int dma_and_interrupt_enable;
//  int normal_operation, prev_normal_operation;
//  int drive_select;
//  int motor_on_drive0, motor_on_drive1;
//
//  if (DEBUG) System.out.printf(("write access to port 0x%04x, value=0x%02x", address, value));
//
//  switch (address) {
//#if BX_DMA_FLOPPY_IO
//    case 0x3F2: /* diskette controller digital output register */
//      motor_on_drive0 = value & 0x10;
//      motor_on_drive1 = value & 0x20;
//      /* set status bar conditions for Floppy 0 and Floppy 1 */
//      if (this.statusbar_id[0] >= 0) {
//        if (motor_on_drive0 != (this.DOR & 0x10))
//          bx_gui->statusbar_setitem(this.statusbar_id[0], motor_on_drive0);
//      }
//      if (this.statusbar_id[1] >= 0) {
//        if (motor_on_drive1 != (this.DOR & 0x20))
//          bx_gui->statusbar_setitem(this.statusbar_id[1], motor_on_drive1);
//      }
//      dma_and_interrupt_enable = value & 0x08;
//      if (!dma_and_interrupt_enable)
//        if (DEBUG) System.out.printf(("DMA and interrupt capabilities disabled"));
//      normal_operation = value & 0x04;
//      drive_select = value & 0x03;
//
//      prev_normal_operation = this.DOR & 0x04;
//      this.DOR = value;
//
//      if (prev_normal_operation==0 && normal_operation) {
//        // transition from RESET to NORMAL
//        bx_pc_system.activate_timer(this.floppy_timer_index, 250, 0);
//      } else if (prev_normal_operation && normal_operation==0) {
//        // transition from NORMAL to RESET
//        this.main_status_reg &= FD_MS_NDMA;
//        this.pending_command = 0xfe; // RESET pending
//      }
//      if (DEBUG) System.out.printf(("io_write: digital output register"));
//      if (DEBUG) System.out.printf(("  motor on, drive1 = %d", motor_on_drive1 > 0));
//      if (DEBUG) System.out.printf(("  motor on, drive0 = %d", motor_on_drive0 > 0));
//      if (DEBUG) System.out.printf(("  dma_and_interrupt_enable=%02x",
//        (int) dma_and_interrupt_enable));
//      if (DEBUG) System.out.printf(("  normal_operation=%02x",
//        (int) normal_operation));
//      if (DEBUG) System.out.printf(("  drive_select=%02x",
//        (int) drive_select));
//      if (this.device_type[drive_select] == FDRIVE_NONE) {
//        if (DEBUG) System.out.printf(("WARNING: non existing drive selected"));
//      }
//      break;
//
//    case 0x3f4: /* diskette controller data rate select register */
//      this.data_rate = value & 0x03;
//      if (value & 0x80) {
//        this.main_status_reg &= FD_MS_NDMA;
//        this.pending_command = 0xfe; // RESET pending
//        bx_pc_system.activate_timer(this.floppy_timer_index, 250, 0);
//      }
//      if ((value & 0x7c) > 0) {
//        BX_ERROR(("write to data rate select register: unsupported bits set"));
//      }
//      break;
//
//    case 0x3F5: /* diskette controller data */
//      System.out.printf("floppy: io write 3f5\n");
//      if (DEBUG) System.out.printf(("command = 0x%02x", (int) value));
//      if ((this.main_status_reg & FD_MS_NDMA) && ((this.pending_command & 0x4f) == 0x45)) {
//        System.out.printf("floppy: io write 3f5 A\n");
//        BX_FD_THIS dma_read((int *) &value, 1);
//        BX_FD_THIS lower_interrupt();
//        break;
//      } else if (this.command_complete) {
//        System.out.printf("floppy: io write 3f5 B\n");
//        if (this.pending_command != 0)
//          BX_PANIC(("write 0x03f5: receiving new command 0x%02x, old one (0x%02x) pending",
//            value, this.pending_command));
//        this.command[0] = value;
//        this.command_complete = 0;
//        this.command_index = 1;
//        /* read/write command in progress */
//        this.main_status_reg &= ~FD_MS_DIO; // leave drive status untouched
//        this.main_status_reg |= FD_MS_MRQ | FD_MS_BUSY;
//        switch (value) {
//          case 0x03: /* specify */
//            this.command_size = 3;
//            break;
//          case 0x04: // get status
//            this.command_size = 2;
//            break;
//          case 0x07: /* recalibrate */
//            this.command_size = 2;
//            break;
//          case 0x08: /* sense interrupt status */
//            this.command_size = 1;
//            break;
//          case 0x0f: /* seek */
//            this.command_size = 3;
//            break;
//          case 0x4a: /* read ID */
//            this.command_size = 2;
//            break;
//          case 0x4d: /* format track */
//            this.command_size = 6;
//            break;
//          case 0x45:
//          case 0xc5: /* write normal data */
//            this.command_size = 9;
//            break;
//          case 0x46:
//          case 0x66:
//          case 0xc6:
//          case 0xe6: /* read normal data */
//            this.command_size = 9;
//            break;
//
//          case 0x0e: // dump registers (Enhanced drives)
//          case 0x10: // Version command, enhanced controller returns 0x90
//          case 0x14: // Unlock command (Enhanced)
//          case 0x94: // Lock command (Enhanced)
//            this.command_size = 0;
//            this.pending_command = value;
//            enter_result_phase();
//            break;
//          case 0x12: // Perpendicular mode (Enhanced)
//            this.command_size = 2;
//            break;
//          case 0x13: // Configure command (Enhanced)
//            this.command_size = 4;
//            break;
//
//          case 0x18: // National Semiconductor version command; return 80h
//            // These commands are not implemented on the standard
//            // controller and return an error.  They are available on
//            // the enhanced controller.
//            if (DEBUG) System.out.printf(("io_write: 0x3f5: unsupported floppy command 0x%02x",
//              (int) value));
//            this.command_size = 0;   // make sure we don't try to process this command
//            this.status_reg0 = 0x80; // status: invalid command
//            enter_result_phase();
//            break;
//
//          default:
//            BX_ERROR(("io_write: 0x3f5: invalid floppy command 0x%02x",
//              (int) value));
//            this.command_size = 0;   // make sure we don't try to process this command
//            this.status_reg0 = 0x80; // status: invalid command
//            enter_result_phase();
//            break;
//        }
//      } else {
//        System.out.printf("floppy: io write 3f5 C\n");
//        this.command[this.command_index++] =
//          value;
//      }
//      if (this.command_index ==
//        this.command_size) {
//        System.out.printf("floppy: io write 3f5 D\n");
//        /* read/write command not in progress any more */
//        floppy_command();
//        this.command_complete = 1;
//      }
//      if (DEBUG) System.out.printf(("io_write: diskette controller data"));
//      return;
//      break;
//#endif  // #if BX_DMA_FLOPPY_IO
//
//    case 0x3F6: /* diskette controller (reserved) */
//      if (DEBUG) System.out.printf(("io_write: reserved register 0x3f6 unsupported"));
//      // this address shared with the hard drive controller
//      DEV_hd_write_handler(bx_devices.pluginHardDrive, address, value, io_len);
//      break;
//
//#if BX_DMA_FLOPPY_IO
//    case 0x3F7: /* diskette controller configuration control register */
//      if ((value & 0x03) != this.data_rate)
//        BX_INFO(("io_write: config control register: 0x%02x", value));
//      this.data_rate = value & 0x03;
//      switch (this.data_rate) {
//        case 0: if (DEBUG) System.out.printf(("  500 Kbps")); break;
//        case 1: if (DEBUG) System.out.printf(("  300 Kbps")); break;
//        case 2: if (DEBUG) System.out.printf(("  250 Kbps")); break;
//        case 3: if (DEBUG) System.out.printf(("  1 Mbps")); break;
//      }
//      break;
//
//   default:
//      BX_ERROR(("io_write ignored: 0x%04x = 0x%02x", (int) address, (int) value));
//      break;
//#endif  // #if BX_DMA_FLOPPY_IO
//    }
//}
//
//void floppy_command()
//{
//  int i;
//  int motor_on;
//  int head, drive, cylinder, sector, eot;
//  int sector_size;
////int data_length;
//  long logical_sector, sector_time, step_delay;
//
//  // Print command
//  char buf[9+(9*5)+1], *p = buf;
//  p += sprintf(p, "COMMAND: ");
//  for (i=0; i<this.command_size; i++) {
//    p += sprintf(p, "[%02x] ", (int) this.command[i]);
//  }
//  if (DEBUG) System.out.printf(("%s", buf));
//  System.out.printf("floppy command: %s\n", buf);
//  this.pending_command = this.command[0];
//  switch (this.pending_command) {
//    case 0x03: // specify
//      // execution: specified parameters are loaded
//      // result: no result bytes, no interrupt
//      this.SRT = this.command[1] >> 4;
//      this.HUT = this.command[1] & 0x0f;
//      this.HLT = this.command[2] >> 1;
//      this.main_status_reg |= (this.command[2] & 0x01) ? FD_MS_NDMA : 0;
//      if (this.main_status_reg & FD_MS_NDMA)
//        BX_ERROR(("non DMA mode not fully implemented yet"));
//      enter_idle_phase();
//      return;
//
//    case 0x04: // get status
//      drive = (this.command[1] & 0x03);
//      this.head[drive] = (this.command[1] >> 2) & 0x01;
//      this.status_reg3 = 0x28 | (this.head[drive]<<2) | drive
//        | (this.media[drive].write_protected ? 0x40 : 0x00);
//      if ((this.device_type[drive] != FDRIVE_NONE) &&
//          (this.cylinder[drive] == 0))
//        this.status_reg3 |= 0x10;
//      enter_result_phase();
//      return;
//
//    case 0x07: // recalibrate
//      drive = (this.command[1] & 0x03);
//      this.DOR &= 0xfc;
//      this.DOR |= drive;
//      if (DEBUG) System.out.printf(("floppy_command(): recalibrate drive %u",
//        (int) drive));
//      step_delay = calculate_step_delay(drive, 0);
//      bx_pc_system.activate_timer(this.floppy_timer_index, step_delay, 0);
//      /* command head to track 0
//       * controller set to non-busy
//       * error condition noted in Status reg 0's equipment check bit
//       * seek end bit set to 1 in Status reg 0 regardless of outcome
//       * The last two are taken care of in timer().
//       */
//      this.cylinder[drive] = 0;
//      this.main_status_reg &= FD_MS_NDMA;
//      this.main_status_reg |= (1 << drive);
//      return;
//
//    case 0x08: /* sense interrupt status */
//      /* execution:
//       *   get status
//       * result:
//       *   no interupt
//       *   byte0 = status reg0
//       *   byte1 = current cylinder number (0 to 79)
//       */
//      if (this.reset_sensei > 0) {
//        drive = 4 - this.reset_sensei;
//        this.status_reg0 &= 0xf8;
//        this.status_reg0 |= (this.head[drive] << 2) | drive;
//        this.reset_sensei--;
//      } else if (!this.pending_irq) {
//        this.status_reg0 = 0x80;
//      }
//      if (DEBUG) System.out.printf(("sense interrupt status"));
//      enter_result_phase();
//      return;
//
//    case 0x0f: /* seek */
//      /* command:
//       *   byte0 = 0F
//       *   byte1 = drive & head select
//       *   byte2 = cylinder number
//       * execution:
//       *   postion head over specified cylinder
//       * result:
//       *   no result bytes, issues an interrupt
//       */
//      drive = this.command[1] & 0x03;
//      this.DOR &= 0xfc;
//      this.DOR |= drive;
//
//      this.head[drive] = (this.command[1] >> 2) & 0x01;
//      step_delay = calculate_step_delay(drive, this.command[2]);
//      bx_pc_system.activate_timer(this.floppy_timer_index, step_delay, 0);
//      /* ??? should also check cylinder validity */
//      this.cylinder[drive] = this.command[2];
//      /* data reg not ready, drive not busy */
//      this.main_status_reg &= FD_MS_NDMA;
//      this.main_status_reg |= (1 << drive);
//      return;
//
//    case 0x13: // Configure
//      if (DEBUG) System.out.printf(("configure (eis     = 0x%02x)", this.command[2] & 0x40));
//      if (DEBUG) System.out.printf(("configure (efifo   = 0x%02x)", this.command[2] & 0x20));
//      if (DEBUG) System.out.printf(("configure (no poll = 0x%02x)", this.command[2] & 0x10));
//      if (DEBUG) System.out.printf(("configure (fifothr = 0x%02x)", this.command[2] & 0x0f));
//      if (DEBUG) System.out.printf(("configure (pretrk  = 0x%02x)", this.command[3]));
//      this.config = this.command[2];
//      this.pretrk = this.command[3];
//      enter_idle_phase();
//      return;
//
//    case 0x4a: // read ID
//      drive = this.command[1] & 0x03;
//      this.head[drive] = (this.command[1] >> 2) & 0x01;
//      this.DOR &= 0xfc;
//      this.DOR |= drive;
//
//      motor_on = (this.DOR>>(drive+4)) & 0x01;
//      if (motor_on == 0) {
//        BX_ERROR(("floppy_command(): read ID: motor not on"));
//        this.main_status_reg &= FD_MS_NDMA;
//        this.main_status_reg |= FD_MS_BUSY;
//        return; // Hang controller
//      }
//      if (this.device_type[drive] == FDRIVE_NONE) {
//        BX_ERROR(("floppy_command(): read ID: bad drive #%d", drive));
//        this.main_status_reg &= FD_MS_NDMA;
//        this.main_status_reg |= FD_MS_BUSY;
//        return; // Hang controller
//      }
//      if (this.media_present[drive] == 0) {
//        BX_INFO(("attempt to read sector ID with media not present"));
//        this.main_status_reg &= FD_MS_NDMA;
//        this.main_status_reg |= FD_MS_BUSY;
//        return; // Hang controller
//      }
//      this.status_reg0 = (this.head[drive]<<2) | drive;
//      // time to read one sector at 300 rpm
//      sector_time = 200000 / this.media[drive].sectors_per_track;
//      bx_pc_system.activate_timer(this.floppy_timer_index, sector_time, 0);
//      /* data reg not ready, controller busy */
//      this.main_status_reg &= FD_MS_NDMA;
//      this.main_status_reg |= FD_MS_BUSY;
//      return;
//
//    case 0x4d: // format track
//        drive = this.command[1] & 0x03;
//        this.DOR &= 0xfc;
//        this.DOR |= drive;
//
//        motor_on = (this.DOR>>(drive+4)) & 0x01;
//        if (motor_on == 0)
//          BX_PANIC(("floppy_command(): format track: motor not on"));
//        this.head[drive] = (this.command[1] >> 2) & 0x01;
//        sector_size = this.command[2];
//        this.format_count = this.command[3];
//        this.format_fillbyte = this.command[5];
//        if (this.device_type[drive] == FDRIVE_NONE)
//          BX_PANIC(("floppy_command(): format track: bad drive #%d", drive));
//
//        if (sector_size != 0x02) { // 512 bytes
//          BX_PANIC(("format track: sector size %d not supported", 128<<sector_size));
//        }
//        if (this.format_count != this.media[drive].sectors_per_track) {
//          BX_PANIC(("format track: %d sectors/track requested (%d expected)",
//                    this.format_count, this.media[drive].sectors_per_track));
//        }
//        if (this.media_present[drive] == 0) {
//          BX_INFO(("attempt to format track with media not present"));
//          return; // Hang controller
//        }
//        if (this.media[drive].write_protected) {
//          // media write-protected, return error
//          BX_INFO(("attempt to format track with media write-protected"));
//          this.status_reg0 = 0x40 | (this.head[drive]<<2) | drive; // abnormal termination
//          this.status_reg1 = 0x27; // 0010 0111
//          this.status_reg2 = 0x31; // 0011 0001
//          enter_result_phase();
//          return;
//        }
//
//      /* 4 header bytes per sector are required */
//      this.format_count <<= 2;
//
//      if (this.main_status_reg & FD_MS_NDMA) {
//        if (DEBUG) System.out.printf(("non-DMA floppy format unimplemented"));
//      } else {
//        DEV_dma_set_drq(FLOPPY_DMA_CHAN, 1);
//      }
//      /* data reg not ready, controller busy */
//      this.main_status_reg &= FD_MS_NDMA;
//      this.main_status_reg |= FD_MS_BUSY;
//      if (DEBUG) System.out.printf(("format track"));
//      return;
//
//    case 0x46: // read normal data, MT=0, SK=0
//    case 0x66: // read normal data, MT=0, SK=1
//    case 0xc6: // read normal data, MT=1, SK=0
//    case 0xe6: // read normal data, MT=1, SK=1
//    case 0x45: // write normal data, MT=0
//    case 0xc5: // write normal data, MT=1
//      this.multi_track = (this.command[0] >> 7);
//      if ((this.DOR & 0x08) == 0)
//        BX_PANIC(("read/write command with DMA and int disabled"));
//      drive = this.command[1] & 0x03;
//      this.DOR &= 0xfc;
//      this.DOR |= drive;
//
//      motor_on = (this.DOR>>(drive+4)) & 0x01;
//      if (motor_on == 0)
//        BX_PANIC(("floppy_command(): read/write: motor not on"));
//      head = this.command[3] & 0x01;
//      cylinder = this.command[2]; /* 0..79 depending */
//      sector = this.command[4];   /* 1..36 depending */
//      eot = this.command[6];      /* 1..36 depending */
//      sector_size = this.command[5];
////    data_length = this.command[8];
//      if (DEBUG) System.out.printf(("read/write normal data"));
//      if (DEBUG) System.out.printf(("BEFORE"));
//      if (DEBUG) System.out.printf(("  drive    = %u", (int) drive));
//      if (DEBUG) System.out.printf(("  head     = %u", (int) head));
//      if (DEBUG) System.out.printf(("  cylinder = %u", (int) cylinder));
//      if (DEBUG) System.out.printf(("  sector   = %u", (int) sector));
//      if (DEBUG) System.out.printf(("  eot      = %u", (int) eot));
//      if (this.device_type[drive] == FDRIVE_NONE)
//        BX_PANIC(("floppy_command(): read/write: bad drive #%d", drive));
//
//      // check that head number in command[1] bit two matches the head
//      // reported in the head number field.  Real floppy drives are
//      // picky about this, as reported in SF bug #439945, (Floppy drive
//      // read input error checking).
//      if (head != ((this.command[1]>>2)&1)) {
//        BX_ERROR(("head number in command[1] doesn't match head field"));
//        this.status_reg0 = 0x40 | (this.head[drive]<<2) | drive; // abnormal termination
//        this.status_reg1 = 0x04; // 0000 0100
//        this.status_reg2 = 0x00; // 0000 0000
//        enter_result_phase();
//        return;
//      }
//
//      if (this.media_present[drive] == 0) {
//        BX_INFO(("attempt to read/write sector %u with media not present", (int) sector));
//        return; // Hang controller
//      }
//
//      if (sector_size != 0x02) { // 512 bytes
//        BX_PANIC(("read/write command: sector size %d not supported", 128<<sector_size));
//      }
//
//      if (cylinder >= this.media[drive].tracks) {
//        BX_PANIC(("io: norm r/w parms out of range: sec#%02xh cyl#%02xh eot#%02xh head#%02xh",
//          (int) sector, (int) cylinder, (int) eot,
//          (int) head));
//        return;
//      }
//
//      if (sector > this.media[drive].sectors_per_track) {
//        BX_INFO(("attempt to read/write sector %u past last sector %u",
//                     (int) sector,
//                     (int) this.media[drive].sectors_per_track));
//        this.cylinder[drive] = cylinder;
//        this.head[drive]     = head;
//        this.sector[drive]   = sector;
//
//        this.status_reg0 = 0x40 | (this.head[drive]<<2) | drive;
//        this.status_reg1 = 0x04;
//        this.status_reg2 = 0x00;
//        enter_result_phase();
//        return;
//      }
//
//      if (cylinder != this.cylinder[drive]) {
//        if (DEBUG) System.out.printf(("io: cylinder request != current cylinder"));
//        reset_changeline();
//      }
//
//      logical_sector = (cylinder * this.media[drive].heads * this.media[drive].sectors_per_track) +
//                       (head * this.media[drive].sectors_per_track) +
//                       (sector - 1);
//
//      if (logical_sector >= this.media[drive].sectors) {
//        BX_PANIC(("io: logical sector out of bounds"));
//      }
//      // This hack makes older versions of the Bochs BIOS work
//      if (eot == 0) {
//        eot = this.media[drive].sectors_per_track;
//      }
//      this.cylinder[drive] = cylinder;
//      this.head[drive]     = head;
//      this.sector[drive]   = sector;
//      this.eot[drive]      = eot;
//
//      if ((this.command[0] & 0x4f) == 0x46) { // read
//        floppy_xfer(drive, logical_sector*512, this.floppy_buffer,
//                    512, FROM_FLOPPY);
//        /* controller busy; if DMA mode, data reg not ready */
//        this.main_status_reg &= FD_MS_NDMA;
//        this.main_status_reg |= FD_MS_BUSY;
//        if (this.main_status_reg & FD_MS_NDMA) {
//          this.main_status_reg |= (FD_MS_MRQ | FD_MS_DIO);
//        }
//        // time to read one sector at 300 rpm
//        sector_time = 200000 / this.media[drive].sectors_per_track;
//        bx_pc_system.activate_timer(this.floppy_timer_index,
//                                    sector_time , 0);
//      } else if ((this.command[0] & 0x7f) == 0x45) { // write
//        /* controller busy; if DMA mode, data reg not ready */
//        this.main_status_reg &= FD_MS_NDMA;
//        this.main_status_reg |= FD_MS_BUSY;
//        if (this.main_status_reg & FD_MS_NDMA) {
//          this.main_status_reg |= FD_MS_MRQ;
//        } else {
//          DEV_dma_set_drq(FLOPPY_DMA_CHAN, 1);
//        }
//      } else {
//        BX_PANIC(("floppy_command(): unknown read/write command"));
//        return;
//      }
//      break;
//
//    case 0x12: // Perpendicular mode
//      this.perp_mode = this.command[1];
//      BX_INFO(("perpendicular mode: config=0x%02x", this.perp_mode));
//      enter_idle_phase();
//      break;
//
//    default: // invalid or unsupported command; these are captured in write() above
//      BX_PANIC(("You should never get here! cmd = 0x%02x",
//                this.command[0]));
//  }
//}
//
//void floppy_xfer(int drive, long offset, int *buffer,
//            long bytes, int direction)
//{
//  int ret = 0;
//
//  if (this.device_type[drive] == FDRIVE_NONE)
//    BX_PANIC(("floppy_xfer: bad drive #%d", drive));
//
//  if (DEBUG) System.out.printf(("floppy_xfer: drive=%u, offset=%u, bytes=%u, direction=%s floppy",
//            drive, offset, bytes, (direction==FROM_FLOPPY)? "from" : "to"));
//
//#if BX_WITH_MACOS
//  if (strcmp(SIM->get_param_string(BXPN_FLOPPYA_PATH)->getptr(), SuperDrive))
//#endif
//  {
//    if (this.media[drive].vvfat_floppy) {
//      ret = (int)this.media[drive].vvfat->lseek(offset, SEEK_SET);
//    } else {
//      ret = (int)lseek(this.media[drive].fd, offset, SEEK_SET);
//    }
//    if (ret < 0) {
//      BX_PANIC(("could not perform lseek() to %d on floppy image file", offset));
//      return;
//    }
//  }
//
//  if (direction == FROM_FLOPPY) {
//    if (this.media[drive].vvfat_floppy) {
//      ret = this.media[drive].vvfat->read(buffer, bytes);
//#if BX_WITH_MACOS
//    } else if (!strcmp(SIM->get_param_string(BXPN_FLOPPYA_PATH)->getptr(), SuperDrive))
//      ret = fd_read((char *) buffer, offset, bytes);
//#endif
//    } else {
//      ret = ::read(this.media[drive].fd, (bx_ptr_t) buffer, bytes);
//    }
//    if (ret < int(bytes)) {
//      if (ret > 0) {
//        BX_INFO(("partial read() on floppy image returns %u/%u",
//          (int) ret, (int) bytes));
//        memset(buffer + ret, 0, bytes - ret);
//      } else {
//        BX_INFO(("read() on floppy image returns 0"));
//        memset(buffer, 0, bytes);
//      }
//    }
//  } else { // TO_FLOPPY
//    BX_ASSERT (!this.media[drive].write_protected);
//    if (this.media[drive].vvfat_floppy) {
//      ret = this.media[drive].vvfat->write(buffer, bytes);
//#if BX_WITH_MACOS
//    } else if (!strcmp(SIM->get_param_string(BXPN_FLOPPYA_PATH)->getptr(), SuperDrive))
//      ret = fd_write((char *) buffer, offset, bytes);
//#endif
//    } else {
//      ret = ::write(this.media[drive].fd, (bx_ptr_t) buffer, bytes);
//    }
//    if (ret < int(bytes)) {
//      BX_PANIC(("could not perform write() on floppy image file"));
//    }
//  }
//}
//
//void timer_handler(void *this_ptr)
//{
//  bx_floppy_ctrl_c *class_ptr = (bx_floppy_ctrl_c *) this_ptr;
//  class_ptr->timer();
//}
//
//void timer()
//{
//  int drive, motor_on;
//
//  drive = this.DOR & 0x03;
//  switch (this.pending_command) {
//    case 0x07: // recal
//      this.status_reg0 = 0x20 | drive;
//      motor_on = ((this.DOR>>(drive+4)) & 0x01);
//      if ((this.device_type[drive] == FDRIVE_NONE) || (motor_on == 0)) {
//        this.status_reg0 |= 0x50;
//      }
//      enter_idle_phase();
//      BX_FD_THIS raise_interrupt();
//      break;
//
//    case 0x0f: // seek
//      this.status_reg0 = 0x20 | (this.head[drive]<<2) | drive;
//      enter_idle_phase();
//      BX_FD_THIS raise_interrupt();
//      break;
//
//    case 0x4a: /* read ID */
//      enter_result_phase();
//      break;
//
//    case 0x45: /* write normal data */
//    case 0xc5:
//      if (this.TC) { // Terminal Count line, done
//        this.status_reg0 = (this.head[drive] << 2) | drive;
//        this.status_reg1 = 0;
//        this.status_reg2 = 0;
//
//        if (DEBUG) System.out.printf(("<<WRITE DONE>>"));
//        if (DEBUG) System.out.printf(("AFTER"));
//        if (DEBUG) System.out.printf(("  drive    = %u", drive));
//        if (DEBUG) System.out.printf(("  head     = %u", this.head[drive]));
//        if (DEBUG) System.out.printf(("  cylinder = %u", this.cylinder[drive]));
//        if (DEBUG) System.out.printf(("  sector   = %u", this.sector[drive]));
//
//        enter_result_phase();
//      } else {
//        // transfer next sector
//        if (!(this.main_status_reg & FD_MS_NDMA)) {
//          DEV_dma_set_drq(FLOPPY_DMA_CHAN, 1);
//        }
//      }
//      break;
//
//    case 0x46: /* read normal data */
//    case 0x66:
//    case 0xc6:
//    case 0xe6:
//      // transfer next sector
//      if (this.main_status_reg & FD_MS_NDMA) {
//        this.main_status_reg &= ~FD_MS_BUSY;  // clear busy bit
//        this.main_status_reg |= FD_MS_MRQ | FD_MS_DIO;  // data byte waiting
//      } else {
//        DEV_dma_set_drq(FLOPPY_DMA_CHAN, 1);
//      }
//      break;
//
//    case 0x4d: /* format track */
//      if ((this.format_count == 0) || this.TC) {
//        this.format_count = 0;
//        this.status_reg0 = (this.head[drive] << 2) | drive;
//        enter_result_phase();
//      } else {
//        // transfer next sector
//        if (!(this.main_status_reg & FD_MS_NDMA)) {
//          DEV_dma_set_drq(FLOPPY_DMA_CHAN, 1);
//        }
//      }
//      break;
//
//    case 0xfe: // (contrived) RESET
//      theFloppyController->reset(BX_RESET_SOFTWARE);
//      this.pending_command = 0;
//      this.status_reg0 = 0xc0;
//      BX_FD_THIS raise_interrupt();
//      this.reset_sensei = 4;
//      break;
//
//    case 0x00: // nothing pending?
//      break;
//
//    default:
//      BX_PANIC(("floppy:timer(): unknown case %02x",
//        (int) this.pending_command));
//  }
//}
//
//int dma_write(int *buffer, int maxlen)
//{
//  // A DMA write is from I/O to Memory
//  // We need to return the next data byte(s) from the floppy buffer
//  // to be transfered via the DMA to memory. (read block from floppy)
//  //
//  // maxlen is the maximum length of the DMA transfer
//
//  int drive = this.DOR & 0x03;
//  int len = 512 - this.floppy_buffer_index;
//  if (len > maxlen) len = maxlen;
//  memcpy(buffer, &this.floppy_buffer[this.floppy_buffer_index], len);
//  this.floppy_buffer_index += len;
//  this.TC = get_tc() && (len == maxlen);
//
//  if ((this.floppy_buffer_index >= 512) || (this.TC)) {
//
//    if (this.floppy_buffer_index >= 512) {
//      increment_sector(); // increment to next sector before retrieving next one
//      this.floppy_buffer_index = 0;
//    }
//    if (this.TC) { // Terminal Count line, done
//      this.status_reg0 = (this.head[drive] << 2) | drive;
//      this.status_reg1 = 0;
//      this.status_reg2 = 0;
//
//      if (DEBUG) System.out.printf(("<<READ DONE>>"));
//      if (DEBUG) System.out.printf(("AFTER"));
//      if (DEBUG) System.out.printf(("  drive    = %u", drive));
//      if (DEBUG) System.out.printf(("  head     = %u", this.head[drive]));
//      if (DEBUG) System.out.printf(("  cylinder = %u", this.cylinder[drive]));
//      if (DEBUG) System.out.printf(("  sector   = %u", this.sector[drive]));
//
//      if (!(this.main_status_reg & FD_MS_NDMA)) {
//        DEV_dma_set_drq(FLOPPY_DMA_CHAN, 0);
//      }
//      enter_result_phase();
//    } else { // more data to transfer
//      long logical_sector, sector_time;
//
//      // remember that not all floppies have two sides, multiply by s.head[drive]
//      logical_sector = (this.cylinder[drive] * this.media[drive].heads *
//                        this.media[drive].sectors_per_track) +
//                       (this.head[drive] *
//                        this.media[drive].sectors_per_track) +
//                       (this.sector[drive] - 1);
//
//      floppy_xfer(drive, logical_sector*512, this.floppy_buffer,
//                  512, FROM_FLOPPY);
//      if (!(this.main_status_reg & FD_MS_NDMA)) {
//        DEV_dma_set_drq(FLOPPY_DMA_CHAN, 0);
//      }
//      // time to read one sector at 300 rpm
//      sector_time = 200000 / this.media[drive].sectors_per_track;
//      bx_pc_system.activate_timer(this.floppy_timer_index,
//                                  sector_time , 0);
//    }
//  }
//  return len;
//}
//
//int dma_read(int *buffer, int maxlen)
//{
//  // A DMA read is from Memory to I/O
//  // We need to write the data_byte which was already transfered from memory
//  // via DMA to I/O (write block to floppy)
//  //
//  // maxlen is the length of the DMA transfer (not implemented yet)
//
//  int drive = this.DOR & 0x03;
//  long logical_sector, sector_time;
//
//  if (this.pending_command == 0x4d) { // format track in progress
//    this.format_count--;
//    switch (3 - (this.format_count & 0x03)) {
//      case 0:
//        this.cylinder[drive] = *buffer;
//        break;
//      case 1:
//        if (*buffer != this.head[drive])
//          BX_ERROR(("head number does not match head field"));
//        break;
//      case 2:
//        this.sector[drive] = *buffer;
//        break;
//      case 3:
//        if (*buffer != 2) BX_ERROR(("dma_read: sector size %d not supported", 128<<(*buffer)));
//        if (DEBUG) System.out.printf(("formatting cylinder %u head %u sector %u",
//                  this.cylinder[drive], this.head[drive],
//                  this.sector[drive]));
//        for (int i = 0; i < 512; i++) {
//          this.floppy_buffer[i] = this.format_fillbyte;
//        }
//        logical_sector = (this.cylinder[drive] * this.media[drive].heads * this.media[drive].sectors_per_track) +
//                         (this.head[drive] * this.media[drive].sectors_per_track) +
//                         (this.sector[drive] - 1);
//        floppy_xfer(drive, logical_sector*512, this.floppy_buffer,
//                    512, TO_FLOPPY);
//        if (!(this.main_status_reg & FD_MS_NDMA)) {
//          DEV_dma_set_drq(FLOPPY_DMA_CHAN, 0);
//        }
//        // time to write one sector at 300 rpm
//        sector_time = 200000 / this.media[drive].sectors_per_track;
//        bx_pc_system.activate_timer(this.floppy_timer_index,
//                                    sector_time , 0);
//        break;
//    }
//    return 1;
//  } else { // write normal data
//    int len = 512 - this.floppy_buffer_index;
//    if (len > maxlen) len = maxlen;
//    memcpy(&this.floppy_buffer[this.floppy_buffer_index], buffer, len);
//    this.floppy_buffer_index += len;
//    this.TC = get_tc() && (len == maxlen);
//
//    if ((this.floppy_buffer_index >= 512) || (this.TC)) {
//      logical_sector = (this.cylinder[drive] * this.media[drive].heads * this.media[drive].sectors_per_track) +
//                       (this.head[drive] * this.media[drive].sectors_per_track) +
//                       (this.sector[drive] - 1);
//      if (this.media[drive].write_protected) {
//        // write protected error
//        BX_INFO(("tried to write disk %u, which is write-protected", drive));
//        // ST0: IC1,0=01  (abnormal termination: started execution but failed)
//        this.status_reg0 = 0x40 | (this.head[drive]<<2) | drive;
//        // ST1: DataError=1, NDAT=1, NotWritable=1, NID=1
//        this.status_reg1 = 0x27; // 0010 0111
//        // ST2: CRCE=1, SERR=1, BCYL=1, NDAM=1.
//        this.status_reg2 = 0x31; // 0011 0001
//        enter_result_phase();
//        return 1;
//      }
//      floppy_xfer(drive, logical_sector*512, this.floppy_buffer,
//                  512, TO_FLOPPY);
//      increment_sector(); // increment to next sector after writing current one
//      this.floppy_buffer_index = 0;
//      if (!(this.main_status_reg & FD_MS_NDMA)) {
//        DEV_dma_set_drq(FLOPPY_DMA_CHAN, 0);
//      }
//      // time to write one sector at 300 rpm
//      sector_time = 200000 / this.media[drive].sectors_per_track;
//      bx_pc_system.activate_timer(this.floppy_timer_index,
//                                  sector_time , 0);
//      // the following is a kludge; i (jc) don't know how to work with the timer
//      if ((this.main_status_reg & FD_MS_NDMA) && this.TC) {
//        enter_result_phase();
//      }
//    }
//    return len;
//  }
//}
//
//void raise_interrupt()
//{
//  System.out.printf("Floppy: raise irq 6\n");
//  DEV_pic_raise_irq(6);
//  this.pending_irq = 1;
//  this.reset_sensei = 0;
//}
//
//void lower_interrupt()
//{
//  if (this.pending_irq) {
//    DEV_pic_lower_irq(6);
//    this.pending_irq = 0;
//  }
//}
//
//void increment_sector()
//{
//  int drive;
//
//  drive = this.DOR & 0x03;
//
//  // values after completion of data xfer
//  // ??? calculation depends on base_count being multiple of 512
//  this.sector[drive] ++;
//  if ((this.sector[drive] > this.eot[drive]) ||
//      (this.sector[drive] > this.media[drive].sectors_per_track)) {
//    this.sector[drive] = 1;
//    if (this.multi_track) {
//      this.head[drive] ++;
//      if (this.head[drive] > 1) {
//        this.head[drive] = 0;
//        this.cylinder[drive] ++;
//        reset_changeline();
//      }
//    } else {
//      this.cylinder[drive] ++;
//      reset_changeline();
//    }
//    if (this.cylinder[drive] >= this.media[drive].tracks) {
//      // Set to 1 past last possible cylinder value.
//      // I notice if I set it to tracks-1, prama linux won't boot.
//      this.cylinder[drive] = this.media[drive].tracks;
//      BX_INFO(("increment_sector: clamping cylinder to max"));
//    }
//  }
//}
//
//int set_media_status(int drive, boolean status)
//{
//  char *path;
//  int type;
//
//  if (drive == 0)
//    type = SIM->get_param_enum(BXPN_FLOPPYA_TYPE)->get();
//  else
//    type = SIM->get_param_enum(BXPN_FLOPPYB_TYPE)->get();
//
//  // if setting to the current value, nothing to do
//  if ((status == this.media_present[drive]) &&
//      ((status == 0) || (type == this.media[drive].type)))
//    return(status);
//
//  if (status == 0) {
//    // eject floppy
//    close_media(&this.media[drive]);
//    this.media_present[drive] = 0;
//    if (drive == 0) {
//      SIM->get_param_enum(BXPN_FLOPPYA_STATUS)->set(BX_EJECTED);
//    } else {
//      SIM->get_param_enum(BXPN_FLOPPYB_STATUS)->set(BX_EJECTED);
//    }
//    this.DIR[drive] |= 0x80; // disk changed line
//    return(0);
//  } else {
//    // insert floppy
//    if (drive == 0) {
//      path = SIM->get_param_string(BXPN_FLOPPYA_PATH)->getptr();
//    } else {
//      path = SIM->get_param_string(BXPN_FLOPPYB_PATH)->getptr();
//    }
//    if (!strcmp(path, "none"))
//      return(0);
//    if (evaluate_media(this.device_type[drive], type, path, & this.media[drive])) {
//      this.media_present[drive] = 1;
//      if (drive == 0) {
//#define MED (this.media[0])
//        BX_INFO(("fd0: '%s' ro=%d, h=%d,t=%d,spt=%d",
//          SIM->get_param_string(BXPN_FLOPPYA_PATH)->getptr(),
//          MED.write_protected, MED.heads, MED.tracks, MED.sectors_per_track));
//        if (MED.write_protected)
//          SIM->get_param_bool(BXPN_FLOPPYA_READONLY)->set(1);
//#undef MED
//        SIM->get_param_enum(BXPN_FLOPPYA_STATUS)->set(BX_INSERTED);
//      } else {
//#define MED (this.media[1])
//        BX_INFO(("fd1: '%s' ro=%d, h=%d,t=%d,spt=%d",
//          SIM->get_param_string(BXPN_FLOPPYB_PATH)->getptr(),
//          MED.write_protected, MED.heads, MED.tracks, MED.sectors_per_track));
//        if (MED.write_protected)
//          SIM->get_param_bool(BXPN_FLOPPYB_READONLY)->set(1);
//#undef MED
//        SIM->get_param_enum(BXPN_FLOPPYB_STATUS)->set(BX_INSERTED);
//      }
//      return(1);
//    } else {
//      this.media_present[drive] = 0;
//      if (drive == 0) {
//        SIM->get_param_enum(BXPN_FLOPPYA_STATUS)->set(BX_EJECTED);
//        SIM->get_param_enum(BXPN_FLOPPYA_TYPE)->set(BX_FLOPPY_NONE);
//      } else {
//        SIM->get_param_enum(BXPN_FLOPPYB_STATUS)->set(BX_EJECTED);
//        SIM->get_param_enum(BXPN_FLOPPYB_TYPE)->set(BX_FLOPPY_NONE);
//      }
//      return(0);
//    }
//  }
//}
//
//#ifdef O_BINARY
//#define BX_RDONLY O_RDONLY | O_BINARY
//#define BX_RDWR O_RDWR | O_BINARY
//#else
//public static int BX_RDONLY = O_RDONLY;
//public static int BX_RDWR = O_RDWR;
//#endif
//
//boolean evaluate_media(int devtype, int type, char *path, floppy_t *media)
//{
//  struct stat stat_buf;
//  int i, ret;
//  int type_idx = -1;
//#ifdef __linux__
//  struct floppy_struct floppy_geom;
//#endif
//#ifdef WIN32
//  char sTemp[1024];
//  boolean raw_floppy = 0;
//  HANDLE hFile;
//  DWORD bytes;
//  DISK_GEOMETRY dg;
//  int tracks = 0, heads = 0, spt = 0;
//#endif
//
//  //If media file is already open, close it before reopening.
//  close_media(media);
//
//  // check media type
//  if (type == BX_FLOPPY_NONE) {
//    return 0;
//  }
//  for (i = 0; i < 8; i++) {
//    if (type == floppy_type[i].id) type_idx = i;
//  }
//  if (type_idx == -1) {
//    BX_ERROR(("evaluate_media: unknown media type %d", type));
//    return 0;
//  }
//  if ((floppy_type[type_idx].drive_mask & devtype) == 0) {
//    BX_ERROR(("evaluate_media: media type %d not valid for this floppy drive", type));
//    return 0;
//  }
//
//  // use virtual VFAT support if requested
//  if (!strncmp(path, "vvfat:", 6) && (devtype == FDRIVE_350HD)) {
//    media->vvfat = DEV_hdimage_init_image(BX_HDIMAGE_MODE_VVFAT, 1474560, "");
//    if (media->vvfat != NULL) {
//      if (media->vvfat->open(path + 6) == 0) {
//        media->type              = BX_FLOPPY_1_44;
//        media->tracks            = media->vvfat->cylinders;
//        media->heads             = media->vvfat->heads;
//        media->sectors_per_track = media->vvfat->spt;
//        media->sectors           = 2880;
//        media->vvfat_floppy = 1;
//        media->fd = 0;
//      }
//    }
//    if (media->vvfat_floppy) return 1;
//  }
//  // open media file (image file or device)
//#ifdef macintosh
//  media->fd = 0;
//  if (strcmp(SIM->get_param_string(BXPN_FLOPPYA_PATH)->getptr(), SuperDrive))
//#endif
//#ifdef WIN32
//  if ((isalpha(path[0])) && (path[1] == ':') && (strlen(path) == 2)) {
//    raw_floppy = 1;
//    wsprintf(sTemp, "\\\\.\\%s", path);
//    hFile = CreateFile(sTemp, GENERIC_READ, FILE_SHARE_WRITE, NULL,
//                       OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
//    if (hFile == INVALID_HANDLE_VALUE) {
//      BX_ERROR(("Cannot open floppy drive"));
//      return(0);
//    }
//    if (!DeviceIoControl(hFile, IOCTL_DISK_GET_DRIVE_GEOMETRY, NULL, 0, &dg, sizeof(dg), &bytes, NULL)) {
//      BX_ERROR(("No media in floppy drive"));
//      CloseHandle(hFile);
//      return(0);
//    } else {
//      tracks = (int)dg.Cylinders.QuadPart;
//      heads  = (int)dg.TracksPerCylinder;
//      spt    = (int)dg.SectorsPerTrack;
//    }
//    CloseHandle(hFile);
//    if (!media->write_protected)
//      media->fd = open(sTemp, BX_RDWR);
//    else
//      media->fd = open(sTemp, BX_RDONLY);
//  }
//  else
//#endif
//  {
//    if (!media->write_protected)
//      media->fd = open(path, BX_RDWR);
//    else
//      media->fd = open(path, BX_RDONLY);
//  }
//
//  if (!media->write_protected && (media->fd < 0)) {
//    BX_INFO(("tried to open '%s' read/write: %s",path,strerror(errno)));
//    // try opening the file read-only
//    media->write_protected = 1;
//#ifdef macintosh
//    media->fd = 0;
//    if (strcmp(SIM->get_param_string(BXPN_FLOPPYA_PATH)->getptr(), SuperDrive))
//#endif
//#ifdef WIN32
//    if (raw_floppy == 1)
//      media->fd = open(sTemp, BX_RDONLY);
//    else
//#endif
//      media->fd = open(path, BX_RDONLY);
//
//    if (media->fd < 0) {
//      // failed to open read-only too
//      BX_INFO(("tried to open '%s' read only: %s",path,strerror(errno)));
//      media->type = type;
//      return(0);
//    }
//  }
//
//#if BX_WITH_MACOS
//  if (!strcmp(SIM->get_param_string(BXPN_FLOPPYA_PATH)->getptr(), SuperDrive))
//    ret = fd_stat(&stat_buf);
//  else
//    ret = fstat(media->fd, &stat_buf);
//#elif defined(WIN32)
//  if (raw_floppy) {
//    memset (&stat_buf, 0, sizeof(stat_buf));
//    stat_buf.st_mode = S_IFCHR;
//    ret = 0;
//  }
//  else
//#endif
//  { // unix
//    ret = fstat(media->fd, &stat_buf);
//  }
//  if (ret) {
//    BX_PANIC(("fstat floppy 0 drive image file returns error: %s", strerror(errno)));
//    return(0);
//  }
//
//  if (S_ISREG(stat_buf.st_mode)) {
//    // regular file
//    switch (type) {
//      // use CMOS reserved types
//      case BX_FLOPPY_160K: // 160K 5.25"
//      case BX_FLOPPY_180K: // 180K 5.25"
//      case BX_FLOPPY_320K: // 320K 5.25"
//      // standard floppy types
//      case BX_FLOPPY_360K: // 360K 5.25"
//      case BX_FLOPPY_720K: // 720K 3.5"
//      case BX_FLOPPY_1_2: // 1.2M 5.25"
//      case BX_FLOPPY_2_88: // 2.88M 3.5"
//        media->type              = type;
//        media->tracks            = floppy_type[type_idx].trk;
//        media->heads             = floppy_type[type_idx].hd;
//        media->sectors_per_track = floppy_type[type_idx].spt;
//        media->sectors           = floppy_type[type_idx].sectors;
//        if (stat_buf.st_size > (int)(media->sectors * 512)) {
//          BX_ERROR(("evaluate_media: size of file '%s' (%lu) too large for selected type",
//                   path, (int long) stat_buf.st_size));
//          return 0;
//        }
//        break;
//      default: // 1.44M 3.5"
//        media->type              = type;
//        if (stat_buf.st_size <= 1474560) {
//          media->tracks            = floppy_type[type_idx].trk;
//          media->heads             = floppy_type[type_idx].hd;
//          media->sectors_per_track = floppy_type[type_idx].spt;
//        }
//        else if (stat_buf.st_size == 1720320)
//        {
//          media->sectors_per_track = 21;
//          media->tracks            = 80;
//          media->heads             = 2;
//        }
//        else if (stat_buf.st_size == 1763328)
//        {
//          media->sectors_per_track = 21;
//          media->tracks            = 82;
//          media->heads             = 2;
//        }
//        else if (stat_buf.st_size == 1884160)
//        {
//          media->sectors_per_track = 23;
//          media->tracks            = 80;
//          media->heads             = 2;
//        }
//        else
//        {
//          BX_ERROR(("evaluate_media: file '%s' of unknown size %lu",
//            path, (int long) stat_buf.st_size));
//          return 0;
//        }
//        media->sectors = media->heads * media->tracks * media->sectors_per_track;
//    }
//    return (media->sectors > 0); // success
//  }
//
//  else if (S_ISCHR(stat_buf.st_mode)
//#if BX_WITH_MACOS == 0
//#ifdef S_ISBLK
//            || S_ISBLK(stat_buf.st_mode)
//#endif
//#endif
//           ) {
//    // character or block device
//    // assume media is formatted to typical geometry for drive
//    media->type              = type;
//#ifdef __linux__
//    if (ioctl(media->fd, FDGETPRM, &floppy_geom) < 0) {
//      BX_ERROR(("cannot determine media geometry, trying to use defaults"));
//      media->tracks            = floppy_type[type_idx].trk;
//      media->heads             = floppy_type[type_idx].hd;
//      media->sectors_per_track = floppy_type[type_idx].spt;
//      media->sectors           = floppy_type[type_idx].sectors;
//      return (media->sectors > 0);
//    }
//    media->tracks            = floppy_geom.track;
//    media->heads             = floppy_geom.head;
//    media->sectors_per_track = floppy_geom.sect;
//    media->sectors           = floppy_geom.size;
//#elif defined(WIN32)
//    media->tracks            = tracks;
//    media->heads             = heads;
//    media->sectors_per_track = spt;
//    media->sectors = media->heads * media->tracks * media->sectors_per_track;
//#else
//    media->tracks            = floppy_type[type_idx].trk;
//    media->heads             = floppy_type[type_idx].hd;
//    media->sectors_per_track = floppy_type[type_idx].spt;
//    media->sectors           = floppy_type[type_idx].sectors;
//#endif
//    return (media->sectors > 0); // success
//  } else {
//    // unknown file type
//    BX_ERROR(("unknown mode type"));
//    return 0;
//  }
//}
//
//
//void enter_result_phase()
//{
//  int drive;
//  int i;
//
//  drive = this.DOR & 0x03;
//
//  /* these are always the same */
//  this.result_index = 0;
//  // not necessary to clear any status bits, we're about to set them all
//  this.main_status_reg |= FD_MS_MRQ | FD_MS_DIO | FD_MS_BUSY;
//
//  /* invalid command */
//  if ((this.status_reg0 & 0xc0) == 0x80) {
//    this.result_size = 1;
//    this.result[0] = this.status_reg0;
//    return;
//  }
//
//  switch (this.pending_command) {
//    case 0x04: // get status
//      this.result_size = 1;
//      this.result[0] = this.status_reg3;
//      break;
//    case 0x08: // sense interrupt
//      this.result_size = 2;
//      this.result[0] = this.status_reg0;
//      this.result[1] = this.cylinder[drive];
//      break;
//    case 0x0e: // dump registers
//      this.result_size = 10;
//      for (i = 0; i < 4; i++) {
//        this.result[i] = this.cylinder[i];
//      }
//      this.result[4] = (this.SRT << 4) | this.HUT;
//      this.result[5] = (this.HLT << 1) | ((this.main_status_reg & FD_MS_NDMA) ? 1 : 0);
//      this.result[6] = this.eot[drive];
//      this.result[7] = (this.lock << 7) | (this.perp_mode & 0x7f);
//      this.result[8] = this.config;
//      this.result[9] = this.pretrk;
//      break;
//    case 0x10: // version
//      this.result_size = 1;
//      this.result[0] = 0x90;
//      break;
//    case 0x14: // unlock
//    case 0x94: // lock
//      this.lock = (this.pending_command >> 7);
//      this.result_size = 1;
//      this.result[0] = (this.lock << 4);
//      break;
//    case 0x4a: // read ID
//    case 0x4d: // format track
//    case 0x46: // read normal data
//    case 0x66:
//    case 0xc6:
//    case 0xe6:
//    case 0x45: // write normal data
//    case 0xc5:
//      this.result_size = 7;
//      this.result[0] = this.status_reg0;
//      this.result[1] = this.status_reg1;
//      this.result[2] = this.status_reg2;
//      this.result[3] = this.cylinder[drive];
//      this.result[4] = this.head[drive];
//      this.result[5] = this.sector[drive];
//      this.result[6] = 2; /* sector size code */
//      BX_FD_THIS raise_interrupt();
//      break;
//  }
//
//  // Print command result (max. 10 bytes)
//  char buf[8+(10*5)+1], *p = buf;
//  p += sprintf(p, "RESULT: ");
//  for (i=0; i<this.result_size; i++) {
//    p += sprintf(p, "[%02x] ", (int) this.result[i]);
//  }
//  if (DEBUG) System.out.printf(("%s", buf));
//}
//
//void enter_idle_phase()
//{
//  this.main_status_reg &= (FD_MS_NDMA | 0x0f);  // leave drive status untouched
//  this.main_status_reg |= FD_MS_MRQ; // data register ready
//
//  this.command_complete = 1; /* waiting for new command */
//  this.command_index = 0;
//  this.command_size = 0;
//  this.pending_command = 0;
//  this.result_size = 0;
//
//  this.floppy_buffer_index = 0;
//}
//
//long calculate_step_delay(int drive, int new_cylinder)
//{
//  int steps;
//  long one_step_delay;
//
//  if (new_cylinder == this.cylinder[drive]) {
//    steps = 1;
//  } else {
//    steps = abs(new_cylinder - this.cylinder[drive]);
//    reset_changeline();
//  }
//  one_step_delay = ((this.SRT ^ 0x0f) + 1) * 500000 / drate_in_k[this.data_rate];
//  return (steps * one_step_delay);
//}
//
//void reset_changeline()
//{
//  int drive = this.DOR & 0x03;
//  if (this.media_present[drive])
//    this.DIR[drive] &= ~0x80;
//}
//
//boolean get_tc()
//{
//  int drive;
//  boolean terminal_count;
//  if (this.main_status_reg & FD_MS_NDMA) {
//    drive = this.DOR & 0x03;
//    /* figure out if we've sent all the data, in non-DMA mode...
//     * the drive stays on the same cylinder for a read or write, so that's
//     * not going to be an issue. EOT stands for the last sector to be I/Od.
//     * it does all the head 0 sectors first, then the second if any.
//     * now, regarding reaching the end of the sector:
//     *  == 512 would make it more precise, allowing one to spot bugs...
//     *  >= 512 makes it more robust, but allows for sloppy code...
//     *  pick your poison?
//     * note: byte and head are 0-based; eot, sector, and heads are 1-based. */
//    terminal_count = ((this.floppy_buffer_index == 512) &&
//     (this.sector[drive] == this.eot[drive]) &&
//     (this.head[drive] == (this.media[drive].heads - 1)));
//  } else {
//    terminal_count = DEV_dma_get_tc();
//  }
//  return terminal_count;
//}
//
//// floppy runtime parameter handling
//
//long floppy_param_handler(bx_param_c *param, int set, long val)
//{
//  bx_list_c *base = (bx_list_c*) param->get_parent();
//  int drive;
//
//  if (set) {
//    drive = atoi(base->get_name());
//    if (!strcmp(param->get_name(), "status")) {
//      this.media[drive].status_changed = 1;
//    } else if (!strcmp(param->get_name(), "readonly")) {
//      this.media[drive].write_protected = (boolean)val;
//      this.media[drive].status_changed = 1;
//    }
//  }
//  return val;
//}
//
//const char* floppy_param_string_handler(bx_param_string_c *param,
//                                int set, const char *oldval, const char *val, int maxlen)
//{
//  char pname[BX_PATHNAME_LEN];
//  int drive;
//
//  bx_list_c *base = (bx_list_c*) param->get_parent();
//  if ((strlen(val) < 1) || !strcmp ("none", val)) {
//    val = "none";
//  }
//  param->get_param_path(pname, BX_PATHNAME_LEN);
//  if ((!strcmp(pname, BXPN_FLOPPYA_PATH)) ||
//      (!strcmp(pname, BXPN_FLOPPYB_PATH))) {
//    if (set==1) {
//      drive = atoi(base->get_name());
//      if (SIM->get_param_enum("devtype", base)->get() == BX_FDD_NONE) {
//        BX_ERROR(("Cannot add a floppy drive at runtime"));
//        SIM->get_param_string("path", base)->set("none");
//      }
//      if (SIM->get_param_enum("status", base)->get() == BX_INSERTED) {
//        // tell the device model that we removed, then inserted the disk
//        this.media[drive].status_changed = 1;
//      }
//    }
//  } else {
//    BX_PANIC(("floppy_param_string_handler called with unknown parameter '%s'", pname));
//  }
//  return val;
//}
//
//#if BX_DEBUGGER
//void debug_dump(int argc, char **argv)
//{
//  int i;
//
//  dbg_printf("i82077AA FDC\n\n");
//  for (i = 0; i < 2; i++) {
//    dbg_printf("fd%d: ", i);
//    if (this.device_type[i] == FDRIVE_NONE) {
//      dbg_printf("not installed\n");
//    } else if (this.media[i].type == BX_FLOPPY_NONE) {
//      dbg_printf("media not present\n");
//    } else {
//#define MED (this.media[i])
//      dbg_printf("tracks=%d, heads=%d, spt=%d, readonly=%d\n",
//                 MED.tracks, MED.heads, MED.sectors_per_track, MED.write_protected);
//#undef MED
//    }
//  }
//  dbg_printf("\ncontroller status: ");
//  if (this.pending_command == 0) {
//    if (this.command_complete) {
//      dbg_printf("idle phase\n");
//    } else {
//      dbg_printf("command phase (command=0x%02x)\n", this.command[0]);
//    }
//  } else {
//    if (this.result_size == 0) {
//      dbg_printf("execution phase (command=0x%02x)\n", this.pending_command);
//    } else {
//      dbg_printf("result phase (command=0x%02x)\n", this.pending_command);
//    }
//  }
//  dbg_printf("DOR = 0x%02x\n", this.DOR);
//  dbg_printf("MSR = 0x%02x\n", this.main_status_reg);
//  dbg_printf("DSR = 0x%02x\n", this.data_rate);
//  if (argc > 0) {
//    dbg_printf("\nAdditional options not supported\n");
//  }
//}
//#endif
}