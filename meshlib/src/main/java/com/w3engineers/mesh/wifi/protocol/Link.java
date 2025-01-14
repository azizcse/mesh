/*
 * Copyright (c) 2016 Vladimir L. Shabanov <virlof@gmail.com>
 *
 * Licensed under the Underdark License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://underdark.io/LICENSE.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.w3engineers.mesh.wifi.protocol;

/**
 * <p>Class for the connection objects with discovered remote devices</>
 * All methods and properties of this class must be accessed
 * only on the delegate handler of corresponding
 * {@link MeshTransport}.
 */
public interface Link {
    /**
     * Connection link type
     * for ui level recognize
     */
    enum Type {
        NA(80),
        WIFI(1),
        BT(2),
        WIFI_MESH(3),
        BT_MESH(4),
        INTERNET(5),
        HB(8),
        HB_MESH(9);
        private int type;

        Type(int value){
            this.type = value;
        }

        public int getValue(){
            return type;
        }
    }

    /**
     * nodeId of remote device
     *
     * @return nodeId of remote device
     */
    String getNodeId();

    /**
     * Disconnects remote device after all
     * pending output frame have been sent to it.
     */
    void disconnect();

    /**
     * Connection status check
     *
     * @return : boolean
     */
    boolean isConnected();

    /**
     * Sends bytes to remote device as single atomic frame.
     *
     * @param frameData bytes to send.
     */
    default int sendFrame(String senderId, String receiverId,String messageId, byte[] frameData) {
        return -1;
    }
    /**
     * Link type provider
     *
     */
    Type getType();

    int getUserMode();

    int sendMeshMessage(byte[] data);

} // Link
