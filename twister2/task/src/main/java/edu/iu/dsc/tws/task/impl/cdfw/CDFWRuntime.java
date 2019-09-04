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
package edu.iu.dsc.tws.task.impl.cdfw;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import edu.iu.dsc.tws.api.comms.Communicator;
import edu.iu.dsc.tws.api.compute.executor.ExecutionPlan;
import edu.iu.dsc.tws.api.compute.graph.ComputeGraph;
import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.config.Context;
import edu.iu.dsc.tws.api.dataset.DataObject;
import edu.iu.dsc.tws.api.resource.JobListener;
import edu.iu.dsc.tws.api.util.KryoSerializer;
import edu.iu.dsc.tws.master.worker.JMWorkerAgent;
import edu.iu.dsc.tws.master.worker.JMWorkerMessenger;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI;
import edu.iu.dsc.tws.proto.system.job.CDFWJobAPI;
import edu.iu.dsc.tws.task.impl.TaskExecutor;

//import java.util.LinkedHashSet;
//import java.util.Set;
//import edu.iu.dsc.tws.api.compute.graph.Vertex;
//import edu.iu.dsc.tws.api.compute.modifiers.Collector;
//import edu.iu.dsc.tws.api.compute.nodes.INode;

public class CDFWRuntime implements JobListener {

  private static final Logger LOG = Logger.getLogger(CDFWRuntime.class.getName());

  /**
   * Messages from the driver
   */
  private BlockingQueue<Any> executeMessageQueue;

  /**
   * Worker id
   */
  private int workerId;

  /**
   * Kryo serializer
   */
  private KryoSerializer serializer;

  /**
   * The outputs from previous graphs
   * [graph, [output name, data set]]
   */
  private Map<String, Map<String, DataObject<Object>>> outPuts = new HashMap<>();

  /**
   * Task executor
   */
  private TaskExecutor taskExecutor;


  private static int index = 0;

  /**
   * Connected dataflow runtime
   *
   * @param cfg configuration
   * @param wId worker id
   * @param workerInfoList worker information list
   * @param net network
   */
  public CDFWRuntime(Config cfg, int wId, List<JobMasterAPI.WorkerInfo> workerInfoList,
                     Communicator net) {
    taskExecutor = new TaskExecutor(cfg, wId, workerInfoList, net, null);
    this.executeMessageQueue = new LinkedBlockingQueue<>();
    this.workerId = wId;
    this.serializer = new KryoSerializer();
  }

  /**
   * execute
   */
  public boolean execute() {
    Any msg;
    while (true) {
      try {
        msg = executeMessageQueue.take();

        if (msg.is(CDFWJobAPI.ExecuteMessage.class)) {
          if (handleExecuteMessage(msg)) {
            return false;
          }
        } else if (msg.is(CDFWJobAPI.CDFWJobCompletedMessage.class)) {
          LOG.log(Level.INFO, workerId + "Received CDFW job completed message. Leaving execution "
              + "loop");
          break;
        } else {
          LOG.log(Level.WARNING, workerId + "Unknown message for cdfw task execution");
        }
      } catch (InterruptedException e) {
        LOG.log(Level.SEVERE, "Unable to insert message to the queue", e);
      }
    }

    LOG.log(Level.INFO, workerId + " Execution Completed");
    return true;
  }

