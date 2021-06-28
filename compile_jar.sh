#!/bin/bash

# compile source files
javac -cp lib/protobuf-java-3.17.3.jar protobuf_code/com/microphantom/protos/*.java
javac -cp src/:lib/microrts.jar:lib/protobuf-java-3.17.3.jar:protobuf_code src/ai/microPhantom/MicroPhantom.java

# # extract the contents of the JAR dependencies
# find ../lib -name "microrts.jar" | xargs -n 1 jar xvf
# find ../lib -name "protobuf-java-3.17.3.jar" | xargs -n 1 jar xvf

# create a single JAR file with sources and dependencies
jar cvf microPhantom.jar $(find ./src/ai/microPhantom/ -name '*.class' -type f)

mkdir -p bin
mv microPhantom.jar bin
