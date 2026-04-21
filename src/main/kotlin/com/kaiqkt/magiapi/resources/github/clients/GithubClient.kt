package com.kaiqkt.magiapi.resources.github.clients

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.isSuccessful
import com.kaiqkt.magiapi.resources.exceptions.UnexpectedResourceException
import com.kaiqkt.magiapi.resources.github.requests.GithubContentRequest
import com.kaiqkt.magiapi.resources.github.requests.GithubRepositoryRequest
import com.kaiqkt.magiapi.resources.github.requests.GithubWorkflowDispatchRequest
import com.kaiqkt.magiapi.resources.github.responses.GithubContentResponse
import com.kaiqkt.magiapi.resources.github.responses.GithubRepositoryResponse
import com.kaiqkt.magiapi.resources.github.responses.GithubUserResponse
import com.kaiqkt.magiapi.resources.github.responses.GithubWorkflowDispatchResponse
import com.kaiqkt.magiapi.resources.github.responses.GithubWorkflowRunResponse
import com.kaiqkt.magiapi.utils.MetricsUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component

@Component
class GithubClient(
    private val metricsUtils: MetricsUtils,
    private val mapper: ObjectMapper,
    @param:Value($$"${github.url}")
    private val apiUrl: String,
    @param:Value($$"${github.content-path}")
    private val contentPath: String
) {
    fun getUser(accessToken: String): GithubUserResponse? {
        val (_, response, result) =
            metricsUtils.request(GITHUB_GET_USER) {
                Fuel
                    .get("$apiUrl/user")
                    .header(
                        mapOf(
                            "Accept" to "application/vnd.github+json",
                            "X-GitHub-Api-Version" to "2026-03-10",
                            HttpHeaders.AUTHORIZATION to "Bearer $accessToken",
                        ),
                    )
                    .response()
            }

        println( "stats: ${response.statusCode}")

        return when {
            response.isSuccessful -> mapper.readValue(result.get(), GithubUserResponse::class.java)
            response.statusCode == 401 || response.statusCode == 403 -> null
            else -> throw UnexpectedResourceException("Fail to get user ${response.responseMessage}")
        }
    }

    fun createRepository(request: GithubRepositoryRequest, accessToken: String): GithubRepositoryResponse? {
        val (_, response, result) =
            metricsUtils.request(GITHUB_CREATE_REPO) {
                Fuel
                    .post("$apiUrl/user/repos")
                    .header(
                        mapOf(
                            "Content-Type" to MediaType.APPLICATION_JSON,
                            "Accept" to "application/vnd.github+json",
                            "X-GitHub-Api-Version" to "2026-03-10",
                            "Authorization" to "Bearer $accessToken",
                        ),
                    ).body(mapper.writeValueAsString(request))
                    .response()
            }

        return when {
            response.isSuccessful -> mapper.readValue(result.get(), GithubRepositoryResponse::class.java)

            response.statusCode == 401 || response.statusCode == 403 -> null

            else -> throw UnexpectedResourceException("Fail to create github repository ${response.responseMessage}")
        }
    }

    fun uploadContent(
        owner: String,
        repo: String,
        accessToken: String,
        request: GithubContentRequest
    ): GithubContentResponse? {
        val (_, response, _) =
            metricsUtils.request(GITHUB_CREATE_REPO) {
                Fuel
                    .put("$apiUrl/repos/$owner/$repo/contents/$contentPath")
                    .header(
                        mapOf(
                            "Accept" to "application/vnd.github+json",
                            "X-GitHub-Api-Version" to "2026-03-10",
                            "Content-Type" to MediaType.APPLICATION_JSON,
                            "Authorization" to "Bearer $accessToken",
                        ),
                    ).body(mapper.writeValueAsString(request))
                    .response()
            }

        return when {
            response.isSuccessful -> GithubContentResponse.Success

            response.statusCode == 401 || response.statusCode == 403 -> null

            else -> throw UnexpectedResourceException("Fail to upload content to github repository ${response.responseMessage}")
        }
    }

    fun triggerWorkflow(
        owner: String,
        repo: String,
        ref: String,
        accessToken: String,
    ): GithubWorkflowDispatchResponse? {
        val workflowId = contentPath.substringAfterLast("/")
        val request = GithubWorkflowDispatchRequest(ref = ref)

        val (_, response, result) =
            metricsUtils.request(GITHUB_TRIGGER_WORKFLOW) {
                Fuel
                    .post("$apiUrl/repos/$owner/$repo/actions/workflows/$workflowId/dispatches")
                    .header(
                        mapOf(
                            "Accept" to "application/vnd.github+json",
                            "X-GitHub-Api-Version" to "2026-03-10",
                            "Content-Type" to MediaType.APPLICATION_JSON,
                            "Authorization" to "Bearer $accessToken",
                        ),
                    ).body(mapper.writeValueAsString(request))
                    .response()
            }

        return when {
            response.isSuccessful -> mapper.readValue(result.get(), GithubWorkflowDispatchResponse::class.java)
            response.statusCode == 401 || response.statusCode == 403 -> null
            else -> throw UnexpectedResourceException("Fail to trigger workflow ${response.responseMessage}")
        }
    }

    fun getWorkflowRun(
        owner: String,
        repo: String,
        runId: Long,
        accessToken: String,
    ): GithubWorkflowRunResponse? {
        val (_, response, result) =
            metricsUtils.request(GITHUB_GET_WORKFLOW_RUN) {
                Fuel
                    .get("$apiUrl/repos/$owner/$repo/actions/runs/$runId")
                    .header(
                        mapOf(
                            "Accept" to "application/vnd.github+json",
                            "X-GitHub-Api-Version" to "2026-03-10",
                            "Authorization" to "Bearer $accessToken",
                        ),
                    ).response()
            }

        return when {
            response.isSuccessful -> mapper.readValue(result.get(), GithubWorkflowRunResponse::class.java)
            response.statusCode == 401 || response.statusCode == 403 -> null
            else -> throw UnexpectedResourceException("Fail to get workflow run ${response.responseMessage}")
        }
    }

    companion object {
        private const val GITHUB_GET_USER = "github_get_user"
        private const val GITHUB_CREATE_REPO = "github_create_repository"
        private const val GITHUB_TRIGGER_WORKFLOW = "github_trigger_workflow"
        private const val GITHUB_GET_WORKFLOW_RUN = "github_get_workflow_run"
    }
}