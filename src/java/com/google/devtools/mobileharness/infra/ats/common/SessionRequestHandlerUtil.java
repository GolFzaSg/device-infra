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

package com.google.devtools.mobileharness.infra.ats.common;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.primitives.Ints.saturatedCast;
import static com.google.devtools.mobileharness.shared.constant.LogRecordImportance.IMPORTANCE;
import static com.google.devtools.mobileharness.shared.constant.LogRecordImportance.Importance.IMPORTANT;
import static com.google.devtools.mobileharness.shared.util.base.ProtoTextFormat.shortDebugString;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.devtools.mobileharness.api.model.error.InfraErrorId;
import com.google.devtools.mobileharness.api.model.error.MobileHarnessException;
import com.google.devtools.mobileharness.api.model.error.MobileHarnessExceptionFactory;
import com.google.devtools.mobileharness.infra.ats.common.XtsPropertyName.Job;
import com.google.devtools.mobileharness.infra.ats.common.plan.TestPlanParser;
import com.google.devtools.mobileharness.infra.ats.common.proto.XtsCommonProto.ShardingMode;
import com.google.devtools.mobileharness.infra.ats.console.result.report.CertificationSuiteInfoFactory;
import com.google.devtools.mobileharness.infra.client.api.controller.device.DeviceQuerier;
import com.google.devtools.mobileharness.infra.client.longrunningservice.Annotations.SessionGenDir;
import com.google.devtools.mobileharness.infra.client.longrunningservice.Annotations.SessionTempDir;
import com.google.devtools.mobileharness.platform.android.sdktool.adb.AndroidAdbUtil;
import com.google.devtools.mobileharness.platform.android.sdktool.adb.AndroidProperty;
import com.google.devtools.mobileharness.platform.android.xts.common.util.AbiUtil;
import com.google.devtools.mobileharness.platform.android.xts.common.util.MoblyTestLoader;
import com.google.devtools.mobileharness.platform.android.xts.common.util.XtsDirUtil;
import com.google.devtools.mobileharness.platform.android.xts.config.ConfigurationUtil;
import com.google.devtools.mobileharness.platform.android.xts.config.ModuleConfigurationHelper;
import com.google.devtools.mobileharness.platform.android.xts.config.proto.ConfigurationProto.Configuration;
import com.google.devtools.mobileharness.platform.android.xts.config.proto.ConfigurationProto.Device;
import com.google.devtools.mobileharness.platform.android.xts.config.proto.DeviceConfigurationProto.DeviceConfigurations;
import com.google.devtools.mobileharness.platform.android.xts.config.proto.DeviceConfigurationProto.ModuleDeviceConfiguration;
import com.google.devtools.mobileharness.platform.android.xts.suite.SuiteTestFilter;
import com.google.devtools.mobileharness.platform.android.xts.suite.TestSuiteHelper;
import com.google.devtools.mobileharness.platform.android.xts.suite.TestSuiteHelper.DeviceInfo;
import com.google.devtools.mobileharness.platform.android.xts.suite.subplan.SubPlan;
import com.google.devtools.mobileharness.shared.util.base.ProtoTextFormat;
import com.google.devtools.mobileharness.shared.util.file.local.LocalFileUtil;
import com.google.devtools.mobileharness.shared.util.file.local.ResUtil;
import com.google.devtools.mobileharness.shared.util.flags.Flags;
import com.google.devtools.mobileharness.shared.util.jobconfig.JobInfoCreator;
import com.google.gson.Gson;
import com.google.inject.Provider;
import com.google.protobuf.TextFormat.ParseException;
import com.google.wireless.qa.mobileharness.shared.model.job.JobInfo;
import com.google.wireless.qa.mobileharness.shared.proto.Job.Priority;
import com.google.wireless.qa.mobileharness.shared.proto.JobConfig;
import com.google.wireless.qa.mobileharness.shared.proto.JobConfig.DeviceList;
import com.google.wireless.qa.mobileharness.shared.proto.JobConfig.Driver;
import com.google.wireless.qa.mobileharness.shared.proto.JobConfig.StringList;
import com.google.wireless.qa.mobileharness.shared.proto.JobConfig.StringMap;
import com.google.wireless.qa.mobileharness.shared.proto.JobConfig.SubDeviceSpec;
import com.google.wireless.qa.mobileharness.shared.proto.query.DeviceQuery;
import com.google.wireless.qa.mobileharness.shared.proto.query.DeviceQuery.DeviceQueryFilter;
import com.google.wireless.qa.mobileharness.shared.proto.query.DeviceQuery.DeviceQueryResult;
import java.io.File;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Inject;

/** Helper class for ATS applications to create job config. */
public class SessionRequestHandlerUtil {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static final String ANDROID_REAL_DEVICE_TYPE = "AndroidRealDevice";
  public static final String ANDROID_LOCAL_EMULATOR_TYPE = "AndroidLocalEmulator";
  public static final String ANDROID_DEVICE_TYPE = "AndroidDevice";
  private static final Pattern MODULE_PARAMETER_PATTERN =
      Pattern.compile(".*\\[(?<moduleParam>.*)]$");

  /**
   * The same as
   * com.google.wireless.qa.mobileharness.shared.api.driver.MoblyGenericTest.TEST_SELECTOR_KEY
   */
  public static final String MOBLY_TEST_SELECTOR_KEY = "test_case_selector";

  /**
   * The same as
   * com.google.devtools.mobileharness.platform.android.xts.plugin.NonTradefedReportGenerator.PARAM_RUN_CERTIFICATION_TEST_SUITE
   */
  public static final String PARAM_RUN_CERTIFICATION_TEST_SUITE = "run_certification_test_suite";

  /**
   * The same as
   * com.google.devtools.mobileharness.platform.android.xts.plugin.NonTradefedReportGenerator.PARAM_XTS_SUITE_INFO
   */
  public static final String PARAM_XTS_SUITE_INFO = "xts_suite_info";

  /**
   * The same as
   * com.google.devtools.deviceinfra.ext.devicemanagement.device.platform.android.realdevice.AndroidRealDeviceConstants.PARAM_CLEAR_GSERVICES_OVERRIDES
   */
  public static final String PARAM_CLEAR_GSERVICES_OVERRIDES = "clear_gservices_overrides";

  /**
   * The same as
   * com.google.wireless.qa.mobileharness.shared.api.step.android.InstallApkStepConstants.PARAM_CHECK_INSTALLED_GMS_CORE_VERSION
   */
  public static final String PARAM_CHECK_INSTALLED_GMS_CORE_VERSION =
      "check_installed_gms_core_version";

