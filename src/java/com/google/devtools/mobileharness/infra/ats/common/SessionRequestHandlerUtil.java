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
import static com.google.protobuf.TextFormat.shortDebugString;
import static java.lang.Math.min;
import static java.util.Objects.requireNonNull;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.common.flogger.FluentLogger;
import com.google.devtools.mobileharness.api.model.error.InfraErrorId;
import com.google.devtools.mobileharness.api.model.error.MobileHarnessException;
import com.google.devtools.mobileharness.infra.ats.common.proto.XtsCommonProto.XtsType;
import com.google.devtools.mobileharness.infra.ats.console.result.proto.ReportProto.Result;
import com.google.devtools.mobileharness.infra.ats.console.result.report.CertificationSuiteInfoFactory;
import com.google.devtools.mobileharness.infra.ats.console.result.report.CompatibilityReportCreator;
import com.google.devtools.mobileharness.infra.ats.console.result.report.CompatibilityReportMerger;
import com.google.devtools.mobileharness.infra.ats.console.result.report.CompatibilityReportParser;
import com.google.devtools.mobileharness.infra.ats.console.result.report.MoblyReportParser.MoblyReportInfo;
import com.google.devtools.mobileharness.infra.client.api.controller.device.DeviceQuerier;
import com.google.devtools.mobileharness.platform.android.sdktool.adb.AndroidAdbInternalUtil;
import com.google.devtools.mobileharness.platform.android.sdktool.adb.DeviceState;
import com.google.devtools.mobileharness.platform.android.xts.common.util.AbiUtil;
import com.google.devtools.mobileharness.platform.android.xts.config.ConfigurationUtil;
import com.google.devtools.mobileharness.platform.android.xts.config.ModuleConfigurationHelper;
import com.google.devtools.mobileharness.platform.android.xts.config.proto.ConfigurationProto.Configuration;
import com.google.devtools.mobileharness.platform.android.xts.config.proto.ConfigurationProto.Device;
import com.google.devtools.mobileharness.platform.android.xts.suite.TestSuiteHelper;
import com.google.devtools.mobileharness.shared.util.file.local.LocalFileUtil;
import com.google.devtools.mobileharness.shared.util.flags.Flags;
import com.google.devtools.mobileharness.shared.util.jobconfig.JobInfoCreator;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.Gson;
import com.google.inject.Provider;
import com.google.wireless.qa.mobileharness.shared.constant.PropertyName.Test;
import com.google.wireless.qa.mobileharness.shared.model.job.JobInfo;
import com.google.wireless.qa.mobileharness.shared.model.job.TestInfo;
import com.google.wireless.qa.mobileharness.shared.proto.Job.Priority;
import com.google.wireless.qa.mobileharness.shared.proto.JobConfig;
import com.google.wireless.qa.mobileharness.shared.proto.JobConfig.DeviceList;
import com.google.wireless.qa.mobileharness.shared.proto.JobConfig.Driver;
import com.google.wireless.qa.mobileharness.shared.proto.JobConfig.StringList;
import com.google.wireless.qa.mobileharness.shared.proto.JobConfig.StringMap;
import com.google.wireless.qa.mobileharness.shared.proto.JobConfig.SubDeviceSpec;
import com.google.wireless.qa.mobileharness.shared.proto.query.DeviceQuery.DeviceInfo;
import com.google.wireless.qa.mobileharness.shared.proto.query.DeviceQuery.DeviceQueryFilter;
import com.google.wireless.qa.mobileharness.shared.proto.query.DeviceQuery.DeviceQueryResult;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.inject.Inject;

/** Helper class for ATS applications to create job config. */
public class SessionRequestHandlerUtil {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static final String XTS_TF_JOB_PROP = "xts-tradefed-job";
  public static final String XTS_NON_TF_JOB_PROP = "xts-non-tradefed-job";
  public static final String XTS_MODULE_NAME_PROP = "xts-module-name";
  public static final String XTS_MODULE_ABI_PROP = "xts-module-abi";
  public static final String XTS_MODULE_PARAMETER_PROP = "xts-module-parameter";

  private static final String ANDROID_REAL_DEVICE_TYPE = "AndroidRealDevice";
  private static final String ANDROID_DEVICE_TYPE = "AndroidDevice";
  private static final Pattern MODULE_PARAMETER_PATTERN =
      Pattern.compile(".*\\[(?<moduleParam>.*)]$");
  public static final String TEST_RESULT_XML_FILE_NAME = "test_result.xml";
  private static final ImmutableSet<String> MOBLY_TEST_RESULT_FILE_NAMES =
      ImmutableSet.of(
          "test_summary.yaml",
          "device_build_fingerprint.txt",
          "mobly_run_build_attributes.textproto",
          "mobly_run_result_attributes.textproto");

  private final DeviceQuerier deviceQuerier;
  private final LocalFileUtil localFileUtil;
  private final ConfigurationUtil configurationUtil;
  private final ModuleConfigurationHelper moduleConfigurationHelper;
  private final CertificationSuiteInfoFactory certificationSuiteInfoFactory;
  private final CompatibilityReportMerger compatibilityReportMerger;
  private final CompatibilityReportCreator reportCreator;
  private final CompatibilityReportParser compatibilityReportParser;
  private final Provider<AndroidAdbInternalUtil> androidAdbInternalUtilProvider;

  @Inject
  SessionRequestHandlerUtil(
      DeviceQuerier deviceQuerier,
      LocalFileUtil localFileUtil,
      ConfigurationUtil configurationUtil,
      ModuleConfigurationHelper moduleConfigurationHelper,
      CertificationSuiteInfoFactory certificationSuiteInfoFactory,
      CompatibilityReportMerger compatibilityReportMerger,
      CompatibilityReportCreator reportCreator,
      CompatibilityReportParser compatibilityReportParser,
      Provider<AndroidAdbInternalUtil> androidAdbInternalUtilProvider) {
    this.deviceQuerier = deviceQuerier;
    this.localFileUtil = localFileUtil;
    this.configurationUtil = configurationUtil;
    this.moduleConfigurationHelper = moduleConfigurationHelper;
    this.certificationSuiteInfoFactory = certificationSuiteInfoFactory;
    this.compatibilityReportMerger = compatibilityReportMerger;
    this.reportCreator = reportCreator;
    this.compatibilityReportParser = compatibilityReportParser;
    this.androidAdbInternalUtilProvider = androidAdbInternalUtilProvider;
  }

