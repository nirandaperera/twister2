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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.sun.org.apache.xerces.internal.impl.dv.xs.BooleanDV;
import com.sun.org.apache.xpath.internal.operations.Bool;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.comms.api.DataFlowOperation;
import edu.iu.dsc.tws.comms.api.MessageFlags;
import edu.iu.dsc.tws.comms.api.MessageReceiver;
import edu.iu.dsc.tws.comms.mpi.MPIContext;
import edu.iu.dsc.tws.comms.mpi.MPIMessage;

public class GatherBatchPartialReceiver implements MessageReceiver {
  private static final Logger LOG = Logger.getLogger(GatherBatchPartialReceiver.class.getName());

  // lets keep track of the messages
  // for each task we need to keep track of incoming messages
  private Map<Integer, Map<Integer, List<Object>>> messages = new HashMap<>();
  private Map<Integer, Map<Integer, Integer>> counts = new HashMap<>();
  private Map<Integer, Map<Integer, Boolean>> finished = new HashMap<>();
  private DataFlowOperation dataFlowOperation;
  private int executor;
  private int sendPendingMax = 128;
  private int destination;
  private Map<Integer, Boolean> batchDone = new HashMap<>();

  public GatherBatchPartialReceiver(int dst) {
    this.destination = dst;
  }

  @Override
  public void init(Config cfg, DataFlowOperation op, Map<Integer, List<Integer>> expectedIds) {
    executor = op.getTaskPlan().getThisExecutor();
    sendPendingMax = MPIContext.sendPendingMax(cfg);

    LOG.fine(String.format("%d gather partial expected ids %s", executor, expectedIds));
    for (Map.Entry<Integer, List<Integer>> e : expectedIds.entrySet()) {
      Map<Integer, List<Object>> messagesPerTask = new HashMap<>();
      Map<Integer, Boolean> finishedPerTask = new HashMap<>();
      Map<Integer, Integer> countsPerTask = new HashMap<>();

      for (int i : e.getValue()) {
        messagesPerTask.put(i, new ArrayList<Object>());
        finishedPerTask.put(i, false);
        countsPerTask.put(i, 0);
      }
      messages.put(e.getKey(), messagesPerTask);
      finished.put(e.getKey(), finishedPerTask);
      counts.put(e.getKey(), countsPerTask);
      batchDone.put(e.getKey(), false);
    }
    this.dataFlowOperation = op;
    this.executor = dataFlowOperation.getTaskPlan().getThisExecutor();
  }

  @Override
  public boolean onMessage(int source, int path, int target, int flags, Object object) {
    // add the object to the map
    boolean canAdd = true;

    if (messages.get(target) == null) {
      throw new RuntimeException(String.format("%d Partial receive error %d", executor, target));
    }
    List<Object> m = messages.get(target).get(source);
    Map<Integer, Boolean> finishedMessages = finished.get(target);

    if (m.size() > sendPendingMax) {
//     LOG.info(String.format("%d Partial add FALSE target %d source %d", executor, target, source));
      canAdd = false;
    } else {
//     LOG.info(String.format("%d Partial add TRUE target %d source %d", executor, target, source));
      if (object instanceof MPIMessage) {
        ((MPIMessage) object).incrementRefCount();
      }
      Integer c = counts.get(target).get(source);
      counts.get(target).put(source, c + 1);

      m.add(object);
      if ((flags & MessageFlags.FLAGS_LAST) == MessageFlags.FLAGS_LAST) {
        finishedMessages.put(source, true);
      }
    }
    return canAdd;
  }

  @Override
  public void progress() {
    for (int t : messages.keySet()) {
      if (batchDone.get(t)) {
        continue;
      }
      boolean canProgress = true;
      while (canProgress) {
        // now check weather we have the messages for this source
        Map<Integer, List<Object>> map = messages.get(t);
        Map<Integer, Boolean> finishedForTarget = finished.get(t);
        Map<Integer, Integer> countMap = counts.get(t);
//        LOG.info(String.format("%d gather partial path %d counts %s",
//            executor, destination, countMap));
        boolean found = true;
        boolean allFinished = true;
        for (Map.Entry<Integer, List<Object>> e : map.entrySet()) {
          if (e.getValue().size() == 0 && !finishedForTarget.get(e.getKey())) {
            found = false;
            canProgress = false;
          }

          if (!finishedForTarget.get(e.getKey())) {
            allFinished = false;
          } /*else {
            LOG.info(String.format("%d partial finished receiving to %d from %d path %d",
                executor, t, e.getKey(), destination));
          }*/
        }

        if (found) {
          List<Object> out = new ArrayList<>();
          for (Map.Entry<Integer, List<Object>> e : map.entrySet()) {
            List<Object> valueList = e.getValue();
            if (valueList.size() > 0) {
              Object value = valueList.get(0);
              out.add(value);
            }
          }
          int flags = 0;
          if (allFinished) {
//            LOG.info(String.format("%d partial all finished path %d %d %s",
//                executor, destination, t, batchDone));
            batchDone.put(t, true);
            flags = MessageFlags.FLAGS_LAST;
          }
          if (dataFlowOperation.sendPartial(t, out, flags, destination)) {
            for (Map.Entry<Integer, List<Object>> e : map.entrySet()) {
              List<Object> value = e.getValue();
              if (value.size() > 0) {
                value.remove(0);
              }
            }
            for (Map.Entry<Integer, Integer> e : countMap.entrySet()) {
              Integer i = e.getValue();
              e.setValue(i - 1);
            }
            if (allFinished) {
              batchDone.put(t, true);
              // we don't want to go through the while loop for this one
              break;
            }
          } else {
            canProgress = false;
          }
        }
      }
    }
  }
}

