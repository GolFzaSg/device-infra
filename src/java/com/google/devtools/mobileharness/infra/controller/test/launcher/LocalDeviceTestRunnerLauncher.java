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

package com.google.devtools.mobileharness.infra.controller.test.launcher;

import static com.google.devtools.mobileharness.shared.util.concurrent.Callables.threadRenaming;
import static com.google.devtools.mobileharness.shared.util.concurrent.MoreFutures.getUnchecked;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.SettableFuture;
import com.google.devtools.mobileharness.api.model.error.InfraErrorId;
import com.google.devtools.mobileharness.api.model.error.MobileHarnessException;
import com.google.devtools.mobileharness.infra.controller.device.LocalDeviceRunner;
import com.google.devtools.mobileharness.infra.controller.device.LocalDeviceTestExecutor;
import com.google.devtools.mobileharness.infra.controller.test.TestRunner;
import com.google.devtools.mobileharness.infra.controller.test.TestRunnerLauncher;
import com.google.devtools.mobileharness.infra.controller.test.model.TestExecutionResult;
import com.google.devtools.mobileharness.shared.constant.closeable.NonThrowingAutoCloseable;
import com.google.devtools.mobileharness.shared.context.InvocationContext;
import com.google.devtools.mobileharness.shared.context.InvocationContext.ContextScope;
import com.google.devtools.mobileharness.shared.context.InvocationContext.InvocationInfo;
import com.google.devtools.mobileharness.shared.context.InvocationContext.InvocationType;
import com.google.wireless.qa.mobileharness.shared.api.device.Device;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

/** Local device test runner launcher which uses {@code LocalDeviceRunner}s to launch the test. */
public class LocalDeviceTestRunnerLauncher extends TestRunnerLauncher<TestRunner> {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  /**
   * Primary local device test executor which will invoke {@link #executeTest()} synchronously on
   * the main thread of the primary {@code LocalDeviceRunner} of the test.
   *
   * <p>When a test has multiple {@code LocalDeviceRunner}, one and only one will be the primary
   * {@code LocalDeviceRunner} and the other ones will wait until the primary one finishes the test.
   */
  private class PrimaryDeviceTestExecutor extends AbstractDeviceTestExecutor {

    private PrimaryDeviceTestExecutor(LocalDeviceRunner deviceRunner) {
      super(deviceRunner);
    }

    @Override
    public TestExecutionResult doExecuteTest() throws InterruptedException {
      // Waits until all devices are ready.
      if (testExecutors.size() > 1) {
        logger.atInfo().log("Waiting until all devices are ready");
      }
      try {
        barrier.await();
      } catch (BrokenBarrierException e) {
        throw new InterruptedException("Other device runner is interrupted");
      }

      // Executes the test and broadcasts the result or the error.
      hasExecuted = true;
      try (NonThrowingAutoCloseable ignored =
          threadRenaming(getTestRunner().getTestRunnerThreadName())) {
        logger.atInfo().log(
            "Executing test [%s] on its primary device runner [%s]",
            getTestRunner().getTestExecutionUnit().locator().id(),
            getDeviceRunner().getDevice().getDeviceId());
        TestExecutionResult result = LocalDeviceTestRunnerLauncher.this.executeTest();
        resultFuture.set(result);
        return result;
      } catch (InterruptedException | RuntimeException e) {
        resultFuture.setException(e);
        throw e;
      }
    }
  }

  /**
   * Secondary local device test executor which will let the main thread of the device runner wait
   * the primary test executor to execute the test.
   */
  private class SecondaryDeviceTestExecutor extends AbstractDeviceTestExecutor {

    private SecondaryDeviceTestExecutor(LocalDeviceRunner deviceRunner) {
      super(deviceRunner);
    }

    @Override
    public TestExecutionResult doExecuteTest() throws InterruptedException {
      // Waits until all device are ready.
      logger.atInfo().log("Waiting until all devices are ready");
      try {
        barrier.await();
      } catch (BrokenBarrierException e) {
        throw new InterruptedException("Other device runner is interrupted");
      }

      // Waits until the test finished.
      logger.atInfo().log(
          "Secondary device runner [%s] of test [%s] is waiting test execution result",
          getDeviceRunner().getDevice().getDeviceId(),
          getTestRunner().getTestExecutionUnit().locator().id());
      return getUnchecked(resultFuture);
    }
  }

