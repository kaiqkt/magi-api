package ${package}.domain.exceptions

class DomainException(
    val type: ErrorType,
) : Exception(type.message)
