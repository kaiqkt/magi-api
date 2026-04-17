package com.kaiqkt.magiapi.domain.services

import com.kaiqkt.magiapi.domain.dtos.ApplicationDto
import com.kaiqkt.magiapi.domain.exceptions.DomainException
import com.kaiqkt.magiapi.domain.exceptions.ErrorType
import com.kaiqkt.magiapi.domain.models.Application
import com.kaiqkt.magiapi.domain.repositories.ApplicationRepository
import com.kaiqkt.magiapi.utils.MetricsUtils
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.CREATED
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.STATUS
import com.kaiqkt.magiapi.utils.slugify
import org.springframework.stereotype.Service

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
        val project = projectService.findByTenantId(tenantId)
        val membership = projectService.findMembership(project.id, userId)

        if (!membership.hasPermission()) {
            metricsUtils.counter(APPLICATION, STATUS, "insufficient_permissions")

            throw DomainException(ErrorType.INSUFFICIENT_PERMISSION)
        }

        val repositoryName = "${project.name.slugify()}_${applicationDto.name.slugify()}"
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

    companion object {
        private const val APPLICATION = "application"
    }
}