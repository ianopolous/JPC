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

###History
The JPC project was originally started in the Particle Physics department of Oxford university by Dr Rhys Newman and Dr Jeff Tseng. The original team included Chris Dennis, Ian Preston, Mike Moleschi and Guillaume Kirsch. The current team includes Ian Preston and Kevin O'Dwyer. 
