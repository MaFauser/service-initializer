package com.mafauser.service

import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser

@WithMockUser
class ApplicationTests : BaseIntegrationTest() {
    @Test
    fun contextLoads() {
    }
}
