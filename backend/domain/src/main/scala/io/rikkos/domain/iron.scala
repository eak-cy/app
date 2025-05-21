package io.rikkos.domain

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

type NonEmptyTrimmedLowerCase = Trimmed & LettersLowerCase & MinLength[1]

object AppName extends RefinedType[String, Pure]
type AppName = AppName.T

object MemberID extends RefinedType[String, NonEmptyTrimmedLowerCase]
type MemberID = MemberID.T

object Email extends RefinedType[String, NonEmptyTrimmedLowerCase]
type Email = Email.T
