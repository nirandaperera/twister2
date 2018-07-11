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
package edu.iu.dsc.tws.rsched.schedulers.k8s;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.common.config.Context;
import edu.iu.dsc.tws.common.logging.LoggingContext;
import edu.iu.dsc.tws.master.JobMasterContext;
import edu.iu.dsc.tws.rsched.core.SchedulerContext;
import edu.iu.dsc.tws.rsched.spi.resource.RequestedResources;
import edu.iu.dsc.tws.rsched.spi.resource.ResourceContainer;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.models.V1Affinity;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1ContainerPort;
import io.kubernetes.client.models.V1EmptyDirVolumeSource;
import io.kubernetes.client.models.V1EnvVar;
import io.kubernetes.client.models.V1EnvVarSource;
import io.kubernetes.client.models.V1LabelSelector;
import io.kubernetes.client.models.V1LabelSelectorRequirement;
import io.kubernetes.client.models.V1NFSVolumeSource;
import io.kubernetes.client.models.V1NodeAffinity;
import io.kubernetes.client.models.V1NodeSelector;
import io.kubernetes.client.models.V1NodeSelectorRequirement;
import io.kubernetes.client.models.V1NodeSelectorTerm;
import io.kubernetes.client.models.V1ObjectFieldSelector;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1PersistentVolume;
import io.kubernetes.client.models.V1PersistentVolumeClaim;
import io.kubernetes.client.models.V1PersistentVolumeClaimSpec;
import io.kubernetes.client.models.V1PersistentVolumeClaimVolumeSource;
import io.kubernetes.client.models.V1PersistentVolumeSpec;
import io.kubernetes.client.models.V1PodAffinity;
import io.kubernetes.client.models.V1PodAffinityTerm;
import io.kubernetes.client.models.V1PodAntiAffinity;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1PodTemplateSpec;
import io.kubernetes.client.models.V1ResourceRequirements;
import io.kubernetes.client.models.V1SecretVolumeSource;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1ServicePort;
import io.kubernetes.client.models.V1ServiceSpec;
import io.kubernetes.client.models.V1Volume;
import io.kubernetes.client.models.V1VolumeMount;
import io.kubernetes.client.models.V1beta2StatefulSet;
import io.kubernetes.client.models.V1beta2StatefulSetSpec;

/**
 * build objects to submit to Kubernetes master
 */
public final class RequestObjectBuilder {

  private RequestObjectBuilder() { }

  /**
   * create StatefulSet object for a job
   * @param jobName
   * @param resourceRequest
   * @return
   */
  public static V1beta2StatefulSet createStatefulSetObjectForJob(String jobName,
                                                                 RequestedResources resourceRequest,
                                                                 long jobFileSize,
                                                                 Config config) {

    V1beta2StatefulSet statefulSet = new V1beta2StatefulSet();
    statefulSet.setApiVersion("apps/v1beta2");
    statefulSet.setKind("StatefulSet");

    // construct metadata and set for jobName setting
    V1ObjectMeta meta = new V1ObjectMeta();
    meta.setName(jobName);
    statefulSet.setMetadata(meta);

    // construct JobSpec and set
    V1beta2StatefulSetSpec setSpec = new V1beta2StatefulSetSpec();
    setSpec.serviceName(KubernetesUtils.createServiceName(jobName));
    // pods will be started in parallel
    // by default they are started sequentially
    setSpec.setPodManagementPolicy("Parallel");

    int containersPerPod = KubernetesContext.workersPerPod(config);
    int numberOfPods = resourceRequest.getNoOfContainers() / containersPerPod;
    setSpec.setReplicas(numberOfPods);

    // add selector for the job
    V1LabelSelector selector = new V1LabelSelector();
    String serviceLabel = KubernetesUtils.createServiceLabel(jobName);
    selector.putMatchLabelsItem(KubernetesConstants.SERVICE_LABEL_KEY, serviceLabel);
    setSpec.setSelector(selector);

    // construct the pod template
    V1PodTemplateSpec template = constructPodTemplate(
        resourceRequest.getContainer(), serviceLabel, jobFileSize, config);
    setSpec.setTemplate(template);

    statefulSet.setSpec(setSpec);

    return statefulSet;
  }

