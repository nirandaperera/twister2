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

package edu.iu.dsc.tws.data.fs.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.channels.FileChannel;

import edu.iu.dsc.tws.api.data.FSDataInputStream;

/**
 * The <code>LocalDataInputStream</code> class is a wrapper class for a data
 * input stream to the local file system.
 */
public class LocalDataInputStream extends FSDataInputStream {

  /**
   * The file input stream used to read data from.
   */
  private final FileInputStream fis;
  private final FileChannel fileChannel;

  private File fileN;

  /**
   * Constructs a new <code>LocalDataInputStream</code> object from a given {@link File} object.
   *
   * @param file The File the data stream is read from
   * @throws IOException Thrown if the data input stream cannot be created.
   */
  public LocalDataInputStream(File file) throws IOException {
    this.fileN = file;
    this.fis = new FileInputStream(file);
    this.fileChannel = fis.getChannel();
  }

  @Override
  public void seek(long desired) throws IOException {
    if (desired != getPos()) {
      this.fileChannel.position(desired);
    }
  }

  @Override
  public long getPos() throws IOException {
    return this.fileChannel.position();
  }

  @Override
  public int read() throws IOException {
    return this.fis.read();
  }

  public Reader getReader() throws IOException {
    return new FileReader(this.fileN);
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    return this.fis.read(buffer, offset, length);
  }

  @Override
  public void close() throws IOException {
    // Accoring to javadoc, this also closes the channel
    this.fis.close();
  }

  @Override
  public int available() throws IOException {
    return this.fis.available();
  }

  @Override
  public long skip(final long n) throws IOException {
    return this.fis.skip(n);
  }
}
