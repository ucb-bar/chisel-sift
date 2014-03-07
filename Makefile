SBT          := sbt
CHISEL_FLAGS :=

executables := $(filter-out main Image,\
            $(notdir $(basename $(wildcard source/*.scala))))

exec_outs    := $(addprefix emulator/, $(addsuffix .out, $(executables)))

top: emulator/ScaleSpaceExtrema.out

outs: $(exec_outs)

all: emulator verilog

exec: $(executables)

check: emulator/outputs.xml

clean:
	-rm -f emulator/* verilog/*
	-rm -rf project target

verilog: $(addprefix verilog/, $(addsuffix .v, $(executables)))

emulator/outputs.xml: $(exec_outs)
	./tools/check $(exec_outs) > $@

emulator/%.out: source/*.scala
	$(SBT) "run $(notdir $(basename $@)) --genHarness --compile --test --backend c --targetDir emulator $(CHISEL_FLAGS)" | tee $@

verilog/%.v: source/*.scala
	$(SBT) "run $(notdir $(basename $@)) --genHarness --backend v --targetDir verilog $(CHISEL_FLAGS)"

.PHONY: top outs all exec check clean verilog
