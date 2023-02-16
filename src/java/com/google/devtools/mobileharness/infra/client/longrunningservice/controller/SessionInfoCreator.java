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

package com.google.devtools.mobileharness.infra.client.longrunningservice.controller;

import com.google.common.collect.ImmutableList;
import com.google.devtools.mobileharness.infra.client.longrunningservice.model.SessionDetailHolder;
import com.google.devtools.mobileharness.infra.client.longrunningservice.model.SessionInfo;
import com.google.devtools.mobileharness.infra.client.longrunningservice.proto.SessionProto.SessionConfig;
import com.google.wireless.qa.mobileharness.shared.model.job.JobInfo;

/** Creator for creating {@link SessionInfo}. */
public class SessionInfoCreator {

  /**
   * Creates a {@link SessionInfo} and generates job list of the session based on {@link
   * SessionConfig}.
   */
  public SessionInfo create(SessionDetailHolder sessionDetailHolder) {
    return new SessionInfo(sessionDetailHolder, createJobs(sessionDetailHolder.getSessionConfig()));
  }

  private ImmutableList<JobInfo> createJobs(
      @SuppressWarnings("unused") SessionConfig sessionConfig) {
    // TODO: Create jobs from SessionConfig.
    return ImmutableList.of();
  }
}
