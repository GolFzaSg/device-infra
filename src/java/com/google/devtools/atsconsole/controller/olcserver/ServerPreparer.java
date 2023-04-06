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

package com.google.devtools.atsconsole.controller.olcserver;

import static com.google.protobuf.TextFormat.shortDebugString;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.devtools.atsconsole.Annotations.DeviceInfraServiceFlags;
import com.google.devtools.atsconsole.controller.olcserver.Annotations.ServerBinary;
import com.google.devtools.atsconsole.controller.olcserver.Annotations.ServerVersionStub;
import com.google.devtools.common.metrics.stability.rpc.grpc.GrpcExceptionWithErrorId;
import com.google.devtools.deviceinfra.shared.util.flags.Flags;
import com.google.devtools.deviceinfra.shared.util.time.Sleeper;
import com.google.devtools.mobileharness.api.model.error.InfraErrorId;
import com.google.devtools.mobileharness.api.model.error.MobileHarnessException;
import com.google.devtools.mobileharness.infra.client.longrunningservice.proto.VersionServiceProto.GetVersionResponse;
import com.google.devtools.mobileharness.infra.client.longrunningservice.rpc.stub.VersionStub;
import com.google.devtools.mobileharness.shared.util.command.Command;
import com.google.devtools.mobileharness.shared.util.command.CommandExecutor;
import com.google.devtools.mobileharness.shared.util.command.CommandProcess;
import com.google.devtools.mobileharness.shared.util.command.CommandStartException;
import com.google.devtools.mobileharness.shared.util.command.LineCallback;
import com.google.devtools.mobileharness.shared.util.file.local.LocalFileUtil;
import com.google.devtools.mobileharness.shared.util.system.SystemUtil;
import io.grpc.Status.Code;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/** Server preparer for preparing an OLC server instance. */
@Singleton
public class ServerPreparer {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final CommandExecutor commandExecutor;
  private final Sleeper sleeper;
  private final SystemUtil systemUtil;
  private final LocalFileUtil localFileUtil;
  private final VersionStub versionStub;
  private final Provider<Path> serverBinary;
  private final ImmutableList<String> deviceInfraServiceFlags;

  @Inject
  ServerPreparer(
      CommandExecutor commandExecutor,
      Sleeper sleeper,
      SystemUtil systemUtil,
      LocalFileUtil localFileUtil,
      @ServerVersionStub VersionStub versionStub,
      @ServerBinary Provider<Path> serverBinary,
      @DeviceInfraServiceFlags ImmutableList<String> deviceInfraServiceFlags) {
    this.commandExecutor = commandExecutor;
    this.sleeper = sleeper;
    this.systemUtil = systemUtil;
    this.localFileUtil = localFileUtil;
    this.versionStub = versionStub;
    this.serverBinary = serverBinary;
    this.deviceInfraServiceFlags = deviceInfraServiceFlags;
  }

  public void prepareOlcServer() throws MobileHarnessException, InterruptedException {
    logger.atInfo().log("Preparing OLC server");

    // Tries to get server version.
    try {
      GetVersionResponse version = versionStub.getVersion();
      // TODO: Checks server version.
      logger.atInfo().log(
          "Connected to existing OLC server, version=[%s]", shortDebugString(version));
      return;
    } catch (GrpcExceptionWithErrorId e) {
      if (!e.getUnderlyingRpcException().getStatus().getCode().equals(Code.UNAVAILABLE)) {
        throw new MobileHarnessException(
            InfraErrorId.ATSC_SERVER_PREPARER_CONNECT_EXISTING_OLC_SERVER_ERROR,
            "Failed to connect existing OLC server",
            e);
      }
    }

    // Starts a new server if not exists.
    logger.atInfo().log("OLC server doesn't exist, starting a new one");
    String serverBinaryPath = serverBinary.get().toString();
    localFileUtil.checkFile(serverBinaryPath);

    CommandProcess serverProcess = null;
    CountDownLatch serverStartedLatch = new CountDownLatch(1);
    AtomicBoolean serverStartedSuccessfully = new AtomicBoolean();
    try {
      try {
        serverProcess =
            commandExecutor.start(
                Command.of(
                        systemUtil
                            .getJavaCommandCreator()
                            .createJavaCommand(
                                serverBinaryPath,
                                deviceInfraServiceFlags,
                                /* nativeArguments= */ ImmutableList.of()))
                    .onStderr(
                        new ServerStderrLineCallback(serverStartedLatch, serverStartedSuccessfully))
                    .onExit(result -> serverStartedLatch.countDown())
                    .timeout(ChronoUnit.YEARS.getDuration())
                    .redirectStderr(false)
                    .needStdoutInResult(false)
                    .needStderrInResult(false));
      } catch (CommandStartException e) {
        throw new MobileHarnessException(
            InfraErrorId.ATSC_SERVER_PREPARER_START_OLC_SERVER_ERROR,
            "Failed to start OLC server",
            e);
      }

      // Waits until the server starts.
      logger.atInfo().log("Wait until OLC server starts, command=[%s]", serverProcess.command());
      if (!serverStartedLatch.await(30L, SECONDS)) {
        throw new MobileHarnessException(
            InfraErrorId.ATSC_SERVER_PREPARER_OLC_SERVER_INITIALIZE_ERROR,
            "OLC server didn't start in 30 seconds");
      }
      if (!serverStartedSuccessfully.get()) {
        throw new MobileHarnessException(
            InfraErrorId.ATSC_SERVER_PREPARER_OLC_SERVER_ABNORMAL_EXIT_WHILE_INITIALIZATION,
            "OLC server exited abnormally while initialization");
      }
      connectWithRetry();
      logger.atInfo().log(
          "OLC server started, port=%s, pid=%s",
          Flags.instance().olcServerPort.getNonNull(), serverProcess.getUnixPidIfAny().orElse(0));
    } catch (MobileHarnessException | InterruptedException | RuntimeException | Error e) {
      if (serverProcess != null) {
        logger.atInfo().log("Killing OLC server");
        serverProcess.kill();
      }
      throw e;
    }
  }

  private void connectWithRetry() throws MobileHarnessException, InterruptedException {
    int count = 0;
    while (true) {
      try {
        versionStub.getVersion();
        return;
      } catch (GrpcExceptionWithErrorId e) {
        count++;
        if (count == 15) {
          throw new MobileHarnessException(
              InfraErrorId.ATSC_SERVER_PREPARER_CONNECT_NEW_OLC_SERVER_ERROR,
              "Failed to connect new OLC server",
              e);
        } else {
          sleeper.sleep(Duration.ofSeconds(1L));
        }
      }
    }
  }

  private static class ServerStderrLineCallback implements LineCallback {

    private static final String SERVER_STARTED_SIGNAL = "OLC server started";

    private final CountDownLatch serverStartedLatch;
    private final AtomicBoolean serverStartedSuccessfully;

    private ServerStderrLineCallback(
        CountDownLatch serverStartedLatch, AtomicBoolean serverStartedSuccessfully) {
      this.serverStartedLatch = serverStartedLatch;
      this.serverStartedSuccessfully = serverStartedSuccessfully;
    }

    @Override
    public Response onLine(String stderrLine) {
      logger.atInfo().log("olc_server_stderr: %s", stderrLine);

      if (stderrLine.contains(SERVER_STARTED_SIGNAL)) {
        serverStartedSuccessfully.set(true);
        serverStartedLatch.countDown();
        return Response.stopReadingOutput();
      } else {
        return Response.empty();
      }
    }
  }
}
