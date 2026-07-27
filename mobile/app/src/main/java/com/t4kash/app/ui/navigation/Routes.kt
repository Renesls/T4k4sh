package com.t4kash.app.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val MARKETPLACE = "marketplace"
    const val OPPORTUNITY_MAP = "opportunity-map"
    const val NETWORK = "network"
    const val POST = "post"
    const val CHAT = "chat"
    const val WALLET = "wallet"
    const val APPLICATION_SENT = "application-sent"

    const val TASK_ID_ARG = "taskId"
    const val TASK_DETAILS = "opportunity/{$TASK_ID_ARG}"
    const val OPPORTUNITY_MAP_TASK = "opportunity-map/{$TASK_ID_ARG}"
    const val TASK_APPLICATIONS = "opportunity/{$TASK_ID_ARG}/applications"

    fun taskDetails(taskId: Int): String = "opportunity/$taskId"
    fun opportunityMap(taskId: Int): String = "opportunity-map/$taskId"
    fun taskApplications(taskId: Int): String = "opportunity/$taskId/applications"
}