  /**
   * construct pod template
   * @param reqContainer
   * @param serviceLabel
   * @param jobFileSize
   * @param config
   * @return
   */
  public static V1PodTemplateSpec constructPodTemplate(ResourceContainer reqContainer,
                                                       String serviceLabel,
                                                       long jobFileSize,
                                                       Config config) {

    V1PodTemplateSpec template = new V1PodTemplateSpec();
    V1ObjectMeta templateMetaData = new V1ObjectMeta();
    HashMap<String, String> labels = new HashMap<String, String>();
    labels.put(KubernetesConstants.SERVICE_LABEL_KEY, serviceLabel);
    templateMetaData.setLabels(labels);
    template.setMetadata(templateMetaData);

    V1PodSpec podSpec = new V1PodSpec();
    podSpec.setTerminationGracePeriodSeconds(0L);

    ArrayList<V1Volume> volumes = new ArrayList<>();
    V1Volume memoryVolume = new V1Volume();
    memoryVolume.setName(KubernetesConstants.POD_MEMORY_VOLUME_NAME);
    V1EmptyDirVolumeSource volumeSource1 = new V1EmptyDirVolumeSource();
    volumeSource1.setMedium("Memory");
    memoryVolume.setEmptyDir(volumeSource1);
    volumes.add(memoryVolume);

    // a volatile disk based volume
    // create it if the requested disk space is positive
    if (SchedulerContext.volatileDiskRequested(config)) {
      double volumeSize =
          SchedulerContext.workerVolatileDisk(config) * SchedulerContext.workerInstances(config);
      V1Volume volatileVolume = createVolatileVolumeObject(volumeSize);
      volumes.add(volatileVolume);
    }

    String persistentJobDir = null;

    if (SchedulerContext.persistentVolumeRequested(config)) {
      String claimName = KubernetesUtils.createStorageClaimName(SchedulerContext.jobName(config));
      V1Volume persistentVolume = createPersistentVolumeObject(claimName);
      volumes.add(persistentVolume);

      persistentJobDir =
          KubernetesUtils.createPersistentJobDirName(SchedulerContext.jobName(config),
              KubernetesContext.persistentVolumeUploading(config));
    }

    // if openmpi is used, we initialize a Secret volume on each pod
    if (KubernetesContext.workersUseOpenMPI(config)) {
      String secretName = "kubempi-ssh-key";
      V1Volume secretVolume = createSecretVolumeObject(secretName);
      volumes.add(secretVolume);
    }

    podSpec.setVolumes(volumes);

    int containersPerPod = KubernetesContext.workersPerPod(config);

    // if openmpi is used, we initialize only one container for each pod
    if (KubernetesContext.workersUseOpenMPI(config)) {
      containersPerPod = 1;
    }

    ArrayList<V1Container> containers = new ArrayList<V1Container>();
    for (int i = 0; i < containersPerPod; i++) {
      containers.add(constructContainer(i, reqContainer, jobFileSize, persistentJobDir, config));
    }
    podSpec.setContainers(containers);

    V1Affinity affinity = new V1Affinity();
    boolean affinitySet = false;
    if (KubernetesContext.workerToNodeMapping(config)) {
      setNodeAffinity(config, affinity);
      affinitySet = true;
    }

    String uniformMappingType = KubernetesContext.workerMappingUniform(config);
    if ("all-same-node".equalsIgnoreCase(uniformMappingType)
        || "all-separate-nodes".equalsIgnoreCase(uniformMappingType)) {
      setUniformMappingAffinity(config, affinity);
      affinitySet = true;
    }

    // if affinity is initialized, set it
    if (affinitySet) {
      podSpec.setAffinity(affinity);
    }

    template.setSpec(podSpec);
    return template;
  }

