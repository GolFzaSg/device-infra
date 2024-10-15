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

package com.google.devtools.mobileharness.infra.client.longrunningservice;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/** Annotations for OLC server. */
public class Annotations {

  /** Whether the database is enabled. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface EnableDatabase {}

  /** OLC database connections. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface OlcDatabaseConnections {}

  /** Start time of the server. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ServerStartTime {}

  /** Generated file dir of a session. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface SessionGenDir {}

  /** Temp dir of a session. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface SessionTempDir {}

  private Annotations() {}
}
