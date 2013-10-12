java -cp JPCApplication.jar:Tools.jar tools.Tools
make application
java -cp JPCApplication.jar:Tools.jar tools.Tools -decoder > org/jpc/emulator/execution/decoder/ExecutableTables.java
make application