JAVA_BUILD_OPTS = -source 1.5 -target 1.5

.PHONY: build
build: jpc

.PHONY: debugger
debugger:
	mkdir -p build
	echo "Name: JPC Debugger" > debugger.manifest
	echo "Main-Class: org.jpc.debugger.JPC" >> debugger.manifest
	echo "Build-Date: " `date` >> debugger.manifest
	echo "Default-Args: -fda mem:resources/images/floppy.img -hda mem:resources/images/dosgames.img -boot fda" >> debugger.manifest
	echo "" >> debugger.manifest

	jar -cfm JPCDebugger.jar debugger.manifest \
	    resources/bios/vgabios.bin \
	    resources/bios/bios.bin resources/images/dosgames.img \
	    resources/images/floppy.img resources/icon.png \
	    resources/licence.html resources/jpc.png \
	    resources/smallpause.png \
	    resources/smallplay.png \
	    resources/tick.png \
	    -C build org/jpc/classfile -C build org/jpc/emulator \
	    -C build org/jpc/support -C build org/jpc/j2se \
	    -C build org/jpc/debugger
	rm -f debugger.manifest

.PHONY: build_core
build_core:
	mkdir -p build
	javac $(JAVA_BUILD_OPTS) -d build `find org/jpc/emulator -name \*.java` \
	    `find org/jpc/support -name \*.java` \
	    `find org/jpc/classfile -name \*.java` \
	    `find org/jpc/debugger -name \*.java` \
	    `find org/jpc/j2se -name \*.java`

.PHONY: build_nereus
build_nereus:
	mkdir -p build
	javac $(JAVA_BUILD_OPTS) -cp ".;NereusClient.jar" -d build `find org/jpc/nereus -name \*.java`

.PHONY: clean
clean:
	rm -Rf build
	rm -f jpc_core.jar jpc.jar jpc.manifest application.jar \
	    application.manifest linuxapplication.jar linuxapplication.manifest \
	    applet.jar applet.manifest core.manifest

.PHONY: cleanse
cleanse: clean
	rm -f `find . -iname \*.class`
	rm -f `find . -name \*~ -o -name \*#`

.PHONY: core
core: build_core
	echo "Name: JPC Core" > core.manifest
	echo "Build-Date:" `date` >> core.manifest
	echo "" >> core.manifest
	jar -cf jpc_core.jar resources/bios/bios.bin resources/bios/vgabios.bin \
	    -C build org/jpc/classfile -C build org/jpc/emulator \
	    -C build org/jpc/support -C build org/jpc/j2se \
		resources/smallpause.png \
		resources/smallplay.png
	rm -f core.manifest

.PHONY: jpc
jpc: core
	echo "Name: JPC" > jpc.manifest
	echo "Main-Class: org.jpc.j2se.PCMonitorFrame" >> jpc.manifest
	echo "Class-Path: jpc_core.jar" >> jpc.manifest
	echo "Build-Date: " `date` >> jpc.manifest
	echo "" >> jpc.manifest
	jar -cfm jpc.jar jpc.manifest resources/images/floppy.img resources/images/dosgames.img
	rm -f jpc.manifest

.PHONY: application
application: core
	echo "Name: JPC Application" > application.manifest
	echo "Main-Class: org.jpc.j2se.JPCApplication" >> application.manifest
	echo "Build-Date: " `date` >> application.manifest
	echo "Default-Args: -fda mem:resources/images/floppy.img -hda mem:resources/images/dosgames.img -boot fda" >> application.manifest
	echo "" >> application.manifest

	jar -cfm JPCApplication.jar application.manifest \
	    resources/bios/vgabios.bin \
	    resources/bios/bios.bin resources/images/dosgames.img \
	    resources/images/floppy.img resources/icon.png \
	    resources/licence.html resources/jpc.png \
	    resources/smallpause.png \
	    resources/smallplay.png \
	    resources/tick.png \
	    -C build org/jpc/classfile -C build org/jpc/emulator \
	    -C build org/jpc/support -C build org/jpc/j2se
	rm -f application.manifest

.PHONY: linuxapplication
linuxapplication: core
	echo "Name: JPC Linux Application" > linuxapplication.manifest
	echo "Main-Class: org.jpc.j2se.JPCApplication" >> linuxapplication.manifest
	echo "Class-Path: jpc_core.jar" >> linuxapplication.manifest
	echo "Build-Date: " `date` >> linuxapplication.manifest
	echo "Default-Args: -hda mem:resources/images/linux.img -boot hda" >> linuxapplication.manifest
	echo "" >> linuxapplication.manifest

	jar -cfm linuxapplication.jar linuxapplication.manifest \
	    resources/bios/vgabios.bin \
	    resources/bios/bios.bin resources/images/linux.img \
	    resources/icon.png resources/licence.html resources/jpc.png
	    resources/smallpause.png \
	    resources/smallplay.png \
	    resources/tick.png
	rm -f linuxapplication.manifest

.PHONY: applet
applet: build_core
	echo "Name: JPC Applet" > applet.manifest
	echo "Main-Class: org.jpc.j2se.JPCApplet" >> applet.manifest
	echo "Build-Date: " `date` >> applet.manifest
	echo "" >> applet.manifest

	jar -cfm JPCApplet.jar applet.manifest \
	resources/bios/bios.bin \
	resources/bios/vgabios.bin \
	resources/JPCLogo.png \
	resources/smallpause.png \
	resources/smallplay.png \
	resources/tick.png \
	-C build org/jpc/classfile \
	-C build org/jpc/emulator \
	-C build org/jpc/support \
	-C build org/jpc/j2se
	rm -f applet.manifest
