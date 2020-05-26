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
package edu.iu.dsc.tws.common.table.arrow;

import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.Float4Vector;

import edu.iu.dsc.tws.common.table.ArrowColumn;

public class Float4Column implements ArrowColumn<Float> {
  private Float4Vector vector;

  private int currentIndex;

  public Float4Column(Float4Vector vector) {
    this.vector = vector;
  }

  @Override
  public void addValue(Float value) {
    vector.setSafe(currentIndex, value);
    currentIndex++;
  }

  @Override
  public FieldVector getVector() {
    return vector;
  }

  public Float get(int index) {
    return vector.get(index);
  }

  @Override
  public long currentSize() {
    return vector.getBufferSize();
  }
}
