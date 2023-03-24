package com.uid2.sdk

import org.junit.Assert.assertTrue
import org.junit.Test

class UID2Test {
    @Test
    fun `test version`() {
        // Verify that the reported Version is in the expected format (x.y.z)
        assertTrue(UID2.getVersion().matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+(-.+)?")))
    }
}