  private static final Duration JOB_TEST_TIMEOUT_DIFF = Duration.ofMinutes(1L);
  private static final Duration DEFAULT_TRADEFED_JOB_TIMEOUT = Duration.ofDays(15L);
  private static final Duration DEFAULT_TRADEFED_START_TIMEOUT = Duration.ofDays(14L);
  private static final Duration DEFAULT_NON_TRADEFED_JOB_TIMEOUT = Duration.ofDays(5L);
  private static final Duration DEFAULT_NON_TRADEFED_START_TIMEOUT = Duration.ofDays(4L);

  private final DeviceQuerier deviceQuerier;
  private final LocalFileUtil localFileUtil;
  private final ConfigurationUtil configurationUtil;
  private final ModuleConfigurationHelper moduleConfigurationHelper;
  private final CertificationSuiteInfoFactory certificationSuiteInfoFactory;
  private final Provider<AndroidAdbUtil> androidAdbUtilProvider;
  private final Path sessionGenDir;
  private final Path sessionTempDir;
  private final Provider<ResUtil> resUtilProvider;
  private final DeviceDetailsRetriever deviceDetailsRetriever;
  private final MoblyTestLoader moblyTestLoader;

  @Inject
  SessionRequestHandlerUtil(
      DeviceQuerier deviceQuerier,
      LocalFileUtil localFileUtil,
      ConfigurationUtil configurationUtil,
      ModuleConfigurationHelper moduleConfigurationHelper,
      CertificationSuiteInfoFactory certificationSuiteInfoFactory,
      Provider<AndroidAdbUtil> androidAdbUtilProvider,
      @SessionGenDir Path sessionGenDir,
      @SessionTempDir Path sessionTempDir,
      Provider<ResUtil> resUtilProvider,
      DeviceDetailsRetriever deviceDetailsRetriever,
      MoblyTestLoader moblyTestLoader) {
    this.deviceQuerier = deviceQuerier;
    this.localFileUtil = localFileUtil;
    this.configurationUtil = configurationUtil;
    this.moduleConfigurationHelper = moduleConfigurationHelper;
    this.certificationSuiteInfoFactory = certificationSuiteInfoFactory;
    this.androidAdbUtilProvider = androidAdbUtilProvider;
    this.sessionGenDir = sessionGenDir;
    this.sessionTempDir = sessionTempDir;
    this.resUtilProvider = resUtilProvider;
    this.deviceDetailsRetriever = deviceDetailsRetriever;
    this.moblyTestLoader = moblyTestLoader;
  }

  /** Information used to create the Tradefed job. */
  @AutoValue
  public abstract static class TradefedJobInfo {

    /** Creates a {@link TradefedJobInfo}. */
    public static TradefedJobInfo of(
        JobConfig jobConfig, ImmutableMap<XtsPropertyName, String> extraJobProperties) {
      return new AutoValue_SessionRequestHandlerUtil_TradefedJobInfo(jobConfig, extraJobProperties);
    }

    /** The job config. */
    public abstract JobConfig jobConfig();

    /** Extra properties to set in the created job. */
    public abstract ImmutableMap<XtsPropertyName, String> extraJobProperties();
  }

  /**
   * Gets a list of SubDeviceSpec for the job. One SubDeviceSpec maps to one sub device used for
   * running the job as the job may need multiple devices to run the test.
   */
  private ImmutableList<SubDeviceSpec> getSubDeviceSpecListForTradefed(
      SessionRequestInfo sessionRequestInfo, int shardCount)
      throws MobileHarnessException, InterruptedException {
    if (sessionRequestInfo.isAtsServerRequest() && !sessionRequestInfo.deviceSerials().isEmpty()) {
      if (shouldEnableModuleSharding(sessionRequestInfo)) {
        StringMap dimensions =
            StringMap.newBuilder()
                .putContent(
                    "uuid",
                    String.format(
                        "regex:(%s)", Joiner.on('|').join(sessionRequestInfo.deviceSerials())))
                .build();
        return ImmutableList.of(
            SubDeviceSpec.newBuilder()
                .setType(getTradefedRequiredDeviceType(sessionRequestInfo))
                .setDimensions(dimensions)
                .build());
      } else {
        return sessionRequestInfo.deviceSerials().stream()
            .map(
                deviceSerial ->
                    SubDeviceSpec.newBuilder()
                        .setType(getTradefedRequiredDeviceType(sessionRequestInfo))
                        .setDimensions(StringMap.newBuilder().putContent("uuid", deviceSerial))
                        .build())
            .collect(toImmutableList());
      }
    }

    ImmutableMap<String, DeviceDetails> allAndroidDevices =
        deviceDetailsRetriever.getAllAndroidDevicesWithNeededDetails(sessionRequestInfo);
    logger.atInfo().log("All android devices: %s", allAndroidDevices.keySet());

    DeviceSelectionOptions.Builder optionsBuilder =
        DeviceSelectionOptions.builder()
            .setSerials(sessionRequestInfo.deviceSerials())
            .setExcludeSerials(sessionRequestInfo.excludeDeviceSerials())
            .setProductTypes(sessionRequestInfo.productTypes())
            .setDeviceProperties(sessionRequestInfo.deviceProperties());
    sessionRequestInfo.maxBatteryLevel().ifPresent(optionsBuilder::setMaxBatteryLevel);
    sessionRequestInfo.minBatteryLevel().ifPresent(optionsBuilder::setMinBatteryLevel);
    sessionRequestInfo.maxBatteryTemperature().ifPresent(optionsBuilder::setMaxBatteryTemperature);
    sessionRequestInfo.minSdkLevel().ifPresent(optionsBuilder::setMinSdkLevel);
    sessionRequestInfo.maxSdkLevel().ifPresent(optionsBuilder::setMaxSdkLevel);
    DeviceSelectionOptions deviceSelectionOptions = optionsBuilder.build();

    ImmutableSet<DeviceDetails> availableDevices =
        allAndroidDevices.values().stream()
            .filter(deviceDetails -> DeviceSelection.matches(deviceDetails, deviceSelectionOptions))
            .collect(toImmutableSet());

    if (availableDevices.isEmpty()) {
      throw MobileHarnessExceptionFactory.createUserFacingException(
          InfraErrorId.OLCS_NO_AVAILABLE_DEVICE,
          "No available device is found.",
          /* cause= */ null);
    }

    if (sessionRequestInfo.deviceSerials().isEmpty()) {
      return pickAndroidOnlineDevices(
          sessionRequestInfo,
          availableDevices.stream().map(DeviceDetails::id).collect(toImmutableSet()),
          shardCount);
    }

    return availableDevices.stream()
        .map(
            deviceDetails ->
                SubDeviceSpec.newBuilder()
                    .setType(getTradefedRequiredDeviceType(sessionRequestInfo))
                    .setDimensions(StringMap.newBuilder().putContent("id", deviceDetails.id()))
                    .build())
        .collect(toImmutableList());
  }

