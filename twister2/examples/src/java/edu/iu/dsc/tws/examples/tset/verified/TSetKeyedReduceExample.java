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
package edu.iu.dsc.tws.examples.tset.verified;

import java.util.logging.Logger;

import edu.iu.dsc.tws.examples.tset.BaseTSetBatchWorker;
import edu.iu.dsc.tws.tset.env.BatchTSetEnvironment;

public class TSetKeyedReduceExample extends BaseTSetBatchWorker {
  private static final Logger LOG = Logger.getLogger(TSetKeyedReduceExample.class.getName());

  @Override
  public void execute(BatchTSetEnvironment env) {
    super.execute(env);
/*    super.execute(tc);

    // set the parallelism of source to task stage 0
    List<Integer> taskStages = jobParameters.getTaskStages();
    int sourceParallelism = taskStages.get(0);
    int sinkParallelism = taskStages.get(1);
    BatchSourceTSet<int[]> source = tc.createSource(new TestBaseSource(),
        sourceParallelism).setName("Source");
    KeyedReduceTLink<int[], int[]> reduce = source.
        groupBy(new LoadBalancePartitioner<>(), new IdentitySelector<>()).
          keyedReduce((t1, t2) -> {
            int[] val = new int[t1.length];
            for (int i = 0; i < t1.length; i++) {
              val[i] = t1[i] + t2[i];
            }
            return val;
          });

    reduce.sink(new BaseSink<int[]>() {
      @Override
      public boolean add(int[] value) {
        experimentData.setOutput(value);
        try {
          LOG.info("Task Id : " + context.getIndex() + " Results " + Arrays.toString(value));
          verify(OperationNames.REDUCE);
        } catch (VerificationException e) {
          LOG.info("Exception Message : " + e.getMessage());
        }
        return true;
      }

      @Override
      public void prepare() {
      }
    }, sinkParallelism);*/
  }

}
