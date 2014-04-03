Chisel SIFT
===========

This is an implementation of a Scale Invariant Feature Transform (SIFT)
processing pipeline in Chisel. Currently the design only produces the
first few phases of keypoint identification, including multiple octaves
of gaussian and difference of gaussian calculation.

Running SIFT
============

To test all image stream outputs of the design with a random image, run

`make Random_w_e_t`

Where w, e, and t are replaced by numbers represing the width of the image,
number of extrema outputs per octave, and number of taps in the gaussian blur.

Parameter bounds: t >= 3, e >= 2, w > 8\\3\*t

Rough utilization mapping:

* gauss = e + 3
* delaydiff = sub = e + 2
* mult = floor(t\\2) \* gauss
* reg/mem ~= w \* (floor(t\\2) \* (gauss + delaydiff))
* runtime ~= O(e \* (w^2 \* e \* t))

When this runs, you should see a printout of the SSEParams object, standard
Chisel compile output, then several Check n: true, where n is the pipeline
stage from 0 to 2 + gauss + diff. The Chisel tester will report PASSED if
all checks are true.

Known working configurations, roughly from smallest to largest
(with compile\+runtime):

Design | Runtime
-------| -------
16_2_3 | 23s 
16_2_5 | 23s
16_4_5 | 32s
32_2_5 | 56s
32_2_7 | 57s
32_4_5 | 52s
160_2_5| 339s

Using Images
============

This design will process an input image in Sun Raster format (.ras, .im24,
.im8) and produce an one or more debug images and a coordinate identification
image. By default the design processes a input file data/in.im24 and
configuration file data/control.csv, and produces output files data/outn.im24 
and data/coord.im24. in.im24 is the image to be processed, and control.csv
contains a comma-separated list of output streams to select. outn.im24 is the
nth debug image in the list in control.csv.

After downloading the repository, and having the requirements for running
Chisel (see [Chisel Tutorials](https://github.com/ucb-bar/chisel-tutorial)),
you will need to produce or specify the input file and control sequence.

There are several useful test cases included in the data directory already,
to use one of them add a symlink from the data directory:

`ln -s smiley.im24 in.im24`

The same link command must be used for default control sequences in data. 
Then just

`make`

should create an test the top-level Chisel module ScaleSpaceExtrema, producing
the output image.

Debug Mode
==========

When adding new features, it is useful to check the sequence of data at each
stage of the pipeline. The debug mode of the design facilitates this by reading
a 16x16 where each pixel value is the row and column coordinate. It also sets the
kernel of the gaussian modules to CenterKernel, which should pass the image
through the pipeline unaltered. In this mode the design reads from data/count.im8
and data/debug.csv by default, producing images data/debugn.im8 and
data/debug\_coord.im24.

`make debug`

When switching between debug and regular mode, I suggest running

`make clean`

TODO
====

* Multiple Octaves
* Extremum detection
* Useful coordinate output
* Unit tests