  private boolean handleExecuteMessage(Any msg) {
    JMWorkerMessenger workerMessenger = JMWorkerAgent.getJMWorkerAgent().getJMWorkerMessenger();
    CDFWJobAPI.ExecuteMessage executeMessage;
    ExecutionPlan executionPlan;
    CDFWJobAPI.ExecuteCompletedMessage completedMessage = null;
    try {
      executeMessage = msg.unpack(CDFWJobAPI.ExecuteMessage.class);
      // get the subgraph from the map
      CDFWJobAPI.SubGraph subGraph = executeMessage.getGraph();
      ComputeGraph taskGraph = (ComputeGraph) serializer.deserialize(
          subGraph.getGraphSerialized().toByteArray());
      if (taskGraph == null) {
        LOG.severe(workerId + " Unable to find the subgraph " + subGraph.getName());
        return true;
      }

      // use the taskexecutor to create the execution plan
      executionPlan = taskExecutor.plan(taskGraph);
      if (subGraph.getInputsList().size() != 0) {
        for (CDFWJobAPI.Input input : subGraph.getInputsList()) {
          String inputName = input.getName();
          String inputGraph = input.getParentGraph();
          if (!outPuts.containsKey(inputGraph)) {
            throw new RuntimeException("We cannot find the input graph: " + inputGraph);
          }

          Map<String, DataObject<Object>> outsPerGraph = outPuts.get(inputGraph);
          if (!outsPerGraph.containsKey(inputName)) {
            throw new RuntimeException("We cannot find the input: " + inputName);
          }

          DataObject<Object> outPutObject = outsPerGraph.get(inputName);
          taskExecutor.addSourceInput(taskGraph, executionPlan, inputName, outPutObject);
        }
      }

      List<CDFWJobAPI.Input> inputs = subGraph.getInputsList();
      //now lets get those inputs
      for (CDFWJobAPI.Input in : inputs) {
        DataObject<Object> dataSet = outPuts.get(in.getParentGraph()).get(in.getName());
        taskExecutor.addSourceInput(taskGraph, executionPlan, in.getName(), dataSet);
      }

      // reuse the task executor execute
      /*if (subGraph.getGraphType().equalsIgnoreCase(Context.GRAPH_TYPE)) {
        Set<Vertex> taskVertexSet = new LinkedHashSet<>(taskGraph.getTaskVertexSet());
        String collectorTask = null;
        Set<String> collectibleNameSet = null;
        DataObject<Object> outPutObject;
        for (Vertex vertex : taskVertexSet) {
          INode iNode = vertex.getTask();
          if (iNode instanceof Collector) {
            collectorTask = vertex.getName();
            collectibleNameSet = ((Collector) iNode).getCollectibleNames();
          }
        }
        taskExecutor.execute(taskGraph, executionPlan);
        outPutObject = taskExecutor.getOutput(taskGraph, executionPlan, collectorTask);
        LOG.info("Output object:" + outPutObject);
        for (Object aCollectibleNameSet : collectibleNameSet) {
          taskExecutor.addSourceInput(
              taskGraph, executionPlan, aCollectibleNameSet.toString(), outPutObject);
        }
      } else {
        taskExecutor.execute(taskGraph, executionPlan);
        LOG.info("I am executing non-iterative graph");
      }*/
      taskExecutor.execute(taskGraph, executionPlan);
      if (subGraph.getGraphType().equals(Context.GRAPH_TYPE)) {
        LOG.log(Level.INFO, workerId + " Sending \t" + subGraph.getName()
            + "\tsubgraph completed for\t" + index + "\titeration(s)" + "\tmessage to driver");
        completedMessage = CDFWJobAPI.ExecuteCompletedMessage.newBuilder()
            .setIterations(index)
            .setSubgraphName(subGraph.getName()).build();
        ++index;
      } else {
        LOG.log(Level.INFO, workerId + " Sending " + subGraph.getName()
            + "\tsubgraph completed message to driver");
        completedMessage = CDFWJobAPI.ExecuteCompletedMessage.newBuilder()
            .setSubgraphName(subGraph.getName()).build();
      }
      //LOG.info("Graph Name:" + subGraph.getName() + "\tCompleted Message:" + completedMessage);

      Map<String, DataObject<Object>> outs = new HashMap<>();
      if (subGraph.getOutputsList().size() != 0) {
        List<CDFWJobAPI.Output> outPutNames = subGraph.getOutputsList();
        for (CDFWJobAPI.Output outputs : outPutNames) {
          DataObject<Object> outPut = taskExecutor.getOutput(
              taskGraph, executionPlan, outputs.getTaskname());
          outs.put(outputs.getName(), outPut);
        }
        outPuts.put(subGraph.getName(), outs);
      }
      if (!workerMessenger.sendToDriver(completedMessage)) {
        LOG.severe("Unable to send the subgraph completed message :" + completedMessage);
      }
    } catch (InvalidProtocolBufferException e) {
      LOG.log(Level.SEVERE, "Unable to unpack received message ", e);
    }
    return false;
  }

  @Override
  public void workersScaledUp(int instancesAdded) {
    LOG.log(Level.INFO, workerId + "Workers scaled up msg received. Instances added: "
        + instancesAdded);
  }

  @Override
  public void workersScaledDown(int instancesRemoved) {
    LOG.log(Level.INFO, workerId + "Workers scaled down msg received. Instances removed: "
        + instancesRemoved);
  }

  @Override
  public void driverMessageReceived(Any anyMessage) {
    // put every message on the queue.
    try {
      this.executeMessageQueue.put(anyMessage);
    } catch (InterruptedException e) {
      LOG.log(Level.SEVERE, "Unable to insert message to the queue", e);
    }
  }

  @Override
  public void allWorkersJoined(List<JobMasterAPI.WorkerInfo> workerList) {
    LOG.log(Level.INFO, workerId + "All workers joined msg received");
  }
}
