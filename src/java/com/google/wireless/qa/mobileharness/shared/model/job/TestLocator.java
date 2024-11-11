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

package com.google.wireless.qa.mobileharness.shared.model.job;

import com.google.common.base.Preconditions;
import java.util.Objects;
import javax.annotation.Nullable;

/** Locator of a test. */
public class TestLocator {
  private final String id;
  private final String name;
  private final JobLocator jobLocator;

  /**
   * Creates a test locator.
   *
   * @param id test ID
   * @param name test name
   * @param jobLocator locator of the belonging job
   */
  public TestLocator(String id, String name, JobLocator jobLocator) {
    this.id = Preconditions.checkNotNull(id);
    this.name = Preconditions.checkNotNull(name);
    this.jobLocator = Preconditions.checkNotNull(jobLocator, "Job locator is not specified");
  }

  public TestLocator(com.google.devtools.mobileharness.api.model.job.TestLocator newTestLocator) {
    this(newTestLocator.id(), newTestLocator.name(), new JobLocator(newTestLocator.jobLocator()));
  }

  /** Gets the test ID. */
  public String getId() {
    return id;
  }

  /** Gets the test name. */
  public String getName() {
    return name;
  }

  /** Gets the locator of the belong job. */
  public JobLocator getJobLocator() {
    return jobLocator;
  }

  @Override
  public String toString() {
    return String.format("%s@%s", id, jobLocator);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TestLocator)) {
      return false;
    }
    TestLocator that = (TestLocator) o;
    return Objects.equals(id, that.id)
        && Objects.equals(name, that.name)
        && Objects.equals(jobLocator, that.jobLocator);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, jobLocator);
  }

  public com.google.devtools.mobileharness.api.model.job.TestLocator toNewTestLocator() {
    return com.google.devtools.mobileharness.api.model.job.TestLocator.of(
        id, name, jobLocator.toNewJobLocator());
  }

  /**
   * Parses a test locator from the string generated by {@link #toString()}.
   *
   * @return the test locator with empty test name because {@link #toString()} drops the test name
   *     info, or null if the input is not a valid test locator string
   */
  @Nullable
  public static TestLocator tryParseString(String str) {
    int idx = str.indexOf('@');
    if (idx <= 0 || idx == str.length() - 1) {
      return null;
    }
    return new TestLocator(
        str.substring(0, idx), "", JobLocator.parseString(str.substring(idx + 1)));
  }
}
