$version: "2"
namespace io.rikkos.gateway.smithy

use alloy#simpleRestJson

@simpleRestJson
service UserService {
    version: "1.0.0",
    operations: [OnboardUser]
}

@input
structure OnboardUserInput {
    firstName: String
    lastName: String
    companyName: String
}

structure UserDetails {
    email: String
    firstName: String
    lastName: String
    companyName: String
}

@http(method: "POST", uri: "/users/onboard")
operation OnboardUser {
    input: OnboardUserInput,
    output: UserDetails
}
