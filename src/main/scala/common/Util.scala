package common

import java.io.{IOException, ObjectInputStream, ObjectOutputStream}
import java.net.{ServerSocket, Socket}

import node.FingerTable

import scala.util.Random

object Util {
    def getIdFromPort(port: Int): Int = port - Constants.PORT_RANGE_START

    // Try to bind socket to port
    def tryBindSocket(portRangeStart: Int, portRangeWidth: Int, maxAttempts: Int = 100): Option[ServerSocket] = {
        var socket: Option[ServerSocket] = None
        val randGen = new Random()

        var attemptsCnt: Int = 0

        while (socket.isEmpty && attemptsCnt < maxAttempts) {
            attemptsCnt += 1

            try {
                socket = Option(new ServerSocket(portRangeStart + randGen.nextInt(portRangeWidth)))
            } catch {
                case e: IOException => e.printStackTrace()
            }
        }

        socket
    }

    def pingNode(nodeConn: Connection): Boolean = {
        val toOtherNode: Socket = new Socket(nodeConn.ipAddress, nodeConn.port)

        val out = new ObjectOutputStream(toOtherNode.getOutputStream)
        val in = new ObjectInputStream(toOtherNode.getInputStream)

        // Pint-pong
        out.writeObject("Ping")
        out.flush()

        val pingResponse: String = in.readObject().asInstanceOf[String]

        // Close the socket
        toOtherNode.close()

        // Return whether ping-pong went thru
        pingResponse.equals("Pong")
    }

    def checkIsConnected(nodeConn: Connection): Boolean = {
        val toOtherNode: Socket = new Socket(nodeConn.ipAddress, nodeConn.port)

        val out = new ObjectOutputStream(toOtherNode.getOutputStream)
        val in = new ObjectInputStream(toOtherNode.getInputStream)

        // Check if connected
        out.writeObject("AreYouConnected")
        out.flush()

        val connectedResponse: Boolean = in.readObject().asInstanceOf[Boolean]

        // Close the socket
        toOtherNode.close()

        connectedResponse
    }

    def getClockwiseDistance(left: Int, right: Int): Int = {
        val rightPadded = if (left > right) right + Constants.CHORD_SIZE else right

        rightPadded - left
    }

    def isNodeInBetween(left: Int, right: Int, node: Int, includeLeft: Boolean = false, includeRight: Boolean = false): Boolean = {
        val leftToMid = getClockwiseDistance(left, node)
        val midToRight = getClockwiseDistance(node, right)
        val leftToRight = getClockwiseDistance(left, right)

        val ret: Boolean =
        // Checks if in-between
            ((leftToMid + midToRight == leftToRight) ||
                // left and right are same node, so it is in-between (but make it strict too)
                (leftToRight == 0)) &&
                // apply strictness
                ((leftToMid != 0 || includeLeft) && (midToRight != 0 || includeRight))

        // TODO: Debug output with default Option[Connection] parameter to the method
//        println("\nleft: " + left +
//            "\nright: " + right +
//            "\nnode: " + node +
//            "\nleft-mid: " + leftToMid +
//            "\nmid-right: " + midToRight +
//            "\nleft-right: " + leftToRight +
//            "\nincludeLeft: " + includeLeft +
//            "\nincludeRight: " + includeRight +
//            "\nret: " + ret +
//            "\n"
//        )

        ret
    }

    def getFingerTableFromNode(node: Connection): Option[FingerTable] = {
        val toNodeSocket: Socket = new Socket(node.ipAddress, node.port)

        val out = new ObjectOutputStream(toNodeSocket.getOutputStream)
        val in = new ObjectInputStream(toNodeSocket.getInputStream)

        // Send a request for finger table
        out.writeObject("FingerTableRequest")
        out.flush()

        // Get the table in the response
        val response: FingerTable = in.readObject().asInstanceOf[FingerTable]

        toNodeSocket.close()

        Option(response)
    }

    def log2floor(num: Int): Int = (Math.log(num) / Math.log(2)).toInt
}