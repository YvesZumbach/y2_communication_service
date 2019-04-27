package com.y2.utils

import java.io.{BufferedReader, InputStreamReader}
import java.net.URL

object Utils {
  def isValidIpv4(ip: String): Boolean = {
    // Check the validity of the IP address
    try {
      if (ip == null || ip.isEmpty) return false
      val parts = ip.split("\\.")
      if (parts.length != 4) return false
      for (s <- parts) {
        val i = s.toInt
        if ((i < 0) || (i > 255)) return false
      }
      if (ip.endsWith(".")) return false
      true
    } catch {
      case _ => false
    }
  }

  def getMyIpAddress(): String = {
    val whatIsMyIp = new URL("http://checkip.amazonaws.com")
    val in:BufferedReader = new BufferedReader(new InputStreamReader(whatIsMyIp.openStream()))
    in.readLine()
  }

  def computeSeedNodeConfigurationString(seedNodeIp: String): String = {
    s"""akka.cluster.seed-nodes = ["akka://y2@$seedNodeIp:2551"]\n"""
  }

  def computeArteryHostConfigurationString(ip: String = getMyIpAddress()): String = {
    s"""akka.remote.artery.canonical.hostname = "$ip"\n"""
  }
}