  public static V1Volume createVolatileVolumeObject(double volumeSize) {
    V1Volume volatileVolume = new V1Volume();
    volatileVolume.setName(KubernetesConstants.POD_VOLATILE_VOLUME_NAME);
    V1EmptyDirVolumeSource volumeSource2 = new V1EmptyDirVolumeSource();
    volumeSource2.setSizeLimit(volumeSize + "Gi");
    volatileVolume.setEmptyDir(volumeSource2);
    return volatileVolume;
  }

  public static V1Volume createPersistentVolumeObject(String claimName) {
    V1Volume persistentVolume = new V1Volume();
    persistentVolume.setName(KubernetesConstants.PERSISTENT_VOLUME_NAME);
    V1PersistentVolumeClaimVolumeSource perVolSource = new V1PersistentVolumeClaimVolumeSource();
    perVolSource.setClaimName(claimName);
    persistentVolume.setPersistentVolumeClaim(perVolSource);
    return persistentVolume;
  }

  public static V1Volume createSecretVolumeObject(String secretName) {
    V1Volume secretVolume = new V1Volume();
    secretVolume.setName(KubernetesConstants.SECRET_VOLUME_NAME);
    V1SecretVolumeSource secretVolumeSource = new V1SecretVolumeSource();
    secretVolumeSource.setSecretName(secretName);
    secretVolumeSource.setDefaultMode(256);
    secretVolume.setSecret(secretVolumeSource);
    return secretVolume;
  }

  /**
   * construct a container
   * @param containerIndex
   * @param reqContainer
   * @param jobFileSize
   * @param config
   * @return
   */
  public static V1Container constructContainer(int containerIndex,
                                               ResourceContainer reqContainer,
                                               long jobFileSize,
                                               String persistentJobDir,
                                               Config config) {
    // construct container and add it to podSpec
    V1Container container = new V1Container();
    String containerName = KubernetesUtils.createContainerName(containerIndex);
    container.setName(containerName);
    container.setImage(KubernetesConstants.TWISTER2_DOCKER_IMAGE);
    // by default: IfNotPresent
    // can be set to Always from client.yaml
    container.setImagePullPolicy(KubernetesContext.imagePullPolicy(config));

//        container.setArgs(Arrays.asList("1000000")); parameter to the main method
    if (KubernetesContext.workersUseOpenMPI(config)) {
      container.setCommand(
          Arrays.asList(
              "./init_openmpi.sh"));
    } else {
      container.setCommand(
          Arrays.asList(
              "java", "edu.iu.dsc.tws.rsched.schedulers.k8s.worker.KubernetesWorkerStarter"));
    }

    V1ResourceRequirements resReq = new V1ResourceRequirements();
    if (KubernetesContext.bindWorkerToCPU(config)) {
      resReq.putLimitsItem("cpu", new Quantity(reqContainer.getNoOfCpus() + ""));
      resReq.putLimitsItem("memory", new Quantity(reqContainer.getMemoryMegaBytes() + "Mi"));
    } else {
      resReq.putRequestsItem("cpu", new Quantity(reqContainer.getNoOfCpus() + ""));
      resReq.putRequestsItem("memory", new Quantity(reqContainer.getMemoryMegaBytes() + "Mi"));
    }
    container.setResources(resReq);

    ArrayList<V1VolumeMount> volumeMounts = new ArrayList<>();
    V1VolumeMount memoryVolumeMount = new V1VolumeMount();
    memoryVolumeMount.setName(KubernetesConstants.POD_MEMORY_VOLUME_NAME);
    memoryVolumeMount.setMountPath(KubernetesConstants.POD_MEMORY_VOLUME);
    volumeMounts.add(memoryVolumeMount);

    if (SchedulerContext.volatileDiskRequested(config)) {
      V1VolumeMount volatileVolumeMount = new V1VolumeMount();
      volatileVolumeMount.setName(KubernetesConstants.POD_VOLATILE_VOLUME_NAME);
      volatileVolumeMount.setMountPath(KubernetesConstants.POD_VOLATILE_VOLUME);
      volumeMounts.add(volatileVolumeMount);
    }

    if (SchedulerContext.persistentVolumeRequested(config)) {
      V1VolumeMount persVolumeMount = new V1VolumeMount();
      persVolumeMount.setName(KubernetesConstants.PERSISTENT_VOLUME_NAME);
      persVolumeMount.setMountPath(KubernetesConstants.PERSISTENT_VOLUME_MOUNT);
      volumeMounts.add(persVolumeMount);
    }

    // mount Secret object as a volume
    if (KubernetesContext.workersUseOpenMPI(config)) {
      V1VolumeMount persVolumeMount = new V1VolumeMount();
      persVolumeMount.setName(KubernetesConstants.SECRET_VOLUME_NAME);
      persVolumeMount.setMountPath(KubernetesConstants.SECRET_VOLUME_MOUNT);
      volumeMounts.add(persVolumeMount);
    }

    container.setVolumeMounts(volumeMounts);

    int containerPort = KubernetesContext.workerBasePort(config) + containerIndex;

    V1ContainerPort port = new V1ContainerPort();
    port.name("port11"); // currently not used
    port.containerPort(containerPort);
    port.setProtocol(KubernetesContext.workerTransportProtocol(config));
    container.setPorts(Arrays.asList(port));

    container.setEnv(
        constructEnvironmentVariables(config, containerName, jobFileSize,
            persistentJobDir, containerPort));

    return container;
  }

