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
package edu.iu.dsc.tws.api.tset.sink;

import edu.iu.dsc.tws.api.dataset.DataPartition;
import edu.iu.dsc.tws.api.tset.fn.BaseSinkFunc;
import edu.iu.dsc.tws.dataset.partition.EntityPartition;

public class CacheSink<T> extends BaseSinkFunc<T> {
  private DataPartition<T> partition = null;

  public CacheSink() {
  }

  @Override
  public boolean add(T value) {
    // todo every time add is called, a new partition will be made! how to handle that?
    partition = new EntityPartition<>(getTSetContext().getIndex(), value);
    return true;
  }


  @Override
  public void close() {

  }

  @Override
  public DataPartition<T> get() {
    return partition;
  }
}
