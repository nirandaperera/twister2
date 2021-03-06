load("//tools/rules:java_tests.bzl", "java_tests", "java_tests_debug")
load("//tools/rules:twister2_client.bzl", "twister2_client_common_files")

java_library(
    name = "examples-tests",
    srcs = glob(["**/*.java"]),
    deps = [
        "//third_party:com_github_jnr_jffi_native",
        "//third_party/java:junit4",
        "//third_party/java:mockito",
        "//third_party/java:powermock",
        "//twister2/api/src/java:api-java",
        "//twister2/api/src/java/edu/iu/dsc/tws/api/comms:comms-api-java",
        "//twister2/api/src/java/edu/iu/dsc/tws/api/compute:task-api-java",
        "//twister2/api/src/java/edu/iu/dsc/tws/api/config:config-api-java",
        "//twister2/api/src/java/edu/iu/dsc/tws/api/data:data-api-java",
        "//twister2/common/src/java:common-java",
        "//twister2/comms/src/java:comms-java",
        "//twister2/data/src/main/java:data-java",
        "//twister2/examples/src/java:examples-java",
        "//twister2/proto:proto-java",
        "//twister2/resource-scheduler/src/java:resource-scheduler-java",
        "//twister2/task/src/main/java:task-java",
        "//twister2/taskscheduler/src/java:taskscheduler-java",
        "@com_esotericsoftware_kryo//jar",
        "@com_fasterxml_woodstox_woodstox_core//jar",
        "@com_github_jnr_jnr_constants//jar",
        "@com_github_jnr_jnr_ffi//jar",
        "@com_google_code_findbugs_jsr305//jar",
        "@com_google_guava_guava//jar",
        "@commons_collections_commons_collections//jar",
        "@commons_configuration_commons_configuration//jar",
        "@commons_io_commons_io//jar",
        "@commons_lang_commons_lang//jar",
        "@log4j_log4j//jar",
        "@maven//:com_opencsv_opencsv",
        "@org_apache_commons_commons_lang3//jar",
        "@org_apache_hadoop_hadoop_annotations//jar",
        "@org_apache_hadoop_hadoop_auth//jar",
        "@org_apache_hadoop_hadoop_common//jar",
        "@org_apache_hadoop_hadoop_hdfs//jar",
        "@org_apache_hadoop_hadoop_hdfs_client//jar",
        "@org_apache_hadoop_hadoop_mapreduce_client_core//jar",
        "@org_apache_htrace_htrace_core4//jar",
        "@org_codehaus_woodstox_stax2_api//jar",
        "@org_lmdbjava_lmdbjava//jar",
        "@org_lmdbjava_lmdbjava_native_linux_x86_64//jar",
        "@org_lmdbjava_lmdbjava_native_osx_x86_64//jar",
        "@org_lmdbjava_lmdbjava_native_windows_x86_64//jar",
        "@org_slf4j_slf4j_api//jar",
        "@org_yaml_snakeyaml//jar",
    ],
)

java_tests(
    classpath_resources = twister2_client_common_files(),
    test_classes = [
        "edu.iu.dsc.tws.examples.batch.kmeans.KMeansDataGeneratorTest",
        "edu.iu.dsc.tws.examples.batch.kmeans.TaskGraphBuildTest",
        "edu.iu.dsc.tws.examples.batch.kmeans.MLTest",
        "edu.iu.dsc.tws.examples.batch.kmeans.CSVInputFormatTest",
    ],
    runtime_deps = [
        ":examples-tests",
    ],
)

#java_tests_debug(
#    classpath_resources = twister2_client_common_files(),
#    test_classes = [
#        "edu.iu.dsc.tws.examples.batch.kmeans.KMeansDataGeneratorTest",
#        "edu.iu.dsc.tws.examples.batch.kmeans.TaskGraphBuildTest",
#        "edu.iu.dsc.tws.examples.batch.kmeans.MLTest",
#        "edu.iu.dsc.tws.examples.batch.kmeans.CSVInputFormatTest",
#    ],
#    runtime_deps = [
#        ":examples-tests",
#    ],
#)

