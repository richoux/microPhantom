## About microPhantom

microPhantom        is         an        AI         bot        playing
[microRTS](https://github.com/santiontanon/microrts).      It     uses
[GHOST](https://github.com/richoux/GHOST),  a  Constraint  Programming
toolkit,  to   design  and   solve  combinatorial  problems   for  all
decision-making behaviors.


## Before doing anything

The version of microRTS used here (with `lib/microrts.jar`) is the one
from the 
[commit 88cdc76d0e5c6](https://github.com/santiontanon/microrts/tree/88cdc76d0e5c624e78ae562c8771fc0a863814ad)
from May 16, 2020, compiled with Java OpenJDK 11.

You may want use a more recent version.  To do so:

1. Clone microRTS with `git clone https://github.com/santiontanon/microrts.git`.
2. Compile      its      JAR      file      following      [these instructions](https://github.com/santiontanon/microrts#generating-a-jar-file).
3. Copy `microrts_folder/bin/microrts.jar` in `microPhantom/lib`.


## Compiling microPhantom

Follow this simple procedure:

`$> cd problem_model`  
`$> make`

It will compile the executable `solver_cpp` and make a symbolic link toward it in
`microPhantom/src/ai/microPhantom`.

You can run the script `launch.sh`  to be sure everything is (locally) ok!

## Running microPhantom

The simplest way is to run the following script:

`$> ./launch.sh`

To  run our  bot on  different  maps or  against different  opponents,
simply      comment/uncomment      the      desired      lines      in
`src/tests/POGameVisualSimulationTest.java`.

To  run several  times 1V1  games  with our  bot against  one or  some
opponents, your can run the following script:

`$> ./launch_n.sh param_nb_runs param_path_result_file param_timeout`
with `param_nb_runs` the number of games you want to play against each
opponent, `param_path_result_file` the path of the text result file to
be written and `param_timeout` a timeout  depending on the size of the
map (see below).

Timeouts used in competitions:
- 8x8 maps: 3000
- 16x16 maps: 4000
- 24x24 maps: 5000
- 32x32 maps: 6000
- 64x64 maps: 8000
- larger than 64x64 maps: 12000

To change the map or the list of opponents,
comment/uncomment      the      desired      lines      in
`src/tests/CompareAllAIsPartiallyObservable.java`.

**However**,  you need  a modifed  version  of microRTS  to run  these
experiments     (it     need     modifications     in     the     file
`src/tests/Experimenter.java`). You can found it here: TODO.

## Downloading and compiling GHOST

microPhantom uses  the Constraint Programming toolkit  [GHOST](https://github.com/richoux/GHOST). The solver
executables  you just  compiled (see  previous intructions  above) are
using the static library of GHOST in the lib folder.

However if  you prefer to use  the last version of  GHOST, then follow
these steps:

1. Clone the develop brach of our GHOST framework with:  
`git clone --single-branch -b develop https://github.com/richoux/GHOST.git`
2. Enter into  the GHOST  folder and  compile it  with the  following
   commands (you need cmake and g++ or clang): `./build.sh`
3. Copy the file `libghost_static.a` from the lib directory of the GHOST
   folder to the lib directory of the microPhantom folder.

## microRTS

[microRTS](https://github.com/santiontanon/microrts)   is    a   small
implementation of  an RTS game,  designed to perform AI  research. The
advantage of using microRTS with  respect to using a full-fledged game
like  Wargus or  Starcraft  (using  BWAPI) is  that  microRTS is  much
simpler, and  can be  used to quickly  test theoretical  ideas, before
moving  on  to  full-fledged  RTS games.  microRTS  was  developed  by
[Santiago
Ontañón](https://sites.google.com/site/santiagoontanonvillar/Home). 
