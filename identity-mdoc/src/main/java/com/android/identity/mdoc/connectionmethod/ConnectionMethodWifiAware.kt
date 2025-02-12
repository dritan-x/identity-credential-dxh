package com.android.identity.mdoc.connectionmethod

import com.android.identity.cbor.Cbor.decode
import com.android.identity.cbor.Cbor.encode
import com.android.identity.cbor.CborArray
import com.android.identity.cbor.CborMap
import com.android.identity.internal.Util.toHex
import java.util.OptionalLong

/**
 * Connection method for Wifi Aware.
 *
 * @param passphraseInfoPassphrase  the passphrase or `null`.
 * @param channelInfoChannelNumber  the channel number or unset.
 * @param channelInfoOperatingClass the operating class or unset.
 * @param bandInfoSupportedBands    the supported bands or `null`.
 */
class ConnectionMethodWifiAware(
    val passphraseInfoPassphrase: String?,
    val channelInfoChannelNumber: OptionalLong,
    val channelInfoOperatingClass: OptionalLong,
    val bandInfoSupportedBands: ByteArray?
): ConnectionMethod() {

    override fun toString(): String {
        val builder = StringBuilder("wifi_aware")
        if (passphraseInfoPassphrase != null) {
            builder.append(":passphrase=")
            builder.append(passphraseInfoPassphrase)
        }
        if (channelInfoChannelNumber.isPresent) {
            builder.append(":channel_info_channel_number=")
            builder.append(channelInfoChannelNumber.asLong)
        }
        if (channelInfoOperatingClass.isPresent) {
            builder.append(":channel_info_operating_class=")
            builder.append(channelInfoOperatingClass.asLong)
        }
        if (bandInfoSupportedBands != null) {
            builder.append(":base_info_supported_bands=")
            builder.append(toHex(bandInfoSupportedBands))
        }
        return builder.toString()
    }

    override fun toDeviceEngagement(): ByteArray {
        val builder = CborMap.builder()
        if (passphraseInfoPassphrase != null) {
            builder.put(OPTION_KEY_PASSPHRASE_INFO_PASSPHRASE, passphraseInfoPassphrase)
        }
        if (channelInfoChannelNumber.isPresent) {
            builder.put(
                OPTION_KEY_CHANNEL_INFO_CHANNEL_NUMBER,
                channelInfoChannelNumber.asLong
            )
        }
        if (channelInfoOperatingClass.isPresent) {
            builder.put(
                OPTION_KEY_CHANNEL_INFO_OPERATING_CLASS,
                channelInfoOperatingClass.asLong
            )
        }
        if (bandInfoSupportedBands != null) {
            builder.put(OPTION_KEY_BAND_INFO_SUPPORTED_BANDS, bandInfoSupportedBands)
        }
        return encode(
            CborArray.builder()
                .add(METHOD_TYPE)
                .add(METHOD_MAX_VERSION)
                .add(builder.end().build())
                .end().build()
        )
    }

    companion object {
        const val METHOD_TYPE = 3L
        const val METHOD_MAX_VERSION = 1L
        private const val OPTION_KEY_PASSPHRASE_INFO_PASSPHRASE = 0L
        private const val OPTION_KEY_CHANNEL_INFO_OPERATING_CLASS = 1L
        private const val OPTION_KEY_CHANNEL_INFO_CHANNEL_NUMBER = 2L
        private const val OPTION_KEY_BAND_INFO_SUPPORTED_BANDS = 3L
        fun fromDeviceEngagementWifiAware(encodedDeviceRetrievalMethod: ByteArray): ConnectionMethodWifiAware? {
            val array = decode(encodedDeviceRetrievalMethod)
            val type = array[0].asNumber
            val version = array[1].asNumber
            require(type == METHOD_TYPE)
            if (version > METHOD_MAX_VERSION) {
                return null
            }
            val map = array[2]
            val passphraseInfoPassphrase =
                map.getOrNull(OPTION_KEY_PASSPHRASE_INFO_PASSPHRASE)?.asTstr

            var channelInfoChannelNumber = OptionalLong.empty()
            val cicn = map.getOrNull(OPTION_KEY_CHANNEL_INFO_CHANNEL_NUMBER)
            if (cicn != null) {
                channelInfoChannelNumber = OptionalLong.of(cicn.asNumber)
            }

            var channelInfoOperatingClass = OptionalLong.empty()
            val cioc = map.getOrNull(OPTION_KEY_CHANNEL_INFO_OPERATING_CLASS)
            if (cioc != null) {
                channelInfoOperatingClass = OptionalLong.of(cioc.asNumber)
            }
            val bandInfoSupportedBands =
                    map.getOrNull(OPTION_KEY_BAND_INFO_SUPPORTED_BANDS)?.asBstr
            return ConnectionMethodWifiAware(
                passphraseInfoPassphrase,
                channelInfoChannelNumber,
                channelInfoOperatingClass,
                bandInfoSupportedBands
            )
        }
    }
}
