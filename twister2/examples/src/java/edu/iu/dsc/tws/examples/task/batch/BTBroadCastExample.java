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
package edu.iu.dsc.tws.examples.task.batch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.compute.TaskContext;
import edu.iu.dsc.tws.api.compute.nodes.BaseSource;
import edu.iu.dsc.tws.api.compute.nodes.ICompute;
import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.examples.task.BenchTaskWorker;
import edu.iu.dsc.tws.examples.utils.bench.BenchmarkConstants;
import edu.iu.dsc.tws.examples.utils.bench.BenchmarkUtils;
import edu.iu.dsc.tws.examples.utils.bench.Timing;
import edu.iu.dsc.tws.examples.verification.ResultsVerifier;
import edu.iu.dsc.tws.examples.verification.comparators.IntIteratorComparator;
import edu.iu.dsc.tws.task.impl.ComputeGraphBuilder;
import edu.iu.dsc.tws.task.typed.batch.BBroadCastCompute;

public class BTBroadCastExample extends BenchTaskWorker {

  private static final Logger LOG = Logger.getLogger(BTBroadCastExample.class.getName());

  @Override
  public ComputeGraphBuilder buildTaskGraph() {
    List<Integer> taskStages = jobParameters.getTaskStages();
    int sourceParallelism = taskStages.get(0);
    int sinkParallelism = taskStages.get(1);
    String edge = "edge";
    BaseSource g = new SourceTask(edge);
    ICompute r = new BroadcastSinkTask();
    computeGraphBuilder.addSource(SOURCE, g, sourceParallelism);
    computeConnection = computeGraphBuilder.addCompute(SINK, r, sinkParallelism);

    computeConnection.broadcast(SOURCE).viaEdge(edge);

    return computeGraphBuilder;
  }

  protected static class BroadcastSinkTask extends BBroadCastCompute<int[]> {
    private static final long serialVersionUID = -254264903510284798L;

    private ResultsVerifier<int[], Iterator<int[]>> resultsVerifier;
    private boolean verified = true;
    private boolean timingCondition;

    @Override
    public void prepare(Config cfg, TaskContext ctx) {
      super.prepare(cfg, ctx);
      this.timingCondition = getTimingCondition(SINK, context);
      resultsVerifier = new ResultsVerifier<>(
          inputDataArray,
          (ints, args) -> {
            List<int[]> expected = new ArrayList<>();
            for (int i = 0; i < jobParameters.getTotalIterations(); i++) {
              expected.add(ints);
            }
            return expected.iterator();
          },
          IntIteratorComparator.getInstance()
      );
    }

    @Override
    public boolean broadcast(Iterator<int[]> content) {
      Timing.mark(BenchmarkConstants.TIMING_ALL_RECV, this.timingCondition);
      LOG.info(String.format("%d received broadcast %d", context.getWorkerId(),
          context.globalTaskId()));
      BenchmarkUtils.markTotalTime(resultsRecorder, this.timingCondition);
      resultsRecorder.writeToCSV();
      this.verified = verifyResults(resultsVerifier, content, null, verified);
      return true;
    }
  }
}
