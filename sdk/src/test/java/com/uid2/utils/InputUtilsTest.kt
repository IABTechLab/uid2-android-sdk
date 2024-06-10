package com.uid2.utils

import com.uid2.InputValidationException
import com.uid2.data.IdentityRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class InputUtilsTest {

    @Test
    fun `test phone number normalization detects invalid input`() {
        val utils = InputUtils()

        listOf(
            "",
            "asdaksjdakfj",
            "DH5qQFhi5ALrdqcPiib8cy0Hwykx6frpqxWCkR0uijs",
            "QFhi5ALrdqcPiib8cy0Hwykx6frpqxWCkR0uijs",
            "06a418f467a14e1631a317b107548a1039d26f12ea45301ab14e7684b36ede58",
            "0C7E6A405862E402EB76A70F8A26FC732D07C32931E9FAE9AB1582911D2E8A3B",
            "+",
            "12345678",
            "123456789",
            "1234567890",
            "+12345678",
            "+123456789",
            "+ 12345678",
            "+ 123456789",
            "+ 1234 5678",
            "+ 1234 56789",
            "+1234567890123456",
            "+1234567890A",
            "+1234567890 ",
            "+1234567890+",
            "+12345+67890",
            "555-555-5555",
            "(555) 555-5555",
        ).forEach {
            // Verify that when attempting to normalize these invalid phone numbers, the expected exception is thrown.
            assertThrows(InputValidationException::class.java) {
                utils.normalize(IdentityRequest.Phone(it))
            }
        }
    }

    @Test
    fun `test phone number normalization detects valid input`() {
        val utils = InputUtils()

        listOf(
            "+1234567890",
            "+12345678901",
            "+123456789012",
            "+1234567890123",
            "+12345678901234",
            "+123456789012345",
        ).forEach {
            // Verify that when normalizing these valid phone numbers, they pass as expected (untouched).
            val normalized = utils.normalize(IdentityRequest.Phone(it))
            assertEquals(it, normalized.data)
        }
    }

    @Test
    fun `test email normalization detects invalid input`() {
        val utils = InputUtils()

        listOf(
            "",
            " @",
            "@",
            "a@",
            "@b",
            "@b.com",
            "+",
            " ",
            "+@gmail.com",
            ".+@gmail.com",
            "a@ba@z.com",
        ).forEach {
            // Verify that when attempting to normalize these invalid email addresses, the expected exception is thrown.
            assertThrows(InputValidationException::class.java) {
                utils.normalize(IdentityRequest.Email(it))
            }
        }
    }

    @Test
    fun `test email normalization detects valid input`() {
        val utils = InputUtils()

        mapOf(
            "TEst.TEST@Test.com " to "test.test@test.com",
            "test.test@test.com" to "test.test@test.com",
            "test.test@gmail.com" to "testtest@gmail.com",
            "test+test@test.com" to "test+test@test.com",
            "+test@test.com" to "+test@test.com",
            "test+test@gmail.com" to "test@gmail.com",
            "testtest@test.com" to "testtest@test.com",
            " testtest@test.com" to "testtest@test.com",
            "testtest@test.com " to "testtest@test.com",
            " testtest@test.com " to "testtest@test.com",
            "  testtest@test.com  " to "testtest@test.com",
            " test.test@gmail.com" to "testtest@gmail.com",
            "test.test@gmail.com " to "testtest@gmail.com",
            " test.test@gmail.com " to "testtest@gmail.com",
            "  test.test@gmail.com  " to "testtest@gmail.com",
            "TEstTEst@gmail.com  " to "testtest@gmail.com",
            "TEstTEst@GMail.Com  " to "testtest@gmail.com",
            " TEstTEst@GMail.Com  " to "testtest@gmail.com",
            "TEstTEst@GMail.Com" to "testtest@gmail.com",
            "TEst.TEst@GMail.Com" to "testtest@gmail.com",
            "TEst.TEst+123@GMail.Com" to "testtest@gmail.com",
            "TEst.TEST@Test.com " to "test.test@test.com",
            "TEst.TEST@Test.com " to "test.test@test.com",
            "\uD83D\uDE0Atesttest@test.com" to "\uD83D\uDE0Atesttest@test.com",
            "testtest@\uD83D\uDE0Atest.com" to "testtest@\uD83D\uDE0Atest.com",
            "testtest@test.com\uD83D\uDE0A" to "testtest@test.com\uD83D\uDE0A",
        ).forEach {
            // Verify that when normalizing these valid phone numbers, they pass as expected.
            val normalized = utils.normalize(IdentityRequest.Email(it.key))
            assertEquals(it.value, normalized.data)
        }
    }
}
