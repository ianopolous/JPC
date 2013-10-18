JAVA_BUILD_OPTS = -source 1.5 -target 1.5 -g

.PHONY: build
build: jpc

.PHONY: debugger
debugger: build_core
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
	    `find org/jpc/debugger -name \*.java` \
	    `find org/jpc/classfile -name \*.java` \
	    `find org/jpc/debugger -name \*.java` \
	    `find org/jpc/j2se -name \*.java`

.PHONY: tools
tools:
	mkdir -p build
	javac $(JAVA_BUILD_OPTS) -d build `find tools -name \*.java`
	echo "Name: JPC Tools" > application.manifest
	echo "Main-Class: tools.Tools" >> application.manifest
	echo "Class-Path: Tools.jar:." >> jpc.manifest

	jar -cfm Tools.jar application.manifest \
	    -C build tools
	rm -f application.manifest

.PHONY: tests
tests:
	mkdir -p build
	javac $(JAVA_BUILD_OPTS) -d build `find tools -name \*.java` \
	`find org/jpc/emulator/execution/decoder -name \*.java` \
	org/jpc/emulator/execution/Executable.java \
	org/jpc/j2se/Option.java
	echo "Name: JPC Tools" > application.manifest
	echo "Main-Class: tools.TestGenerator" >> application.manifest
	echo "Class-Path: Tools.jar:." >> jpc.manifest

	jar -cfm TestGen.jar application.manifest \
	    -C build tools -C build org/jpc/emulator/execution/decoder \
	    -C build org/jpc/emulator/execution/Executable.class \
	    -C build org/jpc/j2se
	rm -f application.manifest

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

.PHONY: application
application: build_core
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
	    resources/soundbank-min.gm \
	    -C build org/jpc/classfile -C build org/jpc/emulator \
	    -C build org/jpc/support -C build org/jpc/j2se -C build org/jpc/debugger
	rm -f application.manifest