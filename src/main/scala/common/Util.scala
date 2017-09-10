package common

import java.io.IOException
import java.net.ServerSocket

import scala.util.Random

object Util {
    def getIdFromPort(port: Int): Int = port - Constants.PORT_RANGE_START

    // Try to bind socket to port
    def tryBindSocket(portRangeStart: Int, portRangeWidth: Int, maxAttempts: Int = 100): Option[ServerSocket] = {
        var socket: Option[ServerSocket] = None
        val randGen = new Random()

        var attemptsCnt: Int = 0

        while(socket.isEmpty && attemptsCnt > maxAttempts) {
            attemptsCnt += 1

            try {
                socket = Option(new ServerSocket(portRangeStart + randGen.nextInt(portRangeWidth)))
            } catch {
                case e: IOException => e.printStackTrace()
            }
        }

        socket
    }
}
