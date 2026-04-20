package com.kaiqkt.magiapi.domain.services

import com.kaiqkt.magiapi.domain.exceptions.DomainException
import com.kaiqkt.magiapi.domain.exceptions.ErrorType
import com.kaiqkt.magiapi.domain.models.Project
import com.kaiqkt.magiapi.domain.models.ProjectMembership
import com.kaiqkt.magiapi.domain.models.enums.MemberRole
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
            throw DomainException(ErrorType.PROJECT_ALREADY_EXIST)
        }

        projectRepository.save(project)

        val membership = ProjectMembership(
            userId = project.createdBy,
            projectId = project.id,
            role = MemberRole.OWNER
        )

        membershipRepository.save(membership)

        metricsUtils.counter(PROJECT, STATUS, CREATED)
    }

    //voltar aqui - pulando a etapa de invite
    fun createMembership(userId: String, tenantId: String, guestId: String) {
        val (project, _) = resolveAuthorizedMember(tenantId, userId)

        val newMembership = ProjectMembership(
            userId = guestId,
            projectId = project.id,
            role = MemberRole.MEMBER
        )

        metricsUtils.counter(PROJECT_INVITE, STATUS, "created")

        membershipRepository.save(newMembership)
    }

    fun resolveAuthorizedMember(tenantId: String, userId: String): Pair<Project, ProjectMembership> {
        val project = projectRepository.findByTenantId(tenantId)
            ?: throw DomainException(ErrorType.PROJECT_NOT_FOUND)
        val membership = membershipRepository.findByUserIdAndProjectId(userId, project.id)
            ?: throw DomainException(ErrorType.MEMBERSHIP_NOT_FOUND)

        if (membership.role in setOf(MemberRole.OWNER, MemberRole.ADMIN)) {
            return Pair(project, membership)
        }

        metricsUtils.counter(PROJECT, STATUS, "insufficient_permissions")
        throw DomainException(ErrorType.INSUFFICIENT_PERMISSION)
    }

    fun createGitAccount(userId: String, tenantId: String, accessToken: String) {
        val (project, _) = resolveAuthorizedMember(tenantId, userId)

        gitService.createAccount(accessToken, project.id)
    }

    companion object {
        private const val PROJECT = "project"
        private const val PROJECT_INVITE = "project_invite"
        private const val PROJECT_GIT_ACCOUNT = "project_git_account"
    }
}