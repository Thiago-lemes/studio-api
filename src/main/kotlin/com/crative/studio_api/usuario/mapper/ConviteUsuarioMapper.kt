package com.crative.studio_api.usuario.mapper

import com.crative.studio_api.usuario.dto.response.ConviteCriadoResponse
import com.crative.studio_api.usuario.dto.response.ConviteResumoResponse
import com.crative.studio_api.usuario.dto.response.ConviteValidoResponse
import com.crative.studio_api.usuario.entity.ConviteUsuarioEntity
import com.crative.studio_api.usuario.service.ConviteCriado
import com.crative.studio_api.usuario.service.ConviteResumo

fun ConviteCriado.toConviteCriadoResponse(): ConviteCriadoResponse =
    ConviteCriadoResponse(
        id = convite.id,
        email = convite.email,
        role = convite.role,
        status = convite.status,
        expiraEm = convite.expiraEm,
        linkConvite = link,
        token = convite.token
    )

fun ConviteResumo.toConviteResumoResponse(): ConviteResumoResponse =
    ConviteResumoResponse(
        id = convite.id,
        email = convite.email,
        role = convite.role,
        // O status efetivo do resumo, não `convite.status` — é o ponto do DTO.
        status = status,
        expiraEm = convite.expiraEm,
        criadoEm = convite.criadoEm,
        linkConvite = link,
        token = convite.token
    )

fun ConviteUsuarioEntity.toConviteValidoResponse(): ConviteValidoResponse =
    ConviteValidoResponse(
        email = email,
        role = role
    )