  /**
   * Data holder used to create jobInfo. Data comes from session request handlers, like
   * RunCommandHandler.
   */
  @AutoValue
  public abstract static class SessionRequestInfo {
    public abstract String testPlan();

    public abstract String xtsRootDir();

    public abstract Optional<String> androidXtsZip();

    public abstract ImmutableList<String> deviceSerials();

    public abstract ImmutableList<String> moduleNames();

    public abstract Optional<Integer> shardCount();

    public abstract ImmutableList<String> includeFilters();

    public abstract ImmutableList<String> excludeFilters();

    public abstract ImmutableList<String> extraArgs();

    public abstract XtsType xtsType();

    public abstract Optional<String> pythonPkgIndexUrl();

    public abstract ImmutableSet<String> givenMatchedNonTfModules();

    public abstract ImmutableMap<String, Configuration> v2ConfigsMap();

    public abstract ImmutableMap<String, Configuration> expandedModules();

    public abstract boolean enableModuleParameter();

    public abstract boolean enableModuleOptionalParameter();

    public static Builder builder() {
      return new AutoValue_SessionRequestHandlerUtil_SessionRequestInfo.Builder()
          .setModuleNames(ImmutableList.of())
          .setDeviceSerials(ImmutableList.of())
          .setIncludeFilters(ImmutableList.of())
          .setExcludeFilters(ImmutableList.of())
          .setExtraArgs(ImmutableList.of())
          .setGivenMatchedNonTfModules(ImmutableSet.of())
          .setV2ConfigsMap(ImmutableMap.of())
          .setExpandedModules(ImmutableMap.of())
          .setEnableModuleParameter(false)
          .setEnableModuleOptionalParameter(false);
    }

    public abstract Builder toBuilder();

    /** Builder to create SessionRequestInfo. */
    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setTestPlan(String testPlan);

      public abstract Builder setXtsRootDir(String xtsRootDir);

      public abstract Builder setDeviceSerials(List<String> deviceSerials);

      public abstract Builder setModuleNames(List<String> moduleNames);

      public abstract Builder setShardCount(int shardCount);

      public abstract Builder setIncludeFilters(List<String> includeFilters);

      public abstract Builder setExcludeFilters(List<String> excludeFilters);

      public abstract Builder setExtraArgs(List<String> extraArgs);

      public abstract Builder setXtsType(XtsType xtsType);

      public abstract Builder setPythonPkgIndexUrl(String pythonPkgIndexUrl);

      public abstract Builder setAndroidXtsZip(String androidXtsZip);

      public abstract SessionRequestInfo build();

      public abstract Builder setGivenMatchedNonTfModules(
          ImmutableSet<String> givenMatchedNonTfModules);

      public abstract Builder setV2ConfigsMap(ImmutableMap<String, Configuration> v2ConfigsMap);

      public abstract Builder setExpandedModules(
          ImmutableMap<String, Configuration> expandedModules);

      public abstract Builder setEnableModuleParameter(boolean enableModuleParameter);

