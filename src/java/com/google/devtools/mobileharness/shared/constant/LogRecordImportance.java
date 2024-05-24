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

package com.google.devtools.mobileharness.shared.constant;

import com.google.common.flogger.MetadataKey;

/** Constants of importance of log records. */
public class LogRecordImportance {

  /** Values of importance. */
  public enum Importance {
    DEBUG(50),

    /**
     * Logs from flogger have this importance by default.
     *
     * <p>ATS console logs whose importance is not less than it will be shown in ATS console.
     */
    NORMAL(100),

    /**
     * Logs from TestInfo/JobInfo log() have this importance by default.
     *
     * <p>OLC server logs whose importance is not less than it will be shown in ATS console.
     */
    TEST_INFO(150),
    IMPORTANT(200),

    /** Logs from TF process have this importance. */
    TF(300);

    private final int value;

    Importance(int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }
  }

  public static final MetadataKey<Importance> IMPORTANCE =
      MetadataKey.single("importance", Importance.class);

  private LogRecordImportance() {}
}
