package io.rikkos.gateway.unit

import io.rikkos.gateway.service.UserManagementService
import io.rikkos.gateway.smithy
import io.rikkos.gateway.unit.mock.UserRepositoryMock
import io.rikkos.testkit.base.ZWordSpecBase
import zio.{FiberFailure, Task, ZIO, ZLayer}

class UserManagementServiceSpec extends ZWordSpecBase {

  val userManagementServiceEnv = ZIO.service[smithy.UserManagementService[Task]]

  "UserManagementService" when {
    "onboardUser" should {
      "insert the user successfully" in {
        val userManagementService: ZIO[Any, Nothing, smithy.UserManagementService[Task]] =
          userManagementServiceEnv
            .provide(UserManagementService.live, ZLayer.succeed(UserRepositoryMock()))

        userManagementService
          .flatMap(_.onboardUser(smithy.OnboardUserRequest("rikkos", "mappouros", "rikkosLTD")))
          .zioEither
          .isRight shouldBe true

      }
      "throw exception when repository fail" in {
        val userManagementService: ZIO[Any, Nothing, smithy.UserManagementService[Task]] =
          userManagementServiceEnv
            .provide(UserManagementService.live, ZLayer.succeed(UserRepositoryMock(Some(new RuntimeException()))))

        assertThrows[FiberFailure] {
          userManagementService
            .flatMap(_.onboardUser(smithy.OnboardUserRequest("rikkos", "mappouros", "rikkosLTD")))
            .either
            .zioValue
        }
      }
    }
  }
}
