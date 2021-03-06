# Worker Controller

We developed an interface: IWorkerController and provided its implementations by using various cluster services. 
This interface provides the following services:

* Unique ID assignment to workers
* Discovery of worker address and resource information in a job
* Synchronizing workers using a barrier

**Unique ID assignment to workers**: We require that each worker in Twister2 jobs has a unique sequential ID starting from 0. 
When N workers are started in a Twister2 job, the workers will have the unique IDs in the range of 0 to \(N-1\).

**Discovery of worker addresses and compute resources in a job**: 
We assume that each worker in a Twister2 job has a unique IP address and port number pair. 
More than one worker may run on the same IP address, but they must have different port numbers. 
So, each Twister2 worker in a job must have a unique ID and a unique IP:port pair.

When a Twister2 job is scheduled in a cluster, usually multiple Twister2 workers are started in that cluster. 
All workers need to know the address of other workers in the job to be able to communicate with them.
In addition, workers in a job may have different compute resources such as CPU, RAM, and disk.
All workers need to know the compute resources of other workers.

The submitting client does not know where the workers will be started on the cluster, when it submits the job. 
Therefore, it can not provide this information to the workers when they start. 
Cluster resource schedulers start the workers in the nodes of the cluster.

**Node Location Information**: Twister2 can use worker locations when scheduling tasks. 
Workers may run in virtual machines such as pods in Kubernetes. 
So, worker IP addresses can be different from the IP address of the physical node it is running on. 
We provide a NodeInfo object for workers. It shows the physical node IP address for that worker. 
The rack name of the node it is running on. And the datacenter name where this node is running. 
Rack and datacenter names might not be available in all clusters.

**Synchronizing workers using a barrier**: All workers in a Twister2 job may need to synchronize on a barrier point. 
The workers that arrives earlier to the barrier point wait others to arrive. 
When the last worker arrives to the barrier point, they are all released.

## IWorkerController Interface

We designed an interface to be implemented by worker controllers and to be used by workers to discover other workers in a Twister job.

The interface is:

* [edu.iu.dsc.tws.common.controller.IWorkerController](https://github.com/DSC-SPIDAL/twister2/blob/master/twister2/common/src/java/edu/iu/dsc/tws/common/controller/IWorkerController.java)

## IWorkerController Implementations

We developed worker controllers implementing IWorkerController interface using various cluster services.

### ZooKeeper Based Worker Controller

We implemented a worker controller using a ZooKeeper server. ZooKeeper server runs in many clusters. 
This worker controller can be used in those clusters. The worker controller class is:

* [edu.iu.dsc.tws.rsched.bootstrap.ZKWorkerController](https://github.com/DSC-SPIDAL/twister2/tree/d9edff9bac47239db44731064aa5e2ac98867d97/twister2/resource-scheduler/src/java/edu/iu/dsc/tws/rsched/bootstrap/ZKWorkerController.java)

Details of the implementation is provided in [the document](zk-based-worker-controller.md).

### Job Master Based Worker Controller

Twister2 runs a Job Master in Twister2 jobs. We provide a Job Master based worker controller implementation. The worker controller class is:

* [edu.iu.dsc.tws.master.client.JMWorkerController](https://github.com/DSC-SPIDAL/twister2/tree/d9edff9bac47239db44731064aa5e2ac98867d97/twister2/master/src/java/edu/iu/dsc/tws/master/client/JMWorkerController.java)

Details of the implementation is provided in [the document](../job-master/job-master.md).

### Kubernetes Master Based Worker Controller

We developed a worker controller that uses Kubernetes master to discover other workers in a Twister2 job.  
The worker controller class is:

* [edu.iu.dsc.tws.rsched.schedulers.k8s.worker.K8sWorkerController](https://github.com/DSC-SPIDAL/twister2/tree/d9edff9bac47239db44731064aa5e2ac98867d97/twister2/resource-scheduler/src/java/edu/iu/dsc/tws/rsched/schedulers/k8s/worker/K8sWorkerController.java)

Details of the implementation is provided in [the document](../resource-schedulers/kubernetes/k8s-based-worker-discovery.md).


## Configuration Parameters

We use followibg two configuration parameters to set the maximum wait times for worker controllers. 

Following parameter determines the amount of time to wait for all workers to join the job, when getAllWorkers method is called.
Default value for this parameter is 100 seconds (100000).

```text
twister2.worker.controller.max.wait.time.for.all.workers.to.join
```

Following parameter determines the amount of time to wait on a barrier for all workers to arrive, when waitOnBarrier is called.
Default value for this parameter is 100 seconds (100000).

```text
twister2.worker.controller.max.wait.time.on.barrier
```

Both of these parameters can be found in system.yaml file.