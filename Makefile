.PHONY: all test testSingle testE clean
.SUFFIXES: .java .class
.java.class:
	javac -d bin/ src/*.java

CLASSES=src/*.java

default: classes
classes: $(CLASSES:.java=.class)
javadoc:
	javadoc -d doc/ src/*.java
clean:
	$(RM) bin/*.class
cleandoc:
	$(RM) -r doc/*
testSmooth:
	${RM} bin/*.class
	javac -d bin/ src/*.java
	echo "\nsmooth_no_noise_100disk_1step\n"
	java -cp bin VTRACK a "smooth" testOutput

testSingle:
	${RM} bin/*.class
	javac -d bin/ src/*.java
	java -cp bin VTRACK a "testSingle" testOutput

testE:
	${RM} bin/*.class
	javac -d bin/ src/*.java
	java -cp bin VTRACK a "testE" testOutput

test:
	${RM} bin/*.class
	javac -d bin/ src/*.java
	echo "\nsmooth_60disk_1step\n"
	java -cp bin VTRACK a "test" testOutput

Smooth:
	echo "\nsmooth_no_noise_100disk_1step\n"
	java -cp bin VTRACK a "smooth" 100Disk

Error:
	java -cp bin VTRACK a "testE" testOutput

60Disk:
	echo "\nsmooth_60disk_1step\n"
	java -cp bin VTRACK a "test" 60Disk
