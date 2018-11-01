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
package edu.iu.dsc.tws.task.api;

import java.util.logging.Logger;

import edu.iu.dsc.tws.task.streaming.BaseStreamCompute;

public abstract class ComputeCheckpointableTask extends BaseStreamCompute
    implements ICheckPointable {
  private static final long serialVersionUID = -254264103890214728L;

  private static final Logger LOG = Logger.getLogger(BaseStreamCompute.class.getName());

  public Snapshot snapshot;

  public void addState(String key, Object value) {
    if (snapshot == null) {
      snapshot = new Snapshot();
    }
    snapshot.addState(key, value);
  }

  public Object getState(String key) {
    return snapshot.getState(key);
  }

  @Override
  public Snapshot getSnapshot() {
    return snapshot;
  }

  @Override
  public void restoreSnapshot(Snapshot newsnapshot) {
    this.snapshot = newsnapshot;
  }

}