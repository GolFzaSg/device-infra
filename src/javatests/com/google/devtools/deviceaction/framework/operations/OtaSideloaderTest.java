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

package com.google.devtools.deviceaction.framework.operations;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FakeTimeLimiter;
import com.google.devtools.common.metrics.stability.model.proto.ErrorTypeProto.ErrorType;
import com.google.devtools.deviceaction.common.error.DeviceActionException;
import com.google.devtools.deviceaction.framework.devices.AndroidPhone;
import com.google.devtools.mobileharness.platform.android.sdktool.adb.RebootMode;
import com.google.devtools.mobileharness.shared.util.quota.QuotaManager;
import com.google.devtools.mobileharness.shared.util.quota.QuotaManager.Lease;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import java.io.File;
import java.time.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(TestParameterInjector.class)
public final class OtaSideloaderTest {

  @Rule public final TemporaryFolder tmpFolder = new TemporaryFolder();
  @Rule public final MockitoRule mockito = MockitoJUnit.rule();

  @Mock private QuotaManager mockManager;
  @Mock private AndroidPhone mockDevice;
  @Mock private Lease lease;

  private File otaPackage;

  private OtaSideloader sideloader;

  @Before
  public void setUp() throws Exception {
    otaPackage = tmpFolder.newFile("ota.zip");
    when(mockDevice.getUuid()).thenReturn("id");
    when(mockManager.acquire(any(), anyInt())).thenReturn(lease);
    sideloader = new OtaSideloader(mockDevice, mockManager, new FakeTimeLimiter());
  }

  @Test
  public void sideload_autoReboot_success() throws Exception {
    sideloader.sideload(otaPackage, Duration.ofSeconds(1), /* useAutoReboot= */ true);

    verify(mockDevice).reboot(RebootMode.SIDELOAD_AUTO_REBOOT);
    verify(mockDevice).sideload(eq(otaPackage), any(Duration.class), any(Duration.class));
    verify(mockDevice).waitUntilReady(any(Duration.class));
  }

  @Test
  public void sideload_noAutoReboot_success() throws Exception {
    sideloader.sideload(otaPackage, Duration.ofSeconds(1), /* useAutoReboot= */ false);

    verify(mockDevice).reboot(RebootMode.SIDELOAD);
    verify(mockDevice).sideload(eq(otaPackage), any(Duration.class), any(Duration.class));
    verify(mockDevice).waitUntilReady(eq(RebootMode.RECOVERY), any(Duration.class));
    verify(mockDevice).reboot(any(Duration.class));
  }

  @Test
  public void sideload_fail(@TestParameter boolean autoReboot) throws Exception {
    doThrow(new DeviceActionException("FAKE", ErrorType.UNDETERMINED, ""))
        .when(mockDevice)
        .sideload(eq(otaPackage), any(Duration.class), any(Duration.class));

    DeviceActionException t =
        assertThrows(
            DeviceActionException.class,
            () -> sideloader.sideload(otaPackage, Duration.ofMillis(10), autoReboot));
    assertThat(t.getErrorId().name()).isEqualTo("FAKE");
  }

  @Test
  public void sideload_timeout(@TestParameter boolean autoReboot) throws Exception {
    doAnswer(
            invocation -> {
              Thread.sleep(20);
              return null;
            })
        .when(mockDevice)
        .sideload(eq(otaPackage), any(Duration.class), any(Duration.class));

    DeviceActionException t =
        assertThrows(
            DeviceActionException.class,
            () -> sideloader.sideload(otaPackage, Duration.ofMillis(10), autoReboot));
    assertThat(t.getErrorId().name()).isEqualTo("TIMEOUT");
  }
}
