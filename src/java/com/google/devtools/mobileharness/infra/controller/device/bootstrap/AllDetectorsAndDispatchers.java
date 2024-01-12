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

package com.google.devtools.mobileharness.infra.controller.device.bootstrap;


import com.google.common.collect.ImmutableList;
import com.google.devtools.mobileharness.api.devicemanager.detector.BaseAdbDetector;
import com.google.devtools.mobileharness.api.devicemanager.detector.Detector;
import com.google.devtools.mobileharness.api.devicemanager.detector.NoOpDeviceDetector;
import com.google.devtools.mobileharness.api.devicemanager.dispatcher.AndroidRealDeviceDispatcher;
import com.google.devtools.mobileharness.api.devicemanager.dispatcher.Dispatcher;
import com.google.devtools.mobileharness.api.devicemanager.dispatcher.NoOpDeviceDispatcher;
import com.google.devtools.mobileharness.api.devicemanager.util.ClassUtil;
import com.google.devtools.mobileharness.api.model.error.MobileHarnessException;
import com.google.devtools.mobileharness.infra.controller.device.DispatcherManager;
import com.google.devtools.mobileharness.shared.util.flags.Flags;
import com.google.devtools.mobileharness.shared.util.reflection.ReflectionUtil;

/** All detectors and dispatchers for {@link DetectorDispatcherSelector} in different components. */
final class AllDetectorsAndDispatchers {

  private static final ReflectionUtil REFLECTION_UTIL = new ReflectionUtil();

  private AllDetectorsAndDispatchers() {}

  public static ImmutableList<Detector> detectorCandidatesForLocalMode3pAndOss() {
    return detectorCandidatesForLocalModeInternal3pOssAndLabServerOss();
  }

  public static ImmutableList<Detector> detectorCandidatesForLabServerOss() {
    return detectorCandidatesForLocalModeInternal3pOssAndLabServerOss();
  }

  private static ImmutableList<Detector>
      detectorCandidatesForLocalModeInternal3pOssAndLabServerOss() {
    ImmutableList.Builder<Detector> detectorCandidates = ImmutableList.builder();

    // ADB detector.
    if (Flags.instance().detectAdbDevice.getNonNull()) {
      detectorCandidates.add(createAdbDetectorOss());
    }

    // NoOpDevice detector.
    if (Flags.instance().noOpDeviceNum.getNonNull() > 0) {
      detectorCandidates.add(new NoOpDeviceDetector());
    }
    return detectorCandidates.build();
  }

  private static Detector createAdbDetectorOss() {
    return new BaseAdbDetector();
  }

  public static void addDispatchersForLocalMode3pAndOss(DispatcherManager dispatcherManager) {
    addDispatchersForAll(dispatcherManager);
  }

  public static void addDispatchersForLabServerOss(DispatcherManager dispatcherManager) {
    addDispatchersForAll(dispatcherManager);
  }

  private static void addDispatchersForAll(DispatcherManager dispatcherManager) {
    // AndroidRealDevice dispatcher.
    if (Flags.instance().detectAdbDevice.getNonNull()) {
      dispatcherManager.add(AndroidRealDeviceDispatcher.class);
    }

    // NoOpDevice dispatcher.
    if (Flags.instance().noOpDeviceNum.getNonNull() > 0) {
      dispatcherManager.add(NoOpDeviceDispatcher.class);
    }
  }

  private static Detector createDetector(String detectorClassSimpleName) {
    String detectorClassName = Detector.class.getPackageName() + "." + detectorClassSimpleName;
    try {
      return REFLECTION_UTIL
          .loadClass(
              detectorClassName, Detector.class, AllDetectorsAndDispatchers.class.getClassLoader())
          .getConstructor()
          .newInstance();
    } catch (MobileHarnessException | ReflectiveOperationException e) {
      throw new IllegalStateException(
          String.format("Detector class [%s] is not added as runtime_deps", detectorClassName), e);
    }
  }

  private static Class<? extends Dispatcher> loadDispatcherClass(String dispatcherClassSimpleName) {
    try {
      return ClassUtil.getDispatcherClass(dispatcherClassSimpleName);
    } catch (MobileHarnessException e) {
      throw new IllegalStateException(
          String.format(
              "Dispatcher class [%s] is not added as runtime_deps", dispatcherClassSimpleName),
          e);
    }
  }
}
