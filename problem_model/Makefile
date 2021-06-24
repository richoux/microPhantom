EXEC=solver_cpp

# Compiler flags
CXXFIRSTFLAGS= -O3 -DBENCH -W -Wall -Wextra -pedantic -Wno-sign-compare -Wno-unused-parameter
CXXFIRSTFLAGSDEBUG= -g -O0 -DBENCH -W -Wall -Wextra -pedantic -Wno-sign-compare -Wno-unused-parameter 

UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
	CXX=g++
	CXXFLAGS= -std=c++17 $(CXXFIRSTFLAGS)
	CXXFLAGSDEBUG= -std=c++17 $(CXXFIRSTFLAGSDEBUG)
endif
ifeq ($(UNAME_S),Darwin)
	CXX=clang++
	CXXFLAGS= -std=c++17  -stdlib=libc++ $(CXXFIRSTFLAGS)
	CXXFLAGSDEBUG= -std=c++17  -stdlib=libc++ $(CXXFIRSTFLAGSDEBUG)
	LDFLAGS=-lghost_static -lc++ -lc++abi -pthread
endif

# Directories
SRCDIR=src ../protobuf_code
OBJDIR=obj
OBJ=$(addprefix $(OBJDIR)/,constraints_rts.o obj_rts.o main.o builder.o microphantom.pb.o)
BINDIR=.
INCLUDEDIR=-I../include -I../protobuf_code
LIBDIR=../lib

# Linker flags
LDFLAGS=-lghost_static -pthread -lprotobuf

# For rm
SOURCESTILDE=$(foreach sdir, $(SRCDIR), $(wildcard $(sdir)/*.cpp~))

vpath %.cpp $(SRCDIR)

# Reminder, 'cause it is easy to forget makefile's fucked-up syntax...
# $@ is what triggered the rule, ie the target before :
# $^ is the whole dependencies list, ie everything after :
# $< is the first item in the dependencies list

# Rules
all: $(BINDIR)/$(EXEC)

debug: CXXFLAGS= $(CXXFLAGSDEBUG)
debug: $(BINDIR)/$(EXEC)

$(BINDIR)/$(EXEC): $(OBJ)
	$(CXX) -o  $@ $^ -L$(LIBDIR) $(LDFLAGS)
	ln -sf ../../../problem_model/$(EXEC) ../src/ai/microPhantom/

$(OBJDIR)/%.o: %.cpp
	$(CXX) $(CXXFLAGS) -c $(INCLUDEDIR) $< -o $@

$(OBJDIR)/microphantom.pb.o: ../protobuf_code/microphantom.pb.cc
	$(CXX) $(CXXFLAGS) -c $(INCLUDEDIR) $< -o $@

$(OBJ): | $(OBJDIR)

$(OBJDIR):
	mkdir -p $(OBJDIR)

.PHONY: clean 

clean:
	rm -fr core *~ $(EXEC) $(OBJDIR)/*.o $(SOURCESTILDE) 
