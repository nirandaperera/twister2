//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package edu.iu.dsc.tws.tset;

import java.util.Set;
import java.util.StringJoiner;

import edu.iu.dsc.tws.api.comms.messaging.types.MessageType;
import edu.iu.dsc.tws.api.comms.messaging.types.MessageTypes;
import edu.iu.dsc.tws.api.tset.TBase;

public final class TSetUtils {
  private TSetUtils() {
  }

  public static String generateBuildId(Set<? extends TBase> roots) {
    StringJoiner joiner = new StringJoiner("_");
    joiner.add("build");
    for (TBase t : roots) {
      joiner.add(t.getId());
    }
    return joiner.toString();
  }

  public static String generateBuildId(TBase root) {
    return "build_" + root.getId();
  }

  public static MessageType getDataType(Class type) {
    if (type == int[].class) {
      return MessageTypes.INTEGER_ARRAY;
    } else if (type == double[].class) {
      return MessageTypes.DOUBLE_ARRAY;
    } else if (type == short[].class) {
      return MessageTypes.SHORT_ARRAY;
    } else if (type == byte[].class) {
      return MessageTypes.BYTE_ARRAY;
    } else if (type == long[].class) {
      return MessageTypes.LONG_ARRAY;
    } else if (type == char[].class) {
      return MessageTypes.CHAR_ARRAY;
    } else {
      return MessageTypes.OBJECT;
    }
  }

  public static MessageType getKeyType(Class type) {
    if (type == Integer.class) {
      return MessageTypes.INTEGER_ARRAY;
    } else if (type == Double.class) {
      return MessageTypes.DOUBLE_ARRAY;
    } else if (type == Short.class) {
      return MessageTypes.SHORT_ARRAY;
    } else if (type == Byte.class) {
      return MessageTypes.BYTE_ARRAY;
    } else if (type == Long.class) {
      return MessageTypes.LONG_ARRAY;
    } else if (type == Character.class) {
      return MessageTypes.CHAR_ARRAY;
    } else {
      return MessageTypes.OBJECT;
    }
  }
}
