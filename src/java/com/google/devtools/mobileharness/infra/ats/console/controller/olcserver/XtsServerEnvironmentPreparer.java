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

package com.google.devtools.mobileharness.infra.ats.console.controller.olcserver;

import static com.google.devtools.mobileharness.shared.constant.LogRecordImportance.IMPORTANCE;
import static com.google.devtools.mobileharness.shared.constant.LogRecordImportance.Importance.DEBUG;

import com.google.common.flogger.FluentLogger;
import com.google.devtools.mobileharness.api.model.error.MobileHarnessException;
import com.google.devtools.mobileharness.infra.ats.common.olcserver.ServerEnvironmentPreparer;
import com.google.devtools.mobileharness.infra.ats.console.ConsoleInfo;
import com.google.devtools.mobileharness.infra.ats.console.util.command.CommandHelper;
import com.google.devtools.mobileharness.platform.android.xts.common.util.XtsCommandUtil;
import com.google.devtools.mobileharness.platform.android.xts.common.util.XtsDirUtil;
import com.google.devtools.mobileharness.shared.util.file.local.LocalFileUtil;
import com.google.devtools.mobileharness.shared.util.flags.Flags;
import java.nio.file.Path;
import javax.inject.Inject;

/**
 * {@link ServerEnvironmentPreparer} implementation which copies JDK and binaries from xTS dir to
 * xTS resource dir.
 */
public class XtsServerEnvironmentPreparer implements ServerEnvironmentPreparer {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ConsoleInfo consoleInfo;
  private final CommandHelper commandHelper;
  private final LocalFileUtil localFileUtil;

  @Inject
  XtsServerEnvironmentPreparer(
      ConsoleInfo consoleInfo, CommandHelper commandHelper, LocalFileUtil localFileUtil) {
    this.consoleInfo = consoleInfo;
    this.commandHelper = commandHelper;
    this.localFileUtil = localFileUtil;
  }

  @Override
  public ServerEnvironment prepareServerEnvironment()
      throws MobileHarnessException, InterruptedException {
    Path serverBinary;
    Path javaBinary;

    String xtsType = commandHelper.getXtsType();
    Path xtsRootDir = consoleInfo.getXtsRootDirectoryNonEmpty();
    Path initialServerBinary = Path.of(Flags.instance().atsConsoleOlcServerPath.getNonNull());

    if (Flags.instance().atsConsoleOlcServerCopyServerResource.getNonNull()) {
      // Prepares server resource dir.
      Path serverResDir = Path.of(Flags.instance().xtsResDirRoot.getNonNull(), "olc_server_res");
      logger.atInfo().with(IMPORTANCE, DEBUG).log("Preparing xTS resource dir [%s]", serverResDir);
      localFileUtil.prepareDir(serverResDir);
      grantFileOrDirFullAccess(serverResDir);

      // Removes all files under server resource dir.
      logger
          .atInfo()
          .with(IMPORTANCE, DEBUG)
          .log("Cleaning up xTS resource dir [%s]", serverResDir);
      localFileUtil.removeFilesOrDirs(serverResDir);

      // Copies server binary into server resource dir and grants access.
      logger
          .atInfo()
          .with(IMPORTANCE, DEBUG)
          .log(
              "Copying OLC server binary [%s] into xTS resource dir [%s]",
              initialServerBinary, serverResDir);
      localFileUtil.checkFile(initialServerBinary);
      localFileUtil.copyFileOrDir(initialServerBinary, serverResDir);
      serverBinary = serverResDir.resolve(initialServerBinary.getFileName());
      grantFileOrDirFullAccess(serverBinary);

      boolean useXtsJavaBinary = XtsCommandUtil.useXtsJavaBinary(xtsType, xtsRootDir);
      if (useXtsJavaBinary) {
        // Copies JDK dir into server resource dir and grants access.
        Path xtsJdkDir = XtsDirUtil.getXtsJdkDir(xtsRootDir, xtsType);
        logger
            .atInfo()
            .with(IMPORTANCE, DEBUG)
            .log("Copying xTS JDK dir [%s] into xTS resource dir [%s]", xtsJdkDir, serverResDir);
        localFileUtil.checkDir(xtsJdkDir);
        localFileUtil.copyFileOrDir(xtsJdkDir, serverResDir);
        Path jdkDir = serverResDir.resolve(xtsJdkDir.getFileName());
        javaBinary = jdkDir.resolve("bin/java");
        grantFileOrDirFullAccess(jdkDir);
      } else {
        javaBinary = XtsCommandUtil.getJavaBinary(xtsType, xtsRootDir);
      }
    } else {
      serverBinary = initialServerBinary;
      javaBinary = XtsCommandUtil.getJavaBinary(xtsType, xtsRootDir);
    }

    // Checks files.
    localFileUtil.checkFile(serverBinary);
    localFileUtil.checkFile(javaBinary);
    return ServerEnvironment.of(serverBinary, javaBinary);
  }

  /** Grants full access to a file/dir and ignores errors if any. */
  private void grantFileOrDirFullAccess(Path path) {
    try {
      localFileUtil.grantFileOrDirFullAccess(path);
    } catch (MobileHarnessException e) {
      logger
          .atInfo()
          .with(IMPORTANCE, DEBUG)
          .withCause(e)
          .log("Failed to grant access to [%s]", path);
    }
  }
}
