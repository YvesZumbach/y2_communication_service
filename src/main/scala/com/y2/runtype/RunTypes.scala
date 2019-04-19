package com.y2.runtype

object RunTypes {
  sealed trait RunType
  case object Client extends RunType
  case object Node extends RunType
  case object Null extends RunType
}

