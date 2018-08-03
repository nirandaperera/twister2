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
package edu.iu.dsc.tws.api.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.common.discovery.IWorkerController;
import edu.iu.dsc.tws.common.discovery.WorkerNetworkInfo;
import edu.iu.dsc.tws.common.net.NetworkInfo;
import edu.iu.dsc.tws.common.net.tcp.TCPChannel;
import edu.iu.dsc.tws.common.net.tcp.TCPContext;
import edu.iu.dsc.tws.comms.api.TWSChannel;
import edu.iu.dsc.tws.comms.core.TWSNetwork;
import edu.iu.dsc.tws.comms.core.TaskPlan;
import edu.iu.dsc.tws.comms.mpi.TWSMPIChannel;
import edu.iu.dsc.tws.comms.tcp.TWSTCPChannel;
import edu.iu.dsc.tws.rsched.spi.resource.ResourceContainer;
import edu.iu.dsc.tws.rsched.spi.resource.ResourcePlan;

import mpi.MPI;

public final class Network {
  private Network() {
  }

  public static TWSChannel initializeChannel(Config config, IWorkerController wController,
                                             ResourcePlan resourcePlan) {
    if (config.getStringValue("twister2.network.channel.class").equals(
        "edu.iu.dsc.tws.comms.dfw.tcp.TWSTCPChannel")) {
      return initializeTCPNetwork(config, wController, resourcePlan);
    } else {
      return initializeMPIChannel(config, wController, resourcePlan);
    }
  }

  public static TWSNetwork initializeNetwork(Config config, IWorkerController wController,
                                             TaskPlan plan, ResourcePlan resourcePlan) {
    if (config.getStringValue("twister2.network.channel.class").equals(
        "edu.iu.dsc.tws.comms.dfw.tcp.TWSTCPChannel")) {
      TWSChannel channel = initializeTCPNetwork(config, wController, resourcePlan);
      return new TWSNetwork(config, channel, plan);
    } else {
      return initializeMPINetwork(config, wController, plan);
    }
  }

  private static TWSChannel initializeMPIChannel(Config config,
                                                 IWorkerController wController,
                                                 ResourcePlan plan) {
    //first get the communication config file
    return new TWSMPIChannel(config, MPI.COMM_WORLD, plan.getThisId());
  }

  private static TWSNetwork initializeMPINetwork(Config config,
                                                 IWorkerController wController, TaskPlan plan) {
    //first get the communication config file
    return new TWSNetwork(config, plan);
  }

  private static TWSChannel initializeTCPNetwork(Config config,
                                                 IWorkerController wController,
                                                 ResourcePlan resourcePlan) {
    TCPChannel channel;
    int index = wController.getWorkerNetworkInfo().getWorkerID();
    Integer workerPort = wController.getWorkerNetworkInfo().getWorkerPort();
    String localIp = wController.getWorkerNetworkInfo().getWorkerIP().getHostAddress();
    try {
      channel = createChannel(config,
          new WorkerNetworkInfo(InetAddress.getByName(localIp), workerPort, index), index);
      // now lets start listening before sending the ports to master,
      channel.startListening();
    } catch (UnknownHostException e) {
      throw new RuntimeException("Failed to get network address: " + localIp, e);
    }
    // wait for everyone to start the job master
    wController.waitOnBarrier(30000);

    // now talk to a central server and get the information about the worker
    // this is a synchronization step
    List<WorkerNetworkInfo> wInfo = wController.getWorkerList();

    // lets start the client connections now
    List<NetworkInfo> nInfos = new ArrayList<>();
    for (WorkerNetworkInfo w : wInfo) {
      NetworkInfo networkInfo = new NetworkInfo(w.getWorkerID());
      networkInfo.addProperty(TCPContext.NETWORK_PORT, w.getWorkerPort());
      networkInfo.addProperty(TCPContext.NETWORK_HOSTNAME, w.getWorkerIP().getHostAddress());
      nInfos.add(networkInfo);

      ResourceContainer container = new ResourceContainer(w.getWorkerID());
      resourcePlan.addContainer(container);
    }
    // start the connections
    channel.startConnections(nInfos);
    // now lets wait for connections to be established
    channel.waitForConnections();

    // now lets create a tcp channel
    return new TWSTCPChannel(config, resourcePlan.getThisId(), channel);
  }

  /**
   * Start the TCP servers here
   * @param cfg the configuration
   * @param networkInfo network info
   * @param workerId worker id
   */
  private static TCPChannel createChannel(Config cfg, WorkerNetworkInfo networkInfo,
                                          int workerId) {
    NetworkInfo netInfo = new NetworkInfo(workerId);
    netInfo.addProperty(TCPContext.NETWORK_HOSTNAME, networkInfo.getWorkerIP().getHostAddress());
    netInfo.addProperty(TCPContext.NETWORK_PORT, networkInfo.getWorkerPort());
    return new TCPChannel(cfg, netInfo);
  }
}