  /** Base class of {@link LocalDeviceTestExecutor}. */
  private abstract class AbstractDeviceTestExecutor implements LocalDeviceTestExecutor {

    private final LocalDeviceRunner deviceRunner;

    private AbstractDeviceTestExecutor(LocalDeviceRunner deviceRunner) {
      this.deviceRunner = deviceRunner;
    }

    protected final LocalDeviceRunner getDeviceRunner() {
      return deviceRunner;
    }

    @Override
    public final TestRunner getTestRunner() {
      return LocalDeviceTestRunnerLauncher.this.getTestRunner();
    }

    @Override
    public final TestExecutionResult executeTest() throws InterruptedException {
      try (var ignored = new ContextScope(context)) {
        return doExecuteTest();
      }
    }

    protected abstract TestExecutionResult doExecuteTest() throws InterruptedException;
  }

  private final ImmutableMap<InvocationType, InvocationInfo> context =
      InvocationContext.getCurrentContextImmutable();
  private final ImmutableList<AbstractDeviceTestExecutor> testExecutors;
  private final CyclicBarrier barrier;
  private final SettableFuture<TestExecutionResult> resultFuture = SettableFuture.create();
  private volatile boolean hasReserved;
  private volatile boolean hasExecuted;

  public LocalDeviceTestRunnerLauncher(
      LocalDeviceRunner primaryDeviceRunner, List<LocalDeviceRunner> secondaryDeviceRunners) {
    this.testExecutors =
        ImmutableList.<AbstractDeviceTestExecutor>builder()
            .add(new PrimaryDeviceTestExecutor(primaryDeviceRunner))
            .addAll(
                secondaryDeviceRunners.stream()
                    .map(SecondaryDeviceTestExecutor::new)
                    .collect(Collectors.toList()))
            .build();
    this.barrier = new CyclicBarrier(testExecutors.size());
  }

  @Override
  protected void asyncLaunchTest() throws MobileHarnessException {
    logger.atInfo().log(
        "Reserving devices %s for test [%s]",
        testExecutors.stream()
            .map(AbstractDeviceTestExecutor::getDeviceRunner)
            .map(LocalDeviceRunner::getDevice)
            .map(Device::getDeviceId)
            .collect(Collectors.toList()),
        getTestRunner().getTestExecutionUnit().locator().id());
    try {
      for (AbstractDeviceTestExecutor testExecutor : testExecutors) {
        testExecutor.getDeviceRunner().reserve(testExecutor);
      }
    } catch (MobileHarnessException e) {
      logger.atInfo().log("Failed to reserve all devices, cancel reservations");
      killTest();
      throw e;
    }
    hasReserved = true;
  }

  @Override
  protected void killTest() {
    testExecutors.forEach(testExecutor -> testExecutor.getDeviceRunner().cancel(testExecutor));
  }

  @Override
  protected boolean isTestRunning() {
    boolean isRunning = isTestExecuting();
    if (isRunning) {
      return true;
    }

    // If the test is not executing, only if all device runners are holding the test, we treat the
    // test as running.
    boolean allDeviceReserved = true;
    List<LocalDeviceRunner> disconnectedDevices = new ArrayList<>();
    for (AbstractDeviceTestExecutor testExecutor : testExecutors) {
      if (!(testExecutor.getDeviceRunner().isAlive()
          && testExecutor.getDeviceRunner().getTest() == testExecutor)) {
        allDeviceReserved = false;
        disconnectedDevices.add(testExecutor.getDeviceRunner());
      }
    }
    if (allDeviceReserved) {
      return true;
    }
    if (hasReserved && !hasExecuted) {
      // The test will not be executed anymore because some device runners do not hold the test.
      finalizeTest(
          new MobileHarnessException(
              InfraErrorId.TR_DEVICE_DISCONNECTED_BEFORE_TEST_START,
              String.format(
                  "Devices %s disconnected before test executes",
                  disconnectedDevices.stream()
                      .map(LocalDeviceRunner::getDevice)
                      .map(Device::getDeviceId)
                      .collect(Collectors.toList()))));
    }
    return false;
  }
}
