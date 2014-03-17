#!/usr/bin/python

import cv2
import numpy
import time

from xillybus_accel import xb_accel

def invert(img):
  c = numpy.empty(img.shape,numpy.uint8)
  c.fill(255)
  img_out = numpy.subtract(c,img)

  return img_out

def rgb_to_flat(rgb):
  shape = numpy.shape(rgb)
  flat = numpy.zeros(shape[0:2], dtype=numpy.uint32)
  
  for i in range(shape[0]):
    for j in range(shape[1]):
      #flat[i,j] = img[i,j,0] + (img[i,j,1]<<8) + (img[i,j,2]<<16)
      flat[i,j] = sum(rgb[i,j,:] * [1,256,65536])

  return flat

def flat_to_rgb(flat):
  shape = numpy.shape(flat)
  rgb = numpy.zeros((shape[0], shape[1], 3), numpy.uint8)
  for i in range(shape[0]):
    for j in range(shape[1]):
      rgb[i,j,:] = numpy.ndarray((3), numpy.uint8, flat[i,j])
  
  return rgb

def display_loop():
  while True:
    try:
      key = cv2.waitKey(50)
      if key == 27: # exit on ESC
        break
    except KeyboardInterrupt:
      break

if __name__ == "__main__":

	print 'Reading Image'

	#img = cv2.imread('campanile.jpg',cv2.CV_LOAD_IMAGE_GRAYSCALE)
	img = cv2.imread('campanile.jpg')

	print 'Creating windows'

	cv2.namedWindow('input')
	cv2.namedWindow('software')
	cv2.namedWindow('hardware')

	print 'Software start'

	soft_start = time.time()
	soft_img = invert(img)
	soft_time = time.time() - soft_start

	print 'Software time: %f' % soft_time

	print 'Hardware start'

	hard_start = time.time()
	print '>Input convert'
	flat = rgb_to_flat(img)

	print '>Accel'
	hard_flat_img = xb_accel(flat)

	print '>Output convert'
	hard_img = flat_to_rgb(hard_flat_img)

	hard_time = time.time() - hard_start

	print 'Hardware time: %f' % hard_time

	print 'Showing images'

	cv2.imshow('input', img)
	cv2.imshow('software', soft_img)
	cv2.imshow('hardware', hard_img)

	print 'Waiting for Esc'
	
	display_loop()


