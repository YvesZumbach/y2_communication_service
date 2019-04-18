package com.y2.runtype

sealed trait Runtype
case object CLIENT extends Runtype
case object NODE extends Runtype
case object NULL extends Runtype
