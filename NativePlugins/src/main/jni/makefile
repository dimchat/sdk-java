
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home
CFLAGS=-fPIC -Wall -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/darwin

LIBS=/Users/moky/Documents/GitHub/dimchat/sdk-java/NativePlugins/libs

clean:
	rm -f $(LIBS)/libSecp256k1.so

all:
	gcc $(CFLAGS) -shared -o $(LIBS)/libSecp256k1.so -lc ecc_jni.c micro-ecc/uECC.c
