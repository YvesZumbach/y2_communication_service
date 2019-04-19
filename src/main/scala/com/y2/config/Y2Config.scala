package com.y2.config

import com.y2.runtype.{RunType, Null}

case class Y2Config(
                     runType: RunType = Null,
                     local: Boolean = false,
                     localNodeCount: Int = 3
                   )
