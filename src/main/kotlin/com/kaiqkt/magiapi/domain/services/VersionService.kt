package com.kaiqkt.magiapi.domain.services

import org.springframework.stereotype.Service
import java.util.Base64

@Service
class VersionService(
    private val gitService: GitService,
    private val projectService: ProjectService,
    private val applicationService: ApplicationService
) {
    private val encodedCiContent: String =
        requireNotNull(this::class.java.getResourceAsStream("/github-actions/ci.yml"))
            .bufferedReader()
            .use { Base64.getEncoder().encodeToString(it.readText().toByteArray()) }

    fun createCI(
        applicationId: String,
        projectId: String,
        userId: String
    ) {
        val project = projectService.findByIdAndUserId(projectId, userId)
        val application = applicationService.findByIdAndProjectId(applicationId, projectId)

        gitService.uploadContent(
            repository = application.name,
            projectId = project.id,
            content = encodedCiContent,
            path = CI_PATH
        )
    }

    companion object {
        private const val CI_PATH = ".github/workflows/magi-ci.yml"
    }
}

//createCiWorkflow
//trigger