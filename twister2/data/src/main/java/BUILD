package(default_visibility = ["//visibility:public"])

load("//tools:maven.bzl", "t2_java_lib")

common_deps_files = [
    "@org_yaml_snakeyaml//jar",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/config:config-api-java",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/util:api-utils-java",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/dataset:dataset-api-java",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/comms:comms-api-java",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/data:data-api-java",
    "//twister2/api/src/java/edu/iu/dsc/tws/api/exceptions:exceptions-java",
    "@maven//:com_opencsv_opencsv",
    #"//twister2/comms/src/java:comms-java",
    "@com_esotericsoftware_kryo//jar",
    "//third_party:com_github_jnr_jffi_native",
    "@com_google_guava_guava//jar",
    "@org_lmdbjava_lmdbjava//jar",
    "@org_lmdbjava_lmdbjava_native_linux_x86_64//jar",
    "@org_lmdbjava_lmdbjava_native_windows_x86_64//jar",
    "@org_lmdbjava_lmdbjava_native_osx_x86_64//jar",
    "@com_github_jnr_jnr_ffi//jar",
    "@com_github_jnr_jnr_constants//jar",
    "@org_apache_commons_commons_lang3//jar",
    "@org_apache_hadoop_hadoop_hdfs//jar",
    "@commons_lang_commons_lang//jar",
    "@org_apache_hadoop_hadoop_common//jar",
    "@org_apache_hadoop_hadoop_annotations//jar",
    "@org_apache_hadoop_hadoop_auth//jar",
    "@org_apache_hadoop_hadoop_mapreduce_client_core//jar",
    "@com_google_code_findbugs_jsr305//jar",
    "@com_fasterxml_woodstox_woodstox_core//jar",
    "@org_codehaus_woodstox_stax2_api//jar",
    "@commons_io_commons_io//jar",
    "@commons_collections_commons_collections//jar",
    "@commons_configuration_commons_configuration//jar",
    "@log4j_log4j//jar",
    "@org_apache_htrace_htrace_core4//jar",
    "@org_apache_hadoop_hadoop_hdfs_client//jar",
    "@org_slf4j_slf4j_api//jar",
]

t2_java_lib(
    name = "data-java",
    srcs = glob(["**/*.java"]),
    artifact_name = "Twister2 Data",
    deps = common_deps_files,
)

filegroup(
    name = "srcs",
    srcs = ["BUILD"] + glob(["**/*.java"]),
)
