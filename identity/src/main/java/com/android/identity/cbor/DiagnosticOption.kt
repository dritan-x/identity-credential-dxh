package com.android.identity.cbor

/**
 * Enumeration of options that can be passed to [Cbor.toDiagnostics].
 */
enum class DiagnosticOption {
    /**
     * Prints out embedded CBOR, that is, byte strings tagged with [Tagged.ENCODED_CBOR].
     */
    EMBEDDED_CBOR,

    /**
     * Inserts newlines and indentation to make the output more readable.
     */
    PRETTY_PRINT
}
