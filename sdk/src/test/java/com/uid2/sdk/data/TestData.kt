package com.uid2.sdk.data

object TestData {
    // A valid json representation, but missing the required "refresh_response_key" parameter.
    const val INVALID_IDENTITY = "{\"advertising_token\":\"token\",\"refresh_token\":\"refresh\",\"identity_expires\":123456,\"refresh_from\":321,\"refresh_expires\":654321}"

    // A valid, yet made up, json representation.
    const val VALID_IDENTITY = "{\"advertising_token\":\"token\",\"refresh_token\":\"refresh\",\"identity_expires\":123456,\"refresh_from\":321,\"refresh_expires\":654321,\"refresh_response_key\":\"response key\"}"
    const val VALID_IDENTITY_AD_TOKEN = "token"
    const val VALID_IDENTITY_REFRESH_TOKEN = "refresh"
    const val VALID_IDENTITY_EXPIRES = 123456L
    const val VALID_IDENTITY_REFRESH_FROM = 321L
    const val VALID_IDENTITY_REFRESH_EXPIRES = 654321L
    const val VALID_IDENTITY_REFRESH_RESPONSE_KEY = "response key"
}
