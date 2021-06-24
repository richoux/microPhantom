#!/bin/bash

#Timeouts in the competition
#8x8 maps: 3000
#16x16 maps: 4000
#24x24 maps: 5000
#32x32 maps: 6000
#64x64 maps: 8000
#> 64x64 maps: 12000

# $1 number of runs
# $2 result file path 
# $3 timeout
# $4 number of meta-runs (usually = 1, except for non-deterministic experiments)

javac -cp src/:lib/microrts.jar:lib/protobuf-java-3.17.3.jar src/tests/CompareAllAIsPartiallyObservable.java
java -cp src:lib/microrts.jar:lib/protobuf-java-3.17.3.jar tests.CompareAllAIsPartiallyObservable $1 $2 $3 $4
