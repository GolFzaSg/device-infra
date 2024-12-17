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

package com.google.devtools.mobileharness.infra.controller.plugin.provider;

import com.google.common.collect.ImmutableList;
import com.google.devtools.mobileharness.api.model.error.BasicErrorId;
import com.google.devtools.mobileharness.api.model.error.MobileHarnessException;
import com.google.inject.Module;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Provides plugin {@link Module} classes from a list of canonical class names provided. */
public class NamedPluginModuleClassProvider implements PluginModuleClassProvider {

  private final ImmutableList<String> classNames;
  private final ClassLoader classLoader;

  /**
   * @param classNames The canonical class names of the plugin module classes.
   * @param classLoader What to use to load the classes.
   */
  public NamedPluginModuleClassProvider(Collection<String> classNames, ClassLoader classLoader) {
    this.classNames = ImmutableList.copyOf(classNames);
    this.classLoader = classLoader;
  }

  @Override
  public Set<Class<? extends Module>> getPluginModuleClasses() throws MobileHarnessException {
    Set<Class<? extends Module>> pluginClasses = new HashSet<>();

    for (String className : classNames) {
      try {
        pluginClasses.add(classLoader.loadClass(className).asSubclass(Module.class));
      } catch (ClassNotFoundException | ClassCastException e) {
        throw new MobileHarnessException(
            BasicErrorId.PLUGIN_LOADER_FAILED_TO_CREATE_PLUGIN_INSTANCE, "Error loading class.", e);
      }
    }

    return pluginClasses;
  }
}
