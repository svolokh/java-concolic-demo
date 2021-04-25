#!/bin/bash

JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
CLASSPATH=instrumentation/bin

LIBRARY_DEPS_OUT=deps.txt $JAVA_HOME/bin/java -Xmx4g -cp java-concolic-1.0-SNAPSHOT.jar csci699cav.Main \
	-prepend-classpath -soot-class-path "$CLASSPATH:bin" -keep-offset -process-dir bin

cat deps.txt | sed 1d > rt-deps.txt
rm deps.txt
if [[ $(cat rt-deps.txt | wc -l) > 0 ]]; then
	ENTRYPOINTS=rt-deps.txt $JAVA_HOME/bin/java -Xmx4g -cp java-concolic-1.0-SNAPSHOT.jar csci699cav.Main \
		-prepend-classpath -soot-class-path "$CLASSPATH:bin" -keep-offset  -process-dir "$JAVA_HOME/jre/lib/rt.jar" -allow-phantom-refs
fi

cp -r "$CLASSPATH"/* sootOutput
echo '<csci699cav.Concolic: void assume(boolean)>' > conc-deps.txt
echo '<csci699cav.Concolic: void assertTrue(boolean)>' >> conc-deps.txt
echo '<csci699cav.Concolic: void assertFalse(boolean)>' >> conc-deps.txt
ENTRYPOINTS=conc-deps.txt $JAVA_HOME/bin/java -Xmx4g -cp java-concolic-1.0-SNAPSHOT.jar csci699cav.Main \
	-prepend-classpath -soot-class-path "$CLASSPATH:bin" -keep-offset csci699cav.Concolic
