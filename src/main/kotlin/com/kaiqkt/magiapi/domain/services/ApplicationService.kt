package com.kaiqkt.magiapi.domain.services

import com.kaiqkt.magiapi.domain.dtos.ApplicationDto
import com.kaiqkt.magiapi.domain.exceptions.DomainException
import com.kaiqkt.magiapi.domain.exceptions.ErrorType
import com.kaiqkt.magiapi.domain.models.Application
import com.kaiqkt.magiapi.domain.models.enums.ApplicationStatus
import com.kaiqkt.magiapi.domain.repositories.ApplicationRepository
import com.kaiqkt.magiapi.utils.MetricsUtils
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.CREATED
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.STATUS
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class ApplicationService(
    private val applicationRepository: ApplicationRepository,
    private val projectService: ProjectService,
    private val gitService: GitService,
    private val metricsUtils: MetricsUtils
) {

    fun create(
        applicationDto: ApplicationDto.Create,
        userId: String,
        tenantId: String
    ) {
        val (project, _) = projectService.resolveAuthorizedMembership(tenantId, userId)

        if (applicationRepository.existsByNameAndProjectId(applicationDto.name, project.id)) {
            metricsUtils.counter(APPLICATION, STATUS, "already_exists")

            throw DomainException(ErrorType.APPLICATION_ALREADY_EXIST)
        }

        val repositoryName = "${project.tenantId}_${applicationDto.name}"
        val repositoryUrl = gitService.createRepository(repositoryName, project.id)

        val application = Application(
            name = applicationDto.name,
            description = applicationDto.description,
            repositoryUrl = repositoryUrl,
            projectId = project.id,
        )

        applicationRepository.save(application)
        metricsUtils.counter(APPLICATION, STATUS, CREATED)
    }

    @Transactional
    fun provisionCiWorkflow(applicationId: String, userId: String, tenantId: String) {
        val (project, _) = projectService.resolveAuthorizedMembership(tenantId, userId)

        val application = applicationRepository.findById(applicationId).getOrNull()
            ?: throw DomainException(ErrorType.APPLICATION_NOT_FOUND)

        if (application.status == ApplicationStatus.PENDING_INITIALIZATION) {
            val repositoryName = "${project.tenantId}_${application.name}"

            gitService.provisionCiWorkflow(repositoryName, project.id)

            application.status = ApplicationStatus.CREATED
        }
    }

    companion object {
        private const val APPLICATION = "application"
    }
}