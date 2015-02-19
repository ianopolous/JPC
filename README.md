#JPC
The fast x86 PC emulator in 100% pure Java
-----
JPC is a fast modern x86 PC emulator capable of booting Windows up to Windows 95 (and windows 98 in safe mode) and some graphical linuxes. It has a full featured graphical debugger with time travel mode along with standard features like break and watch points. 

###Running
To get started with JPC just run it with:<br>
java -jar JPCApplication.jar -boot hda -hda yourdiskimage.img<br><br>
or get a list of options by running:<br>
java -jar JPCApplication.jar -help

###Building
To build JPC run:<br>
make application

To build the debugger run:<br>
make debugger

To run some dos games easily, put them in a directory on your real computer and use JPC's ability to view a directory tree as a virtual FAT32 drive. For example, if some games are in "dosgames" in the directory where you expanded all the JPC files then type:<br/>
	java -jar JPCApplication.jar -boot fda -fda resources/images/floppy.img -hda dir:dosgames<br/>
This won't save any writes you make to the virtual FAT32 drive. If you would like to sync write to the underlying files, run with -hda dir:sync:dosgames<br/>

###Debugger
The JPC debugger allows you to run x86 code step by step, use breakpoints, memory watchpoints, directly view the memory, cpu state etc in a nice colourful GUI.
Once you've set your disks in the debugger (you can pass command line options identical to the Application or just use the menus), click "create new pc" in the File menu. Then to start execution click "start" in the run menu.
![Debugger](/resources/debugger.png)

###Credits
* The BIOS used in JPC is the Bochs BIOS; see http://bochs.sourceforge.net/
* The VGA BIOS used in JPC is the Plex86/Bochs LGPL'd bios; see http://www.nongnu.org/vgabios
* The test Floppy image "floppy.img" is from the Odin FreeDOS project; see http://odin.fdos.org/

###History
The JPC project was originally started in the Particle Physics department of Oxford university by Dr Rhys Newman and Dr Jeff Tseng. The original team included Chris Dennis, Ian Preston, Mike Moleschi and Guillaume Kirsch. The current team includes Ian Preston and Kevin O'Dwyer. 
