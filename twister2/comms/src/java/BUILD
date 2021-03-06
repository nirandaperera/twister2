package(default_visibility = ["//visibility:public"])

load("//tools:maven.bzl", "t2_java_lib")

srcs_without_ucx = glob(["**/*.java"])

srcs_without_ucx.remove("edu/iu/dsc/tws/comms/ucx/TWSUCXChannel.java")

t2_java_lib(
    name = "comms-java",
    srcs = select({
        "@bazel_tools//src/conditions:darwin": srcs_without_ucx,
        "//conditions:default": glob(["**/*.java"]),
    }),
    artifact_name = "Twsiter2 Communication",
    deps = [
        "//third_party:ompi_javabinding_java",
        "//twister2/api/src/java/edu/iu/dsc/tws/api/comms:comms-api-java",
        "//twister2/api/src/java/edu/iu/dsc/tws/api/config:config-api-java",
        "//twister2/api/src/java/edu/iu/dsc/tws/api/dataset:dataset-api-java",
        "//twister2/api/src/java/edu/iu/dsc/tws/api/exceptions:exceptions-java",
        "//twister2/api/src/java/edu/iu/dsc/tws/api/resource:resource-api-java",  ## todo get rid of this, Had to add this because of Network class
        "//twister2/api/src/java/edu/iu/dsc/tws/api/util:api-utils-java",
        "//twister2/common/src/java:common-java",
        "//twister2/data/src/main/java:data-java",
        "//twister2/proto:jobmaster-proto",
        "//twister2/proto:proto-java",
        "//twister2/proto/utils:proto-utils-java",
        "@com_google_guava_guava//jar",
        "@commons_io_commons_io//jar",
        "@it_unimi_dsi_fastutil//jar",
        "@org_apache_commons_commons_collections4//jar",
        "@org_apache_commons_commons_lang3//jar",
    ] + select({
        "@bazel_tools//src/conditions:darwin": [],
        "//conditions:default": ["//third_party:ucx_javabinding_java"],
    }),
)

#java_binary(
#    name = "comms-java-bin",
#    srcs = glob(["**/*.java"]),
#    jvm_flags = ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"],
#    main_class = "edu.iu.dsc.tws.comms.mpi.io.Test",
#    deps = [
#        "//third_party:ompi_javabinding_java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/comms:comms-api-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/config:config-api-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/dataset:dataset-api-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/exceptions:exceptions-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/resource:resource-api-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/util:api-utils-java",
#        "//twister2/common/src/java:common-java",
#        "//twister2/data/src/main/java:data-java",
#        "//twister2/proto:jobmaster-proto",
#        "//twister2/proto/utils:proto-utils-java",
#        "@com_google_guava_guava//jar",
#        "@commons_io_commons_io//jar",
#        "@it_unimi_dsi_fastutil//jar",
#        "@org_apache_commons_commons_lang3//jar",
#    ],
#)

#java_binary(
#    name = "comms-java-master",
#    srcs = glob(["**/*.java"]),
#    deps = [
#        "//third_party:ompi_javabinding_java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/comms:comms-api-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/config:config-api-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/dataset:dataset-api-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/exceptions:exceptions-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/resource:resource-api-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/util:api-utils-java",
#        "//twister2/common/src/java:common-java",
#        "//twister2/data/src/main/java:data-java",
#        "//twister2/proto:jobmaster-proto",
#        "//twister2/proto/utils:proto-utils-java",
#        "@com_google_guava_guava//jar",
#        "@commons_io_commons_io//jar",
#        "@it_unimi_dsi_fastutil//jar",
#        "@org_apache_commons_commons_lang3//jar",
#    ],
#)

#java_binary(
#    name = "comms-java-channel-1",
#    srcs = glob(["**/*.java"]),
#    jvm_flags = ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006 -Djava.util.logging.config.file=twister2/config/src/yaml/conf/standalone/logger.properties"],
#    main_class = "edu.iu.dsc.tws.comms.tcp.net.TCPChannel",
#    deps = [
#        "//third_party:ompi_javabinding_java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/comms:comms-api-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/config:config-api-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/dataset:dataset-api-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/exceptions:exceptions-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/resource:resource-api-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/util:api-utils-java",
#        "//twister2/common/src/java:common-java",
#        "//twister2/data/src/main/java:data-java",
#        "//twister2/proto:jobmaster-proto",
#        "//twister2/proto/utils:proto-utils-java",
#        "@com_esotericsoftware_kryo//jar",
#        "@com_esotericsoftware_minlog//jar",
#        "@com_esotericsoftware_reflectasm//jar",
#        "@com_google_guava_guava//jar",
#        "@commons_io_commons_io//jar",
#        "@it_unimi_dsi_fastutil//jar",
#        "@net_openhft_chronicle_queue//jar",
#        "@org_apache_commons_commons_lang3//jar",
#        "@org_objenesis_objenesis//jar",
#        "@org_ow2_asm_asm//jar",
#        "@org_yaml_snakeyaml//jar",
#    ],
#)

#java_binary(
#    name = "comms-java-channel-2",
#    srcs = glob(["**/*.java"]),
#    jvm_flags = ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5007 -Djava.util.logging.config.file=twister2/config/src/yaml/conf/standalone/logger.properties"],
#    main_class = "edu.iu.dsc.tws.comms.tcp.net.TCPChannel",
#    deps = [
#        "//third_party:ompi_javabinding_java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/comms:comms-api-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/config:config-api-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/dataset:dataset-api-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/exceptions:exceptions-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/resource:resource-api-java",
#        "//twister2/api/src/java/edu/iu/dsc/tws/api/util:api-utils-java",
#        "//twister2/common/src/java:common-java",
#        "//twister2/data/src/main/java:data-java",
#        "//twister2/proto:jobmaster-proto",
#        "//twister2/proto/utils:proto-utils-java",
#        "@com_esotericsoftware_kryo//jar",
#        "@com_esotericsoftware_minlog//jar",
#        "@com_esotericsoftware_reflectasm//jar",
#        "@com_google_guava_guava//jar",
#        "@commons_io_commons_io//jar",
#        "@it_unimi_dsi_fastutil//jar",
#        "@net_openhft_chronicle_queue//jar",
#        "@org_apache_commons_commons_lang3//jar",
#        "@org_objenesis_objenesis//jar",
#        "@org_ow2_asm_asm//jar",
#        "@org_yaml_snakeyaml//jar",
#    ],
#)
