/*
 * Copyright 2020 ARM Ltd.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arm.peliondevicemanagement.transport.ble

import com.arm.peliondevicemanagement.BuildConfig
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.transport.ISerialDataSink
import com.arm.peliondevicemanagement.transport.sda.SerialMessage
import com.arm.peliondevicemanagement.utils.ByteFactory
import kotlinx.coroutines.*
import java.util.Queue
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalCoroutinesApi
class DummyBleDevice(private val deviceMac: String):
    AbstractBleDevice(), ISerialDataSink {

    companion object{
        private val TAG = DummyBleDevice::class.java.simpleName
        // Maximum transmission unit for data-transfer
        const val MTU_SIZE: Int = 244
        // Should always be 4 bytes less than the MTU
        private const val TMSN_MTU_SIZE: Int = MTU_SIZE - 4
        // Device MAC
        private const val DUMMY_MAC: String = "DD:7E:7E:BD:AB:78"
        // Device endpoint to be used for matching devices
        private const val DUMMY_ENDPOINT: String = "0171cb1bf2d776307e36719503c00000"
        // Nonce response returned by the device
        private val DUMMY_NONCE_RESPONSE: ByteArray
                = byteArrayOf(0, 73, 16, 16, 0, 0, 0, 0, -65, 3, 27,
            113, 14, 115, -25, -60, -47, 20, -53, 1, 2, 2, 0, -1, 0)
        // Operation response returned by the device
        private val DUMMY_OPERATION_RESPONSE: ByteArray
                = byteArrayOf(1, -111, 27, 27, 0, 0, 0, 0, -65, 4,
            83, 70, 105, 108, 101, 32, 87, 114, 105, 116, 101, 32,
            67, 111, 109, 112, 108, 101, 116, 101, 1, 4, 2, 0, -1, -125)
        // Failed operation response returned by the device
        private val DUMMY_FAILED_OPERATION_RESPONSE: ByteArray
                = byteArrayOf(3, 33, 10, 10, 0, 0, 0, 0, -65,
            1, 4, 2, 26, 15, -1, -1, -1, -1, 2)

        private const val TIME_OUT_MILLISECONDS: Long= 5000
    }

    private lateinit var operationResponse: ByteArray
    private lateinit var dataOutQueue: Queue<ByteArray>
    private var totalPacketsToWrite: Int = 0
    private var currentPosition: Int = 0
    private var globalNumber: Int = 0
    private var isAckReceived: Boolean = false
    private var isFinalResponseReceived: Boolean = false

    private var bleConnectionCallback: BleConnectionCallback? = null

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }

    override suspend fun connect(callback: BleConnectionCallback): Boolean {
        delay(200)
        return suspendCoroutine {
            LogHelper.debug(TAG, "->onConnect() MAC: $deviceMac")
            bleConnectionCallback = callback
            it.resume(true)
        }
    }

    override suspend fun disconnect(): Boolean {
        return suspendCoroutine {
            totalPacketsToWrite = 0
            currentPosition = 0
            LogHelper.debug(TAG, "->onDisconnect()")
            bleConnectionCallback!!.onDisconnect()
            it.resume(true)
        }
    }

    override suspend fun releaseLocks() {
        isFinalResponseReceived = true
        disconnect()
    }

    override suspend fun requestHigherMtu(size: Int): Boolean {
        delay(200)
        return suspendCoroutine {
            LogHelper.debug(TAG, "->onMtuChanged() size: $size bytes")
            it.resume(true)
        }
    }

    override suspend fun readEndpoint(): String {
        delay(200)
        return suspendCoroutine {
            val endpoint = when(deviceMac) {
                "FE:7E:7E:BD:AB:87" -> {
                    "xFE:7E:7E:BD:AB:87"
                }
                "XD:7E:7E:BD:AB:70" -> {
                    "026eead293eb926ca57ba92703c00000"
                }
                "DS:7E:7E:BD:AB:79" -> {
                    "036eead293eb926ca57ba92703c00000"
                } else -> {
                    DUMMY_ENDPOINT
                }
            }
            LogHelper.debug(TAG, "->onReadEndpoint() $endpoint")
            it.resume(endpoint)
        }
    }

    private suspend fun write(packetArray: ByteArray): Boolean {
        delay(200)
        return suspendCoroutine {
            LogHelper.debug(TAG, "->onAir() ${packetArray.toHex()}")
            isAckReceived = true
            it.resume(true)
        }
    }

    private suspend fun doWrite(protocolMessage: ByteArray) {
        // Enable feature-flag, if debug-build
        val transmissionMtuSize: Int = if(BuildConfig.DEBUG){
            if(SharedPrefHelper.getDeveloperOptions().isMaxMTUDisabled()){
                20
            } else {
                TMSN_MTU_SIZE
            }
        } else {
            TMSN_MTU_SIZE
        }

        LogHelper.debug(TAG, "->doWrite() MaxTransmissionMTU: $transmissionMtuSize bytes")
        LogHelper.debug(TAG, "->doWrite() protocolMessage" +
                "\nsize: ${protocolMessage.size}," +
                "\ndata: ${protocolMessage.contentToString()}")

        var packetBuffer: ByteArray = byteArrayOf()

        val messageQueue: Queue<ByteArray> = if(protocolMessage.size == 46){
            ByteFactory.splitBytes(protocolMessage, 46)
        } else {
            ByteFactory.splitBytes(protocolMessage, (transmissionMtuSize - 8))
        }
        LogHelper.debug(TAG, "->doWrite() ItemsInMessageQueue: ${messageQueue.size}")

        // Construct packets
        var packetNumber = 1
        val messageSize = messageQueue.size
        var hasMore = 1
        messageQueue.forEach { messageChunk ->
            if(packetNumber == messageSize){
                hasMore = 0
            }

            LogHelper.debug(TAG, "->doWrite() sN: $globalNumber, " +
                    "fN: $packetNumber, isMoreFragment: $hasMore")

            val newPacket = PacketFactory(
                globalNumber,
                PacketFactory.createControlFrame(packetNumber, hasMore),
                (messageChunk.size),
                protocolMessage.size, false,
                byteArrayOf(0, 0), messageChunk
            ).getPacket()

            packetBuffer += newPacket
            packetNumber++
        }
        globalNumber++

        LogHelper.debug(TAG, "->doWrite() PacketBufferSize: ${packetBuffer.size}")

        // Construct packet queue
        dataOutQueue = if(packetBuffer.size == 54) {
            ByteFactory.splitBytes(packetBuffer, 54)
        } else {
            ByteFactory.splitBytes(packetBuffer, transmissionMtuSize)
        }

        // Count total packets to write
        totalPacketsToWrite = dataOutQueue.size
        LogHelper.debug(TAG, "->doWrite() ItemsInPacketQueue: ${dataOutQueue.size}, TotalPacketsToWrite: $totalPacketsToWrite")
        isAckReceived = true

        while (dataOutQueue.size > 0){
            // Terminate if operation aborted
            if(isFinalResponseReceived){
                LogHelper.debug(TAG, "->doWrite() Aborting write-operation")
                break
            }
            if(isAckReceived){
                isAckReceived = false
                delay(200)
                LogHelper.debug(TAG, "->doWrite() Sending packet to device.")
                write(dataOutQueue.poll()!!)
            }
        }

        // Now wait for final response
        LogHelper.debug(TAG, "->doWrite() Write complete, now waiting for response.")
        withTimeoutOrNull(TIME_OUT_MILLISECONDS){
            while (!isFinalResponseReceived){
                // Wait for 2 seconds here then send the response
                delay(2000)
                if(protocolMessage.size == 46){
                    onNewData(DUMMY_NONCE_RESPONSE)
                } else {
                    onNewData(DUMMY_OPERATION_RESPONSE)
                }
            }
        }
        isFinalResponseReceived = false
    }

    override fun sendMessage(operationMessage: ByteArray): ByteArray {
        operationResponse = byteArrayOf()

        LogHelper.debug(TAG, "->sendMessage() operationMessage" +
                "\nsize: ${operationMessage.size}," +
                "\ndata: ${operationMessage.contentToString()}")

        val serialProtocolMessage = SerialMessage.formatSerialProtocolMessage(operationMessage)
        LogHelper.debug(TAG, "->sendMessage() Starting write operation.")
        runBlocking {
            doWrite(serialProtocolMessage)
        }

        LogHelper.debug(TAG, "->sendMessage() Request time-out, now returning back.")

        return operationResponse
    }

    override fun onNewData(buffer: ByteArray) {
        // Parse response
        val packetReceived = PacketFactory.new(buffer)
        LogHelper.debug(TAG, "->onNewData() [ Response received ]" +
                "\npacketSize: ${packetReceived.getPacket().size}," +
                "\npacketContent: ${packetReceived.getPacket().contentToString()}" +
                "\npayloadSize: ${packetReceived.getDataPayload().size}," +
                "\npayloadContent: ${packetReceived.getDataPayload().contentToString()}")

        if(packetReceived.getPacket().size == 24){
            // Nonce Response
            operationResponse = packetReceived.getDataPayload()
            LogHelper.debug(TAG, "onNewData()-> Returning nonce-response")
            isFinalResponseReceived = true
        } else {
            val hasMore = PacketFactory.hasMoreFragment(packetReceived.getPacket()[1])
            if(hasMore){
                // There's more to the response
                val payload = packetReceived.getDataPayload()
                val tempBuffer = ByteArray(operationResponse.size + payload.size)
                System.arraycopy(operationResponse, 0, tempBuffer, 0, operationResponse.size)
                System.arraycopy(payload, 0, tempBuffer, operationResponse.size, payload.size)
                // Assign new buffer to operationResponse
                operationResponse = tempBuffer
                LogHelper.debug(TAG, "onNewData()-> Waiting for more response, hasMore: true")
            } else {
                if(operationResponse.isNotEmpty()){
                    // There's more to the response
                    val payload = packetReceived.getDataPayload()
                    val tempBuffer = ByteArray(operationResponse.size + payload.size)
                    System.arraycopy(operationResponse, 0, tempBuffer, 0, operationResponse.size)
                    System.arraycopy(payload, 0, tempBuffer, operationResponse.size, payload.size)
                    // Assign new buffer to operationResponse
                    operationResponse = tempBuffer
                    LogHelper.debug(TAG, "onNewData()-> Returning final operation-response")
                    isFinalResponseReceived = true
                } else {
                    operationResponse = packetReceived.getDataPayload()
                    LogHelper.debug(TAG, "onNewData()-> Returning operation-response")
                    isFinalResponseReceived = true
                }
            }
        }
    }
}