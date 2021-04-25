JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
CLASSPATH=instrumentation/bin

.PHONY: mkdirs

all: sootOutput/Main.class

clean:
	( rm -fr bin sootOutput || true ) 2>/dev/null
	( rm pathConstraints.txt || true ) 2>/dev/null
	( rm deps.txt rt-deps.txt conc-deps.txt || true ) 2>/dev/null

sootOutput/Main.class: bin/Main.class
	./instrument.bash
	
bin/%.class: %.java
	mkdir -p bin
	$(JAVA_HOME)/bin/javac -d bin -cp "$(CLASSPATH)" $<
