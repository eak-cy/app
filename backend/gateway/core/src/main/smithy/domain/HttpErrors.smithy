$version: "2.0"
namespace io.rikkos.gateway.smithy

@error("server")
@httpError(503)
structure ServiceUnavailable {
    @required
    code: Integer = 503
    @required
    message: String = "The server is currently unavailable."
}
