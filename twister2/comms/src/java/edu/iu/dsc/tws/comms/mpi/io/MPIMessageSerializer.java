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
package edu.iu.dsc.tws.comms.mpi.io;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.logging.Logger;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.comms.api.MessageHeader;
import edu.iu.dsc.tws.comms.api.MessageSerializer;
import edu.iu.dsc.tws.comms.api.MessageType;
import edu.iu.dsc.tws.comms.mpi.MPIBuffer;
import edu.iu.dsc.tws.comms.mpi.MPIMessage;
import edu.iu.dsc.tws.comms.mpi.MPISendMessage;

public class MPIMessageSerializer implements MessageSerializer {
  private static final Logger LOG = Logger.getLogger(MPIMessageSerializer.class.getName());

  private Queue<MPIBuffer> sendBuffers;
  private KryoSerializer serializer;
  private Config config;
  private boolean grouped;

  public MPIMessageSerializer(Queue<MPIBuffer> buffers, KryoSerializer kryoSerializer) {
    this.sendBuffers = buffers;
    this.serializer = kryoSerializer;
  }

  @Override
  public void init(Config cfg, boolean grped) {
    this.config = cfg;
    this.grouped = grped;
  }

  @Override
  public Object build(Object message, Object partialBuildObject) {
    MPISendMessage sendMessage = (MPISendMessage) partialBuildObject;

    // we got an already serialized message, lets just return it
    if (sendMessage.getMPIMessage().isComplete()) {
      sendMessage.setSerializedState(MPISendMessage.SerializedState.FINISHED);
      return sendMessage;
    }

    while (sendBuffers.size() > 0 && sendMessage.serializedState()
        != MPISendMessage.SerializedState.FINISHED) {
      MPIBuffer buffer = sendBuffers.poll();

      if (sendMessage.serializedState() == MPISendMessage.SerializedState.INIT) {
        // build the header
        buildHeader(buffer, sendMessage);
        sendMessage.setSerializedState(MPISendMessage.SerializedState.HEADER_BUILT);
      }

      if (sendMessage.serializedState() == MPISendMessage.SerializedState.HEADER_BUILT) {
        // build the body
        // first we need to serialize the body if needed
        serializeBody(message, sendMessage, buffer);
      } else if (sendMessage.serializedState() == MPISendMessage.SerializedState.BODY) {
        // further build the body
        serializeBody(message, sendMessage, buffer);
      }

      // okay we are adding this buffer
      sendMessage.getMPIMessage().addBuffer(buffer);
      if (sendMessage.serializedState() == MPISendMessage.SerializedState.FINISHED) {
        MPIMessage mpiMessage = sendMessage.getMPIMessage();
        // mark the original message as complete
        mpiMessage.setComplete(true);
//        LOG.info("Message FULLY serialized");
      } else {
        LOG.info("Message NOT FULLY serialized");
      }
    }
    return sendMessage;
  }

  private void buildHeader(MPIBuffer buffer, MPISendMessage sendMessage) {
    if (buffer.getCapacity() < 16) {
      throw new RuntimeException("The buffers should be able to hold the complete header");
    }
//    LOG.info("Building header");
    ByteBuffer byteBuffer = buffer.getByteBuffer();
    // now lets put the content of header in
    byteBuffer.putInt(sendMessage.getSource());
    // the path we are on, if not grouped it will be 0 and ignored
    byteBuffer.putInt(sendMessage.getPath());
    byteBuffer.putInt(sendMessage.getSubEdge());
    // we add 0 for now and late change it
    byteBuffer.putInt(0);
    // at this point we haven't put the length and we will do it at the serialization
    sendMessage.setWrittenHeaderSize(16);
    // lets set the size for 16 for now
    buffer.setSize(16);
  }

  /**
   * Serialized the message into the buffer
   * @param payload
   * @param sendMessage
   * @param buffer
   * @return true if the message is completely written
   */
  private void serializeBody(Object payload,
                             MPISendMessage sendMessage, MPIBuffer buffer) {
    MessageType type = sendMessage.getMPIMessage().getType();
//    LOG.log(Level.INFO, "Serializing body with type: " + type);
    switch (type) {
      case INTEGER:
        break;
      case LONG:
        break;
      case DOUBLE:
        break;
      case OBJECT:
        serializeObject(payload, sendMessage, buffer);
        break;
      case BYTE:
        break;
      case STRING:
        break;
      default:
        break;
    }
  }

  /**
   * Serializes a java object using kryo serialization
   *
   * @param object
   * @param sendMessage
   * @param buffer
   */
  private void serializeObject(Object object, MPISendMessage sendMessage, MPIBuffer buffer) {
    byte[] data;
    int dataPosition;
    ByteBuffer byteBuffer = buffer.getByteBuffer();
    if (sendMessage.serializedState() == MPISendMessage.SerializedState.HEADER_BUILT) {
      // okay we need to serialize the data
      data = serializer.serialize(object);
      // at this point we know the length of the data
      byteBuffer.putInt(12, data.length);
      // now lets set the header
      MessageHeader.Builder builder = MessageHeader.newBuilder(sendMessage.getSource(),
          sendMessage.getEdge(), data.length);
      builder.subEdge(sendMessage.getSubEdge());
      sendMessage.getMPIMessage().setHeader(builder.build());
      dataPosition = 0;
      sendMessage.setSendBytes(data);
//      LOG.log(Level.INFO, String.format("Finished adding header %d %d %d %d",
//          sendMessage.getSource(), sendMessage.getEdge(), sendMessage.getPath(), data.length));
    } else {
      data = sendMessage.getSendBytes();
      dataPosition = sendMessage.getByteCopied();
    }

    if (grouped && MPISendMessage.SerializedState.BODY == sendMessage.serializedState()) {
      // we need to set the path at the begining
      byteBuffer.putInt(sendMessage.getPath());
    }

    int remainingToCopy = data.length - dataPosition;
    // check how much space we have
    int bufferSpace = byteBuffer.capacity() - byteBuffer.position();

    int copyBytes = remainingToCopy > bufferSpace ? bufferSpace : remainingToCopy;
    // check how much space left in the buffer
    byteBuffer.put(data, dataPosition, copyBytes);
    sendMessage.setByteCopied(dataPosition + copyBytes);

    // now set the size of the buffer
//    LOG.log(Level.INFO, String.format("Serialize object body with buffer size: %d copyBytes: "
//        + "%d remainingCopy: %d", byteBuffer.position(), copyBytes, remainingToCopy));
    buffer.setSize(byteBuffer.position());

    // okay we are done with the message
    if (copyBytes == remainingToCopy) {
      sendMessage.setSerializedState(MPISendMessage.SerializedState.FINISHED);
    } else {
      sendMessage.setSerializedState(MPISendMessage.SerializedState.BODY);
    }
  }
}