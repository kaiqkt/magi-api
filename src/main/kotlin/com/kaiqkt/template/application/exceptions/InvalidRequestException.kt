package ${package}.application.exceptions

class InvalidRequestException(
    val errors: Map<String, Any>,
) : Exception()