  /**
   * set environment variables for containers
   * @param config
   * @param containerName
   * @param jobFileSize
   * @param persistentJobDir
   */
  public static List<V1EnvVar> constructEnvironmentVariables(Config config,
                                                   String containerName, long jobFileSize,
                                                   String persistentJobDir, int workerPort) {
    ArrayList<V1EnvVar> envVars = new ArrayList<>();

    envVars.add(new V1EnvVar()
        .name(KubernetesContext.KUBERNETES_NAMESPACE)
        .value(KubernetesContext.namespace(config)));

    envVars.add(new V1EnvVar()
        .name(SchedulerContext.JOB_PACKAGE_FILENAME)
        .value(SchedulerContext.jobPackageFileName(config)));

    envVars.add(new V1EnvVar()
        .name(KubernetesField.JOB_PACKAGE_FILE_SIZE + "")
        .value(jobFileSize + ""));

    envVars.add(new V1EnvVar()
        .name(KubernetesField.CONTAINER_NAME + "")
        .value(containerName));

    envVars.add(new V1EnvVar()
        .name(KubernetesField.USER_JOB_JAR_FILE + "")
        .value(SchedulerContext.userJobJarFile(config)));

    // POD_IP with downward API
    V1ObjectFieldSelector fieldSelector = new V1ObjectFieldSelector();
    fieldSelector.setFieldPath("status.podIP");
    V1EnvVarSource varSource = new V1EnvVarSource();
    varSource.setFieldRef(fieldSelector);

    envVars.add(new V1EnvVar()
        .name(KubernetesField.POD_IP + "")
        .valueFrom(varSource));

    envVars.add(new V1EnvVar()
        .name(KubernetesContext.PERSISTENT_JOB_DIRECTORY)
        .value(persistentJobDir));

    envVars.add(new V1EnvVar()
        .name(KubernetesContext.WORKERS_PER_POD)
        .value(KubernetesContext.workersPerPod(config) + ""));

    envVars.add(new V1EnvVar()
        .name(LoggingContext.PERSISTENT_LOGGING_REQUESTED)
        .value(LoggingContext.persistentLoggingRequested(config) + ""));

    envVars.add(new V1EnvVar()
        .name(LoggingContext.LOGGING_LEVEL)
        .value(LoggingContext.loggingLevel(config)));

    envVars.add(new V1EnvVar()
        .name(LoggingContext.REDIRECT_SYS_OUT_ERR)
        .value(LoggingContext.redirectSysOutErr(config) + ""));

    envVars.add(new V1EnvVar()
        .name(LoggingContext.MAX_LOG_FILE_SIZE)
        .value(LoggingContext.maxLogFileSize(config) + ""));

    envVars.add(new V1EnvVar()
        .name(LoggingContext.MAX_LOG_FILES)
        .value(LoggingContext.maxLogFiles(config) + ""));

    envVars.add(new V1EnvVar()
        .name(KubernetesContext.PERSISTENT_VOLUME_UPLOADING)
        .value(KubernetesContext.persistentVolumeUploading(config) + ""));

    envVars.add(new V1EnvVar()
        .name(Context.TWISTER2_WORKER_INSTANCES)
        .value(Context.workerInstances(config) + ""));

    envVars.add(new V1EnvVar()
        .name(KubernetesContext.K8S_WORKER_PORT)
        .value(workerPort + ""));

    envVars.add(new V1EnvVar()
        .name(JobMasterContext.JOB_MASTER_PORT)
        .value(JobMasterContext.jobMasterPort(config) + ""));

    String masterAddress = null;
    if (JobMasterContext.jobMasterRunsInClient(config)) {
      masterAddress = KubernetesUtils.getLocalAddress();
    }
    envVars.add(new V1EnvVar()
        .name(JobMasterContext.JOB_MASTER_IP)
        .value(masterAddress));

    envVars.add(new V1EnvVar()
        .name(JobMasterContext.PING_INTERVAL)
        .value(JobMasterContext.pingInterval(config) + ""));

    envVars.add(new V1EnvVar()
        .name(JobMasterContext.JOB_MASTER_ASSIGNS_WORKER_IDS)
        .value(JobMasterContext.jobMasterAssignsWorkerIDs(config) + ""));

    envVars.add(new V1EnvVar()
        .name(JobMasterContext.WORKER_TO_JOB_MASTER_RESPONSE_WAIT_DURATION)
        .value(JobMasterContext.responseWaitDuration(config) + ""));

    envVars.add(new V1EnvVar()
        .name(KubernetesContext.KUBERNETES_NAMESPACE)
        .value(KubernetesContext.namespace(config)));

    envVars.add(new V1EnvVar()
        .name(KubernetesField.CLASS_TO_RUN + "")
        .value("edu.iu.dsc.tws.rsched.schedulers.k8s.mpi.MPIMasterStarter"));

    envVars.add(new V1EnvVar()
        .name(KubernetesField.POD_MEMORY_VOLUME + "")
        .value(KubernetesConstants.POD_MEMORY_VOLUME));

    envVars.add(new V1EnvVar()
        .name(KubernetesField.DIR_PREFIX_FOR_JOB_ARCHIVE + "")
        .value(Context.DIR_PREFIX_FOR_JOB_ARCHIVE));

    envVars.add(new V1EnvVar()
        .name(KubernetesField.JOB_PACKAGE_FILENAME + "")
        .value(SchedulerContext.jobPackageFileName(config)));

    return envVars;
  }

