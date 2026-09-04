package info.skyblond.nsp.data

import org.json.JSONObject

/**
 * A previously paired Nikon camera. [device] and [nonce] are stored as unsigned 32-bit values.
 * [name] is the hardware Bluetooth advertised name (e.g. Z 8_1234567).
 * [customName] is an optional user-defined nickname/alias (e.g. "My Studio Z8").
 */
data class PairedCamera(
    val name: String,
    val address: String,
    val addressType: Int,
    val device: Long,
    val nonce: Long,
    val controllerName: String,
    val customName: String? = null
) {
    val displayName: String
        get() = customName?.takeIf { it.isNotBlank() } ?: name

    fun toJson(): String = JSONObject().apply {
        put("name", name)
        put("address", address)
        put("addressType", addressType)
        put("device", device)
        put("nonce", nonce)
        put("controllerName", controllerName)
        if (!customName.isNullOrBlank()) {
            put("customName", customName)
        }
    }.toString()

    companion object {
        fun fromJson(json: String): PairedCamera {
            val obj = JSONObject(json)
            return PairedCamera(
                name = obj.getString("name"),
                address = obj.getString("address"),
                addressType = obj.getInt("addressType"),
                device = obj.getLong("device"),
                nonce = obj.getLong("nonce"),
                controllerName = obj.getString("controllerName"),
                customName = if (obj.has("customName") && !obj.isNull("customName")) {
                    obj.getString("customName").takeIf { it.isNotBlank() }
                } else null
            )
        }
    }
}
