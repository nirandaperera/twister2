package(default_visibility = ["//visibility:public"])

load("//tools:maven.bzl", "t2_java_lib")

common_deps_files = [
    "@org_yaml_snakeyaml//jar",
    "//twister2/resource-scheduler/src/java:resource-scheduler-java",
    "//twister2/comms/src/java:comms-java",
    "//twister2/common/src/java:common-java",
    "//twister2/connectors/src/java:connector-java",
    "//twister2/task/src/main/java:task-java",
    "//twister2/taskscheduler/src/java:taskscheduler-java",
    "//twister2/proto:proto-java",
    "//twister2/api/src/java:api-java",
    "//twister2/data/src/main/java:data-java",
    "//twister2/executor/src/java:executor-java",
    "//twister2/master/src/java:master-java",
    "//twister2/checkpointing/src/java:checkpointing-java",
    "//third_party:ompi_javabinding_java",
    "@com_google_guava_guava//jar",
    "@org_lmdbjava_lmdbjava//jar",
    "@org_apache_commons_commons_lang3//jar",
    "@commons_cli_commons_cli//jar",
    "@com_google_protobuf//:protobuf_java",
    "@commons_io_commons_io//jar",
    "@maven//:org_apache_curator_curator_framework",
    "@maven//:com_opencsv_opencsv",
    "@org_apache_hadoop_hadoop_hdfs//jar",
    "@org_apache_hadoop_hadoop_common//jar",
    "@org_apache_hadoop_hadoop_annotations//jar",
    "@org_apache_hadoop_hadoop_auth//jar",
    "@org_apache_hadoop_hadoop_mapreduce_client_core//jar",
    "@com_google_code_findbugs_jsr305//jar",
    "@com_fasterxml_woodstox_woodstox_core//jar",
    "@org_codehaus_woodstox_stax2_api//jar",
    "@commons_collections_commons_collections//jar",
    "@commons_configuration_commons_configuration//jar",
    "@log4j_log4j//jar",
    "@org_apache_htrace_htrace_core4//jar",
    "@org_apache_hadoop_hadoop_hdfs_client//jar",
    "@com_fasterxml_jackson_core_jackson_databind//:com_fasterxml_jackson_core_jackson_databind",
    "//third_party:harp_collective",
    "//twister2/compatibility/storm:twister2-storm",
    "//twister2/compatibility/beam:twister2-beam",
    "//twister2/compatibility/harp:twister2-harp",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/compute:task-api-java",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/config:config-api-java",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/checkpointing:checkpointing-api-java",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/comms:comms-api-java",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/data:data-api-java",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/dataset:dataset-api-java",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/exceptions:exceptions-java",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/resource:resource-api-java",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/scheduler:scheduler-api-java",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/tset:tset-api-java",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/driver:driver-api-java",
    "//twister2/proto/utils:proto-utils-java",
    "//twister2/tset/src/java:tset-java",
    "@org_apache_kafka_kafka_clients//:org_apache_kafka_kafka_clients",
]

t2_java_lib(
    name = "examples-java",
    srcs = glob(["**/*.java"]),
    artifact_name = "Twister2 Examples",
    deps = common_deps_files,
)
