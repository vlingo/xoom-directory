package io.vlingo.xoom.directory.implnative;

import io.vlingo.xoom.actors.World;
import io.vlingo.xoom.directory.client.ServiceRegistrationInfo;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

import java.util.Arrays;

public final class NativeImpl {
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