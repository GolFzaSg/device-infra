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

load("@rules_java//java:defs.bzl", "java_library")

package(
    default_applicable_licenses = ["//:license"],
    default_testonly = 1,
    default_visibility = ["//src/javatests/com/google/devtools/mobileharness/shared/util/junit/rule:__pkg__"],
)

java_library(
    name = "finish_with_failure_test_watcher",
    srcs = ["FinishWithFailureTestWatcher.java"],
    deps = [
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:junit_junit",
    ],
)

java_library(
    name = "strings_debugger",
    srcs = ["StringsDebugger.java"],
    deps = [
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
    ],
)
