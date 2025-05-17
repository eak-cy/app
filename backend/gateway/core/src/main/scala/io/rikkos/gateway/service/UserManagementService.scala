package io.rikkos.gateway.service

import io.rikkos.domain.UserDetails
import io.rikkos.gateway.repository.UserRepository
import io.rikkos.gateway.smithy
import io.rikkos.gateway.smithy.UserManagementService
import zio.*

object UserManagementService {

  final private class UserManagementServiceImpl(userRepository: UserRepository)
      extends smithy.UserManagementService[Task] {

    override def onboardUser(request: smithy.OnboardUserRequest): Task[Unit] =
      userRepository.insertUserDetails(
        UserDetails(request.firstName, request.lastName, request.companyName)
      )
  }

  val live: ZLayer[UserRepository, Nothing, UserManagementService[Task]] = ZLayer(
    for {
      userRepository <- ZIO.service[UserRepository]
    } yield new UserManagementServiceImpl(userRepository): smithy.UserManagementService[Task]
  )

}