      public abstract Builder setEnableModuleOptionalParameter(
          boolean enableModuleOptionalParameter);
    }
  }

  @AutoValue
  abstract static class SuiteTestFilter {

    /** The original filter string in --include-filter or --exclude-filter. */
    abstract String filterString();

    abstract Optional<String> abi();

    /**
     * Module name with module parameter (if any), e.g., "FooModuleName" or
     * "FooModuleName[instant]".
     */
    abstract String moduleName();

    abstract Optional<String> testName();

    private static final Splitter FILTER_STRING_SPLITTER = Splitter.on(' ').omitEmptyStrings();

    private static SuiteTestFilter create(String filterString) {
      List<String> tokens = FILTER_STRING_SPLITTER.splitToList(filterString);
      switch (tokens.size()) {
        case 1:
          return create(
              filterString, /* abi= */ null, /* moduleName= */ tokens.get(0), /* testName= */ null);
        case 2:
          if (AbiUtil.isAbiSupportedByCompatibility(tokens.get(0))) {
            return create(
                filterString,
                /* abi= */ tokens.get(0),
                /* moduleName= */ tokens.get(1),
                /* testName= */ null);
          } else {
            return create(
                filterString,
                /* abi= */ null,
                /* moduleName= */ tokens.get(0),
                /* testName= */ tokens.get(1));
          }
        case 3:
          return create(
              filterString,
              /* abi= */ tokens.get(0),
              /* moduleName= */ tokens.get(1),
              /* testName= */ tokens.get(2));
        default:
          throw new IllegalArgumentException(
              String.format("Invalid filter string: [%s]", filterString));
      }
    }

    private static SuiteTestFilter create(
        String filterString, @Nullable String abi, String moduleName, @Nullable String testName) {
      return new AutoValue_SessionRequestHandlerUtil_SuiteTestFilter(
          filterString, Optional.ofNullable(abi), moduleName, Optional.ofNullable(testName));
    }

    /**
     * Exactly matches {@code originalModuleName} and {@code moduleParameter}. If {@link #abi()} is
     * empty, matches any {@code moduleAbi}. Otherwise exactly matches {@code moduleAbi}.
     */
    private boolean matchModule(
        String originalModuleName, @Nullable String moduleAbi, @Nullable String moduleParameter) {
      String moduleName =
          moduleParameter == null
              ? originalModuleName
              : String.format("%s[%s]", originalModuleName, moduleParameter);
      if (!moduleName().equals(moduleName)) {
        return false;
      }
      if (abi().isEmpty()) {
        return true;
      }
      return abi().get().equals(moduleAbi);
    }

    @Memoized
    @Override
    public String toString() {
      return filterString();
    }
  }

  /**
   * Gets a list of SubDeviceSpec for the job. One SubDeviceSpec maps to one sub device used for
   * running the job as the job may need multiple devices to run the test.
   */
  private ImmutableList<SubDeviceSpec> getSubDeviceSpecListForTradefed(
      List<String> passedInDeviceSerials, int shardCount)
      throws MobileHarnessException, InterruptedException {
    ImmutableSet<String> allAndroidDevices = getAllAndroidDevices();
    logger.atInfo().log("All android devices: %s", allAndroidDevices);
    if (passedInDeviceSerials.isEmpty()) {
      return pickAndroidOnlineDevices(allAndroidDevices, shardCount);
    }

    ArrayList<String> existingPassedInDeviceSerials = new ArrayList<>();
    passedInDeviceSerials.forEach(
        serial -> {
          if (allAndroidDevices.contains(serial)) {
            existingPassedInDeviceSerials.add(serial);
          } else {
            logger.atInfo().log("Passed in device serial [%s] is not detected, skipped.", serial);
          }
        });
    if (existingPassedInDeviceSerials.isEmpty()) {
      logger.atInfo().log("None of passed in devices exist [%s], skipped.", passedInDeviceSerials);
      return ImmutableList.of();
    }
    return existingPassedInDeviceSerials.stream()
        .map(
            serial ->
                SubDeviceSpec.newBuilder()
                    .setType(getTradefedRequiredDeviceType())
                    .setDimensions(StringMap.newBuilder().putContent("serial", serial))
                    .build())
        .collect(toImmutableList());
  }

  public Optional<Result> getTestResultFromTest(TestInfo testInfo) throws MobileHarnessException {
    // TODO: Stop reading from lab's gen dir after file transfer is ready.
    if (!testInfo.properties().has(Test.LAB_TEST_GEN_FILE_DIR)) {
      return Optional.empty();
    }
    List<Path> testResultXmlFiles =
        localFileUtil.listFilePaths(
            Path.of(testInfo.properties().get(Test.LAB_TEST_GEN_FILE_DIR)),
            /* recursively= */ true,
            path -> path.getFileName().toString().equals(TEST_RESULT_XML_FILE_NAME));
    if (!testResultXmlFiles.isEmpty()) {
      return compatibilityReportParser.parse(testResultXmlFiles.get(0));
    }
    return Optional.empty();
  }

  private ImmutableList<SubDeviceSpec> pickAndroidOnlineDevices(
      Set<String> allAndroidOnlineDevices, int shardCount) {
    if (shardCount <= 1 && !allAndroidOnlineDevices.isEmpty()) {
      return ImmutableList.of(
          SubDeviceSpec.newBuilder().setType(getTradefedRequiredDeviceType()).build());
    }
    int numOfNeededDevices = min(allAndroidOnlineDevices.size(), shardCount);
    ImmutableList.Builder<SubDeviceSpec> deviceSpecList = ImmutableList.builder();
    for (int i = 0; i < numOfNeededDevices; i++) {
      deviceSpecList.add(
          SubDeviceSpec.newBuilder().setType(getTradefedRequiredDeviceType()).build());
    }
    return deviceSpecList.build();
  }

  private static String getTradefedRequiredDeviceType() {
    return Flags.instance().atsRunTfOnAndroidRealDevice.getNonNull()
        ? ANDROID_REAL_DEVICE_TYPE
        : ANDROID_DEVICE_TYPE;
  }

  private ImmutableSet<String> getAllAndroidDevices()
      throws MobileHarnessException, InterruptedException {
    if (Flags.instance().enableAtsMode.getNonNull()) {
      return getAllAndroidDevicesFromMaster();
    } else {
      return getAllLocalAndroidDevices();
    }
  }

  private ImmutableSet<String> getAllLocalAndroidDevices()
      throws MobileHarnessException, InterruptedException {
    if (Flags.instance().detectAdbDevice.getNonNull()) {
      return ImmutableSet.copyOf(
          androidAdbInternalUtilProvider
              .get()
              .getDeviceSerialsByState(DeviceState.DEVICE, /* timeout= */ null));
    } else {
      return ImmutableSet.of();
    }
  }

  private ImmutableSet<String> getAllAndroidDevicesFromMaster()
      throws MobileHarnessException, InterruptedException {
    DeviceQueryResult queryResult;
    try {
      queryResult = deviceQuerier.queryDevice(DeviceQueryFilter.getDefaultInstance());
    } catch (com.google.wireless.qa.mobileharness.shared.MobileHarnessException e) {
      throw new MobileHarnessException(
          InfraErrorId.ATSC_RUN_COMMAND_QUERY_DEVICE_ERROR, "Failed to query device", e);
    }
    return queryResult.getDeviceInfoList().stream()
        .filter(
            deviceInfo ->
                deviceInfo.getTypeList().stream()
                    .anyMatch(deviceType -> deviceType.startsWith("Android")))
        .map(DeviceInfo::getId)
        .collect(toImmutableSet());
  }

  public Optional<JobInfo> createXtsTradefedTestJob(SessionRequestInfo sessionRequestInfo)
      throws MobileHarnessException, InterruptedException {
    String xtsRootDir = sessionRequestInfo.xtsRootDir();
    if (!localFileUtil.isDirExist(xtsRootDir)) {
      logger.atInfo().log(
          "xTS root dir [%s] doesn't exist, skip creating tradefed jobs.", xtsRootDir);
      return Optional.empty();
    }

    XtsType xtsType = sessionRequestInfo.xtsType();
    ImmutableMap<String, Configuration> configsMap =
        configurationUtil.getConfigsFromDirs(
            ImmutableList.of(getXtsTestCasesDir(Path.of(xtsRootDir), xtsType).toFile()));

    ImmutableList<String> modules = sessionRequestInfo.moduleNames();
    ImmutableSet<String> allTfModules =
        configsMap.values().stream()
            .map(config -> config.getMetadata().getXtsModule())
            .collect(toImmutableSet());
    ImmutableList<String> givenMatchedTfModules =
        modules.stream().filter(allTfModules::contains).collect(toImmutableList());
    boolean noGivenModuleForTf = !modules.isEmpty() && givenMatchedTfModules.isEmpty();
    if (noGivenModuleForTf) {
      logger.atInfo().log(
          "Skip creating tradefed jobs as none of given modules is for tradefed module: %s",
          modules);
      return Optional.empty();
    }

    Optional<JobConfig> jobConfig =
        createXtsTradefedTestJobConfig(sessionRequestInfo, givenMatchedTfModules);
    if (jobConfig.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(JobInfoCreator.createJobInfo(jobConfig.get(), ImmutableList.of(), null));
  }

  @VisibleForTesting
  Optional<JobConfig> createXtsTradefedTestJobConfig(
      SessionRequestInfo sessionRequestInfo, ImmutableList<String> tfModules)
      throws MobileHarnessException, InterruptedException {
    String testPlan = sessionRequestInfo.testPlan();
    String xtsRootDir = sessionRequestInfo.xtsRootDir();
    String xtsType = sessionRequestInfo.xtsType().name();
    ImmutableList<String> deviceSerials = sessionRequestInfo.deviceSerials();
    int shardCount = sessionRequestInfo.shardCount().orElse(0);
    ImmutableList<String> extraArgs = sessionRequestInfo.extraArgs();

    ImmutableList<SubDeviceSpec> subDeviceSpecList =
        getSubDeviceSpecListForTradefed(deviceSerials, shardCount);
    if (subDeviceSpecList.isEmpty()) {
      logger.atInfo().log("Found no devices to create the job config.");
      return Optional.empty();
    }

    JobConfig.Builder jobConfigBuilder =
        JobConfig.newBuilder()
            .setName("xts-tradefed-test-job")
            .setExecMode("local")
            .setJobTimeoutSec(saturatedCast(Duration.ofDays(3L).toSeconds()))
            .setTestTimeoutSec(saturatedCast(Duration.ofDays(3L).toSeconds()))
            .setStartTimeoutSec(Duration.ofMinutes(5L).toSeconds())
            .setPriority(Priority.HIGH)
            .setTestAttempts(1)
            .setTests(
                StringList.newBuilder()
                    .addContent(String.format("xts-tradefed-test-%s", testPlan)));
    jobConfigBuilder.setDevice(DeviceList.newBuilder().addAllSubDeviceSpec(subDeviceSpecList));

    Map<String, String> driverParams = new HashMap<>();
    driverParams.put("xts_type", xtsType);

    // Use android xts zip file path if specified in request. Otherwise use root directory path.
    if (sessionRequestInfo.androidXtsZip().isPresent()) {
      driverParams.put("android_xts_zip", sessionRequestInfo.androidXtsZip().get());
    } else {
      driverParams.put("xts_root_dir", xtsRootDir);
    }
    driverParams.put("xts_test_plan", testPlan);

    ImmutableList<String> shardCountArg =
        shardCount > 0
            ? ImmutableList.of(String.format("--shard-count %s", shardCount))
            : ImmutableList.of();
    String sessionRequestInfoArgs =
        Joiner.on(' ')
            .join(
                Streams.concat(
                        tfModules.stream().map(module -> String.format("-m %s", module)),
                        shardCountArg.stream(),
                        sessionRequestInfo.includeFilters().stream()
                            .map(
                                includeFilter ->
                                    String.format("--include-filter \"%s\"", includeFilter)),
                        sessionRequestInfo.includeFilters().stream()
                            .map(
                                excludeFilter ->
                                    String.format("--exclude-filter \"%s\"", excludeFilter)),
                        extraArgs.stream())
                    .collect(toImmutableList()));
    if (!sessionRequestInfoArgs.isEmpty()) {
      driverParams.put("run_command_args", sessionRequestInfoArgs);
    }

    jobConfigBuilder.setDriver(
        Driver.newBuilder().setName("XtsTradefedTest").setParam(new Gson().toJson(driverParams)));

    JobConfig jobConfig = jobConfigBuilder.build();
    logger.atInfo().log("XtsTradefedTest job config: %s", shortDebugString(jobConfig));

    return Optional.of(jobConfig);
  }

  /**
   * Adds non-tradefed module info to the {@code SessionRequestInfo}. This is required step before
   * creating non-tradefed jobs or checking if it can do so.
   *
   * @return an updated {@code SessionRequestInfo}
   */
  public SessionRequestInfo addNonTradefedModuleInfo(SessionRequestInfo sessionRequestInfo)
      throws MobileHarnessException {
    SessionRequestInfo.Builder updatedSessionRequestInfo = sessionRequestInfo.toBuilder();

    String xtsRootDir = sessionRequestInfo.xtsRootDir();
    if (!localFileUtil.isDirExist(xtsRootDir)) {
      logger.atInfo().log(
          "xTS root dir [%s] doesn't exist, skip creating non-tradefed jobs.", xtsRootDir);
      return updatedSessionRequestInfo.build();
    }

    XtsType xtsType = sessionRequestInfo.xtsType();
    ImmutableMap<String, Configuration> configsMap =
        configurationUtil.getConfigsV2FromDirs(
            ImmutableList.of(getXtsTestCasesDir(Path.of(xtsRootDir), xtsType).toFile()));
    updatedSessionRequestInfo.setV2ConfigsMap(configsMap);

    // Gets expanded modules with abi and module parameters (if any).
    TestSuiteHelper testSuiteHelper = getTestSuiteHelper(xtsRootDir, xtsType, sessionRequestInfo);
    updatedSessionRequestInfo.setExpandedModules(ImmutableMap.copyOf(testSuiteHelper.loadTests()));

    ImmutableList<String> modules = sessionRequestInfo.moduleNames();
    ImmutableSet<String> allNonTfModules =
        configsMap.values().stream()
            .map(config -> config.getMetadata().getXtsModule())
            .collect(toImmutableSet());
    updatedSessionRequestInfo.setGivenMatchedNonTfModules(
        modules.stream().filter(allNonTfModules::contains).collect(toImmutableSet()));
    return updatedSessionRequestInfo.build();
  }

  /**
   * Checks if non-tradefed jobs can be created based on the {@code SessionRequestInfo}.
   *
   * @return true if non-tradefed jobs can be created.
   */
  public boolean canCreateNonTradefedJobs(SessionRequestInfo sessionRequestInfo) {
    String testPlan = sessionRequestInfo.testPlan();
    // Currently only support CTS
    if (!testPlan.equals("cts")) {
      return false;
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
  public ImmutableList<JobInfo> createXtsNonTradefedJobs(SessionRequestInfo sessionRequestInfo)
      throws MobileHarnessException, InterruptedException {
    if (!canCreateNonTradefedJobs(sessionRequestInfo)) {
      logger.atInfo().log(
          "Skip creating non-tradefed jobs as none of given modules is for non-tradefed module: %s",
          sessionRequestInfo.moduleNames());
      return ImmutableList.of();
    }
    String testPlan = sessionRequestInfo.testPlan();
    String xtsRootDir = sessionRequestInfo.xtsRootDir();
    if (!localFileUtil.isDirExist(xtsRootDir)) {
      logger.atInfo().log(
          "xTS root dir [%s] doesn't exist, skip creating non-tradefed jobs.", xtsRootDir);
      return ImmutableList.of();
    }

    XtsType xtsType = sessionRequestInfo.xtsType();
    ImmutableSet<String> givenMatchedNonTfModules = sessionRequestInfo.givenMatchedNonTfModules();
    ImmutableList.Builder<JobInfo> jobInfos = ImmutableList.builder();

    ImmutableList<String> androidDeviceSerials = sessionRequestInfo.deviceSerials();

    ImmutableMap<String, String> moduleNameToConfigFilePathMap =
        sessionRequestInfo.v2ConfigsMap().entrySet().stream()
            .collect(toImmutableMap(e -> e.getValue().getMetadata().getXtsModule(), Entry::getKey));

    ImmutableList<SuiteTestFilter> includeFilters =
        sessionRequestInfo.includeFilters().stream()
            .map(SuiteTestFilter::create)
            .collect(toImmutableList());
    ImmutableList<SuiteTestFilter> excludeFilters =
        sessionRequestInfo.excludeFilters().stream()
            .map(SuiteTestFilter::create)
            .collect(toImmutableList());

    for (Entry<String, Configuration> entry :
        sessionRequestInfo.expandedModules().entrySet().stream()
            .filter(e -> e.getValue().getMetadata().getIsConfigV2())
            .collect(toImmutableList())) {
      String originalModuleName = entry.getValue().getMetadata().getXtsModule();
      String expandedModuleName = entry.getKey();
      if (givenMatchedNonTfModules.isEmpty()
          || givenMatchedNonTfModules.contains(originalModuleName)) {
        // Gets module abi
        String moduleAbi = getModuleAbi(expandedModuleName).orElse(null);
        // Gets module parameter
        String moduleParameter = getModuleParameter(expandedModuleName).orElse(null);

        // Filters the module by include-filter and exclude-filter.
        if (!includeFilters.isEmpty()
            && includeFilters.stream()
                .noneMatch(
                    includeFilter ->
                        includeFilter.matchModule(
                            originalModuleName, moduleAbi, moduleParameter))) {
          continue;
        }
        if (excludeFilters.stream()
            .anyMatch(
                excludeFilter ->
                    excludeFilter.matchModule(originalModuleName, moduleAbi, moduleParameter))) {
          continue;
        }

        Optional<JobInfo> jobInfoOpt =
            createXtsNonTradefedJob(
                Path.of(xtsRootDir),
                xtsType,
                testPlan,
                Path.of(requireNonNull(moduleNameToConfigFilePathMap.get(originalModuleName))),
                entry.getValue(),
                expandedModuleName,
                moduleAbi,
                moduleParameter);
        if (jobInfoOpt.isPresent()) {
          JobInfo jobInfo = jobInfoOpt.get();
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
                            .add("serial", serialDimensionValue));
          }
          jobInfos.add(jobInfo);
        }
      }
    }

    return jobInfos.build();
  }

  private Optional<JobInfo> createXtsNonTradefedJob(
      Path xtsRootDir,
      XtsType xtsType,
      String testPlan,
      Path moduleConfigPath,
      Configuration moduleConfig,
      String expandedModuleName,
      @Nullable String moduleAbi,
      @Nullable String moduleParameter)
      throws MobileHarnessException, InterruptedException {
    Optional<JobInfo> jobInfoOpt = createBaseXtsNonTradefedJob(moduleConfig, expandedModuleName);
    if (jobInfoOpt.isEmpty()) {
      return Optional.empty();
    }

    ImmutableList<File> fileDepDirs =
        ImmutableList.of(
            moduleConfigPath.getParent().toFile(),
            getXtsTestCasesDir(xtsRootDir, xtsType).toFile());

    JobInfo jobInfo = jobInfoOpt.get();
    moduleConfigurationHelper.updateJobInfo(jobInfo, moduleConfig, fileDepDirs);
    jobInfo.properties().add(XTS_NON_TF_JOB_PROP, "true");
    jobInfo.properties().add(XTS_MODULE_NAME_PROP, moduleConfig.getMetadata().getXtsModule());
    if (moduleAbi != null) {
      jobInfo.properties().add(XTS_MODULE_ABI_PROP, moduleAbi);
    }
    if (moduleParameter != null) {
      jobInfo.properties().add(XTS_MODULE_PARAMETER_PROP, moduleParameter);
    }
    jobInfo.params().add("run_certification_test_suite", "true");
    jobInfo
        .params()
        .add(
            "xts_suite_info",
            generateXtsSuiteInfoMap(xtsRootDir.toAbsolutePath().toString(), xtsType, testPlan));
    return Optional.of(jobInfo);
  }

  private String generateXtsSuiteInfoMap(String xtsRootDir, XtsType xtsType, String testPlan) {
    Map<String, String> xtsSuiteInfoMap =
        certificationSuiteInfoFactory.generateSuiteInfoMap(xtsRootDir, xtsType, testPlan);
    return Joiner.on(",").withKeyValueSeparator("=").join(xtsSuiteInfoMap);
  }

  private Optional<JobInfo> createBaseXtsNonTradefedJob(
      Configuration moduleConfig, String expandedModuleName)
      throws MobileHarnessException, InterruptedException {
    List<Device> moduleDevices = moduleConfig.getDevicesList();
    if (moduleDevices.isEmpty()) {
      logger.atInfo().log(
          "Found no devices to create the job config for xts non-tradefed job with module '%s'.",
          expandedModuleName);
      return Optional.empty();
    }

    List<SubDeviceSpec> subDeviceSpecList = new ArrayList<>();
    for (Device device : moduleDevices) {
      if (device.getName().isEmpty()) {
        logger.atWarning().log(
            "Device name is missing in a <device> in module '%s'", expandedModuleName);
        return Optional.empty();
      } else {
        subDeviceSpecList.add(SubDeviceSpec.newBuilder().setType(device.getName()).build());
      }
    }

    JobConfig.Builder jobConfigBuilder =
        JobConfig.newBuilder()
            .setName(
                String.format(
                    "xts-mobly-aosp-package-job-%s", expandedModuleName.replace(' ', '_')))
            .setExecMode("local")
            .setJobTimeoutSec(saturatedCast(Duration.ofDays(5L).toSeconds()))
            .setTestTimeoutSec(saturatedCast(Duration.ofDays(5L).toSeconds()))
            .setStartTimeoutSec(Duration.ofHours(1L).toSeconds())
            .setPriority(Priority.HIGH)
            .setTestAttempts(1)
            .setTests(
                StringList.newBuilder()
                    .addContent(
                        String.format(
                            "xts-mobly-aosp-package-test-%s",
                            expandedModuleName.replace(' ', '_'))));
    jobConfigBuilder.setDevice(DeviceList.newBuilder().addAllSubDeviceSpec(subDeviceSpecList));
    jobConfigBuilder.setDriver(Driver.newBuilder().setName("MoblyAospPackageTest"));
    JobConfig jobConfig = jobConfigBuilder.build();
    logger.atInfo().log(
        "Non-tradefed job base config for module '%s': %s",
        expandedModuleName, shortDebugString(jobConfig));

    return Optional.of(JobInfoCreator.createJobInfo(jobConfig, ImmutableList.of(), null));
  }

  private Path getXtsTestCasesDir(Path xtsRootDir, XtsType xtsType) {
    return xtsRootDir.resolve(
        String.format("android-%s/testcases", Ascii.toLowerCase(xtsType.name())));
  }

  @VisibleForTesting
  TestSuiteHelper getTestSuiteHelper(
      String xtsRootDir, XtsType xtsType, SessionRequestInfo sessionRequestInfo) {
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

  /**
   * Processes the results of the given jobs.
   *
   * <p>The results are merged and saved to the given result directory. The logs are saved to the
   * given log directory.
   *
   * @param xtsType the xTS type
   * @param resultDir the directory to save the merged result
   * @param logDir the directory to save the logs
   * @param jobs the jobs to process
   */
  public void processResult(XtsType xtsType, Path resultDir, Path logDir, List<JobInfo> jobs)
      throws MobileHarnessException, InterruptedException {
    ImmutableMap<JobInfo, Optional<TestInfo>> tradefedTests =
        jobs.stream()
            .filter(jobInfo -> jobInfo.properties().has(XTS_TF_JOB_PROP))
            .collect(
                toImmutableMap(
                    Function.identity(),
                    jobInfo -> jobInfo.tests().getAll().values().stream().findFirst()));

    ImmutableMap<JobInfo, Optional<TestInfo>> nonTradefedTests =
        jobs.stream()
            .filter(jobInfo -> jobInfo.properties().has(XTS_NON_TF_JOB_PROP))
            .collect(
                toImmutableMap(
                    Function.identity(),
                    jobInfo -> jobInfo.tests().getAll().values().stream().findFirst()));
    Path tradefedTestResultsDir = resultDir.resolve("tradefed_results");
    Path nonTradefedTestResultsDir = resultDir.resolve("non-tradefed_results");
    Path tradefedTestLogsDir = logDir.resolve("tradefed_logs");
    Path nonTradefedTestLogsDir = logDir.resolve("non-tradefed_logs");

    List<Path> tradefedTestResultXmlFiles = new ArrayList<>();
    // Copies tradefed test relevant log and result files to dedicated locations
    for (Entry<JobInfo, Optional<TestInfo>> testEntry : tradefedTests.entrySet()) {
      if (testEntry.getValue().isEmpty()) {
        logger.atInfo().log(
            "Found no test in tradefed job [%s], skip it.", testEntry.getKey().locator().getId());
        continue;
      }

      TestInfo test = testEntry.getValue().get();

      copyTradefedTestLogFiles(test, tradefedTestLogsDir);
      Optional<Path> tradefedTestResultXmlFile =
          copyTradefedTestResultFiles(test, tradefedTestResultsDir);
      tradefedTestResultXmlFile.ifPresent(tradefedTestResultXmlFiles::add);
    }

    List<MoblyReportInfo> moblyReportInfos = new ArrayList<>();
    // Copies non-tradefed test relevant log and result files to dedicated locations
    for (Entry<JobInfo, Optional<TestInfo>> testEntry : nonTradefedTests.entrySet()) {
      if (testEntry.getValue().isEmpty()) {
        logger.atInfo().log(
            "Found no test in non-tradefed job [%s], skip it.",
            testEntry.getKey().locator().getId());
        continue;
      }
      TestInfo test = testEntry.getValue().get();

      copyNonTradefedTestLogFiles(test, nonTradefedTestLogsDir);
      Optional<NonTradefedTestResult> nonTradefedTestResult =
          copyNonTradefedTestResultFiles(
              test,
              nonTradefedTestResultsDir,
              testEntry.getKey().properties().get(XTS_MODULE_NAME_PROP),
              testEntry.getKey().properties().get(XTS_MODULE_ABI_PROP),
              testEntry.getKey().properties().get(XTS_MODULE_PARAMETER_PROP));
      nonTradefedTestResult.ifPresent(
          res ->
              moblyReportInfos.add(
                  MoblyReportInfo.of(
                      res.moduleName(),
                      res.moduleAbi().orElse(null),
                      res.moduleParameter().orElse(null),
                      res.testSummaryFile(),
                      res.resultAttributesFile(),
                      res.deviceBuildFingerprint(),
                      res.buildAttributesFile())));
    }

    Optional<Result> mergedTradefedReport = Optional.empty();
    if (!tradefedTestResultXmlFiles.isEmpty()) {
      mergedTradefedReport = compatibilityReportMerger.mergeXmlReports(tradefedTestResultXmlFiles);
    }

    Optional<Result> mergedNonTradefedReport = Optional.empty();
    if (!moblyReportInfos.isEmpty()) {
      mergedNonTradefedReport = compatibilityReportMerger.mergeMoblyReports(moblyReportInfos);
    }

    List<Result> reportList = new ArrayList<>();
    mergedTradefedReport.ifPresent(reportList::add);
    mergedNonTradefedReport.ifPresent(reportList::add);

    Optional<Result> finalReport =
        compatibilityReportMerger.mergeReports(reportList, /* validateReports= */ true);
    if (finalReport.isEmpty()) {
      logger.atWarning().log("Failed to merge reports.");
    } else {
      reportCreator.createReport(finalReport.get(), resultDir, null);
    }
  }

  /**
   * Copies tradefed test relevant log files to directory {@code logDir} for the given tradefed
   * test.
   *
   * <p>The destination log files structure looks like:
   *
   * <pre>
   * .../android-<xts>/logs/YYYY.MM.DD_HH.mm.ss/
   *    tradefed_logs/
   *      <driver>_test_<test_id>/
   *        command_history.txt
   *        xts_tf_output.log
   *        raw_tradefed_log/
   *    non-tradefed_logs/
   *      <driver>_test_<test_id>/
   *        command_history.txt
   *        mobly_command_output.log
   *        mobly_run_build_attributes.textproto
   *        mobly_run_result_attributes.textproto
   *        ...
   *        raw_mobly_logs/
   * </pre>
   */
  private void copyTradefedTestLogFiles(TestInfo tradefedTestInfo, Path logDir)
      throws MobileHarnessException, InterruptedException {
    Path testLogDir = prepareLogOrResultDirForTest(tradefedTestInfo, logDir);
    ImmutableList<Path> genFiles = getGenFilesFromTest(tradefedTestInfo);
    for (Path genFile : genFiles) {
      if (genFile.getFileName().toString().endsWith("gen-files")) {
        Path logsDir = genFile.resolve("logs");
        if (logsDir.toFile().exists()) {
          List<Path> logsSubFilesOrDirs =
              localFileUtil.listFilesOrDirs(
                  logsDir, filePath -> !filePath.getFileName().toString().equals("latest"));
          for (Path logsSubFileOrDir : logsSubFilesOrDirs) {
            if (logsSubFileOrDir.toFile().isDirectory()) {
              // If it's a dir, copy its content into the new log dir.
              List<Path> logFilesOrDirs =
                  localFileUtil.listFilesOrDirs(logsSubFileOrDir, path -> true);
              for (Path logFileOrDir : logFilesOrDirs) {
                logger.atInfo().log(
                    "Copying tradefed test log relevant file/dir [%s] into dir [%s]",
                    logFileOrDir, testLogDir);
                localFileUtil.copyFileOrDirWithOverridingCopyOptions(
                    logFileOrDir, testLogDir, ImmutableList.of("-rf"));
              }
            }
          }
        }
      } else {
        logger.atInfo().log(
            "Copying tradefed test log relevant file/dir [%s] into dir [%s]", genFile, testLogDir);
        localFileUtil.copyFileOrDirWithOverridingCopyOptions(
            genFile, testLogDir, ImmutableList.of("-rf"));
      }
    }
  }

  /**
   * Copies tradefed test relevant result files to directory {@code resultDir} for the given
   * tradefed test.
   *
   * <p>The destination result files structure looks like:
   *
   * <pre>
   * .../android-<xts>/results/YYYY.MM.DD_HH.mm.ss/
   *    the merged report relevant files (test_result.xml, html, checksum-suite.data, etc)
   *    tradefed_results/
   *      <driver>_test_<test_id>/
   *        test_result.xml
   *        test_result.html
   *        ...
   *    non-tradefed_results/
   *      <driver>_test_<test_id>/
   *        test_summary.yaml
   *        mobly_run_build_attributes.textproto
   *        mobly_run_result_attributes.textproto
   *        ...
   * </pre>
   *
   * @return the path to the tradefed test result xml file if any
   */
  @CanIgnoreReturnValue
  private Optional<Path> copyTradefedTestResultFiles(TestInfo tradefedTestInfo, Path resultDir)
      throws MobileHarnessException, InterruptedException {
    Path testResultDir = prepareLogOrResultDirForTest(tradefedTestInfo, resultDir);
    ImmutableList<Path> genFiles = getGenFilesFromTest(tradefedTestInfo);
    for (Path genFile : genFiles) {
      if (genFile.getFileName().toString().endsWith("gen-files")) {
        Path genFileResultsDir = genFile.resolve("results");
        if (genFileResultsDir.toFile().exists()) {
          List<Path> resultsSubFilesOrDirs =
              localFileUtil.listFilesOrDirs(
                  genFileResultsDir,
                  filePath -> !filePath.getFileName().toString().equals("latest"));
          for (Path resultsSubFileOrDir : resultsSubFilesOrDirs) {
            if (resultsSubFileOrDir.toFile().isDirectory()) {
              // If it's a dir, copy its content into the new result dir.
              List<Path> resultFilesOrDirs =
                  localFileUtil.listFilesOrDirs(resultsSubFileOrDir, path -> true);
              for (Path resultFileOrDir : resultFilesOrDirs) {
                logger.atInfo().log(
                    "Copying tradefed test result relevant file/dir [%s] into dir [%s]",
                    resultFileOrDir, testResultDir);
                localFileUtil.copyFileOrDirWithOverridingCopyOptions(
                    resultFileOrDir, testResultDir, ImmutableList.of("-rf"));
              }
            }
          }
        }
      }
    }

    List<Path> testResultXmlFiles =
        localFileUtil.listFilePaths(
            testResultDir,
            /* recursively= */ false,
            path -> path.getFileName().toString().equals(TEST_RESULT_XML_FILE_NAME));

    return testResultXmlFiles.stream().findFirst();
  }

  /**
   * Copies non-tradefed test relevant log files to directory {@code logDir} for the given
   * non-tradefed test.
   *
   * <p>The destination log files structure looks like:
   *
   * <pre>
   * .../android-<xts>/logs/YYYY.MM.DD_HH.mm.ss/
   *    tradefed_logs/
   *      <driver>_test_<test_id>/
   *        command_history.txt
   *        xts_tf_output.log
   *        raw_tradefed_log/
   *    non-tradefed_logs/
   *      <driver>_test_<test_id>/
   *        command_history.txt
   *        mobly_command_output.log
   *        mobly_run_build_attributes.textproto
   *        mobly_run_result_attributes.textproto
   *        ...
   *        raw_mobly_logs/
   * </pre>
   */
  private void copyNonTradefedTestLogFiles(TestInfo nonTradefedTestInfo, Path logDir)
      throws MobileHarnessException, InterruptedException {
    Path testLogDir = prepareLogOrResultDirForTest(nonTradefedTestInfo, logDir);
    ImmutableList<Path> genFiles = getGenFilesFromTest(nonTradefedTestInfo);
    for (Path genFile : genFiles) {
      logger.atInfo().log(
          "Copying non-tradefed test log relevant file/dir [%s] into dir [%s]",
          genFile, testLogDir);
      localFileUtil.copyFileOrDirWithOverridingCopyOptions(
          genFile, testLogDir, ImmutableList.of("-rf"));
    }
  }

  /**
   * Copies non-tradefed test relevant result files to directory {@code resultDir} for the given
   * non-tradefed test.
   *
   * <p>The destination result files structure looks like:
   *
   * <pre>
   * .../android-<xts>/results/YYYY.MM.DD_HH.mm.ss/
   *    the merged report relevant files (test_result.xml, html, checksum-suite.data, etc)
   *    tradefed_results/
   *      <driver>_test_<test_id>/
   *        test_result.xml
   *        test_result.html
   *        ...
   *    non-tradefed_results/
   *      <driver>_test_<test_id>/
   *        test_summary.yaml
   *        mobly_run_build_attributes.textproto
   *        mobly_run_result_attributes.textproto
   *        ...
   * </pre>
   *
   * @param moduleName the xts module name
   * @return {@code NonTradefedTestResult} if any
   */
  @CanIgnoreReturnValue
  private Optional<NonTradefedTestResult> copyNonTradefedTestResultFiles(
      TestInfo nonTradefedTestInfo,
      Path resultDir,
      String moduleName,
      @Nullable String moduleAbi,
      @Nullable String moduleParameter)
      throws MobileHarnessException, InterruptedException {
    Path testResultDir = prepareLogOrResultDirForTest(nonTradefedTestInfo, resultDir);

    NonTradefedTestResult.Builder nonTradefedTestResultBuilder =
        NonTradefedTestResult.builder().setModuleName(moduleName);
    if (moduleAbi != null) {
      nonTradefedTestResultBuilder.setModuleAbi(moduleAbi);
    }
    if (moduleParameter != null) {
      nonTradefedTestResultBuilder.setModuleParameter(moduleParameter);
    }
    String testGenFileDir = nonTradefedTestInfo.getGenFileDir();

    List<Path> moblyTestResultFiles =
        localFileUtil.listFilePaths(
            Path.of(testGenFileDir),
            /* recursively= */ true,
            path -> MOBLY_TEST_RESULT_FILE_NAMES.contains(path.getFileName().toString()));

    // TODO: Stop reading from lab's gen dir after file transfer is ready.
    // Directly read lab side gen file directory to copy the test result and log files. This is a
    // temporary solution before file transfer functionality is enabled.
    Optional<String> labTestGenFileDir =
        nonTradefedTestInfo.properties().getOptional(Test.LAB_TEST_GEN_FILE_DIR);
    if (labTestGenFileDir.isPresent() && !labTestGenFileDir.get().equals(testGenFileDir)) {
      moblyTestResultFiles.addAll(
          localFileUtil.listFilePaths(
              Path.of(labTestGenFileDir.get()),
              /* recursively= */ true,
              path -> MOBLY_TEST_RESULT_FILE_NAMES.contains(path.getFileName().toString())));
    }

    for (Path moblyTestResultFile : moblyTestResultFiles) {
      logger.atInfo().log(
          "Copying non-tradefed test result relevant file [%s] into dir [%s]",
          moblyTestResultFile, testResultDir);
      localFileUtil.copyFileOrDirWithOverridingCopyOptions(
          moblyTestResultFile, testResultDir, ImmutableList.of("-rf"));
      updateNonTradefedTestResult(
          nonTradefedTestResultBuilder,
          moblyTestResultFile.getFileName().toString(),
          moblyTestResultFile);
    }

    return Optional.of(nonTradefedTestResultBuilder.build());
  }

  public ImmutableList<Path> getGenFilesFromTest(TestInfo test) throws MobileHarnessException {
    String testGenFileDir = test.getGenFileDir();
    List<Path> genFiles = localFileUtil.listFilesOrDirs(Path.of(testGenFileDir), path -> true);

    // TODO: Stop reading from lab's gen dir after file transfer is ready.
    // Directly read lab side gen file directory to copy the test result and log files. This is a
    // temporary solution before file transfer functionality is enabled.
    Optional<String> labTestGenFileDir = test.properties().getOptional(Test.LAB_TEST_GEN_FILE_DIR);
    if (labTestGenFileDir.isPresent() && !labTestGenFileDir.get().equals(testGenFileDir)) {
      genFiles.addAll(
          localFileUtil.listFilesOrDirs(Path.of(labTestGenFileDir.get()), path -> true));
    }
    return ImmutableList.copyOf(genFiles);
  }

  private Path prepareLogOrResultDirForTest(TestInfo test, Path parentDir)
      throws MobileHarnessException {
    Path targetDir =
        parentDir.resolve(
            String.format("%s_test_%s", test.jobInfo().type().getDriver(), test.locator().getId()));
    localFileUtil.prepareDir(targetDir);
    return targetDir;
  }

  private void updateNonTradefedTestResult(
      NonTradefedTestResult.Builder resultBuilder, String fileName, Path filePath)
      throws MobileHarnessException {
    switch (fileName) {
      case "test_summary.yaml":
        resultBuilder.setTestSummaryFile(filePath);
        break;
      case "device_build_fingerprint.txt":
        resultBuilder.setDeviceBuildFingerprint(localFileUtil.readFile(filePath).trim());
        break;
      case "mobly_run_result_attributes.textproto":
        resultBuilder.setResultAttributesFile(filePath);
        break;
      case "mobly_run_build_attributes.textproto":
        resultBuilder.setBuildAttributesFile(filePath);
        break;
      default:
        break;
    }
  }

  /** Data class for the non-tradefed test result. */
  @AutoValue
  public abstract static class NonTradefedTestResult {

    /** The xTS module name. */
    public abstract String moduleName();

    /** The abi of the xTS module. */
    public abstract Optional<String> moduleAbi();

    /** The parameter of the xTS module. */
    public abstract Optional<String> moduleParameter();

    /**
     * The build fingerprint for the major device on which the test run, it's used to identify the
     * generated report.
     */
    public abstract String deviceBuildFingerprint();

    /** The path of the test summary file being parsed. */
    public abstract Path testSummaryFile();

    /**
     * The path of the text proto file that stores {@link
     * com.google.devtools.mobileharness.infra.ats.console.result.proto.ReportProto.AttributeList}
     * which will be set in the {@link Result}.{@code attribute}.
     */
    public abstract Path resultAttributesFile();

    /**
     * The path of the text proto file that stores {@link
     * com.google.devtools.mobileharness.infra.ats.console.result.proto.ReportProto.AttributeList}
     * which will be set in the {@link
     * com.google.devtools.mobileharness.infra.ats.console.result.proto.ReportProto.BuildInfo}.{@code
     * attribute}.
     */
    public abstract Path buildAttributesFile();

    public static Builder builder() {
      return new AutoValue_SessionRequestHandlerUtil_NonTradefedTestResult.Builder();
    }

    /** Builder for {@link NonTradefedTestResult}. */
    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setModuleName(String moduleName);

      public abstract Builder setModuleAbi(String moduleAbi);

      public abstract Builder setModuleParameter(String moduleParameter);

      public abstract Builder setDeviceBuildFingerprint(String deviceBuildFingerprint);

      public abstract Builder setTestSummaryFile(Path testSummaryFile);

      public abstract Builder setResultAttributesFile(Path resultAttributesFile);

      public abstract Builder setBuildAttributesFile(Path buildAttributesFile);

      public abstract NonTradefedTestResult build();
    }
  }

  public void cleanUpJobGenDirs(List<JobInfo> jobInfoList)
      throws MobileHarnessException, InterruptedException {
    for (JobInfo jobInfo : jobInfoList) {
      if (jobInfo.setting().hasGenFileDir()) {
        logger.atInfo().log(
            "Cleaning up job [%s] gen dir [%s]",
            jobInfo.locator().getId(), jobInfo.setting().getGenFileDir());
        localFileUtil.removeFileOrDir(jobInfo.setting().getGenFileDir());
      }
    }
  }
}