  public static void setNodeAffinity(Config config, V1Affinity affinity) {

    String key = KubernetesContext.workerMappingKey(config);
    String operator = KubernetesContext.workerMappingOperator(config);
    List<String> values = KubernetesContext.workerMappingValues(config);

    V1NodeSelectorRequirement nsRequirement = new V1NodeSelectorRequirement();
    nsRequirement.setKey(key);
    nsRequirement.setOperator(operator);
    nsRequirement.setValues(values);

    V1NodeSelectorTerm selectorTerm = new V1NodeSelectorTerm();
    selectorTerm.addMatchExpressionsItem(nsRequirement);

    V1NodeSelector nodeSelector = new V1NodeSelector();
    nodeSelector.addNodeSelectorTermsItem(selectorTerm);

    V1NodeAffinity nodeAffinity = new V1NodeAffinity();
    nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution(nodeSelector);

    affinity.setNodeAffinity(nodeAffinity);
  }

  public static void setUniformMappingAffinity(Config config, V1Affinity affinity) {

    String mappingType = KubernetesContext.workerMappingUniform(config);
    String key = KubernetesConstants.SERVICE_LABEL_KEY;
    String operator = "In";
    String serviceLabel = KubernetesUtils.createServiceLabel(SchedulerContext.jobName(config));
    List<String> values = Arrays.asList(serviceLabel);

    V1LabelSelectorRequirement labelRequirement = new V1LabelSelectorRequirement();
    labelRequirement.setKey(key);
    labelRequirement.setOperator(operator);
    labelRequirement.setValues(values);

    V1LabelSelector labelSelector = new V1LabelSelector();
    labelSelector.addMatchExpressionsItem(labelRequirement);

    V1PodAffinityTerm affinityTerm = new V1PodAffinityTerm();
    affinityTerm.setLabelSelector(labelSelector);
    affinityTerm.setTopologyKey("kubernetes.io/hostname");

    if ("all-same-node".equalsIgnoreCase(mappingType)) {
      V1PodAffinity podAffinity = new V1PodAffinity();
      podAffinity.requiredDuringSchedulingIgnoredDuringExecution(Arrays.asList(affinityTerm));
      affinity.setPodAffinity(podAffinity);
    } else if ("all-separate-nodes".equalsIgnoreCase(mappingType)) {
      V1PodAntiAffinity podAntiAffinity = new V1PodAntiAffinity();
      podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution(Arrays.asList(affinityTerm));
      affinity.setPodAntiAffinity(podAntiAffinity);
    }

  }

