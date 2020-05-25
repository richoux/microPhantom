#!/bin/bash

javac -cp src/:lib/microrts.jar src/tests/POGameVisualSimulationTest.java
java -cp src:lib/microrts.jar tests.POGameVisualSimulationTest