  private ImmutableList<SubDeviceSpec> pickAndroidOnlineDevices(
      SessionRequestInfo sessionRequestInfo,
      Set<String> allMatchAndroidOnlineDevices,
      int shardCount) {
    StringMap dimensions =
        StringMap.newBuilder()
            .putContent(
                "id",
                String.format("regex:(%s)", Joiner.on('|').join(allMatchAndroidOnlineDevices)))
            .build();
    SubDeviceSpec subDeviceSpec =
        SubDeviceSpec.newBuilder()
            .setType(getTradefedRequiredDeviceType(sessionRequestInfo))
            .setDimensions(dimensions)
            .build();
    if (shardCount <= 1 && !allMatchAndroidOnlineDevices.isEmpty()) {
      return ImmutableList.of(subDeviceSpec);
    }
    int numOfNeededDevices = min(allMatchAndroidOnlineDevices.size(), shardCount);
    return ImmutableList.copyOf(Collections.nCopies(numOfNeededDevices, subDeviceSpec));
  }

  private static String getTradefedRequiredDeviceType(SessionRequestInfo info) {
    if (Flags.instance().atsRunTfOnAndroidRealDevice.getNonNull()) {
      return ANDROID_REAL_DEVICE_TYPE;
    }
    return info.deviceType().orElse(ANDROID_DEVICE_TYPE);
  }

  public JobInfo createXtsTradefedTestJob(
      SessionRequestInfo sessionRequestInfo, TradefedJobInfo tradefedJobInfo)
      throws MobileHarnessException, InterruptedException {
    JobInfo jobInfo =
        JobInfoCreator.createJobInfo(
            tradefedJobInfo.jobConfig(),
            ImmutableList.of(),
            tradefedJobInfo.jobConfig().getGenFileDir(),
            createJobTmpDir(tradefedJobInfo.jobConfig().getName()).toString());
    addSessionClientIdToJobInfo(jobInfo, sessionRequestInfo);
    jobInfo.properties().add(Job.IS_XTS_TF_JOB, "true");
    injectCommonParams(jobInfo);
    tradefedJobInfo
        .extraJobProperties()
        .forEach((key, value) -> jobInfo.properties().add(key, value));
    printCreatedJobInfo(jobInfo, /* isTf= */ true);
    return jobInfo;
  }

  /** Gets all local tradefed modules which doesn't include the mcts modules. */
  public ImmutableSet<String> getAllLocalTradefedModules(SessionRequestInfo sessionRequestInfo) {
    Path xtsRootDir = Path.of(sessionRequestInfo.xtsRootDir());
    if (!localFileUtil.isDirExist(xtsRootDir)) {
      logger.atInfo().log(
          "xTS root dir [%s] doesn't exist, skip getting all TF modules.", xtsRootDir);
      return ImmutableSet.of();
    }
    String xtsType = sessionRequestInfo.xtsType();
    ImmutableMap<String, Configuration> configsMap =
        configurationUtil.getConfigsFromDirs(
            ImmutableList.of(XtsDirUtil.getXtsTestCasesDir(xtsRootDir, xtsType).toFile()));

    return configsMap.values().stream()
        .map(config -> config.getMetadata().getXtsModule())
        .collect(toImmutableSet());
  }

  private ImmutableSet<String> getStringSetFromResourceFile(String resPathInJar)
      throws MobileHarnessException {
    String filePath = resUtilProvider.get().getResourceFile(getClass(), resPathInJar);
    return localFileUtil.readLineListFromFile(filePath).stream()
        .map(String::trim)
        .filter(line -> !line.isEmpty())
        .collect(toImmutableSet());
  }

  /** Gets all local tradefed modules including the mcts modules from the static list. */
  public ImmutableSet<String> getStaticFullTradefedModules() throws MobileHarnessException {
    return getStringSetFromResourceFile(
        "/devtools/mobileharness/infra/controller/test/util/xtsdownloader/configs/cts_list.txt");
  }

  /** Gets all mcts modules from the static list. */
  public ImmutableSet<String> getStaticMctsModules() throws MobileHarnessException {
    return getStringSetFromResourceFile(
        "/devtools/mobileharness/infra/controller/test/util/xtsdownloader/configs/mcts_list.txt");
  }

  /**
   * Gets a list of filtered tradefed modules.
   *
   * <p>The list of modules is filtered by include/exclude filters and the given module names.
   *
   * @return a list of filtered tradefed modules if the given modules are not empty. Or an empty
   *     list if the given modules are empty.
   * @throws MobileHarnessException if no tradefed modules could satisfy the given filters.
   */
  public ImmutableList<String> getFilteredTradefedModules(SessionRequestInfo sessionRequestInfo)
      throws MobileHarnessException {
    Path xtsRootDir = Path.of(sessionRequestInfo.xtsRootDir());
    if (!localFileUtil.isDirExist(xtsRootDir)) {
      throw MobileHarnessExceptionFactory.createUserFacingException(
          InfraErrorId.OLCS_INEXISTENT_XTS_ROOT_DIR,
          String.format("Inexistent xTS root dir: %s", xtsRootDir),
          /* cause= */ null);
    }

    ImmutableSet<String> localTfModules = getAllLocalTradefedModules(sessionRequestInfo);
    ImmutableSet<String> staticTfModules = getStaticMctsModules();
    ImmutableSet<String> allTfModules =
        Stream.concat(localTfModules.stream(), staticTfModules.stream()).collect(toImmutableSet());

    // For "run retry" command handled by TF, consider the module filter as include filter.
    boolean isTfRetryWithModules =
        SessionRequestHandlerUtil.isRunRetry(sessionRequestInfo.testPlan())
            && SessionHandlerHelper.useTfRetry()
            && !sessionRequestInfo.moduleNames().isEmpty();

    ImmutableList<String> modules =
        isTfRetryWithModules ? ImmutableList.of() : sessionRequestInfo.moduleNames();
    ImmutableSet<String> givenMatchedTfModules =
        modules.isEmpty() ? allTfModules : matchModules(modules, allTfModules);

    // Filter modules by include/exclude filters.
    ImmutableList<SuiteTestFilter> includeFilters =
        Stream.concat(
                sessionRequestInfo.includeFilters().stream(),
                isTfRetryWithModules ? sessionRequestInfo.moduleNames().stream() : Stream.empty())
            .map(SuiteTestFilter::create)
            .collect(toImmutableList());
    ImmutableList<SuiteTestFilter> excludeFilters =
        sessionRequestInfo.excludeFilters().stream()
            .map(SuiteTestFilter::create)
            .collect(toImmutableList());
    ImmutableList.Builder<String> filteredModulesBuilder = ImmutableList.builder();
    for (String module : givenMatchedTfModules) {
      if (excludeFilters.stream()
          .anyMatch(
              filter -> filter.testName().isEmpty() && filter.matchModule(module, null, null))) {
        continue;
      }
      if (!includeFilters.isEmpty()
          && includeFilters.stream().noneMatch(filter -> filter.matchModuleName(module))) {
        continue;
      }
      filteredModulesBuilder.add(module);
    }
    ImmutableList<String> filteredModules = filteredModulesBuilder.build();

    if (filteredModules.isEmpty()) {
      throw MobileHarnessExceptionFactory.createUserFacingException(
          InfraErrorId.XTS_NO_MATCHED_TRADEFED_MODULES,
          String.format("No matched tradefed modules from the given modules: %s", modules),
          /* cause= */ null);
    }

    return sessionRequestInfo.moduleNames().isEmpty() ? ImmutableList.of() : filteredModules;
  }

