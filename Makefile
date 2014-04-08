SBT          := sbt
CHISEL_FLAGS :=

executables := $(filter-out main Image,\
            $(notdir $(basename $(wildcard source/*.scala))))

exec_outs    := $(addprefix emulator/, $(addsuffix .out, $(executables)))

top: data/out.im24

data/out.im24: emulator/ScaleSpaceExtrema.out

outs: $(exec_outs)

all: emulator verilog

exec: $(executables)

check: emulator/outputs.xml

clean:
	-rm -f emulator/* verilog/*
	-rm -rf project target
	-rm -f *.vcd
	-rm -f data/out*.im* data/debug*.im* data/coord.im24 data/debug_coord.im24

verilog: $(addprefix verilog/, $(addsuffix .v, $(executables)))

emulator/outputs.xml: $(exec_outs)
	./tools/check $(exec_outs) > $@

data/debug.im8: data/count.im8 source/*.scala
	mkdir -p emulator
	$(SBT) "run Debug --genHarness --compile --test --backend c --targetDir emulator --vcd --debug $(CHISEL_FLAGS)" | tee emulator/Debug.out

debug: data/debug.im8

emulator/%.out: data/in.im24 source/*.scala
	$(SBT) "run $(notdir $(basename $@)) --genHarness --compile --test --backend c --targetDir emulator --vcd --debug $(CHISEL_FLAGS)" | tee $@

#emulator/%: source/*.scala
#	$(SBT) "run $(notdir $(basename $@)) --genHarness --compile --backend c --targetDir emulator $(CHISEL_FLAGS)"

verilog/%.v: source/*.scala
	$(SBT) "run $(notdir $(basename $@)) --genHarness --backend v --targetDir verilog $(CHISEL_FLAGS)"

zedboard: source/*.scala
	$(SBT) "run Zedboard --genHarness --backend v --targetDir verilog $(CHISEL_FLAGS)"

random_16_12_5: source/*.scala
	mkdir -p emulator
	$(SBT) "run Random_16_12_5 --genHarness --compile --test --backend c --targetDir emulator --vcd --debug $(CHISEL_FLAGS)" | tee emulator/random_16_12_5

random_160_120_5: source/*.scala
	mkdir -p emulator
	$(SBT) "run Random_160_120_5 --genHarness --compile --test --backend c --targetDir emulator --vcd --debug $(CHISEL_FLAGS)" | tee emulator/random_160_120_5

Random_%: source/*.scala
	mkdir -p emulator
	$(SBT) "run $@ --genHarness --compile --test --backend c --targetDir emulator --vcd --debug $(CHISEL_FLAGS)" | tee emulator/$@.out

VecRandom_%: source/*.scala
	mkdir -p emulator
	$(SBT) "run $@ --genHarness --compile --test --backend c --targetDir emulator --vcd --debug $(CHISEL_FLAGS)" | tee emulator/$@.out

.PHONY: top outs all exec check clean verilog debug
