SBT          := sbt
CHISEL_FLAGS := --vcd

executables := $(filter-out main Image,\
            $(notdir $(basename $(wildcard source/*.scala))))

exec_outs    := $(addprefix emulator/, $(addsuffix .out, $(executables)))

top: data/out.im24

data/out.im24: data/in.im24 emulator/ScaleSpaceExtrema.out

outs: $(exec_outs)

all: emulator verilog

exec: $(executables)

check: emulator/outputs.xml

clean:
	-rm -f emulator/* verilog/*
	-rm -rf project target
	-rm -f *.vcd

verilog: $(addprefix verilog/, $(addsuffix .v, $(executables)))

emulator/outputs.xml: $(exec_outs)
	./tools/check $(exec_outs) > $@

data/debug.im8: data/count.im8 source/*.scala
	$(SBT) "run Debug --genHarness --compile --test --backend c --targetDir emulator $(CHISEL_FLAGS)" | tee Debug.out

debug: data/debug.im8

emulator/%.out: emulator/% data/in.im24
	$(SBT) "run $(notdir $(basename $@)) --test --targetDir emulator $(CHISEL_FLAGS)" | tee $@

emulator/%: source/*.scala
	$(SBT) "run $(notdir $(basename $@)) --genHarness --compile --backend c --targetDir emulator $(CHISEL_FLAGS)"

verilog/%.v: source/*.scala
	$(SBT) "run $(notdir $(basename $@)) --genHarness --backend v --targetDir verilog $(CHISEL_FLAGS)"

.PHONY: top outs all exec check clean verilog debug