  public static V1Service createJobServiceObject(String jobName) {

    String serviceName = KubernetesUtils.createServiceName(jobName);
    String serviceLabel = KubernetesUtils.createServiceLabel(jobName);

    return createHeadlessServiceObject(serviceName, serviceLabel);
  }

  public static V1Service createJobMasterServiceObject(String jobName) {

    String serviceName = KubernetesUtils.createJobMasterServiceName(jobName);
    String serviceLabel = KubernetesUtils.createJobMasterServiceLabel(jobName);

    return createHeadlessServiceObject(serviceName, serviceLabel);
  }

  public static V1Service createHeadlessServiceObject(String serviceName, String serviceLabel) {

    V1Service service = new V1Service();
    service.setKind("Service");
    service.setApiVersion("v1");

    // construct and set metadata
    V1ObjectMeta meta = new V1ObjectMeta();
    meta.setName(serviceName);
    service.setMetadata(meta);

    // construct and set service spec
    V1ServiceSpec serviceSpec = new V1ServiceSpec();
    // ClusterIP needs to be None for headless service
    serviceSpec.setClusterIP("None");
    // set selector
    HashMap<String, String> selectors = new HashMap<String, String>();
    selectors.put(KubernetesConstants.SERVICE_LABEL_KEY, serviceLabel);
    serviceSpec.setSelector(selectors);

    service.setSpec(serviceSpec);

    return service;
  }

  public static V1Service createNodePortServiceObject(Config config, String jobName) {

    String serviceName = KubernetesUtils.createServiceName(jobName);
    String serviceLabel = KubernetesUtils.createServiceLabel(jobName);
    int workerPort = KubernetesContext.workerBasePort(config);
    int nodePort = KubernetesContext.serviceNodePort(config);
    String protocol = KubernetesContext.workerTransportProtocol(config);

    V1Service service = new V1Service();
    service.setKind("Service");
    service.setApiVersion("v1");

    // construct and set metadata
    V1ObjectMeta meta = new V1ObjectMeta();
    meta.setName(serviceName);
    service.setMetadata(meta);

    // construct and set service spec
    V1ServiceSpec serviceSpec = new V1ServiceSpec();
    // ClusterIP needs to be None for headless service
    serviceSpec.setType("NodePort");
    // set selector
    HashMap<String, String> selectors = new HashMap<String, String>();
    selectors.put(KubernetesConstants.SERVICE_LABEL_KEY, serviceLabel);
    serviceSpec.setSelector(selectors);

    ArrayList<V1ServicePort> ports = new ArrayList<V1ServicePort>();
    V1ServicePort servicePort = new V1ServicePort();
    servicePort.setPort(workerPort);
    servicePort.setProtocol(protocol);
//    servicePort.setTargetPort(new IntOrString("port11"));
    if (nodePort != 0) {
      servicePort.nodePort(nodePort);
    }
    ports.add(servicePort);
    serviceSpec.setPorts(ports);

    service.setSpec(serviceSpec);

    return service;
  }

