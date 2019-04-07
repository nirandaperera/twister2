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
package edu.iu.dsc.tws.comms.api;

import java.nio.ByteBuffer;

import org.apache.commons.lang3.tuple.Pair;

import edu.iu.dsc.tws.comms.dfw.DataBuffer;
import edu.iu.dsc.tws.comms.dfw.InMessage;
import edu.iu.dsc.tws.comms.dfw.io.SerializeState;

/**
 * Key packer will be removed in future releases
 *
 * @deprecated Will be removed
 */
@Deprecated
public interface KeyPacker<D> {
  /**
   * Pack the key and return the size of the data in bytes once packed
   *
   * @param key the key (can be Integer, Object etc)
   * @param state state
   * @return the size of the packed data in bytes
   * @deprecated Will be removed
   */
  @Deprecated
  int packKey(D key, SerializeState state);

  /**
   * Transfer the data to the buffer
   *
   * @param key the key
   * @param targetBuffer target buffer
   * @param state this can be used to keep the sate about the packing object
   * @return true if all the data is packed
   * @deprecated Will be removed
   */
  @Deprecated
  boolean writeKeyToBuffer(D key,
                           ByteBuffer targetBuffer, SerializeState state);

  /**
   * Initialize the key
   *
   * @param message the message
   * @param buffer the data buffer
   * @param location the current location of the buffer
   * @return keyLength and readBytes
   * @deprecated Will be removed
   */
  @Deprecated
  Pair<Integer, Integer> getKeyLength(InMessage message, DataBuffer buffer, int location);


  /**
   * Initialize an empty object
   *
   * @deprecated Will be removed
   */
  @Deprecated
  D initializeUnPackKeyObject(int size);

  /**
   * Read the data from the buffer
   *
   * @param currentMessage the current message
   * @param currentLocation current location
   * @param buffer buffer
   * @param currentObjectLength the current object length
   * @return the number of bytes read
   * @deprecated Will be removed
   */
  @Deprecated
  int readKeyFromBuffer(InMessage currentMessage, int currentLocation,
                        DataBuffer buffer, int currentObjectLength);

  /**
   * Return true if header is required
   *
   * @deprecated Will be removed
   */
  @Deprecated
  boolean isKeyHeaderRequired();
}
