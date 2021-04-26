#!/bin/bash
cp -r ../../../java-concolic/instrumenter/instrumentation .
rm instrumentation/.gitignore
cp ../../../java-concolic/instrumenter/target/java-concolic-1.0-SNAPSHOT.jar .
cp ../../../java-concolic/concolic/concolic.py .
