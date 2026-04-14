package com.kaiqkt.magiapi.domain.services

import com.kaiqkt.magiapi.domain.exceptions.DomainException
import com.kaiqkt.magiapi.domain.exceptions.ErrorType
import com.kaiqkt.magiapi.domain.models.Project
import com.kaiqkt.magiapi.domain.models.ProjectMembership
import com.kaiqkt.magiapi.domain.models.enums.MemberRole
import com.kaiqkt.magiapi.domain.models.enums.MemberStatus
import com.kaiqkt.magiapi.domain.repositories.ProjectMemberShipRepository
import com.kaiqkt.magiapi.domain.repositories.ProjectRepository
import com.kaiqkt.magiapi.utils.MetricsUtils
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.CREATED
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.STATUS
import org.springframework.stereotype.Service

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val membershipRepository: ProjectMemberShipRepository,
    private val gitService: GitService,
    private val metricsUtils: MetricsUtils
) {

    fun create(project: Project) {
        if (projectRepository.existsByTenantId(project.tenantId)) {
            metricsUtils.counter(PROJECT, STATUS, "project_name_already_in_use")
            throw DomainException(ErrorType.PROJECT_ALREADY_EXIST)
        }

        projectRepository.save(project)

        val membership = ProjectMembership(
            userId = project.createdBy,
            projectId = project.id,
            role = MemberRole.OWNER,
            status = MemberStatus.ACTIVE,
        )

        membershipRepository.save(membership)

        metricsUtils.counter(PROJECT, STATUS, CREATED)
    }

    fun invite(userId: String, tenantId: String, guestId: String) {
        val project = findByTenantId(tenantId)
        val membership = findMembership(project.id, userId)

        if (!membership.hasPermission()) {
            metricsUtils.counter(PROJECT_INVITE, STATUS, "insufficient_permissions")

            throw DomainException(ErrorType.INSUFFICIENT_PERMISSION)
        }

        val newMembership = ProjectMembership(
            userId = guestId,
            projectId = project.id,
            role = MemberRole.MEMBER,
            status = MemberStatus.ACTIVE,
        )

        metricsUtils.counter(PROJECT_INVITE, STATUS, "created")

        membershipRepository.save(newMembership)
    }

    private fun findByTenantId(tenantId: String): Project {
        return projectRepository.findByTenantId(tenantId) ?: throw DomainException(ErrorType.PROJECT_NOT_FOUND)
    }

    private fun findMembership(projectId: String, userId: String): ProjectMembership {
       return membershipRepository.findByUserIdAndProjectId(userId, projectId)
            ?: throw DomainException(ErrorType.MEMBERSHIP_NOT_FOUND)
    }

    fun createGitAccount(tenantId: String, userId: String, accessToken: String) {
        val project = findByTenantId(tenantId)
        val membership = findMembership(project.id, userId)

        if (!membership.hasPermission()) {
            metricsUtils.counter(PROJECT_GIT_ACCOUNT, STATUS, "insufficient_permissions")

            throw DomainException(ErrorType.INSUFFICIENT_PERMISSION)
        }

        gitService.create(accessToken, project.id)
    }

    companion object {
        private const val PROJECT = "project"
        private const val PROJECT_INVITE = "project_invite"
        private const val PROJECT_GIT_ACCOUNT = "project_git_account"
    }
}