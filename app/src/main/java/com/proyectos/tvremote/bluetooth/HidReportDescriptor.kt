package com.proyectos.tvremote.bluetooth

/**
 * Combo HID report descriptor: a standard boot-keyboard collection (Report ID 1,
 * 8-byte report: modifier + reserved + 6 key array) plus a Consumer Control
 * collection (Report ID 2, single 16-bit usage array report) for volume/mute/
 * power/home/back that TVs generally recognize without needing a full keyboard.
 *
 * Usage codes come from the USB HID Usage Tables. Report IDs are descriptor-only
 * tags - BluetoothHidDevice.sendReport() takes the report id as a separate param
 * and the payload bytes below do NOT include it.
 */
object HidReportDescriptor {

    val DESCRIPTOR: ByteArray = byteArrayOf(
        // ---- Keyboard collection (Report ID 1) ----
        0x05, 0x01,             // USAGE_PAGE (Generic Desktop)
        0x09, 0x06,             // USAGE (Keyboard)
        0xA1.toByte(), 0x01,    // COLLECTION (Application)
        0x85.toByte(), 0x01,    //   REPORT_ID (1)
        0x05, 0x07,             //   USAGE_PAGE (Keyboard/Keypad)
        0x19, 0xE0.toByte(),    //   USAGE_MINIMUM (224)
        0x29, 0xE7.toByte(),    //   USAGE_MAXIMUM (231)
        0x15, 0x00,             //   LOGICAL_MINIMUM (0)
        0x25, 0x01,             //   LOGICAL_MAXIMUM (1)
        0x75, 0x01,             //   REPORT_SIZE (1)
        0x95.toByte(), 0x08,    //   REPORT_COUNT (8)
        0x81.toByte(), 0x02,    //   INPUT (Data,Var,Abs) - modifier byte
        0x95.toByte(), 0x01,    //   REPORT_COUNT (1)
        0x75, 0x08,             //   REPORT_SIZE (8)
        0x81.toByte(), 0x01,    //   INPUT (Cnst,Arr,Abs) - reserved byte
        0x95.toByte(), 0x06,    //   REPORT_COUNT (6)
        0x75, 0x08,             //   REPORT_SIZE (8)
        0x15, 0x00,             //   LOGICAL_MINIMUM (0)
        0x25, 0x65,             //   LOGICAL_MAXIMUM (101)
        0x05, 0x07,             //   USAGE_PAGE (Keyboard/Keypad)
        0x19, 0x00,             //   USAGE_MINIMUM (0)
        0x29, 0x65,             //   USAGE_MAXIMUM (101)
        0x81.toByte(), 0x00,    //   INPUT (Data,Ary,Abs) - 6 key slots
        0xC0.toByte(),          // END_COLLECTION

        // ---- Consumer Control collection (Report ID 2) ----
        0x05, 0x0C,                               // USAGE_PAGE (Consumer)
        0x09, 0x01,                               // USAGE (Consumer Control)
        0xA1.toByte(), 0x01,                       // COLLECTION (Application)
        0x85.toByte(), 0x02,                       //   REPORT_ID (2)
        0x15, 0x00,                               //   LOGICAL_MINIMUM (0)
        0x26.toByte(), 0x24, 0x02,                 //   LOGICAL_MAXIMUM (0x0224)
        0x19, 0x00,                               //   USAGE_MINIMUM (0)
        0x2A.toByte(), 0x24, 0x02,                 //   USAGE_MAXIMUM (0x0224)
        0x75, 0x10,                               //   REPORT_SIZE (16)
        0x95.toByte(), 0x01,                       //   REPORT_COUNT (1)
        0x81.toByte(), 0x00,                       //   INPUT (Data,Ary,Abs)
        0xC0.toByte()                              // END_COLLECTION
    )

    const val REPORT_ID_KEYBOARD: Byte = 1
    const val REPORT_ID_CONSUMER: Byte = 2

    // USB HID Keyboard/Keypad usage IDs (page 0x07)
    object Key {
        const val RIGHT_ARROW = 0x4F
        const val LEFT_ARROW = 0x50
        const val DOWN_ARROW = 0x51
        const val UP_ARROW = 0x52
        const val ENTER = 0x28
        const val ESCAPE = 0x29
    }

    // USB HID Consumer usage IDs (page 0x0C)
    object Consumer {
        const val POWER = 0x30
        const val MUTE = 0xE2
        const val VOLUME_UP = 0xE9
        const val VOLUME_DOWN = 0xEA
        const val AC_HOME = 0x0223
        const val AC_BACK = 0x0224
    }

    fun keyboardReport(keyUsage: Int?): ByteArray {
        val data = ByteArray(8)
        if (keyUsage != null) data[2] = keyUsage.toByte()
        return data
    }

    fun consumerReport(usage: Int): ByteArray {
        val lo = (usage and 0xFF).toByte()
        val hi = ((usage shr 8) and 0xFF).toByte()
        return byteArrayOf(lo, hi)
    }
}