  /** Initializes a {@link JobConfig} for a tradefed job. */
  public JobConfig initializeJobConfig(
      SessionRequestInfo sessionRequestInfo, Map<String, String> driverParams)
      throws InterruptedException, MobileHarnessException {
    String testPlan = sessionRequestInfo.testPlan();
    String xtsType = sessionRequestInfo.xtsType();
    int shardCount = sessionRequestInfo.shardCount().orElse(0);

    // TODO: migrate multi-device tests to non-TF
    int minDeviceCount = testPlan.matches(xtsType + "-multi-?device") ? 2 : 1;
    ImmutableList<SubDeviceSpec> subDeviceSpecList =
        getSubDeviceSpecListForTradefed(sessionRequestInfo, max(shardCount, minDeviceCount));
    if (subDeviceSpecList.size() < minDeviceCount) {
      throw MobileHarnessExceptionFactory.createUserFacingException(
          InfraErrorId.OLCS_NO_ENOUGH_MATCHED_DEVICES,
          "Found no enough devices to create the job config.",
          /* cause= */ null);
    }

    Duration jobTimeout =
        sessionRequestInfo.jobTimeout().isZero()
            ? DEFAULT_TRADEFED_JOB_TIMEOUT
            : sessionRequestInfo.jobTimeout();
    Duration testTimeout = calculateTestTimeout(jobTimeout);
    Duration startTimeout =
        sessionRequestInfo.startTimeout().isZero()
            ? DEFAULT_TRADEFED_START_TIMEOUT
            : sessionRequestInfo.startTimeout();

    String name = "xts-tradefed-test-job";
    Path jobGenDir = createJobGenDir(name);
    JobConfig jobConfig =
        JobConfig.newBuilder()
            .setName(name)
            .setExecMode("local")
            .setJobTimeoutSec(saturatedCast(jobTimeout.toSeconds()))
            .setTestTimeoutSec(saturatedCast(testTimeout.toSeconds()))
            .setStartTimeoutSec(saturatedCast(startTimeout.toSeconds()))
            .setPriority(Priority.HIGH)
            .setTestAttempts(1)
            .setTests(
                StringList.newBuilder().addContent(String.format("xts-tradefed-test-%s", testPlan)))
            .setDevice(DeviceList.newBuilder().addAllSubDeviceSpec(subDeviceSpecList))
            .setDriver(
                Driver.newBuilder()
                    .setName("XtsTradefedTest")
                    .setParam(new Gson().toJson(driverParams)))
            .setGenFileDir(jobGenDir.toString())
            .build();
    logger.atInfo().log("XtsTradefedTest job config: %s", shortDebugString(jobConfig));
    return jobConfig;
  }

  /**
   * Adds non-tradefed module info to the {@code SessionRequestInfo}. This is required step before
   * creating non-tradefed jobs or checking if it can do so.
   *
   * @return an updated {@code SessionRequestInfo}
   */
  public SessionRequestInfo addNonTradefedModuleInfo(SessionRequestInfo sessionRequestInfo)
      throws MobileHarnessException, InterruptedException {
    SessionRequestInfo.Builder updatedSessionRequestInfo = sessionRequestInfo.toBuilder();

    Path xtsRootDir = Path.of(sessionRequestInfo.xtsRootDir());
    if (!localFileUtil.isDirExist(xtsRootDir)) {
      logger.atInfo().log(
          "xTS root dir [%s] doesn't exist, skip creating non-tradefed jobs.", xtsRootDir);
      return updatedSessionRequestInfo.build();
    }

    String xtsType = sessionRequestInfo.xtsType();
    ImmutableMap<String, Configuration> configsMap =
        configurationUtil.getConfigsV2FromDirs(
            ImmutableList.of(XtsDirUtil.getXtsTestCasesDir(xtsRootDir, xtsType).toFile()));
    updatedSessionRequestInfo.setV2ConfigsMap(configsMap);

    // Gets expanded modules with abi and module parameters (if any).
    TestSuiteHelper testSuiteHelper =
        getTestSuiteHelper(xtsRootDir.toString(), xtsType, sessionRequestInfo);
    updatedSessionRequestInfo.setExpandedModules(
        ImmutableMap.copyOf(
            testSuiteHelper.loadTests(getDeviceInfo(sessionRequestInfo).orElse(null))));

    ImmutableList<String> modules = sessionRequestInfo.moduleNames();
    ImmutableSet<String> allNonTfModules = getNonTfModules(configsMap);
    updatedSessionRequestInfo.setGivenMatchedNonTfModules(matchModules(modules, allNonTfModules));
    return updatedSessionRequestInfo.build();
  }

  public static boolean shouldEnableModuleSharding(SessionRequestInfo sessionRequestInfo) {
    return sessionRequestInfo.shardingMode().equals(ShardingMode.MODULE)
        && sessionRequestInfo.testName().isEmpty()
        && !SessionRequestHandlerUtil.isRunRetry(sessionRequestInfo.testPlan());
  }

  private Optional<DeviceInfo> getDeviceInfo(SessionRequestInfo sessionRequestInfo)
      throws MobileHarnessException, InterruptedException {
    Optional<DeviceInfo> deviceInfo =
        Flags.instance().enableAtsMode.getNonNull()
            ? getDeviceInfoFromMaster(sessionRequestInfo)
            : getDeviceInfoFromLocal(sessionRequestInfo);

    logger.atInfo().log("Obtained device info: %s", deviceInfo.orElse(null));
    return deviceInfo;
  }

