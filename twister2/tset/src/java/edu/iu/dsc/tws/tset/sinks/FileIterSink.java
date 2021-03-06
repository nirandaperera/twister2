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

package edu.iu.dsc.tws.tset.sinks;

import java.util.Iterator;

import edu.iu.dsc.tws.api.tset.TSetContext;
import edu.iu.dsc.tws.api.tset.fn.SinkFunc;
import edu.iu.dsc.tws.data.api.out.FileOutputWriter;

public class FileIterSink<T> implements SinkFunc<Iterator<T>> {
  private FileOutputWriter<T> output;

  private int partition;

  public FileIterSink(FileOutputWriter<T> out) {
    this.output = out;
  }

  @Override
  public boolean add(Iterator<T> value) {
    while (value.hasNext()) {
      output.write(partition, value.next());
    }
    return true;
  }

  @Override
  public void prepare(TSetContext context) {
    partition = context.getIndex();
  }

  @Override
  public void close() {
    output.close();
  }
}
