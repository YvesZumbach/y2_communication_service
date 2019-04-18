package com.y2.config

import akka.actor.ActorSystem
import com.y2.runtype.{NULL, Runtype}

case class Config(
                   runType: Runtype = NULL
                 )(implicit actorSystem: ActorSystem)
