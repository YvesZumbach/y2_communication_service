package com.y2.config

import com.y2.runtype.{Null, RunType}

case class Y2Config(
                     runType: Runtype = Null,
                     local: Boolean = false
                 )
