import numpy
import threading

class WriteThread(threading.Thread):
  def __init__(self, data, device):
    threading.Thread.__init__(self)
    self.data = data
    self.device = device

  def run(self):
    w = open(self.device,'w')
    w.write(self.data)
    w.close()

class XillybusPipe():
  def __init__(self, nbits):
    self.r = open('/dev/xillybus_read_%d' % nbits,'r')
    self.w = open('/dev/xillybus_write_%d' % nbits, 'w')

  def process(self, data_in):
    self.w.write(data_in)
    return self.r.read(data_in.size * data_in.dtype.itemsize)

  def close(self):
    self.r.close()
    self.w.close()

def xb_accel(data_array, nbits):
  
  r = open('/dev/xillybus_read_%d' % nbits,'r')
  w = WriteThread(data_array, '/dev/xillybus_write_%d' % nbits)
  w.start()

  return r.read(data_array.size * data_array.dtype.itemsize)

