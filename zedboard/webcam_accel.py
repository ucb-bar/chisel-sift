#!/usr/bin/python

import cv2

from xillybus_accel import *
import numpy

def webcam_init():
  #cv2.namedWindow('Input')
  cv2.namedWindow('Output')

  webcam = cv2.VideoCapture(-1)
  #webcam = cv2.VideoCapture('snake.mp4')

  sse = XillybusPipe(32)

  buf_in = numpy.empty((480,640,4), numpy.uint8)
  
  if webcam.isOpened(): # try to get the first frame
    rval, frame = webcam.read()
  else:
    rval = False
  
  return (webcam, sse, frame, rval, buf_in)

def process_rgb(frame, buf_in = None):
  if buf_in is None:
    buf_in = numpy.empty((480,640,4), numpy.uint8)

  dim = frame.shape
  buf_in[0:dim[0],0:dim[1],0:dim[2]] = frame
  return numpy.ndarray((480,640,4),numpy.uint8, sse.process(buf_in))

def change_stream(stream, reset=False):
  mem = open('/dev/xillybus_mem_8','w')
  if reset:
    mem.write('\xFF')
  else:
    mem.write('\x00' + chr(stream))
  mem.close()

if __name__ == '__main__':
  (webcam, sse, frame, rval, buf_in) = webcam_init()

  while rval:
    try:
      #cv2.imshow('Input', frame)
      buf_out = process_rgb(frame)
      cv2.imshow('Output', buf_out[:,:,0:3])
 
      rval, frame = webcam.read()
      key = cv2.waitKey(10)
      if key == 27: # exit on ESC
        break
      elif key in range(ord('0'),ord('9')):
        print 'Selecting stream %d' % (key-0x30)
        change_stream(key-0x30,True)
      elif key == 0x2D:
        print 'Selecting stream 10'
        change_stream(10,True)

    except KeyboardInterrupt:
      break
