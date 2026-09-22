package com.crative.studio_api

import com.crative.studio_api.shared.AbstractIntegrationTest
import org.junit.jupiter.api.Test

/**
 * Sobe o contexto inteiro contra o Postgres do Testcontainers.
 *
 * Passar aqui já cobre duas coisas de graça: as migrations do Flyway aplicam na ordem, e o
 * `ddl-auto: validate` do Hibernate confirma que nenhuma entidade divergiu do schema.
 */
class StudioApiApplicationTests : AbstractIntegrationTest() {

    @Test
    fun contextLoads() {
    }
}
