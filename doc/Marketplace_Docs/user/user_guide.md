# 4G PM Event File Transfer and Processing Service User Guide

[TOC]

## Overview

This document provides an overview of the 4G PM Event File Transfer and Processing service. It gives a brief description
of its main features and its interfaces.

The primary goal of the 4G PM Event File Transfer and Processing service is to collect 4G event files from Ericsson
Network Manager based on
the notifications read from Message Bus KF and produced by the File Notification Service.
4G PM Event File Transfer and Processing service parses the 4G event files it receives and splits the contents of each file by eventID into records in CTR format. The records are written
onto a Message Bus KF Output Topic to be consumed by 4g-pm-event-parser.                                                         .

### Supported Use Cases

This chapter gives an overview of the supported use cases.

| Use Case ID    | Use Case Title                                                                                                                                     | Compliance      |
|----------------|----------------------------------------------------------------------------------------------------------------------------------------------------|-----------------|
| UC.ADC.PM4G.1  | Copy PM event files from Ericsson Network Manager based on notification and split the files into records, write records to a Message Bus KF topic. | Fully Supported |
| UC.ADC.PM4G.2  | Verify dependent services are up and running before starting to listen to the Message Bus KF Input Topic.                                          | Fully Supported |
| UC.ADC.PM4G.3  | Verify Necessary data is available before starting to listen to the Message Bus KF Input Topic.                                                    | Fully Supported |
| UC.ADC.PM4G.4  | Fetch data corresponding to Message Bus KF Input Topic from Data Catalog service.                                                                  | Fully Supported |
| UC.ADC.PM4G.5  | Registration of data for the 4G PM Event File Transfer and Processing service in Data Catalog.                                                     | Fully Supported |
| UC.ADC.PM4G.6  | Consumption of Message Bus KF Input Topic notification.                                                                                            | Fully Supported |
| UC.ADC.PM4G.7  | Parse Message Bus KF Input Topic notification message.                                                                                             | Fully Supported |
| UC.ADC.PM4G.8  | Fetch data from Connected Systems service.                                                                                                         | Fully Supported |
| UC.ADC.PM4G.9  | Establish SFTP connection to Ericsson Network Manager.                                                                                             | Fully Supported |
| UC.ADC.PM4G.10 | Download 4G event file from Ericsson Network Manager.                                                                                              | Fully Supported |
| UC.ADC.PM4G.11 | Process 4G event file.                                                                                                                             | Fully Supported |
| UC.ADC.PM4G.12 | Write Message to Message Bus KF Output Topic for each event in CTR format.                                                                         | Fully Supported |
| UC.ADC.PM4G.13 | Clean up performed post-processing of 4G event file.                                                                                               | Fully Supported |

### Architecture

The following picture shows the 4G PM Event File Transfer and Processing service and its architectural context.

![Architecture](4gpmevent_Architechture_Diagram.png)

Figure 1 Architecture view of 4G PM Event File Transfer and Processing service

#### Logical Interfaces

| Interface Logical Name | Interface Realization                | Description                                                                                                                                                                                                                              | Interface Maturity |
|------------------------|--------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------|
| MB.FNS.TOPIC           | [Message Bus KF microservice][mbkf]  | The  Message Bus KF Input topic is owned and created by the File Notification service. It is used to alert the service that there are new 4G event files available.                                                                      | Stable             |
| MB.4G.OUT.TOPIC        | [Message Bus KF microservice][mbkf]  | The  Message Bus KF Output Topic is owned and created by the service, and it is registered in Data Catalog. The service publishes the events on the  Message Bus KF Output Topic so that they may be consumed by other services. | Stable             |
| CS.Service.REST        | [Connected Systems][cs]              | This service provides SFTP Credentials for the Ericsson Network Manager Scripting VMs.                                                                                                                                                   | Stable             |
| ENM.SVM.SFTP           | [ENM File Notification][enm]         | PM Event files are downloaded onto the service's ephemeral storage from Ericsson Network Manager's file system using it's Scripting VM interface (behaving as an SFTP server).                                                           | Beta               |
| DC.REST                | [Data Catalog][dc]                   | Data Catalog stores prerequisite information on the topic that the service is interested in. Data Catalog also provides the name of Connected Systems to the service.                                                                    | Stable             |
| DC.REG.REST            | [Data Catalog][dc]                   | Registers the Output Topic and Message Schema with Data Catalog.                                                                                                                                                                        | Stable              |
| PM.METRICS.PULL        | [Performance Management Server][pms] | Performance Management Server is used for metrics.                                                                                                                                                                                       | Beta               |


### Deployment View

4G PM Event File Transfer and Processing service is packaged as a Docker image. It supports deployment in
Kubernetes using Helm.

The jar of the 4G PM Event File Transfer and Processing service is stored in one container in a pod. It supports one or
more pod replicas (instances of the service).

Once deployed, the service will verify dependent services(Data Catalog, Kafka, Connected Systems and Scripting VM) and if depended services are up and running then it will query the Data Catalog to get the Input Topic details.

If depended services are not up and running, the service will keep retrying until all the depended services are available.

The service can then register the Output Topic entry on Data Catalog.

The service listens to an Input Topic that is produced by the File Notification service.
The Input Topic records contain the path to the file to be retrieved from the Ericsson Network Manager Scripting VM.
The service will initiate the downloading of PM Event files using
the SFTP service from the Ericsson Network Manager Scripting VM.

![Deployment Overview](4gpmevent_Interface_Overview.png)

Figure 2 Deployment view of 4G PM Event File Transfer and Processing service

