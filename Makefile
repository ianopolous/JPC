JAVA_BUILD_OPTS = -source 1.6 -target 1.6 -g
JAVA_RELEASE_OPTS = -source 1.6 -target 1.6

.PHONY: build
build: jpc

.PHONY: debugger
debugger: build_core
	mkdir -p build
	echo "Name: JPC Debugger" > debugger.manifest
	echo "Author: Ian Preston" >> debugger.manifest
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
	    -C build org/jpc/emulator \
	    -C build org/jpc/support -C build org/jpc/j2se \
	    -C build org/jpc/debugger
	rm -f debugger.manifest
	jar -i JPCDebugger.jar

.PHONY: build_core
build_core:
	mkdir -p build
	javac $(JAVA_BUILD_OPTS) -d build `find src/org/jpc/emulator -name \*.java` \
	    `find src/org/jpc/support -name \*.java` \
	    `find src/org/jpc/debugger -name \*.java` \
	    `find src/org/jpc/j2se -name \*.java`

.PHONY: tools
tools: build_core
	mkdir -p build
	javac $(JAVA_BUILD_OPTS) -cp build/ -d build `find src/tools -name \*.java`
	echo "Name: JPC Tools" > jpc.manifest
	echo "Main-Class: tools.Tools" >> jpc.manifest
	echo "Author: Ian Preston" >> jpc.manifest

	jar -cfm Tools.jar jpc.manifest \
	    -C build tools
	rm -f jpc.manifest

.PHONY: tests
tests: build_core
	mkdir -p build
	javac $(JAVA_BUILD_OPTS) -cp build/ -d build `find src/tools -name \*.java` \
	`find src/org/jpc/emulator/execution/decoder -name \*.java` \
	src/org/jpc/emulator/execution/Executable.java \
	src/org/jpc/j2se/Option.java
	echo "Name: JPC Tools" > jpc.manifest
	echo "Main-Class: tools.TestGenerator" >> jpc.manifest
	echo "Class-Path: Tools.jar:." >> jpc.manifest

	jar -cfm TestGen.jar jpc.manifest \
	    -C build tools -C build org/jpc/emulator/execution/decoder \
	    -C build org/jpc/emulator/execution/Executable.class \
	    -C build org/jpc/j2se
	rm -f jpc.manifest

.PHONY: clean
clean:
	rm -Rf build
	rm -f jpc.manifest

.PHONY: cleanse
cleanse: clean
	rm -f `find . -iname \*.class`
	rm -f `find . -name \*~ -o -name \*#`

.PHONY: fast
fast: build_core
	echo "Name: JPC Application" > jpc.manifest
	echo "Author: Ian Preston" >> jpc.manifest
	echo "Main-Class: org.jpc.j2se.JPCApplication" >> jpc.manifest
	echo "Build-Date: " `date` >> jpc.manifest
	echo "Default-Args: -fda mem:resources/images/floppy.img -hda mem:resources/images/dosgames.img -boot fda" >> jpc.manifest
	echo "" >> jpc.manifest

	jar -cfm JPCApplication.jar jpc.manifest \
	    resources/bios/vgabios.bin \
	    resources/bios/bios.bin  \
	    resources/images/floppy.img resources/icon.png \
	    resources/licence.html resources/jpc.png \
	    resources/smallpause.png \
	    resources/smallplay.png \
	    resources/tick.png \
	    resources/soundbank-min.gm \
	    -C build org/jpc/emulator \
	    -C build org/jpc/support -C build org/jpc/j2se -C build org/jpc/debugger
	rm -f jpc.manifest

.PHONY: application
application: fast
	jar -i JPCApplication.jar

.PHONY: release
release: build_core
	echo "Name: JPC Application" > jpc.manifest
	echo "Author: Ian Preston" >> jpc.manifest
	echo "Main-Class: org.jpc.j2se.JPCApplication" >> jpc.manifest
	echo "Build-Date: " `date` >> jpc.manifest
	echo "Default-Args: -fda mem:resources/images/floppy.img -hda mem:resources/images/dosgames.img -boot fda" >> jpc.manifest
	echo "" >> jpc.manifest

	jar -cfm JPCApplication.jar jpc.manifest \
	    resources/bios/vgabios.bin \
	    resources/bios/bios.bin resources/images/dosgames.img \
	    resources/images/floppy.img resources/icon.png \
	    resources/licence.html resources/jpc.png \
	    resources/smallpause.png \
	    resources/smallplay.png \
	    resources/tick.png \
	    resources/soundbank-min.gm \
	    -C build org/jpc/emulator \
	    -C build org/jpc/support -C build org/jpc/j2se -C build org/jpc/debugger
	rm -f jpc.manifest
	jar -i JPCApplication.jar

.PHONY: secondapplication
secondapplication: build_core
	echo "Name: JPC Application" > jpc.manifest
	echo "Main-Class: org.jpc.j2se.JPCApplication" >> jpc.manifest
	echo "Build-Date: " `date` >> jpc.manifest
	echo "Default-Args: -fda mem:resources/images/floppy.img -hda mem:resources/images/dosgames.img -boot fda" >> jpc.manifest
	echo "" >> jpc.manifest

	jar -cfm JPCApplication2.jar jpc.manifest \
	    resources/bios/vgabios.bin \
	    resources/bios/fuzzerBIOS resources/images/dosgames.img \
	    resources/images/floppy.img resources/icon.png \
	    resources/licence.html resources/jpc.png \
	    resources/smallpause.png \
	    resources/smallplay.png \
	    resources/tick.png \
	    resources/soundbank-min.gm \
	    -C build org/jpc/emulator \
	    -C build org/jpc/support -C build org/jpc/j2se -C build org/jpc/debugger
	rm -f jpc.manifest
	jar -i JPCApplication2.jar
