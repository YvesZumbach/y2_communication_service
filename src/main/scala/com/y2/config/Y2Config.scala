package com.y2.config

import com.y2.runtype.{RunType, Null}

case class Y2Config(
                     runType: RunType = Null,
                     // Client option
                     nodeCount: Int = -1,
                     // Node options
                     local: Boolean = false,
                     localNodeCount: Int = 1
                   )

object Y2Config {
  var config: Y2Config = _
}