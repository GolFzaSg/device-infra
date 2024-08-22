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

package com.google.devtools.mobileharness.api.model.error;

import com.google.common.base.Preconditions;
import com.google.devtools.common.metrics.stability.model.proto.ErrorTypeProto.ErrorType;
import com.google.devtools.common.metrics.stability.util.ErrorIdFormatter;

/**
 * Error IDs for Mobile Harness Android platform supports, or Android related Driver/Decorator,
 * Detector/Device implementations. *
 */
public enum AndroidErrorId implements ErrorId {
  // ***********************************************************************************************
  // Standard Android Platforms: 100_001 ~ 170_000
  // ***********************************************************************************************

  /** Android Low Level Utils: 100_001 ~ 110_000 */

  // ActivityManager: 100_001 ~ 100_020
  ANDROID_AM_PARSE_LOCALE_ERROR(100_001, ErrorType.INFRA_ISSUE),
  ANDROID_AM_GET_LOCALE_ERROR(100_002, ErrorType.DEPENDENCY_ISSUE),

  // AndroidSystemSettingUtil: 100_021 ~ 100_120
  ANDROID_SYSTEM_SETTING_ERROR(100_021, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_TIMEOUT(100_022, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_ENABLE_GPS_ERROR(100_023, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_ENABLE_NETWORK_LOCATION_ERROR(100_024, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_CHECK_LOCATION_SERVICE_ERROR(100_025, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_DEVICE_PROPERTY_ERROR(100_026, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_DEVICE_SDK_ERROR(100_027, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_DEVICE_VERSION_CODE_NAME_ERROR(100_028, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_PACKAGE_STORAGE_MODE_ERROR(100_029, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_SET_PACKAGE_STORAGE_MODE_ERROR(100_030, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_ISOLATED_STORAGE_MODE_ERROR(100_031, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_CLEAR_GSERVICE_OVERRIDE_ERROR(100_032, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_GSERVICE_ANDROID_ID_ERROR(100_033, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_PARSE_SYSTEM_TIME_ERROR(100_034, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_DISABLE_AIRPLANE_MODE_ERROR(100_035, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_PARSE_AIRPLANE_MODE_ERROR(100_036, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_BROADCAST_AIRPLANE_MODE_ERROR(100_037, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_BATTERY_LEVEL_ERROR(100_038, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_BATTERY_TEMP_ERROR(100_039, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_BATTERY_STATS_ERROR(100_040, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_CPU_USAGE_ERROR(100_041, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_BATTERY_STATE_CMD_ERROR(100_042, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_SET_BATTERY_DISCHARGE_ERROR(100_043, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_SYSTEM_TIME_ERROR(100_044, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_EPOCH_SYSTEM_TIME_ERROR(100_045, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_ALLOW_MOCK_LOCATION_ERROR(100_046, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_DISABLE_SCREENLOCK_ERROR(100_047, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_DISMISS_KEYGUARD_ERROR(100_048, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_ENABLE_UNKNOWN_SOURCES_ERROR(100_049, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_PARSE_RESOLUTION_ERROR(100_050, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_SYSTEM_SETTING_SET_MOCK_LOCATION_PROVIDER_ERROR(100_051, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_SET_DEX_PRE_VERIFICATION_ERROR(100_052, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_SET_VERITY_ERROR(100_053, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_TIME_ZONE_OFFSET_ERROR(100_054, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_SET_SYSTEM_TIME_ERROR(100_055, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_INVALID_DENSITY_VALUE(100_056, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_SYSTEM_SETTING_KEEP_AWAKE_ERROR(100_057, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_SCREEN_DENSITY_PROP_ERROR(100_058, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_DUMPSYS_WINDOW_ERROR(100_059, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_BUILD_PROP_ERROR(100_060, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_SET_LOG_LEVEL_PROP_ERROR(100_061, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_SQLITE_CMD_ERROR(100_062, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_FORCE_USB2ADB_MODE_ERROR(100_063, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_DEVICE_INT_PROPERTY_ERROR(100_064, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_GET_PACKAGE_OP_MODE_ERROR(100_065, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SETTING_SET_PACKAGE_OP_MODE_ERROR(100_066, ErrorType.INFRA_ISSUE),

  // AndroidConnectivityUtil: 100_121 ~ 100_220
  ANDROID_CONNECTIVITY_FAIL_CONNECT_TO_WIFI(100_121, ErrorType.UNDETERMINED),
  ANDROID_CONNECTIVITY_ENABLE_WIFI_VIA_WIFIUTIL_ERROR(100_122, ErrorType.INFRA_ISSUE),
  ANDROID_CONNECTIVITY_ENABLE_WIFI_ERROR(100_123, ErrorType.INFRA_ISSUE),
  ANDROID_CONNECTIVITY_GET_NET_STATUS_ERROR(100_124, ErrorType.INFRA_ISSUE),
  ANDROID_CONNECTIVITY_GET_SAVED_SSIDS_PSKS_ERROR(100_125, ErrorType.INFRA_ISSUE),
  ANDROID_CONNECTIVITY_DISABLE_WIFI_ERROR(100_126, ErrorType.INFRA_ISSUE),
  ANDROID_CONNECTIVITY_DISABLE_WIFI_VIA_WIFIUTIL_ERROR(100_127, ErrorType.INFRA_ISSUE),
  ANDROID_CONNECTIVITY_SET_NFC_ERROR(100_128, ErrorType.INFRA_ISSUE),
  ANDROID_CONNECTIVITY_SDK_VERSION_NOT_SUPPORT(100_129, ErrorType.DEPENDENCY_ISSUE),

  // Android fastboot: 100_351 ~ 100_450
  ANDROID_FASTBOOT_UPDATE_MISSING_IMG_ZIP_FILE(100_351, ErrorType.CUSTOMER_ISSUE),
  ANDROID_FASTBOOT_DEVICE_TYPE_NOT_MATCH_ERROR(100_352, ErrorType.CUSTOMER_ISSUE),
  ANDROID_FASTBOOT_UNEXPECTED_IMG_FILE(100_353, ErrorType.INFRA_ISSUE),
  ANDROID_FASTBOOT_UNKNOWN_SLOT(100_354, ErrorType.INFRA_ISSUE),
  ANDROID_FASTBOOT_FLASH_PARTITION_ERROR(100_355, ErrorType.INFRA_ISSUE),
  ANDROID_FASTBOOT_UPDATE_COMMAND_EXEC_ERROR(100_356, ErrorType.INFRA_ISSUE),
  ANDROID_FASTBOOT_DEVICE_LIST_OUTPUT_FORMAT_ERROR(100_357, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_FASTBOOT_UNKNOWN_DEVICE_TYPE(100_358, ErrorType.INFRA_ISSUE),
  ANDROID_FASTBOOT_COMMAND_EXEC_ERROR(100_359, ErrorType.UNDETERMINED),
  ANDROID_FASTBOOT_MISSING_FASTBOOT_BINARY_ERROR(100_360, ErrorType.INFRA_ISSUE),

  // AndroidSystemStateUtil: 100_451 ~ 100_650
  ANDROID_SYSTEM_STATE_FACTORY_RESET_VIA_TEST_HARNESS_ERROR(100_451, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_STATE_BROADCAST_DEVICE_IS_READY_ERROR(100_452, ErrorType.UNDETERMINED),
  ANDROID_SYSTEM_STATE_STOP_ZYGOTE_PROCESS_ERROR(100_453, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_STATE_START_ZYGOTE_PROCESS_ERROR(100_454, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_STATE_DEVICE_NOT_ONLINE_READY(100_455, ErrorType.UNDETERMINED),
  ANDROID_SYSTEM_STATE_FACTORY_RESET_VIA_BROADCAST_ERROR(100_456, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_STATE_REBOOT_ERROR(100_457, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_STATE_REBOOT_TO_BOOTLOADER_ERROR(100_458, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_STATE_REBOOT_TO_RECOVERY_ERROR(100_459, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_STATE_UNROOT_DEVICE_ERROR(100_460, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_STATE_ROOT_DEVICE_ERROR(100_461, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_STATE_WAIT_FOR_DEVICE_CMD_ERROR(100_462, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_STATE_CHECK_DEVICE_ONLINE_BROKEN_PIPE_ERROR(100_463, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_SYSTEM_STATE_FACTORY_RESET_VIA_TEST_HARNESS_SCREEN_LOCKED_ERROR(
      100_464, ErrorType.CUSTOMER_ISSUE),
  ANDROID_SYSTEM_STATE_GET_STATE_ERROR(100_465, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_SYSTEM_STATE_WAIT_FOR_STATE_ERROR(100_466, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_SYSTEM_STATE_SIDELOAD_INVALID_OTA_PACKAGE(100_467, ErrorType.CUSTOMER_ISSUE),
  ANDROID_SYSTEM_STATE_SIDELOAD_INVALID_TIMEOUT(100_468, ErrorType.CUSTOMER_ISSUE),
  ANDROID_SYSTEM_STATE_SIDELOAD_ERROR(100_469, ErrorType.DEPENDENCY_ISSUE),

  // AndroidAdbUtil: 100_651 ~ 100_850
  ANDROID_ADB_UTIL_GET_DEVICE_PROPERTY_ERROR(100_651, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_DUMPSYS_ERROR(100_652, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_CMD_ERROR(100_653, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_CLEAR_LOG_ERROR(100_654, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_ADB_UTIL_GET_TIME_FOR_LOGCAT_ERROR(100_655, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_DUMP_LOG_ERROR(100_656, ErrorType.UNDETERMINED),
  ANDROID_ADB_UTIL_DUMP_LOG_ASYNC_ERROR(100_657, ErrorType.UNDETERMINED),
  ANDROID_ADB_UTIL_SDK_NOT_SUPPORT(100_658, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_ADB_UTIL_SET_LOGCAT_BUFFER_SIZE_ERROR(100_659, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_TIMEOUT(100_660, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_CHECK_ROOT_ERROR(100_661, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_SETTINGS_CMD_ERROR(100_666, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_BUGREPORT_ERROR(100_667, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_FIND_BUGREPORT_DIR_ERROR(100_668, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_SET_DEVICE_PROPERTY_ERROR(100_669, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_KEY_EVENT_ERROR(100_670, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_BROADCAST_ERROR(100_671, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_BROADCAST_EMPTY_INTENT_ARG(100_672, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_SQLITE_ERROR(100_673, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_CONTENT_ERROR(100_674, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_SVC_CMD_ERROR(100_675, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_FORWARD_TCP_PORT_ERROR(100_676, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_REMOVE_ALL_REVERSE_TCP_PORTS_ERROR(100_677, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_REMOVE_FORWARD_PORT_ERROR(100_678, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_REVERSE_TCP_PORT_ERROR(100_679, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_UTIL_WAIT_FOR_SIGNAL_IN_LOG_ERROR(100_680, ErrorType.INFRA_ISSUE),

  // Android File Util: 100_851 ~ 101_000
  ANDROID_FILE_UTIL_LIST_FILE_ERROR(100_851, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_PULL_FILE_ERROR(100_852, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_PUSH_FILE_ADB_ERROR(100_853, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_PUSH_FILE_LOCAL_FILE_ERROR(100_854, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_MAKE_DIRECTORY_ERROR(100_855, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_REMOVE_FILE_ERROR(100_856, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_RENAME_FILE_ERROR(100_857, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_GET_DISK_INFO_ERROR(100_858, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_GET_INTERNAL_STORAGE_INFO_ERROR(100_859, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_INVALID_DISK_INFO(100_860, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_FILE_UTIL_INVALID_INTERNAL_STORAGE_INFO(100_861, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_FILE_UTIL_CHECK_FILE_OR_DIR_EXIST_ERROR(100_862, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_MAKE_FILE_EXECUTABLE_ERROR(100_863, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_SDK_VERSION_NOT_SUPPORT(100_864, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_FILE_UTIL_GET_FILE_MD5_ERROR(100_865, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_INVALID_MD5_OUTPUT(100_866, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_FILE_UTIL_REMOUNT_ERROR(100_867, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_EXTERNAL_STORAGE_NOT_FOUND(100_868, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_FILE_UTIL_GET_EXTERNAL_STORAGE_ERROR(100_869, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_ILLEGAL_ARGUMENT(100_870, ErrorType.CUSTOMER_ISSUE),
  ANDROID_FILE_PERMISSIONS_ILLEGAL_ARGUMENT(100_871, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_REMOVE_FILE_INVALID_ARGUMENT(100_872, ErrorType.CUSTOMER_ISSUE),
  ANDROID_FILE_UTIL_CAT_FILE_EXE_ERROR(100_873, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_CAT_FILE_PARSER_ERROR(100_874, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_NO_SPACE_TO_MAKE_DIRECTORY_ERROR_IN_SHARED_LAB(100_875, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_NO_SPACE_TO_MAKE_DIRECTORY_ERROR_IN_SATELLITE_LAB(
      100_876, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_CREATE_SYMLINK_ERROR(100_877, ErrorType.INFRA_ISSUE),
  ANDROID_FILE_UTIL_MAKE_FILE_EXECUTABLE_READABLE_NOT_WRITEABLE_ERROR(
      100_878, ErrorType.INFRA_ISSUE),

  // AndroidSystemSpecUtil: 101_001 ~ 101_200
  ANDROID_SYSTEM_SPEC_USB_LOCATOR_ADB_INVALID_LINE(101_001, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_SYSTEM_SPEC_USB_LOCATOR_SERIAL_NOT_FOUND(101_002, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SPEC_USB_LOCATOR_INVALID_USB_ID(101_003, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_SYSTEM_SPEC_INVALID_ABI(101_004, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_SYSTEM_SPEC_GET_IMEI_ERROR(101_005, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SPEC_GET_MAC_ADDRESS_ERROR(101_006, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SPEC_GET_CPU_INFO_ERROR(101_007, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SPEC_NO_CPU_FOUND(101_008, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_SYSTEM_SPEC_LIST_FEATURES_ERROR(101_009, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SPEC_GET_TOTAL_MEM_ERROR(101_010, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SPEC_INVALID_TOTAL_MEM_VALUE(101_011, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_SYSTEM_SPEC_TOTAL_MEM_VALUE_NOT_FOUND(101_012, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_SYSTEM_SPEC_GET_ICCID_ERROR(101_013, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SPEC_GET_BLUETOOTH_MAC_ADDRESS_ERROR(101_014, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SPEC_QUERY_SIM_INFO_ERROR(101_015, ErrorType.INFRA_ISSUE),
  ANDROID_SYSTEM_SPEC_SENSOR_SAMPLE_ERROR(101_016, ErrorType.DEPENDENCY_ISSUE),

  // UsbUtil: 101_201 ~ 101_300
  ANDROID_USB_UTIL_RESET_USB_ERROR(101_201, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_USB_UTIL_COMMAND_EXEC_ERROR(101_202, ErrorType.INFRA_ISSUE),

  // AndroidPackageManagerUtil: 101_301 ~ 101_600
  ANDROID_PKG_MNGR_UTIL_CLEAR_PACKAGE_ERROR(101_301, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_DISABLE_PACKAGE_ERROR(101_302, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_DUMPSYS_ERROR(101_303, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_INVALID_VERSION(101_304, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_GET_VERSION_INFO_ERROR(101_305, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_GET_INSTALLED_APK_PATH_ERROR(101_306, ErrorType.INFRA_ISSUE),
  ANDROID_PKG_MNGR_UTIL_PM_PATH_NO_PACKAGE_FOUND(101_307, ErrorType.INFRA_ISSUE),
  ANDROID_PKG_MNGR_UTIL_GRANT_PERMISSION_ERROR(101_308, ErrorType.INFRA_ISSUE),
  ANDROID_PKG_MNGR_UTIL_GET_PACKAGE_NAME_ERROR(101_309, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_INSTALLATION_APP_BLACKLISTED(101_310, ErrorType.CUSTOMER_ISSUE),
  /**
   * @deprecated Please use ANDROID_PKG_MNGR_UTIL_INSTALLATION_ERROR_IN_SHARED_LAB or
   *     ANDROID_PKG_MNGR_UTIL_INSTALLATION_ERROR_IN_SATELLITE_LAB
   */
  @Deprecated
  ANDROID_PKG_MNGR_UTIL_INSTALLATION_ERROR(101_311, ErrorType.UNDETERMINED),
  ANDROID_PKG_MNGR_UTIL_INSTALLATION_ABI_INCOMPATIBLE(101_312, ErrorType.CUSTOMER_ISSUE),
  ANDROID_PKG_MNGR_UTIL_INSTALLATION_MISSING_SHARED_LIBRARY(101_313, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_INSTALLATION_UPDATE_INCOMPATIBLE(101_314, ErrorType.CUSTOMER_ISSUE),
  ANDROID_PKG_MNGR_UTIL_LIST_PACKAGES_ERROR(101_315, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_GET_PACKAGE_LIST_ERROR(101_316, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_UNINSTALLATION_ERROR(101_317, ErrorType.UNDETERMINED),
  ANDROID_PKG_MNGR_UTIL_GET_APK_ABI_ERROR(101_318, ErrorType.CUSTOMER_ISSUE),
  ANDROID_PKG_MNGR_UTIL_GET_APK_MIN_SDK_VERSION_ERROR(101_319, ErrorType.CUSTOMER_ISSUE),
  ANDROID_PKG_MNGR_UTIL_GET_APK_PACKAGE_NAME_ERROR(101_320, ErrorType.CUSTOMER_ISSUE),
  ANDROID_PKG_MNGR_UTIL_GET_APK_VERSION_CODE_ERROR(101_321, ErrorType.CUSTOMER_ISSUE),
  ANDROID_PKG_MNGR_UTIL_GET_APK_VERSION_NAME_ERROR(101_322, ErrorType.CUSTOMER_ISSUE),
  ANDROID_PKG_MNGR_UTIL_SDK_VERSION_NOT_SUPPORT(101_323, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_GET_DEVICE_PROP_ERROR(101_324, ErrorType.INFRA_ISSUE),
  ANDROID_PKG_MNGR_UTIL_GET_APEX_MODULE_VERSION_CODE_ERROR(101_325, ErrorType.INFRA_ISSUE),
  ANDROID_PKG_MNGR_UTIL_INVALID_APEX_VERSION_CODE(101_326, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_MISSING_APEX_VERSION_CODE(101_327, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_PARTIAL_INSTALL_NOT_ALLOWED_ERROR(101_328, ErrorType.CUSTOMER_ISSUE),
  ANDROID_PKG_MNGR_UTIL_INSTALLATION_VERSION_DOWNGRADE(101_329, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_INSTALLATION_INSUFFICIENT_STORAGE(101_330, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_INSTALLATION_ERROR_IN_SHARED_LAB(101_331, ErrorType.INFRA_ISSUE),
  ANDROID_PKG_MNGR_UTIL_INSTALLATION_ERROR_IN_SATELLITE_LAB(101_332, ErrorType.CUSTOMER_ISSUE),
  ANDROID_PKG_MNGR_UTIL_INSTALLATION_PARSE_FAILED_MANIFEST_MALFORMED(
      101_333, ErrorType.CUSTOMER_ISSUE),
  ANDROID_PKG_MNGR_UTIL_INSTALLATION_FAILED_INVALID_APK(101_334, ErrorType.CUSTOMER_ISSUE),
  ANDROID_PKG_MNGR_UTIL_INSTALLATION_FAILED_DUPLICATE_PERMISSION(101_335, ErrorType.CUSTOMER_ISSUE),
  ANDROID_PKG_MNGR_UTIL_INSTALLATION_FAILED_OLDER_SDK(101_336, ErrorType.CUSTOMER_ISSUE),
  ANDROID_PKG_MNGR_UTIL_INSTALLATION_FAILED_NO_VALID_UID_ASSIGNED(
      101_337, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_LIST_APEX_PACKAGES_ERROR(101_338, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_LIST_MODULES_ERROR(101_339, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_PKG_MNGR_UTIL_INSTALLATION_INVALID_APK_SPLIT_NULL(101_340, ErrorType.CUSTOMER_ISSUE),
  ANDROID_PKG_MNGR_UTIL_USER_DISABLE_PACKAGE_ERROR(101_341, ErrorType.DEPENDENCY_ISSUE),

  // AAPT: 101_601 ~ 101_800
  ANDROID_AAPT_GET_APK_ABI_ERROR(101_601, ErrorType.CUSTOMER_ISSUE),
  ANDROID_AAPT_GET_LAUNCHABLE_ACTIVITY_NAME_ERROR(101_602, ErrorType.CUSTOMER_ISSUE),
  ANDROID_AAPT_APK_INVALID_VERSION_CODE(101_603, ErrorType.CUSTOMER_ISSUE),
  ANDROID_AAPT_APK_UNMATCHED_VERSION_INFO_FORMAT(101_604, ErrorType.CUSTOMER_ISSUE),
  ANDROID_AAPT_APK_UNMATCHED_VERSION_NAME_FORMAT(101_605, ErrorType.CUSTOMER_ISSUE),
  ANDROID_AAPT_GET_APK_PACKAGE_NAME_ERROR(101_606, ErrorType.CUSTOMER_ISSUE),
  ANDROID_AAPT_COMMAND_EXEC_TIMEOUT(101_607, ErrorType.INFRA_ISSUE),
  ANDROID_AAPT_PATH_NOT_SET(101_608, ErrorType.INFRA_ISSUE),
  ANDROID_AAPT_COMMAND_START_ERROR(101_609, ErrorType.INFRA_ISSUE),

  // Dexdump Util: 101_801 ~ 101_850
  ANDROID_DEX_DUMP_NO_DEX_ENTRIES(101_801, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_DEX_DUMP_GET_DEX_DUMP_FILES_ERROR(101_802, ErrorType.INFRA_ISSUE),
  ANDROID_DEX_DUMP_GET_DEXES_FROM_APK_ERROR(101_803, ErrorType.INFRA_ISSUE),
  ANDROID_DEX_DUMP_CMD_EXEC_ERROR(101_804, ErrorType.DEPENDENCY_ISSUE),

  // AndroidProcessUtil: 101_851 ~ 102_100
  ANDROID_PROCESS_GET_PROCESS_ID_ERROR(101_851, ErrorType.INFRA_ISSUE),
  ANDROID_PROCESS_DUMP_HEAP_ERROR(101_852, ErrorType.INFRA_ISSUE),
  ANDROID_PROCESS_GET_PROCESS_STATUS_ERROR(101_853, ErrorType.INFRA_ISSUE),
  ANDROID_PROCESS_DUMPSYS_SERVICE_ERROR(101_854, ErrorType.INFRA_ISSUE),
  ANDROID_PROCESS_START_APP_ERROR(101_855, ErrorType.UNDETERMINED),
  ANDROID_PROCESS_START_APP_BY_INTENT_ERROR(101_856, ErrorType.UNDETERMINED),
  ANDROID_PROCESS_STOP_PROCESS_ERROR(101_857, ErrorType.INFRA_ISSUE),
  ANDROID_PROCESS_UPDATE_SERVICE_ERROR(101_858, ErrorType.UNDETERMINED),
  ANDROID_PROCESS_STOP_PROCESS_TIMEOUT(101_859, ErrorType.CUSTOMER_ISSUE),

  // AndroidAccountManagerUtil: 102_101 ~ 102_250
  ANDROID_ACCOUNT_MNGR_UTIL_ADD_ACCOUNT_BY_ACCOUNT_UTIL_ERROR(102_101, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_ACCOUNT_MNGR_UTIL_ADD_ACCOUNT_BY_ACCOUNT_MNGR_ERROR(102_102, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_ACCOUNT_MNGR_UTIL_WRONG_PASSWORD(102_103, ErrorType.CUSTOMER_ISSUE),
  ANDROID_ACCOUNT_MNGR_UTIL_REMOVE_ACCOUNT_ERROR(102_104, ErrorType.INFRA_ISSUE),
  ANDROID_ACCOUNT_MNGR_UTIL_REMOVE_ACCOUNT_BY_ACCOUNT_UTIL_ERROR(102_105, ErrorType.CUSTOMER_ISSUE),

  // AndroidUserUtil: 102_251 ~ 102_400
  ANDROID_USER_UTIL_CREATE_WORK_PROFILE_ERROR(102_251, ErrorType.INFRA_ISSUE),
  ANDROID_USER_UTIL_LIST_USERS_ERROR(102_252, ErrorType.INFRA_ISSUE),
  ANDROID_USER_UTIL_GET_MAX_USERS_ERROR(102_253, ErrorType.INFRA_ISSUE),
  ANDROID_USER_UTIL_REMOVE_USER_ERROR(102_254, ErrorType.INFRA_ISSUE),
  ANDROID_USER_UTIL_START_USER_ERROR(102_255, ErrorType.INFRA_ISSUE),
  ANDROID_USER_UTIL_GET_FOREGROUND_USER_ERROR(102_256, ErrorType.INFRA_ISSUE),
  ANDROID_USER_UTIL_USER_NOT_EXIST(102_257, ErrorType.INFRA_ISSUE),
  ANDROID_USER_UTIL_SWITCH_USER_ERROR(102_258, ErrorType.INFRA_ISSUE),
  ANDROID_USER_UTIL_GET_RUNNING_USER_ERROR(102_259, ErrorType.INFRA_ISSUE),
  ANDROID_USER_UTIL_CREATE_USER_ERROR(102_260, ErrorType.INFRA_ISSUE),
  ANDROID_USER_UTIL_SDK_VERSION_NOT_SUPPORT(102_261, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_USER_UTIL_WAIT_FOR_USER_NOT_READY(102_262, ErrorType.DEPENDENCY_ISSUE),

  // AndroidMediaUtil: 102_401 ~ 102_600
  ANDROID_MEDIA_UTIL_ENTER_VR_MODE_ERROR(102_401, ErrorType.INFRA_ISSUE),
  ANDROID_MEDIA_UTIL_INPUT_TEXT_ERROR(102_402, ErrorType.INFRA_ISSUE),
  ANDROID_MEDIA_UTIL_RECORD_SCREEN_ERROR(102_403, ErrorType.INFRA_ISSUE),
  ANDROID_MEDIA_UTIL_ROTATE_SCREEN_ERROR(102_404, ErrorType.INFRA_ISSUE),
  ANDROID_MEDIA_UTIL_SET_ACCELEROMETER_ROTATION_ERROR(102_405, ErrorType.INFRA_ISSUE),
  ANDROID_MEDIA_UTIL_START_SCREEN_RECORD_VR_ERROR(102_406, ErrorType.INFRA_ISSUE),
  ANDROID_MEDIA_UTIL_STOP_SCREEN_RECORD_TIMEOUT(102_407, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_MEDIA_UTIL_STOP_SCREEN_RECORD_VR_ERROR(102_408, ErrorType.INFRA_ISSUE),
  ANDROID_MEDIA_UTIL_TAKE_SCREEN_SHOT_ERROR(102_409, ErrorType.INFRA_ISSUE),
  ANDROID_MEDIA_UTIL_GET_SCREEN_ORIENTATION_ERROR(102_410, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_MEDIA_UTIL_DUMPSYS_ORIENTATION_INFO_ERROR(102_411, ErrorType.INFRA_ISSUE),

  // Adb: 102_601 ~ 102_700
  ANDROID_ADB_SYNC_CMD_EXECUTION_ERROR(102_601, ErrorType.UNDETERMINED),
  ANDROID_ADB_SYNC_CMD_EXECUTION_TIMEOUT(102_602, ErrorType.UNDETERMINED),
  ANDROID_ADB_SYNC_CMD_EXECUTION_FAILURE(102_603, ErrorType.UNDETERMINED),
  ANDROID_ADB_CMD_RETRY_ERROR(102_604, ErrorType.UNDETERMINED),
  ANDROID_ADB_SYNC_CMD_START_ERROR(102_605, ErrorType.UNDETERMINED),
  ANDROID_ADB_SHELL_RETRY_ERROR(102_606, ErrorType.UNDETERMINED),
  ANDROID_ADB_SHELL_START_ERROR(102_607, ErrorType.UNDETERMINED),
  ANDROID_ADB_ASYNC_CMD_START_ERROR(102_608, ErrorType.UNDETERMINED),
  ANDROID_ADB_WITHOUT_DEVICE_BLOCK_FEATURE_FOR_SANDBOX(102_609, ErrorType.CUSTOMER_ISSUE),
  ANDROID_ADB_CHECK_DEVICE_SANDBOX_SUPPORT_ERROR(102_610, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_ADB_SYNC_CMD_EXECUTION_ASSERTION_FAILURE(102_611, ErrorType.CUSTOMER_ISSUE),

  // AndroidAdbInternalUtil: 102_701 ~ 102_900
  ANDROID_ADB_INTERNAL_UTIL_INVALID_ADB_LINE_FORMAT(102_701, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_ADB_INTERNAL_UTIL_GET_DEVICE_SERIALS_CMD_ERROR(102_702, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_ADB_INTERNAL_UTIL_ADB_KILL_SERVER_ERROR(102_703, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_INTERNAL_UTIL_GET_ADB_VERSION_ERROR(102_704, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_ADB_INTERNAL_UTIL_CONNECT_CMD_ERROR(102_705, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_INTERNAL_UTIL_CONNECT_ERROR(102_706, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_ADB_INTERNAL_UTIL_DISCONNECT_CMD_ERROR(102_707, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_INTERNAL_UTIL_DISCONNECT_ERROR(102_708, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_ADB_INTERNAL_UTIL_DEVICE_DETACH_ERROR(102_709, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_INTERNAL_UTIL_DEVICE_ATTACH_ERROR(102_710, ErrorType.INFRA_ISSUE),
  ANDROID_ADB_INTERNAL_UTIL_GET_HOST_FEATURES_ERROR(102_711, ErrorType.INFRA_ISSUE),

  /** Android Devices: 110_001 ~ 115_000 */
  // Android Detector: 110_001 ~ 110_200
  ANDROID_DM_DETECTOR_ADB_ERROR(110_001, ErrorType.INFRA_ISSUE),
  ANDROID_DM_DETECTOR_FASTBOOT_ERROR(110_002, ErrorType.INFRA_ISSUE),
  ANDROID_DM_DETECTOR_EMULATOR_RECONNECT_ERROR(110_003, ErrorType.INFRA_ISSUE),

  // AndroidRealDevice: 110_201 ~ 110_300
  ANDROID_REAL_DEVICE_RECOVER_RECOVERY_DEVICE_FAILED(110_201, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_REAL_DEVICE_REBOOT_FASTBOOT_DEVICE_TO_RECOVER(110_202, ErrorType.CUSTOMER_ISSUE),
  ANDROID_REAL_DEVICE_ONLINE_DEVICE_NOT_READY(110_203, ErrorType.INFRA_ISSUE),
  ANDROID_REAL_DEVICE_PASSTHROUGH_TO_SANDBOX_TIMEOUT(110_204, ErrorType.INFRA_ISSUE),
  ANDROID_REAL_DEVICE_PASSTHROUGH_FROM_SANDBOX_TIMEOUT(110_205, ErrorType.INFRA_ISSUE),
  ANDROID_REAL_DEVICE_USB_LOCATOR_NOT_SET(110_206, ErrorType.INFRA_ISSUE),
  ANDROID_REAL_DEVICE_PASSTHROUGH_FAILED(110_207, ErrorType.INFRA_ISSUE),
  ANDROID_REAL_DEVICE_PUT_PROXY_ERROR(110_208, ErrorType.INFRA_ISSUE),

  // AndroidFlashableWatchDevice: 110_301 ~ 110_400
  ANDROID_FLASHABLE_WATCH_DEVICE_MISSING_HARDWARE_ERROR(110_301, ErrorType.INFRA_ISSUE),

  // AndroidDeviceDelegate: 111_001 ~ 111_200
  ANDROID_DEVICE_DELEGATE_TEST_PREP_ERROR(111_001, ErrorType.INFRA_ISSUE),

  // AndroidRealDeviceDelegate: 111_201 ~ 111_300
  ANDROID_REAL_DEVICE_DELEGATE_RECOVERY_DEVICE_TO_REBOOT(111_201, ErrorType.INFRA_ISSUE),
  ANDROID_REAL_DEVICE_DELEGATE_FASTBOOT_DEVICE_TO_REBOOT(111_202, ErrorType.INFRA_ISSUE),
  ANDROID_REAL_DEVICE_DELEGATE_UNDETECTED_DURING_INIT(111_203, ErrorType.INFRA_ISSUE),

  // XTSTradefedTest driver: 115_851 ~ 115_950
  XTS_TRADEFED_TEST_FAIL(115_851, ErrorType.CUSTOMER_ISSUE),
  XTS_TRADEFED_TEST_FAIL_NUM_PARSE_ERROR(115_852, ErrorType.INFRA_ISSUE),
  XTS_TRADEFED_INVOCATION_SUMMARY_UNEXPECTED_FORMAT(115_853, ErrorType.INFRA_ISSUE),
  XTS_TRADEFED_RUN_COMMAND_ERROR(115_854, ErrorType.INFRA_ISSUE),
  XTS_TRADEFED_INVOCATION_SUMMARY_NOT_FOUND(115_855, ErrorType.INFRA_ISSUE),
  XTS_TRADEFED_RUN_COMMAND_TIMEOUT(115_856, ErrorType.INFRA_ISSUE),
  XTS_TRADEFED_START_COMMAND_ERROR(115_857, ErrorType.INFRA_ISSUE),
  XTS_TRADEFED_CREATE_TEMP_DIR_ERROR(115_858, ErrorType.INFRA_ISSUE),
  XTS_TRADEFED_CREATE_SYMLINK_ERROR(115_859, ErrorType.INFRA_ISSUE),
  XTS_TRADEFED_CREATE_SYMLINK_UNSUPPORTED_ERROR(115_860, ErrorType.INFRA_ISSUE),
  XTS_TRADEFED_LIST_JARS_ERROR(115_861, ErrorType.INFRA_ISSUE),
  XTS_TRADEFED_SDK_TOOL_NOT_FOUND_ERROR(115_862, ErrorType.CUSTOMER_ISSUE),
  XTS_TRADEFED_GET_XTS_ROOT_DIR_ERROR(115_863, ErrorType.CUSTOMER_ISSUE),
  XTS_TRADEFED_COMMAND_OUTPUT_FILE_ERROR(115_864, ErrorType.INFRA_ISSUE),

  // AndroidDeviceSettingsDecorator: 127_001 ~ 127_300
  ANDROID_DEVICE_SETTINGS_DECORATOR_COMMAND_EXEC_ERROR(127_001, ErrorType.INFRA_ISSUE),
  ANDROID_DEVICE_SETTINGS_DECORATOR_GET_CPU_NUM_ERROR(127_002, ErrorType.INFRA_ISSUE),
  ANDROID_DEVICE_SETTINGS_DECORATOR_SETTINGS_CONFLICT(127_003, ErrorType.CUSTOMER_ISSUE),
  ANDROID_DEVICE_SETTINGS_DECORATOR_SETTING_NOT_SUPPORT_IN_SHARED_LAB(
      127_004, ErrorType.CUSTOMER_ISSUE),
  ANDROID_DEVICE_SETTINGS_DECORATOR_DEVICE_NOT_SUPPORT_REBOOT(127_005, ErrorType.CUSTOMER_ISSUE),
  ANDROID_DEVICE_SETTINGS_DECORATOR_GET_DEVICE_PROPERTY_ERROR(127_006, ErrorType.INFRA_ISSUE),
  ANDROID_DEVICE_SETTINGS_DECORATOR_CHECK_DIR_OR_FILE_ON_DEVICE_ERROR(
      127_007, ErrorType.INFRA_ISSUE),
  ANDROID_DEVICE_SETTINGS_DECORATOR_DEVICE_TEMPERATURE_TOO_HIGH(
      127_008, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_DEVICE_SETTINGS_DECORATOR_SYNC_DEVICE_SYSTEM_TIME_ERROR(127_009, ErrorType.INFRA_ISSUE),
  ANDROID_DEVICE_SETTINGS_DECORATOR_GET_DEVICE_SDK_VERSION_ERROR(127_010, ErrorType.INFRA_ISSUE),
  ANDROID_DEVICE_SETTINGS_DECORATOR_SET_DM_VERITY_ERROR(127_011, ErrorType.INFRA_ISSUE),
  ANDROID_DEVICE_SETTINGS_DECORATOR_DEVICE_INIT_AFTER_REBOOT_ERROR(127_012, ErrorType.INFRA_ISSUE),
  ANDROID_DEVICE_SETTINGS_DECORATOR_CHECK_LAB_EXPERIMENT_ERROR(127_013, ErrorType.INFRA_ISSUE),
  ANDROID_DEVICE_SETTINGS_DECORATOR_SETTING_NOT_SUPPORT_ERROR(127_014, ErrorType.CUSTOMER_ISSUE),
  ANDROID_DEVICE_SETTINGS_DECORATOR_STOP_INVALID_SERVICE_ERROR(127_015, ErrorType.CUSTOMER_ISSUE),

  // AndroidCleanAppsDecorator: 127_401 ~ 127_600
  ANDROID_CLEAN_APPS_DECORATOR_INVALID_PARAM_VALUE(127_401, ErrorType.CUSTOMER_ISSUE),
  ANDROID_CLEAN_APPS_DECORATOR_CANNOT_REMOVE_1P_APP_IN_NONROOT_DEVICE(
      127_402, ErrorType.CUSTOMER_ISSUE),

  // AndroidSwitchUserDecorator: 128_051 ~ 128_100
  ANDROID_SWITCH_USER_DECORATOR_USER_SWITCH_TIMEOUT(128_051, ErrorType.INFRA_ISSUE),
  ANDROID_SWITCH_USER_DECORATOR_VARIANT_MISSING(128_052, ErrorType.INFRA_ISSUE),
  ANDROID_SWITCH_USER_DECORATOR_USER_MISSING(128_053, ErrorType.INFRA_ISSUE),
  ANDROID_SWITCH_USER_DECORATOR_NOT_SUPPORTED(128_054, ErrorType.CUSTOMER_ISSUE),
  ANDROID_SWITCH_USER_DECORATOR_CREATE_TEST_USER_ERROR(128_055, ErrorType.INFRA_ISSUE),

  // AndroidDeviceFeaturesCheckDecorator: 129_351 ~ 129_400
  ANDROID_DEVICE_FEATURES_CHECK_DECORATOR_CEHCK_FAILURE(129_351, ErrorType.DEPENDENCY_ISSUE),

  // AndroidMainlineModulesCheckDecorator: 129_401 ~ 129_450
  ANDROID_MAINLINE_MODULES_CHECK_DECORATOR_SKIP_TEST(129_401, ErrorType.DEPENDENCY_ISSUE),

  // AndroidFeatureFlagDecorator: 129_451 ~ 129_500
  ANDROID_FEATURE_FLAG_DECORATOR_FLAG_FORMAT_CEHCK_FAILURE(129_451, ErrorType.CUSTOMER_ISSUE),
  ANDROID_FEATURE_FLAG_DECORATOR_INVALID_FLAG_ERROR(129_452, ErrorType.CUSTOMER_ISSUE),
  ANDROID_FEATURE_FLAG_DECORATOR_READ_ONLY_FLAG(129_453, ErrorType.CUSTOMER_ISSUE),
  ANDROID_FEATURE_FLAG_DECORATOR_DEVICE_INIT_AFTER_REBOOT_ERROR(129_454, ErrorType.INFRA_ISSUE),

  // AndroidMinSdkVersionDecorator: 129_501 ~ 129_550
  ANDROID_MIN_SDK_VERSION_DECORATOR_SDK_VERSION_TOO_LOW(129_501, ErrorType.CUSTOMER_ISSUE),

  // AndroidAtsDynamicConfigPusherDecorator: 129_551 ~ 129_600
  ANDROID_ATS_DYNAMIC_CONFIG_PUSHER_DECORATOR_PARAM_NOT_SUPPORTED(
      129_551, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_ATS_DYNAMIC_CONFIG_PUSHER_DECORATOR_LOCAL_CONFIG_NOT_FOUND(
      129_552, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_ATS_DYNAMIC_CONFIG_PUSHER_DECORATOR_PARTNER_SERVER_ERROR(
      129_553, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_ATS_DYNAMIC_CONFIG_PUSHER_DECORATOR_MERGE_DYNAMIC_CONFIG_ERROR(
      129_554, ErrorType.DEPENDENCY_ISSUE),

  // InstallSystemApkStep: 134_851 ~ 134_899
  ANDROID_INSTALL_SYSTEM_APK_PATHS_NUM_NOT_THE_SAME(134_851, ErrorType.CUSTOMER_ISSUE),
  ANDROID_INSTALL_SYSTEM_APK_PERMISSION_FILES_NUM_NOT_THE_SAME(134_852, ErrorType.CUSTOMER_ISSUE),

  // InstallApkStep:134_900 ~ 134_950
  ANDROID_INSTALL_APK_STEP_INSTALL_NOT_SUPPORTED(134_900, ErrorType.CUSTOMER_ISSUE),
  ANDROID_INSTALL_APK_STEP_DEX_METADATA_WITHOUT_APK(134_901, ErrorType.CUSTOMER_ISSUE),
  ANDROID_INSTALL_APK_STEP_DEX_METADATA_CONFLICT(134_902, ErrorType.CUSTOMER_ISSUE),
  ANDROID_INSTALL_APK_STEP_REBOOT_ERROR(134_903, ErrorType.INFRA_ISSUE),

  /** Plugins: 135_001 ~ 140_000 */

  // XtsDeviceCompatibilityChecker.java: 135_521 ~ 135_540
  XTS_DEVICE_COMPAT_CHECKER_DEVICE_BUILD_NOT_MATCH_RETRY_PREV_SESSION(
      135_521, ErrorType.CUSTOMER_ISSUE),
  XTS_DEVICE_COMPAT_CHECKER_CHECK_DEVICE_BUILD_ERROR(135_522, ErrorType.INFRA_ISSUE),

  /** Android Apps: 140_001 ~ 145_000 */
  // Device Daemon app: 140_001 ~ 140_020
  ANDROID_DEVICE_DAEMON_V1_APK_NOT_FOUND(140_001, ErrorType.INFRA_ISSUE),
  ANDROID_DEVICE_DAEMON_V2_APK_NOT_FOUND(140_002, ErrorType.INFRA_ISSUE),

  // DeviceDaemonHelper: 140_021 ~ 140_120
  ANDROID_DEVICE_DAEMON_HELPER_GET_SDK_ERROR(140_021, ErrorType.INFRA_ISSUE),
  ANDROID_DEVICE_DAEMON_HELPER_INSTALL_DAEMON_ERROR(140_022, ErrorType.INFRA_ISSUE),
  ANDROID_DEVICE_DAEMON_HELPER_START_DAEMON_ERROR(140_023, ErrorType.INFRA_ISSUE),
  ANDROID_DEVICE_DAEMON_HELPER_GET_NETWORK_SSID_ERROR(140_024, ErrorType.INFRA_ISSUE),
  ANDROID_DEVICE_DAEMON_HELPER_UNINSTALL_DAEMON_ERROR(140_025, ErrorType.INFRA_ISSUE),
  ANDROID_DEVICE_DAEMON_HELPER_GRANT_DAEMON_PERMISSION_ERROR(140_026, ErrorType.INFRA_ISSUE),

  // AndroidDeviceSettingsDecoratorEnvValidator: 145_041 ~ 145_060
  ANDROID_DEVICE_SETTINGS_DECORATOR_ENV_VALIDATOR_DEVICE_NOT_SUPPORTED(
      145_041, ErrorType.CUSTOMER_ISSUE),

  /** MH Lightning API: 150_001 ~ 155_000 */
  // ApkInstaller: 150_001 ~ 150_200
  ANDROID_APK_INSTALLER_UPDATE_DIMENSION_ERROR(150_001, ErrorType.INFRA_ISSUE),
  ANDROID_APK_INSTALLER_DEVICE_SDK_TOO_LOW(150_002, ErrorType.CUSTOMER_ISSUE),
  ANDROID_APK_INSTALLER_GMS_INCOMPATIBLE(150_003, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_APK_INSTALLER_INVALID_GMS_VERSION(150_004, ErrorType.DEPENDENCY_ISSUE),
  ANDROID_APK_INSTALLER_APPLY_MULTI_PACKAGE_INSTALL_TO_GMS(150_005, ErrorType.CUSTOMER_ISSUE),

  // SystemStateManager: 150_201 ~ 150_400
  SYSTEM_STATE_MANAGER_REBOOT_DEVICE_ERROR(150_201, ErrorType.INFRA_ISSUE),

  // AdbShell: 150_801 ~ 150_900
  ADB_SHELL_COMMAND_EMPTY_ARGS(150_801, ErrorType.CUSTOMER_ISSUE),
  ADB_SHELL_COMMAND_INVALID_ARGS(150_802, ErrorType.CUSTOMER_ISSUE),

  // ***********************************************************************************************
  // Other Variant Android Platforms: 170_001 ~ 200_000
  // ***********************************************************************************************

  /** Device Action: 174_101 ~ 174_200 */
  DEVICE_ACTION_VALIDATION_FAILURE(174_101, ErrorType.CUSTOMER_ISSUE),
  DEVICE_ACTION_RESOURCE_CREATE_ERROR(174_102, ErrorType.DEPENDENCY_ISSUE),
  DEVICE_ACTION_CONFIG_CREATE_ERROR(174_103, ErrorType.DEPENDENCY_ISSUE),
  DEVICE_ACTION_EXECUTION_ERROR(174_104, ErrorType.DEPENDENCY_ISSUE),

  /** Xts dynamic downloader: 175_001 ~ 175_100 */
  XTS_DYNAMIC_DOWNLOADER_DEVICE_ABI_NOT_SUPPORT(175_001, ErrorType.DEPENDENCY_ISSUE),
  XTS_DYNAMIC_DOWNLOADER_CONFIG_READER_ERROR(175_002, ErrorType.INFRA_ISSUE),
  XTS_DYNAMIC_DOWNLOADER_DEVICE_SDK_VERSION_NOT_SUPPORT(175_003, ErrorType.DEPENDENCY_ISSUE),
  XTS_DYNAMIC_DOWNLOADER_FILE_DOWNLOAD_ERROR(175_004, ErrorType.UNDETERMINED),
  XTS_DYNAMIC_DOWNLOADER_FILE_NOT_FOUND(175_005, ErrorType.UNDETERMINED),

  ANDROID_ERROR_ID_PLACE_HOLDER_TO_BE_RENAMED(200_000, ErrorType.UNDETERMINED);

  public static final int MIN_CODE = ExtErrorId.MAX_CODE + 1;
  public static final int MAX_CODE = 200_000;

  private final int code;
  private final ErrorType type;

  AndroidErrorId(int code, ErrorType type) {
    Preconditions.checkArgument(code >= MIN_CODE);
    Preconditions.checkArgument(code <= MAX_CODE);
    Preconditions.checkArgument(type != ErrorType.UNCLASSIFIED);
    this.code = code;
    this.type = type;
  }

  @Override
  public int code() {
    return code;
  }

  @Override
  public ErrorType type() {
    return type;
  }

  @Override
  public String toString() {
    return ErrorIdFormatter.formatErrorId(this);
  }
}
