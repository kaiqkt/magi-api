package com.kaiqkt.magiapi.domain.services

import com.kaiqkt.magiapi.domain.exceptions.DomainException
import com.kaiqkt.magiapi.domain.exceptions.ErrorType
import com.kaiqkt.magiapi.domain.models.Project
import com.kaiqkt.magiapi.domain.repositories.ProjectRepository
import com.kaiqkt.magiapi.utils.MetricsUtils
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.CREATED
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.STATUS
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val gitService: GitService,
    private val metricsUtils: MetricsUtils
) {

    fun create(project: Project) {
        if (projectRepository.existsBySlug(project.slug)) {
            throw DomainException(ErrorType.PROJECT_ALREADY_EXIST)
        }

        projectRepository.save(project)

        metricsUtils.counter(PROJECT, STATUS, CREATED)
    }

    fun findByIdAndUserId(id: String, userId: String): Project = projectRepository.findByIdAndUserId(id, userId)
        ?: throw DomainException(ErrorType.PROJECT_NOT_FOUND)

    //mover para o git-service
    fun createGitAccount(userId: String, projectId: String, accessToken: String) {
        val project = findByIdAndUserId(projectId, userId)

        gitService.createAccount(accessToken, project.id)
    }

    companion object {
        private const val PROJECT = "project"
    }
}