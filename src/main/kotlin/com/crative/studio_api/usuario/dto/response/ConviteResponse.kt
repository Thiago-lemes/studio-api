package com.crative.studio_api.usuario.dto.response

import com.crative.studio_api.usuario.entity.RoleType
import com.crative.studio_api.usuario.entity.StatusConvite
import java.time.LocalDateTime
import java.util.UUID

data class ConviteCriadoResponse(
    val id: UUID,
    val email: String,
    val role: RoleType,
    val status: StatusConvite,
    val expiraEm: LocalDateTime,

    /**
     * `linkConvite` e `token` existem só enquanto não há envio de e-mail: são o meio de entrega
     * desta fase. Saem da resposta quando o notification-service entrar.
     */
    val linkConvite: String,
    val token: String
)

/** Resposta do endpoint público de validação: o mínimo para a tela montar o formulário. */
data class ConviteValidoResponse(
    val email: String,
    val role: RoleType
)

/**
 * Item da listagem de convites (tela de gestão de acesso do ADMIN).
 *
 * `status` é o efetivo, não a coluna: um convite vencido chega aqui como EXPIRADO mesmo estando
 * gravado como PENDENTE.
 *
 * `linkConvite` e `token` repetem a ressalva do [ConviteCriadoResponse] — estão aqui para o ADMIN
 * reenviar o link à mão, e saem junto com os do POST quando o envio automático de e-mail entrar.
 */
data class ConviteResumoResponse(
    val id: UUID,
    val email: String,
    val role: RoleType,
    val status: StatusConvite,
    val expiraEm: LocalDateTime,
    val criadoEm: LocalDateTime?,
    val linkConvite: String,
    val token: String
)
