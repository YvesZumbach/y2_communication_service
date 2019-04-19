package com.y2.runtype

sealed trait RunType
case object Client extends RunType
case object Node extends RunType
case object Null extends RunType

