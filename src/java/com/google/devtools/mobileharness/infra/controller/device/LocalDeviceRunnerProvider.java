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

package com.google.devtools.mobileharness.infra.controller.device;

import com.google.devtools.mobileharness.api.model.error.MobileHarnessException;

/** Provider for getting {@linkplain LocalDeviceRunner}. */
public interface LocalDeviceRunnerProvider {

  /**
   * Gets the device runner according to its device ID.
   *
   * @param deviceId ID of the device
   * @return the runner, or null if not found
   */
  LocalDeviceRunner getLocalDeviceRunner(String deviceId) throws MobileHarnessException;
}
