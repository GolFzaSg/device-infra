/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.mobileharness.infra.ats.console.controller.sessionplugin;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.devtools.mobileharness.shared.util.time.TimeUtils.toJavaDuration;
import static com.google.devtools.mobileharness.shared.util.time.TimeUtils.toJavaInstant;
import static com.google.devtools.mobileharness.shared.util.time.TimeUtils.toReadableDurationString;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.devtools.mobileharness.infra.ats.console.controller.proto.SessionPluginProto.AtsSessionPluginOutput;
import com.google.devtools.mobileharness.infra.ats.console.controller.proto.SessionPluginProto.RunCommandState;
import com.google.devtools.mobileharness.infra.ats.console.controller.proto.SessionPluginProto.RunCommandState.Invocation;
import com.google.devtools.mobileharness.infra.ats.console.util.console.ConsoleUtil;
import com.google.devtools.mobileharness.shared.util.base.TableFormatter;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import picocli.CommandLine.ExitCode;

/** Printer for printing {@code AtsSessionPluginOutput} to console. */
public class PluginOutputPrinter {

  private static final ImmutableList<String> LIST_INVOCATIONS_HEADER =
      ImmutableList.of("Command Id", "Exec Time", "Device", "State");

  /**
   * Prints {@code AtsSessionPluginOutput} to ATS console.
   *
   * @return {@link ExitCode#OK} if the output contains a success, {@link ExitCode#SOFTWARE}
   *     otherwise
   */
  @CanIgnoreReturnValue
  public static int printOutput(AtsSessionPluginOutput output, ConsoleUtil consoleUtil) {
    switch (output.getResultCase()) {
      case SUCCESS:
        consoleUtil.printlnStdout(output.getSuccess().getOutputMessage());
        return ExitCode.OK;
      case FAILURE:
        consoleUtil.printlnStderr("Error: %s", output.getFailure().getErrorMessage());
        break;
      default:
    }
    return ExitCode.SOFTWARE;
  }

  /** Prints output of "list commands" command. */
  public static String listCommands(List<AtsSessionPluginConfigOutput> configOutputs) {
    return configOutputs.stream()
        .map(PluginOutputPrinter::getRunCommandState)
        .sorted(comparing(RunCommandState::getCommandId))
        .map(PluginOutputPrinter::formatCommand)
        .collect(joining("\n"));
  }

  /** Prints output of "list invocations" command. */
  public static String listInvocations(List<AtsSessionPluginConfigOutput> configOutputs) {
    ImmutableList<ImmutableList<String>> invocations =
        configOutputs.stream()
            .map(PluginOutputPrinter::getRunCommandState)
            .flatMap(runCommandState -> runCommandState.getRunningInvocationMap().values().stream())
            .sorted(
                comparing(Invocation::getCommandId)
                    .thenComparing(invocation -> toJavaInstant(invocation.getStartTime())))
            .map(PluginOutputPrinter::formatInvocation)
            .collect(toImmutableList());
    if (invocations.isEmpty()) {
      return "";
    }
    ImmutableList<ImmutableList<String>> table =
        Stream.concat(Stream.of(LIST_INVOCATIONS_HEADER), invocations.stream())
            .collect(toImmutableList());
    return TableFormatter.displayTable(table);
  }

  private static RunCommandState getRunCommandState(
      AtsSessionPluginConfigOutput pluginConfigOutput) {
    return pluginConfigOutput
        .output()
        .flatMap(
            pluginOutput ->
                pluginOutput.hasRunCommandState()
                    ? Optional.of(pluginOutput.getRunCommandState())
                    : Optional.empty())
        .orElse(pluginConfigOutput.config().getRunCommand().getInitialState());
  }

  private static String formatCommand(RunCommandState runCommandState) {
    return String.format(
        "Command %s: [%s] %s",
        runCommandState.getCommandId(),
        toReadableDurationString(toJavaDuration(runCommandState.getTotalExecutionTime())),
        runCommandState.getCommandLineArgs());
  }

  private static ImmutableList<String> formatInvocation(Invocation invocation) {
    return ImmutableList.of(
        invocation.getCommandId(),
        toReadableDurationString(
            Duration.between(toJavaInstant(invocation.getStartTime()), Instant.now())),
        invocation.getDeviceIdList().stream().collect(joining(", ", "[", "]")),
        invocation.getStateSummary());
  }

  private PluginOutputPrinter() {}
}