To deploy the service, refer to the [Deployment section](#deployment), which:

- Explains how to get started using the 4G PM Event File Transfer and Processing service in the
  supported environments.
- Specifies configuration options for starting the 4G PM Event File Transfer and Processing docker
  container.

If problems occur when using the service, refer to the [Troubleshooting section](#troubleshooting).

### Dimensioning and Characteristics

#### Dimensioning

The service provides by default resource request values and resource limit values as part of the Helm chart.
It should be noted that the number of listeners inside a pod is bound by the amount of CPU. One CPU core is required for
each listener.

To handle dimensioning configuration at deployment time,
refer to the [Deployment section](#deployment).

#### Scaling


If manual scaling is performed through Helm during the middle of a ROP the Consumer will be rebalanced, work distributed
amongst new pods/listeners by partition.
Scaling performance is bound by the maximum number of SFTP connections supported by the Ericsson Network Manager
Scripting VM and how many Scripting VMs are available. The scaling performance is also dependent on the number of Input
Topic partitions set by the File Notification Service.

> Max number of Replicas pods = Input Topic partitions / consumers per pod (concurrency)

For example:
> 10 Input Topic partitions / 2 Concurrency = 5 Replica pods

| Scaling Supported (Yes/No) | Minimum number of instances | Maximum number of recommended instances |
|----------------------------|-----------------------------|-----------------------------------------|
| Yes (Manual)               | 1                           | 3                                       |

#### Resilience

* 4G PM Event File Transfer and Processing Service uses Liveness and Readiness probes provided by Kubernetes.

* By default, the 4G PM Event File Transfer and Processing Service has a replica count of 1. This can be increased in
  the values.yaml or at deployment time using --set arguments to provided higher availability.

* A circuit breaker design pattern is used for providing [Resilience](#resilience) with SFTP connections and file
  downloading. The related parameters are set in the values.yaml under eventFileDownload,
  see [Configuration Parameters](#configuration-parameters) for details.

* When the 4G PM Event File Transfer and Processing Service pod is deployed when there are no external dependencies
  available the pod waits gracefully for dependent services to be ready. As dependent services become available the
  service proceeds to normal operation. The diagram below shows the flow of these external dependency checks.

  ![Resilience](4gpmevent_Dependency_Check.png)


## Deployment

This section describes the operational procedures for how to deploy and upgrade
the 4G PM Event File Transfer and Processing service in a Kubernetes environment with Helm. It also
covers hardening guidelines to consider when deploying this service.

### Prerequisites

List of prerequisites for service deployment.<br/>

- A running Kubernetes environment with Helm support, some knowledge
  of the Kubernetes environment, including the networking detail, and
  access rights to deploy and manage workloads.

- Availability of the kubectl CLI tool with correct authentication
  details. Contact the Kubernetes System Admin if necessary.

- Availability of the Helm package.

- Availability of Helm charts and Docker images for the service and
  all dependent services:
    - Data Management and Movement
    - File Notification Service
    - Connected Systems
    - ENM Scripting VMs

### Custom Resource Definition (CRD) Deployment

No Custom Resource Definitions are used with the 4G PM Event File Transfer and Processing service.

### Deployment in a Kubernetes Environment Using Helm

This section describes how to deploy the service in Kubernetes using Helm and
the `kubectl` CLI client. Helm is a package manager for Kubernetes that
streamlines the installation and management of Kubernetes applications.

#### Preparation

Prepare Helm chart and Docker images. Helm chart in the following link
can be used for installation:

https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-drop-helm/eric-oss-4gpmevent-filetrans-proc/

#### Pre-Deployment Checks for 4G PM Event Transfer and Processing Service

Ensure the following:

- The <RELEASE_NAME> is not used already in the corresponding cluster.
  Use `helm list` command to list the existing deployments (and delete previous
  deployment with the corresponding <RELEASE_NAME> if needed).

- The same namespace is used for all deployments.

#### Helm Chart Installations of Dependent Services

This service requires Data Management and Movement, File Notification Service and Connected Systems to be installed
on the same namespace prior to deploying the service.


#### Helm Chart Installation of 4G PM Event File Transfer and Processing Service

> **_Note:_** Ensure all dependent services are deployed and healthy before you
> continue with this step (see previous chapter).

Helm is a tool that streamlines installing and managing Kubernetes
applications. 4G PM Event File Transfer and Processing service can be deployed on Kubernetes using
Helm Charts. Charts are packages of pre-configured Kubernetes resources.

Users can override the default values provided in the values.yaml template of
the Helm chart. The recommended parameters to override are listed in the
following section: [Configuration Parameters](#configuration-parameters).

##### Deploy the 4G PM Event File Transfer and Processing Service

Below is the generic format for the Helm installation command:

```text
helm install <RELEASE_NAME> <CHART_REFERENCE> --namespace <NAMESPACE>
```

The variables specified in the command are as follows:

- `<CHART_REFERENCE>`: A path to a packaged chart, a path to an unpacked chart
  directory or a URL.

- `<RELEASE_NAME>`: String value, a name to identify and manage your Helm chart.

- `<NAMESPACE>`: String value, a name to be used dedicated by the user for
  deploying own Helm charts.

To install the 4G PM Event File Transfer and Processing service on the Kubernetes cluster by using the
helm installation command, specifying the name of the Input Topic to subscribe to:

* Example deployment with default parameters:
  ```text
  helm install eric-oss-4gpmevent-filetrans-proc https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-drop-helm/eric-oss-4gpmevent-filetrans-proc/<LATEST_RELEASE> --namespace <NAMESPACE> --set global.registry.username=<USER_NAME> --set global.registry.password=<USER_PASSWORD> --set global.pullSecret=<PULL_SECRET> -- spring.kafka.topics.input.name=<INPUT_TOPIC_NAME>
  ```

* Example deployment with optional parameters:
  ```text
  helm install eric-oss-4gpmevent-filetrans-proc https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-drop-helm/eric-oss-4gpmevent-filetrans-proc/<LATEST_RELEASE> --namespace <NAMESPACE> --set global.registry.username=<USER_NAME> --set global.registry.password=<USER_PASSWORD> --set global.pullSecret=<PULL_SECRET> --set spring.kafka.topics.input.name=<INPUT_TOPIC_NAME> --set resources.eric-oss-4gpmevent-filetrans-proc.limits.memory=384Mi 
  ```

##### Verify the 4G PM Event File Transfer and Processing Service Availability

To verify whether the deployment is successful, do as follows:

*1. Check if the chart is installed with the provided release name and
in related namespace by using the following command:*

```text
$helm ls <namespace>
```

*Chart status should be reported as "DEPLOYED".*

*2. Verify the status of the deployed Helm chart.*

```text
$helm status <release_name>
```

Expected output should be:

```
NAME: eric-oss-4gpmevent-filetrans-proc-release
LAST DEPLOYED: Thu Oct 30 09:07:38 2023
NAMESPACE: <NAMES_SPACE>
STATUS: deployed
REVISION: 1
NOTES:
====
```

*Chart status should be reported as "DEPLOYED". All Pods status should be
reported as "Running" and number of Deployment Available should be the
same as the replica count.*

*3. Verify that the pods are running
by getting the status for your pods.*

```text
$kubectl get pods --namespace=<namespace> -L role
```

*For example:*

```text
$helm ls example
$helm status examplerelease
$kubectl get pods --namespace=example -L role
```

*All pods status should be "Running". All containers in any pod should
be reported as "Ready". There is one POD marked as the master role and
the other PODs are marked as the replica role.*>

### Configuration Parameters


Kafka Optional Parameters, for further reading see [Kafka Configuration][kfc]:

| Variable Name                                         | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | Default                                                     |
|-------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| spring.kafka.admin.retry                              | Maximum number of reattempts for the kafka admin bean. Used to create and check the existence of topics on the Message Bus KF at deployment time.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | 2147483647                                                  |
| spring.kafka.admin.retryBackOffMs                     | The back off time between reattempts to perform operations on the kafka cluster.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | 100 (ms)                                                    |
| spring.kafka.admin.reconnectBackOffMs                 | The base amount of time to wait before attempting to reconnect to a given host. This avoids repeatedly connecting to a host in a tight loop. This backoff applies to all connection attempts by the client to a broker.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | 50 (ms)                                                     |
| spring.kafka.admin.reconnectBackOffMaxMs              | The maximum amount of time in milliseconds to wait when reconnecting to a broker that has repeatedly failed to connect. If provided, the backoff per host will increase exponentially for each consecutive connection failure, up to this maximum. After calculating the backoff increase, 20% random jitter is added to avoid connection storms.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | 30000 (ms)                                                  |
| spring.kafka.admin.requestTimeoutMs                   | The configuration controls the maximum amount of time the client will wait for the response of a request. If the response is not received before the timeout elapses the client will resend the request if necessary or fail the request if retries are exhausted.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | 30000 (ms)                                                  |
| spring.kafka.producer.retryBackOffMs                  | The amount of time to wait before attempting to retry a failed request to a given topic partition. This avoids repeatedly sending requests in a tight loop under some failure scenarios.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | 100 (ms)                                                    |
| spring.kafka.producer.reconnectBackOffMs              | The base amount of time to wait before attempting to reconnect to a given host. This avoids repeatedly connecting to a host in a tight loop. This backoff applies to all connection attempts by the client to a broker.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | 50 (ms)                                                     |
| spring.kafka.producer.reconnectBackOffMaxMs           | The maximum amount of time in milliseconds to wait when reconnecting to a broker that has repeatedly failed to connect. If provided, the backoff per host will increase exponentially for each consecutive connection failure, up to this maximum. After calculating the backoff increase, 20% random jitter is added to avoid connection storms.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | 30000 (ms)                                                  |
| spring.kafka.producer.requestTimeoutMs                | The maximum amount of time the client will wait for the response of a request. If the response is not received before the timeout elapses the client will resend the request if necessary or fail the request if retries are exhausted.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | 30000 (ms)                                                  |
| spring.kafka.consumer.autoOffsetReset                 | 3 possible values. Decides on whether to consume from the beginning of a topic partition or only consume new message when no initial offset is provided ("earliest" = beginning of topic, "latest" = end of topic, "none" = throw exception if no offset present for consumer group).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | earliest                                                    |
| spring.kafka.consumer.retryBackOffMs                  | The amount of time to wait before attempting to retry a failed request to a given topic partition. This avoids repeatedly sending requests in a tight loop under some failure scenarios.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | 100 (ms)                                                    |
| spring.kafka.consumer.reconnectBackOffMs              | The base amount of time to wait before attempting to reconnect to a given host. This avoids repeatedly connecting to a host in a tight loop. This backoff applies to all connection attempts by the client to a broker.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | 50 (ms)                                                     |
| spring.kafka.consumer.reconnectBackOffMaxMs           | The maximum amount of time in milliseconds to wait when reconnecting to a broker that has repeatedly failed to connect. If provided, the backoff per host will increase exponentially for each consecutive connection failure, up to this maximum. After calculating the backoff increase, 20% random jitter is added to avoid connection storms.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | 30000 (ms)                                                  |
| spring.kafka.consumer.requestTimeoutMs                | The configuration controls the maximum amount of time the client will wait for the response of a request. If the response is not received before the timeout elapses the client will resend the request if necessary or fail the request if retries are exhausted.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | 30000 (ms)                                                  |
| spring.kafka.topics.input.prefix                      | The prefix of the Input Topic, prepending the Ericsson Network Manager instance.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | file-notification-service--4g-event--                       |
| spring.kafka.topics.input.partitionAssignmentStrategy | A list of class names or class types, ordered by preference, of supported partition assignment strategies that the client will use to distribute partition ownership amongst consumer instances when group management is used. Guarantees an assignment that is maximally balanced while preserving as many existing partition assignments as possible with cooperative rebalancing.(Should this be configurable)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | org.apache.kafka.clients.consumer.CooperativeStickyAssignor |
| spring.kafka.topics.input.sessionTimeoutMs            | The timeout used to detect client failures when using Kafka's group management facility. The client sends periodic heartbeats to indicate its liveness to the broker. If no heartbeats are received by the broker before the expiration of this session timeout, then the broker will remove this client from the group and initiate a rebalance.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | 60000 (ms)                                                  |
| spring.kafka.topics.input.replicas                    | The number of replicas for each partition. With a replication factor of 3, each message on a partition will have 3 copies spread amongst the kafka brokers. This ensures data durability/availability if a broker goes down.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | 3                                                           |
| spring.kafka.topics.input.maxPollRecords              | The maximum number of records that a consumer/listener will fetch at a time. This is per consumer/listener.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | 20                                                          |
| spring.kafka.topics.input.maxPollIntervalMs           | The maximum delay between invocations of poll() when using consumer group management. This places an upper bound on the amount of time that the consumer can be idle before fetching more records. If poll() is not called before expiration of this timeout, then the consumer is considered failed and the group will rebalance in order to reassign the partitions to another member.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | 600000                                                      |
| spring.kafka.topics.output.name                       | The name of the Output Topic, prepending the Ericsson Network Manager instance.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | 4g-pm-event-file-transfer-and-processing                    |
| spring.kafka.topics.output.acks                       | acknowledgment that the producer gets from a Kafka broker to ensure that the message has been successfully committed to that broker. The config acks is the number of acknowledgments the producer needs to receive before considering a successful commit.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | all                                                         |
| spring.kafka.topics.output.compressionType            | The compression type for all data generated by the producer. The default is none (i.e. no compression). Valid values are none, gzip, snappy, lz4, or zstd. Compression is of full batches of data, so the efficacy of batching will also impact the compression ratio (more batching means better compression).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | lz4                                                         |
| spring.kafka.topics.output.partitions                 | The number of partitions to be created for the Output Topic. It is important to correctly provision this on deployment of the service. A topic cannot be changed to have less partitions than its current number. Increasing the number of partitions changes how messages are mapped to each topic.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | 3                                                           |
| spring.kafka.topics.output.replicas                   | The number of replicas for each partition. With a replication factor of 3, each message on a partition will have 3 copies spread amongst the kafka brokers. This ensures data durability/availability if a broker goes down.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | 3                                                           |
| spring.kafka.topics.output.batchSize                  | The producer will attempt to batch records together into fewer requests whenever multiple records are being sent to the same partition. This helps performance on both the client and the server. This configuration controls the default batch size in bytes. No attempt will be made to batch records larger than this size. Requests sent to brokers will contain multiple batches, one for each partition with data available to be sent. A small batch size will make batching less common and may reduce throughput (a batch size of zero will disable batching entirely). A very large batch size may use memory a bit more wastefully as we will always allocate a buffer of the specified batch size in anticipation of additional records. Note: If linger is set to zero, this parameter is ignored and records will be sent immediately. This parameter only affects the sending of messages once linger is set to above zero. Batches will be sent in one of two ways, first the batch is full, or when the amount of time defined by linger has passed. | 500000 (500KB)                                              |
| spring.kafka.topics.output.bufferMemory               | The total bytes of memory the producer can use to buffer records waiting to be sent to the server. If records are sent faster than they can be delivered to the server the producer will block for max.block.ms after which it will throw an exception. This setting should correspond roughly to the total memory the producer will use, but is not a hard bound since not all memory the producer uses is used for buffering. Some additional memory will be used for compression (if compression is enabled) as well as for maintaining in-flight requests. The buffer will contain multiple batches. By default its 32MB, but this should directly scale with the number of Input Topic partitions and Concurrency setting above.                                                                                                                                                                                                                                                                                                                                 | 32000000 (32MB)                                             |
| spring.kafka.topics.output.maxRequestSize             | The maximum size of a request in bytes. This setting will limit the number of record batches the producer will send in a single request to avoid sending huge requests. This is also effectively a cap on the maximum uncompressed record batch size. Note that the server has its own cap on the record batch size (after compression if compression is enabled) which may be different from this.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | 50000000 (50MB)                                             |
| spring.kafka.topics.output.linger                     | This introduces an artificial delay between sending messages to ensure that messages can be batched together to provide better throughput. Setting this to 0 will prevent batching from happening and may reduce throughput for the service. Increasing it will introduce higher latency but may improve batching and help with I/O constraints.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | 100 (100ms)                                                 |
| spring.kafka.topics.output.retentionPeriodMS          | Duration that the Kafka message bus will retain Output Topic messages before deletion.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | 7200000 (2 hours)                                           |

4G PM Event File Transfer and Processing service Liveness and Readiness Probes:

| Variable Name                                                          | Description                                                                                                                                                                                                                                       | Default |
|------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| probes.eric-oss-4gpmevent-filetrans-proc.livenessProbe.failureThreshold     | When a probe fails, Kubernetes will try failureThreshold times before giving up. Giving up in case of liveness probe means restarting the container. In case of readiness probe the Pod will be marked Unready. Defaults to 3. Minimum value is 1 | 5       |
| probes.eric-oss-4gpmevent-filetrans-proc.livenessProbe.initialDelaySeconds  | Number of seconds after the container has started before liveness or readiness probes are initiated. Defaults to 0 seconds. Minimum value is 0.                                                                                                   | 90      |
| probes.eric-oss-4gpmevent-filetrans-proc.livenessProbe.periodSeconds        | How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1.                                                                                                                                                           | 10      |
| probes.eric-oss-4gpmevent-filetrans-proc.livenessProbe.timeoutSeconds       | Number of seconds after which the probe times out. Defaults to 1 second. Minimum value is 1.                                                                                                                                                      | 10      |
| probes.eric-oss-4gpmevent-filetrans-proc.readinessProbe.failureThreshold    | When a probe fails, Kubernetes will try failureThreshold times before giving up. Giving up in case of liveness probe means restarting the container. In case of readiness probe the Pod will be marked Unready. Defaults to 3. Minimum value is 1 | 5       |
| probes.eric-oss-4gpmevent-filetrans-proc.readinessProbe.initialDelaySeconds | Number of seconds after the container has started before liveness or readiness probes are initiated. Defaults to 0 seconds. Minimum value is 0.                                                                                                   | 90      |
| probes.eric-oss-4gpmevent-filetrans-proc.readinessProbe.periodSeconds       | How often (in seconds) to perform the probe. Default to 10 seconds. Minimum value is 1.                                                                                                                                                           | 10      |
| probes.eric-oss-4gpmevent-filetrans-proc.readinessProbe.timeoutSeconds      | Number of seconds after which the probe times out. Defaults to 1 second. Minimum value is 1.                                                                                                                                                      | 10      |

4G PM Event File Transfer and Processing service Pod Resources:

| Variable Name                                                                 | Description                                                                                                                                       | Default |
|-------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| resources.eric-oss-4gpmevent-filetrans-proc.requests.memory                        | The amount of memory the container is guaranteed.                                                                                                 | 256Mi     |
| resources.eric-oss-4gpmevent-filetrans-proc.requests.cpu                           | The amount of CPU the container is guaranteed.                                                                                                    | 125m   |
| resources.eric-oss-4gpmevent-filetrans-proc.requests.ephemeral-storage             | The amount of ephemeral storage the container is guaranteed.                                                                                      | 4G      |
| resources.eric-oss-4gpmevent-filetrans-proc.limits.memory                          | The maximum amount of memory the container is allowed before it is restricted.                                                                    | 2Gi   |
| resources.eric-oss-4gpmevent-filetrans-proc.limits.cpu                             | The maximum amount of CPU the container is allowed before it is restricted.                                                                       | 500m   |
| resources.eric-oss-4gpmevent-filetrans-proc.limits.ephemeral-storage               | The maximum amount of ephemeral storage the container is allowed before it is restricted.                                                         | 15G     |

4G PM Event File Transfer and Processing service Network Policy:

| Variable Name                              | Description                                                                                                            | Default                                                                                                          |
|--------------------------------------------|------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| networkPolicy.enabled                      | Enable the use of Network Policies, depends on `global.networkPolicy.enabled` to be set to `true` too.                 | true                                                                                                             |
| networkPolicy.podSelector                  | List of labels and the associated values for ingress and egress (incoming and outgoing connections respectively) rules |                                                                                                                  |
| networkPolicy.podSelector.label            | Kubernetes label for an entity for which the rules will apply                                                          | app.kubernetes.io/name                                                                                           |
| networkPolicy.podSelector.ingress          | List of values for the associated label, ingress port and its protocol for which the ingress rule applies              |                                                                                                                  |
| networkPolicy.podSelector.ingress.value    | Value for the associated label                                                                                         | eric-pm-server                                                                                                   |
| networkPolicy.podSelector.ingress.port     | Ingress port number                                                                                                    | 33631                                                                                                            |
| networkPolicy.podSelector.ingress.protocol | Allowed protocol for the port, possible options are TCP and UDP                                                        | TCP                                                                                                              |

Miscellaneous Optional Parameters:

| Variable Name                                              | Description                                                                                                                                                                                                                                                                                                                                                 | Default                        |
|------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------|
| security.tls.enabled                                       | Enable or disable TLS.                                                                                                                                                                                                                                                                                                                                      | false (Currently not enabled)  |
| replicaCount                                               | Number of Service pod Replicas to create.                                                                                                                                                                                                                                                                                                                   | 1                              |
| podDisruptionBudget.minAvailable                           | Minimum number/percentage of Pods that must remain available during the planned disruption. Value can be expressed as an integer or as a percentage. When specified as a percentage of the Pods, if it does not map to an exact number, Kubernetes rounds up to the nearest integer.minAvailable: 2 example value for 2 pods defined as integer.            | 40%                            |
| updateStrategy.type                                        | How to apply updates to the Services deployment to replicas.                                                                                                                                                                                                                                                                                                | RollingUpdate                  |
| updateStrategy.rollingUpdate.maxSurge                      | How many additional replicas can exist while the old replica is still available.                                                                                                                                                                                                                                                                            | 1                              |
| updateStrategy.rollingUpdate.maxUnavailable                | How many replicas can be unavailable during the update of the deployment.                                                                                                                                                                                                                                                                                   | 0                              |
| labels                                                     | Empty labels list for application deployment engineer to add more labels.                                                                                                                                                                                                                                                                                   | {}                             |
| imagePullSecrets                                           | Set the credentials to be used when pulling images from the Docker registry.                                                                                                                                                                                                                                                                                | []                             |
| nameOverride                                               | Overrides the chart name.                                                                                                                                                                                                                                                                                                                                   | ""                             |
| fullnameOverride                                           | Completely overrides the name of the deployment.                                                                                                                                                                                                                                                                                                            | ""                             |
| serviceAccount.create                                      | Specifies whether a service account should be created.                                                                                                                                                                                                                                                                                                      | true                           |
| serviceAccount.name                                        | The name of the service account to use. If not set and create is true, a name is generated using the fullname template.                                                                                                                                                                                                                                     | ""                             |
| podAnnotations                                             | Annotation to be set to pods.                                                                                                                                                                                                                                                                                                                               | {}                             | 
| annotations                                                | Annotation to be set to deployment.                                                                                                                                                                                                                                                                                                                         | {}                             |
| service.type                                               | Specify whether node is ClusterIP or NodePort.                                                                                                                                                                                                                                                                                                              | ClusterIP                      |
| service.port                                               | The port the service is exposed on.                                                                                                                                                                                                                                                                                                                         | 8080                           |
| service.endpoints.chassisapi.tls.enforced                  | Defines whether TLS support should be enforced or HTTP support should be also allowed. Specify either "optional" to allow http also or "required" for TLS only. Only valid if global.security.tls.enabled is set to true.                                                                                                                                   | required                       |
| service.endpoints.chassisapi.tls.verifyClientCertificate   | Defines whether client certificate validation should be enforced or not. Specify either "optional" or "required". Only valid if global.security.tls.enabled is set to true.                                                                                                                                                                                 | required                       |
| ingress.enabled                                            | Enabled ingress feature of exposing REST API outside cluster.                                                                                                                                                                                                                                                                                               | false                          |
| ingress.ingressClass                                       | Ingress class name indicating which ingress controller instance is consuming the ingress resource.                                                                                                                                                                                                                                                          | OAM-IngressClass               |
| ingress.hosts.host                                         | Manages external access to the services in a cluster, typically HTTP.                                                                                                                                                                                                                                                                                       | chart-example.local            |
| ingress.hosts.paths                                        | Manages external access to the services in a cluster, typically HTTP.                                                                                                                                                                                                                                                                                       | [/]                            |
| eric-pm-server.rbac.appMonitoring.enabled                  | Enables RBAC for single Application Monitoring.                                                                                                                                                                                                                                                                                                             | true                           |
| prometheus.path                                            | Sets path to Prometheus endpoint.                                                                                                                                                                                                                                                                                                                           | /actuator/prometheus           |
| prometheus.scrape                                          | Enable or disable Prometheus, either true or false.                                                                                                                                                                                                                                                                                                         | true                           |
| terminationGracePeriodSeconds                              | Sets the amount of time a pod is given to terminate gracefully before it is terminated by force.                                                                                                                                                                                                                                                            | 30                             |
| podPriority.eric-oss-4gpmevent-filetrans-proc.priorityClassName | Sets the pod's priority, e.g. high-priority.                                                                                                                                                                                                                                                                                                           | ""                             |
| autoScaling.enabled                                        | Automatically scales the number of pods based on observed CPU utilization.                                                                                                                                                                                                                                                                                  | false                          |
| autoScaling.minReplicas                                    | Minimum number of replicas to create.                                                                                                                                                                                                                                                                                                                       | 1                              |
| autoScaling.maxReplicas                                    | Maximum number of replicas to create.                                                                                                                                                                                                                                                                                                                       | 100                            |
| autoScaling.targetCPUUtilizationPercentage                 | The threshold of CPU utilization presentation. It will scale out the pod and create a new replica when the threshold is reached.                                                                                                                                                                                                                            | 80                             |
| nodeSelector                                               | Used to constrain a Pod so that it can only run on particular set of Nodes.                                                                                                                                                                                                                                                                                 | {}                             |
| topologySpreadConstraints                                  | The topologySpreadConstraints specifies how to spread pods among the given topology. Valid fields are maxSkew { range=1..max }, topologyKey and whenUnsatisfiable { choice="DoNotSchedule, ScheduleAnyway" }.                                                                                                                                               | []                             |
| affinity.podAntiAffinity                                   | Pod anti-affinity guarantees the distribution of the pods across different nodes in the Kubernetes cluster. Soft anti-affinity is best-effort and might lead to the state that a node runs two replicas of the Service instead of distributing it across different nodes.                                                                                   | soft                           |
| affinity.topologyKey                                       | Pod anti-affinity topology key defines which topology domain to consider when evaluating the anti-affinity rule. Examples are the node's region, zone or hostname, among others.                                                                                                                                                                            | kubernetes.io/hostname         |
| appArmorProfile.type                                       | Configuration of AppArmor profile type. Possible values: unconfined-Indicates that there is no profile loaded. runtime/default-Applies the default profile of the container engine. localhost-Applies a specific profile loaded on the host. AppArmor support is dependent on host node capabilities, it is currently not set due to AWS not supporting it. | ""                             |
| appArmorProfile.localhostProfile                           | Refers to the AppArmor profile loaded on the node.                                                                                                                                                                                                                                                                                                          |                                |
| seccompProfile.type                                        | Seccomp can be used to sandbox the privileges of a process, restricting the calls it is able to make from userspace into the kernel. unconfined-Indicates that there is no profile loaded. runtime/default-Applies the default profile of the container engine. localhost-Applies a specific profile loaded on the host.                                    | ""                             |
| seccompProfile.localhostProfile                            | Refers to the Seccomp profile loaded on the node.                                                                                                                                                                                                                                                                                                           |                                |
| httpTimeoutInSeconds                                       | HTTP Connection Timeout.                                                                                                                                                                                                                                                                                                                                    | 60                             |

##### Interface Specific Optional Parameters

Data Management and Movement Optional Parameters:

| Variable Name                           | Description                                                                                                          | Default                             |
|-----------------------------------------|----------------------------------------------------------------------------------------------------------------------|-------------------------------------|
| dmm.dataCatalog.baseUrl                 | Sets base URL for Data Catalog, used to form URL for REST operations to Data Catalog.                                | http://eric-oss-data-catalog:       |
| dmm.dataCatalog.basePort                | Sets base port for Data Catalog, used to form URL for REST operations to Data Catalog.                               | 9590                                |
| dmm.dataCatalog.dataServiceUri          | Sets URI used to form url for deleting data service instance entity stored on Data Catalog upon de-registration      | /catalog/v1/data-service            |
| dmm.dataCatalog.notificationTopicUri    | Sets URI used to form URL for getting notification topics from the notification topic entity stored on Data Catalog. | /catalog/v1/notification-topic      |
| dmm.dataCatalog.dataCategory            | Sets the data category.                                                                                              | PM_EVENTS                           |
| dmm.dataCatalog.messageBusUri           | Sets URI used to form URL for getting message bus from the Messsage Bus entity stored on Data Catalog.               | /catalog/v1/message-bus             |
| dmm.dataCatalog.dataProviderTypeUri     | Sets URI used to form URL for performing DataProvidertype operations on Data Catalog.                                | /catalog/v1/data-provider-type      |
| dmm.dataCatalog.dataSpaceUri            | Sets URI used to form URL for registering and getting DataSpace to or from Data Catalog.                             | /catalog/v1/data-space              |
| dmm.dataCatalog.dataCollectorUri        | Sets URI used to form URL for registering a DataCollector on Data Catalog.                                           | /catalog/v1/data-collector          |
| dmm.dataCatalog.messageSchemaUriV1      | Sets URI used to form URL for registering and getting message schemas (Output Topic) to and from Data Catalog.       | /catalog/v1/message-schema          |
| dmm.dataCatalog.messageSchemaUriV2      | Sets URI used to form URL for getting message schemas (Input Topic) from Data Catalog                                | /catalog/v2/message-schema          |
| dmm.dataCatalog.messageDataTopicUri     | Sets URI used to form URL for registering and getting message data topics to and from Data Catalog.                  | /catalog/v1/message-data-topic      |
| dmm.dataCatalog.messageStatusTopicUri   | Sets URI used to form URL for registering and getting message status topics to and from Data Catalog.                | /catalog/v1/message-status-topic    |
| dmm.dataCatalog.messageBusName          | Sets the name of the Message Bus to be retrieved from Data Catalog.                                                  | mb2                                 |
| dmm.dataCatalog.messageBusNamespace     | Sets the namespace where the Message Bus to be retrieved is deployed.                                                | nameSpace                           |
| dmm.dataCatalog.dataProviderTypeId      | Sets data provider type ID.                                                                                          | V2                                  |
| dmm.dataCatalog.dataProviderTypeVersion | Sets data provider type version.                                                                                     | v5                                  |
| dmm.dataCatalog.dataSpace               | Sets the data space.                                                                                                 | 4G                                  |
| dmm.dataCatalog.dataCollectorName       | Sets the data collector name.                                                                                        | enm                                 |

Connected Systems Optional Parameters:

| Variable Name             | Description                              | Default                              |
|---------------------------|------------------------------------------|--------------------------------------|
| connected.systems.baseUrl | Sets the base URL for Connected Systems. | http://eric-eo-subsystem-management  |
| connected.systems.uri     | Sets the URI for Connected Systems.      | /subsystem-manager/v1/subsystems/    |
| connected.systems.port    | Sets the port for Connected Systems.     | 80                                   |


SFTP Resilience Optional Parameters:

| Variable Name                                      | Description                                                                                                                                                                                                                  | Default |
|----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| eventFileDownload.numberOfEventFileDownloadRetries | Max number of times to retry downloading event file from Ericsson Network Manager Scripting VM.                                                                                                                              | 3       |
| eventFileDownload.sftpConnectionTimeoutMs          | The amount of time to wait between retries in connecting to the Ericsson Network Manager Scripting VM.                                                                                                                       | 15000   |
| eventFileDownload.sftpSessionTimeoutMs             | The timeout value used for socket connect operations. If connecting to the server takes longer than this value, the connection is broken. The timeout is specified in seconds and a value of zero means that it is disabled. | 15000   |

### Endpoints Exposed

The below table contains all available endpoints for the microservice.

| Endpoint                               | Description                                                                                                    | Media Type                                                                                                   |
|----------------------------------------|----------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| /actuator                              | Returns a list of actuator links                                                                               | application/vnd.spring-boot.actuator.v3+json, application/vnd.spring-boot.actuator.v2+json, application/json |
| /actuator/metrics                      | Returns a list of all metric names                                                                             | application/vnd.spring-boot.actuator.v3+json, application/vnd.spring-boot.actuator.v2+json, application/json |
| /actuator/metrics/{requiredMetricName} | Returns value of the metric specified eg eric-oss-4gpmevent-filetrans-proc:event.files.processed               | application/vnd.spring-boot.actuator.v3+json, application/vnd.spring-boot.actuator.v2+json, application/json |
| /actuator/health                       | Returns group options of liveness and readiness, and status of either up or down                               | application/vnd.spring-boot.actuator.v3+json, application/vnd.spring-boot.actuator.v2+json, application/json |
| /actuator/info                         | Returns description, legal, name and version of the microservice                                               | application/vnd.spring-boot.actuator.v3+json, application/vnd.spring-boot.actuator.v2+json, application/json |
| /actuator/prometheus                   | Returns a Prometheus scrape with the appropriate format                                                        | text/plain;version=0.0.4;charset=utf-8mediaType,   application/openmetrics-text;version=1.0.0;charset=utf-8  |

### Service Dimensioning

The service provides by default resource request values and resource limit
values as part of the Helm chart. These values correspond to a default size for
deployment of an instance. This chapter gives guidance in how to do service
dimensioning and how to change the default values when needed.

#### Override Default Dimensioning Configuration

If other values than the default resource request and default resource limit
values are preferred, they must be overridden at deployment time.

Here is an example of the `helm install` command where resource requests and
resource limits are set:

```text
helm install eric-oss-4gpmevent-filetrans-proc https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-drop-helm/eric-oss-4gpmevent-filetrans-proc/<LATEST_RELEASE> --set imageCredentials.pullSecret=armdocker --namespace test-deployment-namespace --set resources.eric-oss-4gpmevent-filetrans-proc.limits.cpu=100m --set resources.eric-oss-4gpmevent-filetrans-proc.limits.memory=384Mi
```

#### Use Minimum Configuration per Service Instance

This chapter specifies the minimum recommended configuration per service instance:

| Resource Type (Kubernetes Service)               | Resource Request Memory | Resource Limit Memory | Resource Request CPU | Resource Limit CPU | Resource Request Disk | Resource Limit Disk |
|--------------------------------------------------|-------------------------|-----------------------|----------------------|--------------------|-----------------------|---------------------|
| 4G PM Event File Transfer and Processing Service | 256Mi                   | 2Gi                   | 125m                 | 500m               | 4G                    | 15G                 |

To use minimum configuration, override the default values for resource requests
and resource limits in the Helm chart at deployment time.

#### Use Maximum (Default) Configuration per Service Instance

The maximum recommended configuration per instance is provided as default in the
Helm chart. Both Resource Request values and Resource Limit values are included
in the Helm charts.

### Hardening

#### Hardening during product development

* The service is built on a minimalistic container image with small footprints. Only the required libraries are
  included.
* The service utilizes a container optimized operating system (Common Base OS) and latest security patches are applied.
* The containers go through vulnerability scanning.
* The service is configured to the strict minimum of services and ports to minimize the attack surface.
* Seccomp is enabled by default in values.yaml (Please
  see [Configuration Parameters](#configuration-parameters) for further details).
* The service can operate in service mesh for mTLS HTTPS support.

#### Hardening during service delivery

* Seccomp is enabled by default in values.yaml (Please
  see [Configuration Parameters](#configuration-parameters) for further details). Alternative profiles may be set at
  deployment by overriding the default value by using the -- set argument.
* The service can operate in service mesh for mTLS HTTPS support.

#### Automated procedures and hardening scripts

The 4G PM Event File Transfer and Processing service does not include any automated procedures or scripts for hardening.

#### References

See the following documents for more details:

* [Hardening Guideline Instruction](https://erilink.ericsson.se/eridoc/erl/objectId/09004cff8b35654f?docno=LME-16:002235Uen&action=approved&format=msw12)
* [Hardening Guideline Template](https://erilink.ericsson.se/eridoc/erl/objectId/09004cff8b355119?docno=LME-16:002234Uen&action=approved&format=msw12)
* [Recommended Hardening Activities](https://erilink.ericsson.se/eridoc/erl/objectId/09004cffc724ed0d?docno=GFTL-21:000631Uen&action=approved&format=msw12)
* [Kubernetes Security Design Rules](https://confluence.lmera.ericsson.se/pages/viewpage.action?spaceKey=AA&title=Kubernetes+Security+Design+Rules)

### Upgrade Procedures

> **_Note:_** If any chart value is customized at upgrade time through the
> "--set" option of the "helm upgrade" command, all other previously customized
> values will be replaced with the ones included in the new version of the chart.
> To make sure that any customized values are carried forward as part of the
> upgrade, consider keeping a versioned list of such values. That list could be
> provided as input to the upgrade command in order to be able to use the "--set"
> option without side effects.

> **_Note:_** If the user wishes to change the naming convention for the Input Topic name they must also change the
> value of `spring.kafka.topics.input.prefix` to align with the new convention.

## Security Guidelines

### Operative Tasks

This service does not include any
operative tasks.

#### External Ports

None

#### Internal Ports

| Service or Interface name             | Protocol | IP Address Type | Port    | Transport Protocol | IP Version |
|---------------------------------------|----------|-----------------|---------|--------------------|------------|
| Liveness/Readiness                    | HTTP     | OAM IP          | 33631   | TCP                | IPv4       |
| Metrics                               | HTTP     | OAM IP          | 33631   | TCP                | IPv4       |
| Message Bus KF                        | Binary   | OAM IP          | 9092    | TCP                | IPv4       |
| Data Catalog                          | HTTP     | OAM IP          | 9590    | TCP                | IPv4       |
| Connected Systems                     | HTTP     | OAM IP          | Runtime | TCP                | IPv4       |
| Ericsson Network Manager Scripting VM | SFTP     | OAM IP          | Runtime | TCP                | IPv4       |

### Certificates

No certificates are currently used.

### Security Events That Can Be Logged

No security events logged by the service.

## Privacy User Guidelines

The service processes personal data contained in PM Events and has a Privacy Technical Index. There is a
plan to remove personal data from PM Events, bringing the PTI score to 0 at a later date.

## Operation and Maintenance

### Performance Management

#### Default Metrics

Following table lists for all default metrics.

| Metrics Name                                                   | Metric Name in the PM Server                                                |
|----------------------------------------------------------------|-----------------------------------------------------------------------------|                                                                                                                                
|eric-oss-4gpmevent-filetrans-proc:event.files.processed         | eric_oss_4gpmevent_filetrans_proc:event_files_processed_total               |
|eric-oss-4gpmevent-filetrans-proc:num.failed.file.transfer      | eric_oss_4gpmevent_filetrans_proc:num_failed_file_transfer_total            |
|eric-oss-4gpmevent-filetrans-procc:num.successful.file.transfer | eric_oss_4gpmevent_filetrans_proc:num_successful_file_transfer_total        |
|eric-oss-4gpmevent-filetrans-proc:processed.file.data.volume    | eric_oss_4gpmevent_filetrans_proc:processed_file_data_volume                |
|eric-oss-4gpmevent-filetrans-proc:processed.files.time.total    | eric_oss_4gpmevent_filetrans_proc:processed_files__time_total_seconds_count |
|eric-oss-4gpmevent-filetrans-proc:transferred.file.data.volume  | eric_oss_4gpmevent_filetrans_proc:transferred_file_data_volume              |
|eric-oss-4gpmevent-filetrans-proc:files.parsed                  | eric_oss_4gpmevent_filetrans_proc:files_parsed_total                        |
|eric-oss-4gpmevent-filetrans-proc:files.failed.to.parse         | eric_oss_4gpmevent_filetrans_proc:files_failed_to_parse_total               |

### Backup and Restore

The service does not support any kind of backup and restore operation. This is due to the service being stateless and
therefore, has not state to restore.

## Troubleshooting

This section describes the troubleshooting functions and procedures for
the 4G PM Event File Transfer and Processing service. It provides the
following information:

- Simple verification and possible recovery.

- The required information when reporting a bug or writing a support case,
  including all files and system logs that are needed.

- How to retrieve the above information from the system.

### Prerequisites

- `kubectl` CLI tool properly configured

### Installation

If Installation fails, refer [Recovery Procedure](#recovery-procedure) to recover
and [Data Collection](#data-collection) to collect logs.

### Deletion of Release

If Installation fails, refer [Recovery Procedure](#recovery-procedure) to recover
and [Data Collection](#data-collection) to collect logs.

To delete the release the service can be uninstalled by helm uninstall command:

```
  helm uninstall <RELEASE_NAME> --namespace <NAMESPACE>
```

### Health Checks

Deployment or upgrade failures with CrashLoopBackOff status:

The 4G PM Event File Transfer and Processing service has a health check that validates some service configuration
parameters. This health check uses the Kubernetes Readiness and Liveness probe. If the parameters are considered
invalid, the health check will fail. This will prevent the Diagnostic Data Collector from successfully starting, and the
pod will be in status CrashLoopBackOff.

To check if the pod is in status CrashLoopBackOff you can give the following command:

```
$ kubectl get pod --namespace=<pod's namespace>
NAME                                                   READY     STATUS             RESTARTS  AGE
eric-oss-4gpmevent-filetrans-proc-6978479468-hjg2k          0/1       CrashLoopBackOff   1         14s
```

### Log Categories

The 4G PM Event Transfer and Processing service does not use any log categories.

### Data Collection

- The logs are collected from each pod using the command:

```text
kubectl exec <pod name> --namespace=<pod's namespace> > <log file name>.txt
```

- The detailed information about the pod is collected using the command:

```text
kubectl describe pod <pod name> --namespace=<pod's namespace>
kubectl exec <pod-name> --namespace=<pod's namespace> env
```

### Bug Reporting and Additional Support

Issues can be handled in different ways, as listed below:

- For questions, support or hot requesting, see
  Additional Support.

- For reporting of faults, see [Bug Reporting](#bug-reporting).

#### Additional Support

If there are 4G PM Event File Transfer and Processing service support issues, use the [JIRA][jr].

#### Bug Reporting

If there is a suspected fault, report a bug. The bug report must
contain specific 4G PM Event File Transfer and Processing service information and all
applicable troubleshooting information highlighted in the
[Troubleshooting](#troubleshooting), and [Data Collection](#data-collection).

Indicate if the suspected fault can be resolved by restarting the pod, see [Restarting the Pod](#restarting-the-pod).

*When reporting a bug for the 4G PM Event File Transfer and Processing service, specify
the following in the JIRA issue:*

[JIRA][jr]

- *Issue type: Bug*
- *Component: 4G PM Event File Transfer and Processing*
- *Reported from: the level at which the issue was originally detected
  (ADP Program, Application, Customer, etc.)*
- *Application: identity of the application where the issue was observed
  (if applicable)*
- *Business Impact: the business impact caused by the issue for the affected
  users*>

### Recovery Procedure

This section describes how to recover the service in case of malfunction.

The 4G PM Event File Transfer and Processing service is stateless and does not require any recovery procedure.
The 4G PM Event File Transfer and Processing service Kubernetes pod can be restarted if users need to resolve the
problem. To restart a pod, see [Restarting the Pod](#restarting-the-pod).

#### Restarting the Pod

If a pod of the 4G PM Event File Transfer and Processing service is in abnormal status, users can restart the pod to
resolve the relevant problems.

```bash
kubectl delete pod <podname>
```

### Known Issues

The 4G PM Event File Transfer and Processing service does not contain any known issues to date.

## References

[Service Mesh][sm]

[Message Bus KF microservice][mbkf]

[Performance Management Server][pms]

[Data Catalog][dc]

[Connected Systems][cs]

[ENM File Notification][enm]

[Kubernetes Rolling Update][kru]

[Data management and Movement][dmm]

[Kafka Configuration][kfc]

[JIRA][jr]

[sm]: <https://adp.ericsson.se/marketplace/servicemesh-controller>

[mbkf]: <https://adp.ericsson.se/marketplace/message-bus-kf>

[pms]: <https://adp.ericsson.se/marketplace/pm-server>

[dc]: <https://adp.ericsson.se/marketplace/data-catalog>

[cs]: <https://adp.ericsson.se/marketplace/connected-systems>

[enm]: <https://adp.ericsson.se/marketplace/enm-file-notification>

[kru]: <https://kubernetes.io/docs/tutorials/kubernetes-basics/update/update-intro/>

[dmm]: <https://confluence-oss.seli.wh.rnd.internal.ericsson.com/pages/viewpage.action?spaceKey=IDUN&title=DMM+Installation+Instructions>

[kfc]: <https://kafka.apache.org/documentation/#configuration>

[jr]: <https://jira-oss.seli.wh.rnd.internal.ericsson.com/secure/RapidBoard.jspa?rapidView=8360&projectKey=IDUN&view=planning.nodetail&selectedIssue=IDUN-81746&issueLimit=100>