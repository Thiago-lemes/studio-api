package com.crative.studio_api.usuario.entity

enum class StatusConvite {
    PENDENTE,
    UTILIZADO,
    EXPIRADO,

    /** Cancelado pelo ADMIN antes de ser usado — convite enviado por engano. */
    REVOGADO;
}
