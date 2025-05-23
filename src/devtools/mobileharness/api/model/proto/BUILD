# Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

load("@com_google_protobuf//:protobuf.bzl", "py_proto_library")

package(
    default_applicable_licenses = ["//:license"],
    default_visibility = [
        "//:deviceinfra_api_users",
    ],
)

exports_files(["error.proto"])

proto_library(
    name = "device_proto",
    srcs = ["device.proto"],
    deps = [
        ":lab_proto",
        "//src/devtools/common/metrics/stability/model/proto:error_proto",
    ],
)

py_proto_library(
    name = "device_py_pb2",
    srcs = [
        "device.proto",
    ],
    deps = [
        ":lab_py_proto",
        "//src/devtools/common/metrics/stability/model/proto:error_py_proto",
    ],
)

java_proto_library(
    name = "device_java_proto",
    deps = [":device_proto"],
)

proto_library(
    name = "error_proto",
    srcs = ["error.proto"],
    deps = ["//src/devtools/common/metrics/stability/model/proto:error_proto"],
)

cc_proto_library(
    name = "error_cc_proto",
    deps = [":error_proto"],
)

java_proto_library(
    name = "error_java_proto",
    deps = [":error_proto"],
)

proto_library(
    name = "job_proto",
    srcs = ["job.proto"],
)

java_proto_library(
    name = "job_java_proto",
    deps = [":job_proto"],
)

proto_library(
    name = "lab_proto",
    srcs = ["lab.proto"],
    deps = [
        ":universe_proto",
    ],
)

java_proto_library(
    name = "lab_java_proto",
    deps = [":lab_proto"],
)

py_proto_library(
    name = "lab_py_proto",
    srcs = [
        "lab.proto",
    ],
    deps = [
        ":universe_py_proto",
    ],
)

proto_library(
    name = "test_proto",
    srcs = ["test.proto"],
    deps = [":job_proto"],
)

java_proto_library(
    name = "test_java_proto",
    deps = [":test_proto"],
)

proto_library(
    name = "universe_proto",
    srcs = ["universe.proto"],
)

java_proto_library(
    name = "universe_java_proto",
    deps = [":universe_proto"],
)

py_proto_library(
    name = "universe_py_proto",
    srcs = [
        "universe.proto",
    ],
)
