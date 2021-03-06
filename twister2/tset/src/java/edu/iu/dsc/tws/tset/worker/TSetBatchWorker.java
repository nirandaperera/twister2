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

package edu.iu.dsc.tws.tset.worker;

import java.io.Serializable;

import edu.iu.dsc.tws.task.impl.TaskWorker;

/**
 * This is now deprecated. Use {@link edu.iu.dsc.tws.tset.env.BatchTSetEnvironment} instead!
 *
 * @deprecated deprecated abstract class
 */
@Deprecated
public abstract class TSetBatchWorker extends TaskWorker implements Serializable {

  @Override
  public void execute() {
    TwisterBatchContext tbc = new TwisterBatchContext(this.config, this.taskExecutor);
    execute(tbc);
  }

  public abstract void execute(TwisterBatchContext tc);
}