  private Optional<DeviceInfo> getDeviceInfoFromMaster(SessionRequestInfo sessionRequestInfo)
      throws MobileHarnessException, InterruptedException {
    DeviceQueryResult queryResult;
    try {
      queryResult = deviceQuerier.queryDevice(DeviceQueryFilter.getDefaultInstance());
    } catch (com.google.wireless.qa.mobileharness.shared.MobileHarnessException e) {
      throw new MobileHarnessException(
          InfraErrorId.ATSC_RUN_COMMAND_QUERY_DEVICE_ERROR, "Failed to query device", e);
    }

    Optional<com.google.wireless.qa.mobileharness.shared.proto.query.DeviceQuery.DeviceInfo>
        queryDeviceInfo =
            queryResult.getDeviceInfoList().stream()
                .filter(
                    deviceInfo -> {
                      if (sessionRequestInfo.deviceSerials().isEmpty()) {
                        return true;
                      } else {
                        return sessionRequestInfo.deviceSerials().contains(deviceInfo.getId())
                            && deviceInfo.getTypeList().stream()
                                .anyMatch(deviceType -> deviceType.startsWith("Android"));
                      }
                    })
                .findFirst();
    if (queryDeviceInfo.isEmpty()) {
      logger
          .atInfo()
          .with(IMPORTANCE, IMPORTANT)
          .log("No match Android devices, return empty device info.");
      return Optional.empty();
    }

    return Optional.of(
        DeviceInfo.builder()
            .setDeviceId(queryDeviceInfo.get().getId())
            .setSupportedAbiList(
                queryDeviceInfo.get().getDimensionList().stream()
                    .filter(
                        dimension ->
                            dimension
                                .getName()
                                .equals(Ascii.toLowerCase(AndroidProperty.ABILIST.name())))
                    .map(DeviceQuery.Dimension::getValue)
                    .findFirst()
                    .orElse(""))
            .setSupportedAbi(
                queryDeviceInfo.get().getDimensionList().stream()
                    .filter(
                        dimension ->
                            dimension
                                .getName()
                                .equals(Ascii.toLowerCase(AndroidProperty.ABI.name())))
                    .map(DeviceQuery.Dimension::getValue)
                    .findFirst()
                    .orElse(""))
            .build());
  }

  private Optional<DeviceInfo> getDeviceInfoFromLocal(SessionRequestInfo sessionRequestInfo)
      throws MobileHarnessException, InterruptedException {
    ImmutableSet<String> allLocalAndroidDevices =
        deviceDetailsRetriever.getAllAndroidDevicesWithNeededDetails(sessionRequestInfo).keySet();
    if (!allLocalAndroidDevices.isEmpty()) {
      Optional<String> deviceSerial;
      if (sessionRequestInfo.deviceSerials().isEmpty()) {
        deviceSerial = allLocalAndroidDevices.stream().findFirst();
      } else {
        deviceSerial =
            sessionRequestInfo.deviceSerials().stream()
                .filter(allLocalAndroidDevices::contains)
                .findFirst();
      }
      if (deviceSerial.isEmpty()) {
        logger
            .atInfo()
            .with(IMPORTANCE, IMPORTANT)
            .log(
                "No match local Android devices, return empty device info. Detected all local"
                    + " Android devices: %s",
                allLocalAndroidDevices);
        return Optional.empty();
      }

      String abiList =
          androidAdbUtilProvider.get().getProperty(deviceSerial.get(), AndroidProperty.ABILIST);
      String abi =
          androidAdbUtilProvider.get().getProperty(deviceSerial.get(), AndroidProperty.ABI);
      return Optional.of(
          DeviceInfo.builder()
              .setDeviceId(deviceSerial.get())
              .setSupportedAbiList(abiList)
              .setSupportedAbi(abi)
              .build());
    }
    logger
        .atInfo()
        .with(IMPORTANCE, IMPORTANT)
        .log("Detected no local Android devices, return empty device info.");
    return Optional.empty();
  }

  /**
   * Checks if non-tradefed jobs can be created based on the {@code SessionRequestInfo}.
   *
   * @return true if non-tradefed jobs can be created.
   */
  public boolean canCreateNonTradefedJobs(SessionRequestInfo sessionRequestInfo) {
    if (isRunRetry(sessionRequestInfo.testPlan())) {
      return true;
    }
    boolean noGivenModuleForNonTf =
        !sessionRequestInfo.moduleNames().isEmpty()
            && sessionRequestInfo.givenMatchedNonTfModules().isEmpty();
    return !noGivenModuleForNonTf;
  }

