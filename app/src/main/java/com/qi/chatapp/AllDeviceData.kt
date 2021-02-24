package com.example.bluetoothmessaging

data class AllDeviceData(val deviceName: String?, val deviceHardwareAddress: String){

    override fun equals(other: Any?): Boolean {
        val deviceData = other as AllDeviceData
        return deviceHardwareAddress == deviceData.deviceHardwareAddress
    }

    override fun hashCode(): Int {
        return deviceHardwareAddress.hashCode()
    }

}