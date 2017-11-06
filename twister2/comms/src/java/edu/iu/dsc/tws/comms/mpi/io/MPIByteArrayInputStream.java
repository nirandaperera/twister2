//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
package edu.iu.dsc.tws.comms.mpi.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import edu.iu.dsc.tws.comms.mpi.MPIBuffer;

/**
 * This is a specialized input stream targetted to reading a twister object message expanding
 * to multiple MPI buffers.
 */
public class MPIByteArrayInputStream extends InputStream {
  // the buffers which contains the message
  protected List<MPIBuffer> bufs;

  // the absolute position of the current buffer
  // protected int pos;
  protected int mark = 0;

  // the current buffer index
  protected int currentBufferIndex = 0;

  // header size read
  protected int headerSize;

  // weather we are in a grouped collective, where we need to think about message path
  protected boolean grouped;

  public MPIByteArrayInputStream(List<MPIBuffer> buffers, int headerSize, boolean group) {
    this.bufs = buffers;
    this.currentBufferIndex = 0;
    this.headerSize = headerSize;
    this.grouped = group;
  }

  public synchronized int read() {
    ByteBuffer byteBuffer = getReadBuffer();
    // we are at the end
    if (byteBuffer == null) {
      return -1;
    }
    // check to see if this buffer has this information
    if (byteBuffer.remaining() >= 1) {
      return byteBuffer.get();
    } else {
      throw new RuntimeException("Failed to read the next byte");
    }
  }

  public synchronized int read(byte[] b, int off, int len) {
    ByteBuffer byteBuffer = getReadBuffer();
    // we are at the end
    if (byteBuffer == null) {
      return -1;
    }
    // check to see if this buffer has this information
    if (byteBuffer.remaining() >= 1) {
      // we can copy upto len or remaining
      int copiedLength = byteBuffer.remaining() > len ? len : byteBuffer.remaining();
      byteBuffer.get(b, off, copiedLength);
      // increment position
      return copiedLength;
    } else {
      throw new RuntimeException("Failed to read the next byte");
    }
  }

  private ByteBuffer getReadBuffer() {
    ByteBuffer byteBuffer = bufs.get(currentBufferIndex).getByteBuffer();
    // this is the intial time we are reading
    int pos = byteBuffer.position();
    // we are at the 0th position, we need to skip header
    if (currentBufferIndex == 0 && pos == 0) {
      if (byteBuffer.remaining() < headerSize) {
        throw new RuntimeException("The buffer doesn't contain data or complete header");
      }
      // lets rewind the buffer so the position becomes 0
      byteBuffer.rewind();
      // now skip the header size
      byteBuffer.position(headerSize);
      pos = headerSize;
    }

    // now check if we need to go to the next buffer
    if (pos >= byteBuffer.limit() - 1) {
      // if we are at the end we need to move to next
      currentBufferIndex++;
      byteBuffer = bufs.get(currentBufferIndex).getByteBuffer();
      byteBuffer.rewind();
      // if grouped first 4 bytes are for the path
      if (grouped) {
        byteBuffer.position(4);
      }
      //we are at the end so return null
      if (currentBufferIndex >= bufs.size()) {
        return null;
      }
    }
    return byteBuffer;
  }

  public synchronized long skip(long n) {
    if (n < 0) {
      return 0;
    }

    int skipped = 0;
    for (int i = currentBufferIndex; i < bufs.size(); i++) {
      ByteBuffer b = bufs.get(i).getByteBuffer();
      int avail;
      long needSkip = n - skipped;
      int bufPos = b.position();

      // we need to skip header
      if (i == 0) {
        if (bufPos < headerSize) {
          // lets go to the header
          b.position(headerSize);
          bufPos = headerSize;
        }
      }

      avail = b.remaining() - bufPos;
      // now check how much we need to move here
      if (needSkip >= avail) {
        // we go to the end
        b.position(bufPos + avail);
        currentBufferIndex++;
        skipped += avail;
      } else {
        b.position((int) (bufPos + needSkip));
        skipped += needSkip;
      }

      if (skipped >= n) {
        break;
      }
    }
    return skipped;
  }

  public synchronized int available() {
    int avail = 0;
    for (int i = currentBufferIndex; i < bufs.size(); i++) {
      ByteBuffer b = bufs.get(i).getByteBuffer();
      if (i == 0) {
        int position = b.position();
        if (position > headerSize) {
          avail += b.remaining() - position;
        } else {
          avail += b.remaining() - headerSize;
        }
      } else {
        if (grouped) {
          avail += b.remaining() - 4;
        } else {
          avail += b.remaining();
        }
      }
    }
    return avail;
  }

  public boolean markSupported() {
    return false;
  }

  public void mark(int readAheadLimit) {
  }

  public synchronized void reset() {
  }

  public void close() throws IOException {
  }
}