  /**
   * Creates non-tradefed jobs based on the {@code SessionRequestInfo}.
   *
   * @return a list of added non-tradefed jobInfos.
   */
  public ImmutableList<JobInfo> createXtsNonTradefedJobs(
      SessionRequestInfo sessionRequestInfo,
      TestPlanParser.TestPlanFilter testPlanFilter,
      @Nullable SubPlan subPlan,
      Map<XtsPropertyName, String> extraJobProperties)
      throws MobileHarnessException, InterruptedException {
    ImmutableSet<String> givenMatchedNonTfModules = sessionRequestInfo.givenMatchedNonTfModules();
    ImmutableList.Builder<JobInfo> jobInfos = ImmutableList.builder();

    ImmutableList<String> androidDeviceSerials = sessionRequestInfo.deviceSerials();

    ImmutableMap<String, String> moduleNameToConfigFilePathMap =
        sessionRequestInfo.v2ConfigsMap().entrySet().stream()
            .collect(toImmutableMap(e -> e.getValue().getMetadata().getXtsModule(), Entry::getKey));

    ImmutableList<SuiteTestFilter> includeFilters;
    if (subPlan == null) {
      includeFilters =
          Stream.concat(
                  sessionRequestInfo.includeFilters().stream(),
                  testPlanFilter.includeFilters().stream())
              .map(SuiteTestFilter::create)
              .collect(toImmutableList());
    } else {
      includeFilters =
          subPlan.getNonTfIncludeFiltersMultimap().entries().stream()
              .map(
                  e ->
                      SuiteTestFilter.create(
                          e.getKey()
                              + (e.getValue().equals(SubPlan.ALL_TESTS_IN_MODULE)
                                  ? ""
                                  : " " + e.getValue())))
              .collect(toImmutableList());
      logger.atInfo().log("Include filters for Non-TF retry/subplan run: %s", includeFilters);
    }

    ImmutableList<SuiteTestFilter> excludeFilters =
        Stream.concat(
                sessionRequestInfo.excludeFilters().stream(),
                testPlanFilter.excludeFilters().stream())
            .map(SuiteTestFilter::create)
            .collect(toImmutableList());
    if (subPlan != null) {
      ImmutableList<SuiteTestFilter> subplanExcludeFilters =
          subPlan.getNonTfExcludeFiltersMultimap().entries().stream()
              .map(
                  e ->
                      e.getKey()
                          + (e.getValue().equals(SubPlan.ALL_TESTS_IN_MODULE)
                              ? ""
                              : " " + e.getValue()))
              .map(SuiteTestFilter::create)
              .collect(toImmutableList());
      excludeFilters =
          Stream.concat(excludeFilters.stream(), subplanExcludeFilters.stream())
              .collect(toImmutableList());
    }

    Duration jobTimeout =
        sessionRequestInfo.jobTimeout().isZero()
            ? DEFAULT_NON_TRADEFED_JOB_TIMEOUT
            : sessionRequestInfo.jobTimeout();
    Duration testTimeout = calculateTestTimeout(jobTimeout);
    Duration startTimeout =
        sessionRequestInfo.startTimeout().isZero()
            ? DEFAULT_NON_TRADEFED_START_TIMEOUT
            : sessionRequestInfo.startTimeout();

    Path xtsRootDir = Path.of(sessionRequestInfo.xtsRootDir());
    String xtsType = sessionRequestInfo.xtsType();
    // Reads DeviceConfigurations text proto.
    Path xtsDeviceConfigFile = getXtsDeviceConfigFilePath(xtsRootDir, xtsType);
    DeviceConfigurations xtsDeviceConfig = readXtsDeviceConfigFile(xtsDeviceConfigFile);
    ImmutableMap<String, ModuleDeviceConfiguration> moduleDeviceConfigurations =
        groupXtsDeviceConfig(xtsDeviceConfig);

    for (Entry<String, Configuration> entry :
        sessionRequestInfo.expandedModules().entrySet().stream()
            .filter(e -> e.getValue().getMetadata().getIsConfigV2())
            .collect(toImmutableList())) {
      String originalModuleName = entry.getValue().getMetadata().getXtsModule();
      String expandedModuleName = entry.getKey();
      // If it has a subplan(either from the retry command or the subplan command), do a early check
      // for whether the module should be ran
      if (subPlan != null
          && !subPlan.getNonTfIncludeFiltersMultimap().containsKey(expandedModuleName)) {
        continue;
      }
      if (givenMatchedNonTfModules.isEmpty()
          || givenMatchedNonTfModules.contains(originalModuleName)) {
        // Gets module abi
        String moduleAbi = getModuleAbi(expandedModuleName).orElse(null);
        // Gets module parameter
        String moduleParameter = getModuleParameter(expandedModuleName).orElse(null);
        ImmutableList.Builder<String> matchedTestCasesBuilder = ImmutableList.builder();

        // Filters the module by include-filter and exclude-filter.
        if (excludeFilters.stream()
            .anyMatch(
                excludeFilter ->
                    excludeFilter.matchModule(originalModuleName, moduleAbi, moduleParameter)
                        && excludeFilter.testName().isEmpty())) {
          continue;
        }
        if (sessionRequestInfo.testName().isPresent()) {
          String parsedTestName = parseTestName(sessionRequestInfo.testName().get());
          if (!parsedTestName.isEmpty()) {
            matchedTestCasesBuilder.add(parsedTestName);
          } else {
            continue;
          }
        } else if (!includeFilters.isEmpty()) {
          boolean matched = false;
          for (SuiteTestFilter filter : includeFilters) {
            if (filter.matchModule(originalModuleName, moduleAbi, moduleParameter)) {
              matched = true;
              String parsedTestName = parseTestName(filter.testName().orElse(null));
              if (!parsedTestName.isEmpty()) {
                matchedTestCasesBuilder.add(parsedTestName);
              }
            }
          }
          if (!matched) {
            continue;
          }
        }

        // Get excluded test names in current module.
        ImmutableList<String> matchedTestCases = matchedTestCasesBuilder.build();
        ImmutableList<String> excludedTestNames =
            excludeFilters.stream()
                .filter(
                    excludeFilter ->
                        excludeFilter.matchModule(originalModuleName, moduleAbi, moduleParameter))
                .filter(excludeFilter -> excludeFilter.testName().isPresent())
                .map(excludeFilter -> parseTestName(excludeFilter.testName().get()))
                .collect(toImmutableList());
        if (!excludedTestNames.isEmpty()) {
          if (matchedTestCases.isEmpty()) {
            try {
              matchedTestCases =
                  moblyTestLoader.getTestNamesInModule(
                      Path.of(
                          requireNonNull(moduleNameToConfigFilePathMap.get(originalModuleName))),
                      entry.getValue());
            } catch (MobileHarnessException e) {
              logger
                  .atWarning()
                  .with(IMPORTANCE, IMPORTANT)
                  .withCause(e)
                  .log(
                      "Failed to get all test names from module %s. Will run all test cases in the"
                          + " module",
                      originalModuleName);
            }
          }
          matchedTestCases =
              matchedTestCases.stream()
                  .filter(testName -> !excludedTestNames.contains(testName))
                  .collect(toImmutableList());
          if (matchedTestCases.isEmpty()) {
            logger.atInfo().log(
                "Test case exclude filters filtered every test cases: %s.\n"
                    + "No job created for this module: %s.",
                excludedTestNames, expandedModuleName);
            continue;
          }
        }

        JobInfo jobInfo =
            createXtsNonTradefedJob(
                xtsRootDir,
                xtsType,
                sessionRequestInfo.testPlan(),
                subPlan == null ? null : subPlan.getPreviousSessionXtsTestPlan(),
                Path.of(requireNonNull(moduleNameToConfigFilePathMap.get(originalModuleName))),
                entry.getValue(),
                moduleDeviceConfigurations.getOrDefault(
                    originalModuleName, /* defaultValue= */ null),
                expandedModuleName,
                moduleAbi,
                moduleParameter,
                matchedTestCases,
                jobTimeout,
                testTimeout,
                startTimeout,
                isSkipDeviceInfo(sessionRequestInfo, subPlan));
        // TODO: correct the device serial dimension.
        if (!androidDeviceSerials.isEmpty()) {
          String serialDimensionValue =
              String.format("regex:(%s)", Joiner.on('|').join(androidDeviceSerials));
          jobInfo
              .subDeviceSpecs()
              .getAllSubDevices()
              .forEach(
                  subDeviceSpec ->
                      subDeviceSpec
                          .deviceRequirement()
                          .dimensions()
                          .add("id", serialDimensionValue));
        }
        addSessionClientIdToJobInfo(jobInfo, sessionRequestInfo);
        extraJobProperties.forEach((key, value) -> jobInfo.properties().add(key, value));
        printCreatedJobInfo(jobInfo, /* isTf= */ false);
        jobInfos.add(jobInfo);
      }
    }

    return jobInfos.build();
  }

