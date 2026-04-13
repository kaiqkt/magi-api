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
        val project = projectRepository.findByTenantId(tenantId) ?: throw DomainException(ErrorType.PROJECT_NOT_FOUND)
        val membership = membershipRepository.findByUserIdAndProjectId(userId, project.id)
            ?: throw DomainException(ErrorType.USER_NOT_FOUND)

        if (membership.role !in setOf(MemberRole.OWNER, MemberRole.ADMIN)) {
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

    companion object {
        private const val PROJECT = "project"
        private const val PROJECT_INVITE = "project_invite"
    }
}