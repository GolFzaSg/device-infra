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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.devtools.atsconsole.Annotations.DeviceInfraServiceFlags;
import com.google.devtools.atsconsole.controller.olcserver.Annotations.ServerBinary;
import com.google.devtools.atsconsole.controller.olcserver.Annotations.ServerSessionStub;
import com.google.devtools.common.metrics.stability.rpc.grpc.GrpcExceptionWithErrorId;
import com.google.devtools.deviceinfra.shared.util.concurrent.ThreadFactoryUtil;
import com.google.devtools.deviceinfra.shared.util.flags.Flags;
import com.google.devtools.deviceinfra.shared.util.port.PortProber;
import com.google.devtools.deviceinfra.shared.util.runfiles.RunfilesUtil;
import com.google.devtools.deviceinfra.shared.util.time.Sleeper;
import com.google.devtools.mobileharness.api.model.error.InfraErrorId;
import com.google.devtools.mobileharness.infra.client.longrunningservice.proto.SessionServiceProto.GetSessionRequest;
import com.google.devtools.mobileharness.infra.client.longrunningservice.rpc.stub.SessionStub;
import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ServerPreparerTest {

  @Rule public TemporaryFolder tmpFolder = new TemporaryFolder();

  @Bind private ListeningScheduledExecutorService threadPool;
  @Bind private Sleeper sleeper;
  @Bind @ServerBinary private Path serverPath;
  @Bind @DeviceInfraServiceFlags private ImmutableList<String> deviceInfraServiceFlags;

  @Inject private ServerPreparer serverPreparer;
  @Inject @ServerSessionStub private SessionStub sessionStub;

  @Before
  public void setUp() throws Exception {
    int serverPort = PortProber.pickUnusedPort();
    String publicDirPath = tmpFolder.newFolder("public_dir").toString();

    deviceInfraServiceFlags =
        ImmutableList.of(
            "--olc_server_port=" + serverPort,
            "--public_dir=" + publicDirPath,
            "--detect_adb_device=false");
    Flags.parse(deviceInfraServiceFlags.toArray(new String[0]));

    sleeper = Sleeper.defaultSleeper();
    serverPath =
        Path.of(
            RunfilesUtil.getRunfilesLocation(
                "java/com/google/devtools/atsconsole/controller/olcserver/AtsOlcServer_deploy.jar"));
    threadPool =
        MoreExecutors.listeningDecorator(
            Executors.newScheduledThreadPool(
                /* corePoolSize= */ 5, ThreadFactoryUtil.createThreadFactory("main-thread")));

    Guice.createInjector(new OlcServerModule(), BoundFieldModule.of(this)).injectMembers(this);
  }

  @After
  public void tearDown() {
    Flags.resetToDefault();
  }

  @Test
  public void prepareOlcServer() throws Exception {
    serverPreparer.prepareOlcServer();

    assertThat(
            assertThrows(
                    GrpcExceptionWithErrorId.class,
                    () -> sessionStub.getSession(GetSessionRequest.getDefaultInstance()))
                .getApplicationError()
                .orElseThrow()
                .getErrorId()
                .name())
        .isEqualTo(InfraErrorId.OLCS_GET_SESSION_SESSION_NOT_FOUND.name());
  }
}