  private DeviceConfigurations readXtsDeviceConfigFile(Path xtsDeviceConfigFile)
      throws MobileHarnessException {
    if (!localFileUtil.isFileExist(xtsDeviceConfigFile)) {
      logger.atWarning()
          // TODO: Enables with(IMPORTANCE, IMPORTANT) after the feature is ready
          .log("Device config file [%s] not found", xtsDeviceConfigFile);
      return DeviceConfigurations.getDefaultInstance();
    }
    try {
      String fileContent = localFileUtil.readFile(xtsDeviceConfigFile);
      return ProtoTextFormat.parse(fileContent, DeviceConfigurations.class);
    } catch (MobileHarnessException | ParseException e) {
      throw MobileHarnessExceptionFactory.createUserFacingException(
          InfraErrorId.XTS_DEVICE_CONFIG_FILE_PARSE_ERROR,
          String.format("Failed to read device config file [%s]", xtsDeviceConfigFile),
          e);
    }
  }

  /** Key is original module name. Value is configured but unvalidated module device config. */
  private static ImmutableMap<String, ModuleDeviceConfiguration> groupXtsDeviceConfig(
      DeviceConfigurations xtsDeviceConfig) throws MobileHarnessException {
    ImmutableMap.Builder<String, ModuleDeviceConfiguration> result = ImmutableMap.builder();
    for (ModuleDeviceConfiguration moduleDeviceConfiguration : xtsDeviceConfig.getModuleList()) {
      result.put(moduleDeviceConfiguration.getName(), moduleDeviceConfiguration);
    }
    try {
      return result.buildOrThrow();
    } catch (IllegalArgumentException e) {
      throw MobileHarnessExceptionFactory.createUserFacingException(
          InfraErrorId.XTS_DEVICE_CONFIG_FILE_VALIDATE_ERROR, "Invalid device config", e);
    }
  }

  private static Path getXtsDeviceConfigFilePath(Path xtsRootDir, String xtsType) {
    // TODO: Support specifying path from ATS console.
    return XtsDirUtil.getXtsToolsDir(xtsRootDir, xtsType)
        .resolve("device_configurations.textproto");
  }

  /**
   * TestName is set with pattern TestClassName#TestCaseName while Mobly needs the test case name
   * without the test class name.
   */
  private String parseTestName(@Nullable String testName) {
    if (testName == null) {
      return "";
    }
    List<String> list = Splitter.on('#').trimResults().omitEmptyStrings().splitToList(testName);
    if (list.size() == 2) {
      return list.get(1);
    }
    logger
        .atWarning()
        .with(IMPORTANCE, IMPORTANT)
        .log("Failed to parse test case name from [%s].", testName);
    return "";
  }

  private JobInfo createXtsNonTradefedJob(
      Path xtsRootDir,
      String xtsType,
      String testPlan,
      @Nullable String previousSessionTestPlan,
      Path moduleConfigPath,
      Configuration moduleConfig,
      @Nullable ModuleDeviceConfiguration moduleDeviceConfig,
      String expandedModuleName,
      @Nullable String moduleAbi,
      @Nullable String moduleParameter,
      ImmutableList<String> matchedTestCases,
      Duration jobTimeout,
      Duration testTimeout,
      Duration startTimeout,
      boolean skipDeviceInfo)
      throws MobileHarnessException, InterruptedException {
    JobInfo jobInfo =
        createBaseXtsNonTradefedJob(
            moduleConfig, expandedModuleName, jobTimeout, testTimeout, startTimeout);

    ImmutableList<File> fileDepDirs =
        ImmutableList.of(
            moduleConfigPath.getParent().toFile(),
            XtsDirUtil.getXtsTestCasesDir(xtsRootDir, xtsType).toFile());

    moduleConfigurationHelper.updateJobInfo(jobInfo, moduleConfig, moduleDeviceConfig, fileDepDirs);
    jobInfo.properties().add(Job.IS_XTS_NON_TF_JOB, "true");
    jobInfo
        .properties()
        .add(SessionHandlerHelper.XTS_MODULE_NAME_PROP, moduleConfig.getMetadata().getXtsModule());
    if (moduleAbi != null) {
      jobInfo.properties().add(SessionHandlerHelper.XTS_MODULE_ABI_PROP, moduleAbi);
    }
    if (moduleParameter != null) {
      jobInfo.properties().add(SessionHandlerHelper.XTS_MODULE_PARAMETER_PROP, moduleParameter);
    }
    jobInfo.properties().add(Job.SKIP_COLLECTING_DEVICE_INFO, Boolean.toString(skipDeviceInfo));
    if (!matchedTestCases.isEmpty()) {
      jobInfo.params().add(MOBLY_TEST_SELECTOR_KEY, Joiner.on(" ").join(matchedTestCases));
    }
    jobInfo.params().add(PARAM_RUN_CERTIFICATION_TEST_SUITE, "true");
    jobInfo
        .params()
        .add(
            PARAM_XTS_SUITE_INFO,
            generateXtsSuiteInfoMap(
                xtsRootDir.toAbsolutePath().toString(),
                xtsType,
                previousSessionTestPlan != null ? previousSessionTestPlan : testPlan));

    // TODO: Add multi hosts mode support.
    jobInfo
        .params()
        .add("xts_test_dir", XtsDirUtil.getXtsTestCasesDir(xtsRootDir, xtsType).toString());
    injectCommonParams(jobInfo);
    return jobInfo;
  }

  /** Injects common params to the job info for both tradefed and non-tradefed jobs. */
  private void injectCommonParams(JobInfo jobInfo) {
    // Skip to clear gservice flag overrides.
    jobInfo.params().add(PARAM_CLEAR_GSERVICES_OVERRIDES, "false");
    // Skip to check installed gms core version.
    jobInfo.params().add(PARAM_CHECK_INSTALLED_GMS_CORE_VERSION, "false");
  }

  private String generateXtsSuiteInfoMap(String xtsRootDir, String xtsType, String testPlan) {
    Map<String, String> xtsSuiteInfoMap =
        certificationSuiteInfoFactory.generateSuiteInfoMap(xtsRootDir, xtsType, testPlan);
    return Joiner.on(",").withKeyValueSeparator("=").join(xtsSuiteInfoMap);
  }

