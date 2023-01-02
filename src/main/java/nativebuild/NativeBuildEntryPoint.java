// Copyright © 2012-2023 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package nativebuild;

import java.util.Arrays;

import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

import io.vlingo.xoom.actors.World;
import io.vlingo.xoom.directory.client.ServiceRegistrationInfo;

public final class NativeBuildEntryPoint {
  @SuppressWarnings("unused")
  @CEntryPoint(name = "Java_io_vlingo_xoom_directorynative_Native_start")
  public static int start(@CEntryPoint.IsolateThreadContext long isolateId, CCharPointer name) {
    final String nameString = CTypeConversion.toJavaString(name);
    World world = World.startWithDefaults(nameString);

    final ServiceRegistrationInfo info =
        new ServiceRegistrationInfo(
            nameString,
            Arrays.asList(new ServiceRegistrationInfo.Location("1.2.3.4", 111), new ServiceRegistrationInfo.Location("1.2.3.45", 222)));
    return 0;
  }
}