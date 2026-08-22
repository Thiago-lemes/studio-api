package com.crative.studio_api.shared.security

import java.util.UUID

data class AuthenticatedUserDetails(
    val professorId: UUID?
)