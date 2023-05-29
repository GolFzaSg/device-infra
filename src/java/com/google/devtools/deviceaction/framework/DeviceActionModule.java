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

package com.google.devtools.deviceaction.framework;

import com.google.devtools.deviceaction.common.annotations.GuiceAnnotations.FileResolver;
import com.google.devtools.deviceaction.common.annotations.GuiceAnnotations.GCSCredential;
import com.google.devtools.deviceaction.common.annotations.GuiceAnnotations.GenFileDirRoot;
import com.google.devtools.deviceaction.common.error.DeviceActionException;
import com.google.devtools.deviceaction.common.utils.AaptUtil;
import com.google.devtools.deviceaction.common.utils.BundletoolUtil;
import com.google.devtools.deviceaction.common.utils.CompositeResolver;
import com.google.devtools.deviceaction.common.utils.FlagBasedResourceHelper;
import com.google.devtools.deviceaction.common.utils.GCSResolver;
import com.google.devtools.deviceaction.common.utils.LocalFileResolver;
import com.google.devtools.deviceaction.common.utils.Resolver;
import com.google.devtools.deviceaction.common.utils.ResourceHelper;
import com.google.devtools.deviceaction.framework.deviceconfigs.DeviceConfigDao;
import com.google.devtools.deviceaction.framework.deviceconfigs.ResourceFileDao;
import com.google.devtools.deviceinfra.shared.util.time.Sleeper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.io.File;
import javax.inject.Singleton;

/** Module that configures Device Action. */
public final class DeviceActionModule extends AbstractModule {

  public DeviceActionModule() {}

  @Override
  protected void configure() {
    bind(BundletoolUtil.class).in(Singleton.class);
    bind(AaptUtil.class).in(Singleton.class);
    bind(DeviceConfigDao.class).to(ResourceFileDao.class).in(Singleton.class);
    bind(ActionConfigurer.class).to(MergingDeviceConfigurer.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  @FileResolver
  static Resolver provideResolver(GCSResolver gcsResolver) throws DeviceActionException {
    return CompositeResolver.toBuilder()
        .addResolver(LocalFileResolver.getInstance())
        .addResolver(gcsResolver)
        .build();
  }

  @Provides
  @GenFileDirRoot
  static File provideGenFileDirRoot(ResourceHelper resourceHelper) throws DeviceActionException {
    return resourceHelper.getGenFileDir().toFile();
  }

  @Provides
  @GCSCredential
  static File provideGCSCredential(ResourceHelper resourceHelper) {
    return resourceHelper.getCredFile().get().toFile();
  }

  @Provides
  @Singleton
  static ResourceHelper provideResourceHelper() {
    return FlagBasedResourceHelper.getInstance();
  }

  @Provides
  static Sleeper provideSleeper() {
    return Sleeper.defaultSleeper();
  }
}
