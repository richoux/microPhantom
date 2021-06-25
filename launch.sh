#!/bin/bash

cd problem_model
make
cd ..

javac -cp lib/protobuf-java-3.17.3.jar protobuf_code/com/microphantom/protos/*.java

javac -cp src/:lib/microrts.jar:lib/protobuf-java-3.17.3.jar:protobuf_code src/tests/POGameVisualSimulationTest.java
java -cp src:lib/microrts.jar:lib/protobuf-java-3.17.3.jar:protobuf_code tests.POGameVisualSimulationTest
