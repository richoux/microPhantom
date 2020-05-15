EXEC_PESSIMISTIC=solver_cpp_pessimistic
EXEC_OPTIMISTIC=solver_cpp_optimistic
EXEC_NEUTRAL=solver_cpp_neutral

# Compiler flags
CXXFIRSTFLAGS= -O3 -DBENCH -W -Wall -Wextra -pedantic -Wno-sign-compare -Wno-unused-parameter
CXXFIRSTFLAGSDEBUG= -g -O0 -DBENCH -W -Wall -Wextra -pedantic -Wno-sign-compare -Wno-unused-parameter 

UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
	CXX=g++
	CXXFLAGS= -std=c++14 $(CXXFIRSTFLAGS)
	CXXFLAGSDEBUG= -std=c++14 $(CXXFIRSTFLAGSDEBUG)
endif
ifeq ($(UNAME_S),Darwin)
	CXX=clang++
	CXXFLAGS= -std=c++1y  -stdlib=libc++ $(CXXFIRSTFLAGS)
	CXXFLAGSDEBUG= -std=c++1y  -stdlib=libc++ $(CXXFIRSTFLAGSDEBUG)
	LDFLAGS=-lghost_static -lc++ -lc++abi
endif

# Directories
SRCDIR=src
OBJDIR=obj
OBJ_PESSIMISTIC=$(addprefix $(OBJDIR)/,constraints_rts.o obj_rts.o main_pessimistic.o)
OBJ_OPTIMISTIC=$(addprefix $(OBJDIR)/,constraints_rts.o obj_rts.o main_optimistic.o)
OBJ_NEUTRAL=$(addprefix $(OBJDIR)/,constraints_rts.o obj_rts.o main_neutral.o)
#BINDIR=../src/ai/microPhantom
BINDIR=.
INCLUDEDIR=../include
LIBDIR=../lib

# Linker flags
LDFLAGS=-lghost_static

# Files
#SOURCES=$(foreach sdir, $(SRCDIR), $(wildcard $(sdir)/*.cpp))
#OBJECTS=$(patsubst %.cpp, $(OBJDIR)/%.o, $(notdir $(SOURCES)))

# For rm
SOURCESTILDE=$(foreach sdir, $(SRCDIR), $(wildcard $(sdir)/*.cpp~))

vpath %.cpp $(SRCDIR)

# Reminder, 'cause it is easy to forget makefile's fucked-up syntax...
# $@ is what triggered the rule, ie the target before :
# $^ is the whole dependencies list, ie everything after :
# $< is the first item in the dependencies list

# Rules
all: $(BINDIR)/$(EXEC_PESSIMISTIC) $(BINDIR)/$(EXEC_OPTIMISTIC) $(BINDIR)/$(EXEC_NEUTRAL)

debug: CXXFLAGS= $(CXXFLAGSDEBUG)
debug: $(BINDIR)/$(EXEC)

$(BINDIR)/$(EXEC_PESSIMISTIC): $(OBJ_PESSIMISTIC)
	$(CXX) -o  $@ $^ -L$(LIBDIR) $(LDFLAGS)
	ln -sf ../../../problem_model/$(EXEC_PESSIMISTIC) ../src/ai/microPhantom/

$(BINDIR)/$(EXEC_OPTIMISTIC): $(OBJ_OPTIMISTIC)
	$(CXX) -o $@ $^ -L$(LIBDIR) $(LDFLAGS)
	ln -sf ../../../problem_model/$(EXEC_OPTIMISTIC) ../src/ai/microPhantom/

$(BINDIR)/$(EXEC_NEUTRAL): $(OBJ_NEUTRAL)
	$(CXX) -o $@ $^ -L$(LIBDIR) $(LDFLAGS)
	ln -sf ../../../problem_model/$(EXEC_NEUTRAL) ../src/ai/microPhantom/

$(OBJDIR)/main_pessimistic.o: $(SRCDIR)/main.cpp
	$(CXX) $(CXXFLAGS) -c -DPESSIMISTIC -I$(INCLUDEDIR) $< -o $@

$(OBJDIR)/main_optimistic.o: $(SRCDIR)/main.cpp
	$(CXX) $(CXXFLAGS) -c -DOPTIMISTIC -I$(INCLUDEDIR) $< -o $@

$(OBJDIR)/main_neutral.o: $(SRCDIR)/main.cpp
	$(CXX) $(CXXFLAGS) -c -DNEUTRAL -I$(INCLUDEDIR) $< -o $@

$(OBJDIR)/%.o: %.cpp
	$(CXX) $(CXXFLAGS) -c -I$(INCLUDEDIR) $< -o $@

$(OBJ_PESSIMISTIC): | $(OBJDIR)
$(OBJ_OPTIMISTIC): | $(OBJDIR)
$(OBJ_NEUTRAL): | $(OBJDIR)

$(OBJDIR):
	mkdir -p $(OBJDIR)

.PHONY: clean 

clean:
	rm -fr core *~ $(EXEC_PESSIMISTIC) $(EXEC_OPTIMISTIC) $(EXEC_NEUTRAL) $(OBJDIR)/*.o $(SOURCESTILDE) 
