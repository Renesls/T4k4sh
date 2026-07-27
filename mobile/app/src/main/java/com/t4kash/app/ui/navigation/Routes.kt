package com.t4kash.app.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val MARKETPLACE = "marketplace"
    const val OPPORTUNITY_MAP = "opportunity-map"
    const val NETWORK = "network"
    const val POST = "post"
    const val CHAT = "chat"
    const val PROFILE = "profile"
    const val WALLET = "wallet"
    const val APPLICATION_SENT = "application-sent"

    const val TASK_ID_ARG = "taskId"
    const val PUBLICATION_FILTER_ARG = "filter"
    const val TASK_DETAILS = "opportunity/{$TASK_ID_ARG}"
    const val OPPORTUNITY_MAP_TASK = "opportunity-map/{$TASK_ID_ARG}"
    const val TASK_APPLICATIONS = "opportunity/{$TASK_ID_ARG}/applications"
    const val MY_PUBLICATIONS = "profile/publications/{$PUBLICATION_FILTER_ARG}"

    fun taskDetails(taskId: Int): String = "opportunity/$taskId"
    fun opportunityMap(taskId: Int): String = "opportunity-map/$taskId"
    fun taskApplications(taskId: Int): String = "opportunity/$taskId/applications"
    fun myPublications(filter: String = "ALL"): String = "profile/publications/$filter"
}
