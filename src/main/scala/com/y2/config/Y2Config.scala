package com.y2.config

import com.y2.runtype.RunTypes.{RunType, Null}

case class Y2Config(
                     runType: RunType = Null,
                     local: Boolean = false
                 )