  private JobInfo createBaseXtsNonTradefedJob(
      Configuration moduleConfig,
      String expandedModuleName,
      Duration jobTimeout,
      Duration testTimeout,
      Duration startTimeout)
      throws MobileHarnessException, InterruptedException {
    List<Device> moduleDevices = moduleConfig.getDevicesList();
    if (moduleDevices.isEmpty()) {
      throw MobileHarnessExceptionFactory.createUserFacingException(
          InfraErrorId.XTS_NO_DEVICE_SPEC_DEFINED,
          String.format(
              "Found no devices to create the job config for xts non-tradefed job with module"
                  + " '%s'.",
              expandedModuleName),
          /* cause= */ null);
    }

    List<SubDeviceSpec> subDeviceSpecList = new ArrayList<>();
    for (Device device : moduleDevices) {
      if (device.getName().isEmpty()) {
        throw MobileHarnessExceptionFactory.createUserFacingException(
            InfraErrorId.XTS_ILLEGAL_DEVICE_SPEC,
            String.format(
                "Device name is missing in a <device> in module '%s'", expandedModuleName),
            /* cause= */ null);
      } else {
        subDeviceSpecList.add(SubDeviceSpec.newBuilder().setType(device.getName()).build());
      }
    }

    String name =
        String.format("xts-mobly-aosp-package-job-%s", expandedModuleName.replace(' ', '_'));
    Path jobGenDir = createJobGenDir(name);
    Path jobTmpDir = createJobTmpDir(name);
    JobConfig jobConfig =
        JobConfig.newBuilder()
            .setName(name)
            .setExecMode("local")
            .setJobTimeoutSec(saturatedCast(jobTimeout.toSeconds()))
            .setTestTimeoutSec(saturatedCast(testTimeout.toSeconds()))
            .setStartTimeoutSec(saturatedCast(startTimeout.toSeconds()))
            .setPriority(Priority.HIGH)
            .setTestAttempts(1)
            .setTests(
                StringList.newBuilder()
                    .addContent(
                        String.format(
                            "xts-mobly-aosp-package-test-%s",
                            expandedModuleName.replace(' ', '_'))))
            .setDevice(DeviceList.newBuilder().addAllSubDeviceSpec(subDeviceSpecList))
            .setDriver(Driver.newBuilder().setName("MoblyAospPackageTest"))
            .setGenFileDir(jobGenDir.toString())
            .build();
    logger.atInfo().log(
        "Non-tradefed job base config for module '%s': %s",
        expandedModuleName, shortDebugString(jobConfig));

    return JobInfoCreator.createJobInfo(
        jobConfig, ImmutableList.of(), jobGenDir.toString(), jobTmpDir.toString());
  }

  private Path createJobGenDir(String jobName) {
    return sessionGenDir.resolve(
        String.format("job_gen_%s_%s", encodeJobName(jobName), UUID.randomUUID()));
  }

  private Path createJobTmpDir(String jobName) {
    return sessionTempDir.resolve(
        String.format("job_tmp_%s_%s", encodeJobName(jobName), UUID.randomUUID()));
  }

  private static String encodeJobName(String jobName) {
    return URLEncoder.encode(jobName, UTF_8);
  }

  @VisibleForTesting
  TestSuiteHelper getTestSuiteHelper(
      String xtsRootDir, String xtsType, SessionRequestInfo sessionRequestInfo) {
    TestSuiteHelper testSuiteHelper = new TestSuiteHelper(xtsRootDir, xtsType);
    testSuiteHelper.setParameterizedModules(sessionRequestInfo.enableModuleParameter());
    testSuiteHelper.setOptionalParameterizedModules(
        sessionRequestInfo.enableModuleOptionalParameter());
    return testSuiteHelper;
  }

  private Optional<String> getModuleAbi(String expandedModuleName) {
    String abi = AbiUtil.parseAbi(expandedModuleName);
    return isNullOrEmpty(abi) ? Optional.empty() : Optional.of(abi);
  }

  private Optional<String> getModuleParameter(String expandedModuleName) {
    Matcher matcher = MODULE_PARAMETER_PATTERN.matcher(expandedModuleName);
    return matcher.find() ? Optional.of(matcher.group("moduleParam")) : Optional.empty();
  }

  public static boolean isRunRetry(String testPlan) {
    return Ascii.equalsIgnoreCase(testPlan, "retry");
  }

  private static ImmutableSet<String> matchModules(List<String> filters, Set<String> allModules)
      throws MobileHarnessException {
    ImmutableSet.Builder<String> modules = ImmutableSet.builder();
    for (String filter : filters) {
      matchModule(filter, allModules).ifPresent(modules::add);
    }
    return modules.build();
  }

  private static Optional<String> matchModule(String filter, Set<String> allModules)
      throws MobileHarnessException {
    // Do exact match first
    if (allModules.contains(filter)) {
      return Optional.of(filter);
    }

    Pattern pattern = Pattern.compile(filter);
    ImmutableList<String> modules =
        allModules.stream()
            .filter(module -> pattern.matcher(module).find())
            .collect(toImmutableList());
    if (modules.isEmpty()) {
      return Optional.empty();
    } else if (modules.size() == 1) {
      return Optional.of(modules.get(0));
    } else {
      throw new MobileHarnessException(
          InfraErrorId.ATSC_RUN_COMMAND_MULTIPLE_MODULES_FOUND_ERROR,
          String.format(
              "Multiple modules found matching %s:\n%s\nWhich one did you mean?\n",
              filter, String.join("\n", modules)));
    }
  }

  /** Gets all the non-tradefed modules from the config map. */
  public static ImmutableSet<String> getNonTfModules(
      ImmutableMap<String, Configuration> configsMap) {
    return configsMap.values().stream()
        .map(config -> config.getMetadata().getXtsModule())
        .collect(toImmutableSet());
  }

  private static void addSessionClientIdToJobInfo(
      JobInfo jobInfo, SessionRequestInfo sessionRequestInfo) {
    sessionRequestInfo
        .sessionClientId()
        .ifPresent(
            sessionClientId -> jobInfo.params().add("olc_session_client_id", sessionClientId));
  }

  private static void printCreatedJobInfo(JobInfo jobInfo, boolean isTf) {
    logger.atInfo().log(
        "%s job info for %s:\n\"params\": %s\n\"subDeviceSpecs\": %s\n\"files\": %s",
        isTf ? "Tradefed" : "Non-tradefed",
        jobInfo.locator().getName(),
        jobInfo.params().getAll(),
        jobInfo.subDeviceSpecs(),
        jobInfo.files().getAll());
  }

  private static Duration calculateTestTimeout(Duration jobTimeout) {
    return jobTimeout.compareTo(JOB_TEST_TIMEOUT_DIFF.multipliedBy(2L)) < 0
        ? jobTimeout.dividedBy(2L)
        : jobTimeout.minus(JOB_TEST_TIMEOUT_DIFF);
  }

  private static boolean isSkipDeviceInfo(
      SessionRequestInfo sessionRequestInfo, @Nullable SubPlan subPlan) {
    return sessionRequestInfo.skipDeviceInfo().orElse(false)
        || (isRunRetry(sessionRequestInfo.testPlan())
            && subPlan != null
            && subPlan.getPreviousSessionDeviceBuildFingerprint().orElse("").isEmpty());
  }
}
