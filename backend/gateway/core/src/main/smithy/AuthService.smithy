$version: "2"
namespace io.rikkos.gateway.smithy

use alloy#simpleRestJson

@simpleRestJson
@httpBearerAuth
service AuthService {
    version: "1.0.0",
    operations: [UserInt]
}

@http(method: "POST", uri: "/auth", code: 204)
operation UserInt {
    input := {
        @required
        @httpPayload
        request: UserFormRequest
    }
    errors: [Unauthorized]
}

structure UserFormRequest {
    @required
    nickname: String
    @required
    email: String
    @required
    password: String
}