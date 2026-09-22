package com.crative.studio_api.shared

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Um único Postgres para a suíte inteira.
 *
 * O container é um singleton estático iniciado na primeira vez que a classe é carregada e
 * deliberadamente **nunca** é parado: o Ryuk derruba tudo no fim da JVM. Subir um container
 * por classe de teste custaria alguns segundos multiplicados por cada classe — como a limpeza
 * entre testes é feita por TRUNCATE em [AbstractIntegrationTest], compartilhar é seguro.
 *
 * A imagem é fixada (`postgres:16-alpine`, e não `latest`) para o teste não mudar de
 * comportamento quando a tag remota for atualizada.
 */
@TestConfiguration(proxyBeanMethods = false)
class PostgresContainerConfiguration {

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer = postgres

    companion object {
        @JvmStatic
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("studio_api_test")
                .withUsername("test")
                .withPassword("test")
                .also { it.start() }
    }
}