  /**
   * we initially used this method to create PersistentVolumes
   * we no longer use this method
   * it is just here in case we may need it for some reason at one point
   * @param config
   * @param pvName
   * @return
   */
  public static V1PersistentVolume createPersistentVolumeObject(Config config, String pvName) {
    V1PersistentVolume pv = new V1PersistentVolume();
    pv.setApiVersion("v1");

    // set pv name
    V1ObjectMeta meta = new V1ObjectMeta();
    meta.setName(pvName);
    pv.setMetadata(meta);

//    double volumeSize = SchedulerContext.persistentVolumeTotal(config);
    V1PersistentVolumeSpec pvSpec = new V1PersistentVolumeSpec();
    HashMap<String, Quantity> capacity = new HashMap<>();
//    capacity.put("storage", new Quantity(volumeSize + "Gi"));
    pvSpec.setCapacity(capacity);

    String storageClass = KubernetesContext.persistentStorageClass(config);
    String accessMode = KubernetesContext.storageAccessMode(config);
//    String reclaimPolicy = KubernetesContext.storageReclaimPolicy(config);
    pvSpec.setStorageClassName(storageClass);
    pvSpec.setAccessModes(Arrays.asList(accessMode));
//    pvSpec.setPersistentVolumeReclaimPolicy(reclaimPolicy);
//    pvSpec.setMountOptions(Arrays.asList("hard", "nfsvers=4.1"));

    V1NFSVolumeSource nfsVolumeSource = new V1NFSVolumeSource();
    nfsVolumeSource.setServer(SchedulerContext.nfsServerAddress(config));
    nfsVolumeSource.setPath(SchedulerContext.nfsServerPath(config));
    pvSpec.setNfs(nfsVolumeSource);

    pv.setSpec(pvSpec);

    return pv;
  }

  public static V1PersistentVolumeClaim createPersistentVolumeClaimObject(
      Config config, String pvcName) {

    V1PersistentVolumeClaim pvc = new V1PersistentVolumeClaim();
    pvc.setApiVersion("v1");

    // set pvc name
    V1ObjectMeta meta = new V1ObjectMeta();
    meta.setName(pvcName);
    pvc.setMetadata(meta);

    String storageClass = KubernetesContext.persistentStorageClass(config);

    // two methods to set StorageClass, we set in pvcSpec
//    HashMap<String, String> annotations = new HashMap<>();
//    annotations.put("volume.beta.kubernetes.io/storage-class", storageClass);
//    meta.setAnnotations(annotations);

    String accessMode = KubernetesContext.storageAccessMode(config);
    V1PersistentVolumeClaimSpec pvcSpec = new V1PersistentVolumeClaimSpec();
    pvcSpec.setStorageClassName(storageClass);
    pvcSpec.setAccessModes(Arrays.asList(accessMode));

    V1ResourceRequirements resources = new V1ResourceRequirements();
    double storageSize = SchedulerContext.persistentVolumePerWorker(config);
    resources.putRequestsItem("storage", new Quantity(storageSize + "Gi"));
    pvcSpec.setResources(resources);

    pvc.setSpec(pvcSpec);
    return pvc;
  }
}
