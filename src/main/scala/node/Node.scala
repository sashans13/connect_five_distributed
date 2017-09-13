package node

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.{InetAddress, ServerSocket, Socket}

import util.control.Breaks._
import common.{Connection, Constants, Util}

object Node {
    // TODO: A bunch of util stuff should be here in Node

    def contextPrintln(node: Node, string: String): Unit = {
        val nodeInfo = "[Node " + node.id + " @ " + node.ipAddress + ":" + node.port + "]\n"
        println("\n" + nodeInfo + ">> " + string + "\n")
    }
}

class Node extends Runnable {
    val serverSocket: ServerSocket = Util.tryBindSocket(Constants.PORT_RANGE_START, Constants.PORT_RANGE_WIDTH).get

    val ipAddress: String = InetAddress.getLocalHost.getHostAddress
    val port: Int = serverSocket.getLocalPort

    val id: Int = Util.getIdFromPort(port)
    val asConnection: Connection = new Connection(ipAddress, port)

    // Other
    var predecessor: Connection = _
    val fingerTable: FingerTable = new FingerTable(asConnection)
    var isConnected: Boolean = false
    private val predecessorLock: Object = new Object

    /**
      * Use this to start running node in a new Thread
      */
    def startNode(): Unit = {
        val node: Thread = new Thread(this)
        node.start()
    }

    override def run(): Unit = {
        // Start a thread that listens for communication from other nodes
        val nodeServerHandler: NodeServerHandler = new NodeServerHandler(this)
        nodeServerHandler.startNodeServerHandler()

        // Create a thread that runs chord's stabilize() algorithm, but start it after node join the chord
        val nodeStabilizer: NodeStabilizer = new NodeStabilizer(this)

        val bootstrapSocket = new Socket(Constants.BOOTSTRAP_IP_ADDRESS, Constants.BOOTSTRAP_LISTENER_PORT)
        val out = new ObjectOutputStream(bootstrapSocket.getOutputStream)
        val in = new ObjectInputStream(bootstrapSocket.getInputStream)

        var joinAttempts: Int = 0

        while (!bootstrapSocket.isClosed && joinAttempts < Constants.MAX_JOIN_ATTEMPTS) {
            joinAttempts += 1

            out.writeObject("ReportPresence")
            out.writeObject(this.asConnection)
            out.flush()

            // Get a random node from bootstrap
            val hookConnectionOpt: Option[Connection] = in.readObject().asInstanceOf[Option[Connection]]

            if (hookConnectionOpt.isEmpty) {
                // Initial node if there is no connection
                contextPrintln("Initial node joined chord.")
            }
            else {
                // Not initial node
                if (joinChordCircle(hookConnectionOpt.get)) {
                    contextPrintln("Node:run:Noninital node joined chord.\n" + this.fingerTable.toString)
                }
            }

            // We joined the chord
            this.isConnected = true

            // Start stabilizer after we joined
            nodeStabilizer.startStabilizer()

            if (!bootstrapSocket.isClosed) {
                bootstrapSocket.close()
            }
        }

        contextPrintln("Node:run:Finished the run")
    }

    def joinChordCircle(other: Connection): Boolean = {
        contextPrintln("Trying to join node " + other.ipAddress + ":" + other.port)

        // If other node is not connected yet, return false
        val otherIsConnected: Boolean = Util.checkIsConnected(other)
        if (!otherIsConnected) {
            return false
        }

        val closestPredecessorOpt: Option[Connection] = getClosestPredecessor(other, this.asConnection)
        if (closestPredecessorOpt.isEmpty) {
            // Boo, we didn't connect :/
            false
        } else {
            // Not needed here since notifyy() from other nodes, but improves correctness now instead of later
            this.setPredecessor(closestPredecessorOpt.get)

            val validSuccessor: Connection = this.getValidSuccessorFromPredecessor(closestPredecessorOpt.get, this.asConnection)
            this.setSuccessorWithPropagate(validSuccessor)

            this.notifyy()

//            this.fixFingers()
//             Yay, we connected!
            true
        }
    }

    /**
      * Get closest predecessor: start looking from fromNode, predecessor of ofNode
      */
    def getClosestPredecessor(fromNode: Connection, ofNode: Connection): Option[Connection] = {
//        contextPrintln("fromNode: " + fromNode +
//            "\nofNode: " + ofNode +
//            "\n"
//        )
        // Get other's finger table
        val otherFingerTableOpt: Option[FingerTable] = Util.getFingerTableFromNode(fromNode)
//        contextPrintln("OtherFingerTableOpt:\n" + otherFingerTableOpt.get)

        if (otherFingerTableOpt.isDefined) {
            var prevBestConn: Connection = fromNode

            breakable {
                for (currFinger <- otherFingerTableOpt.get.getTable.values) {
//                    println("prev: " + prevBestConn.id + " ofNode: " + ofNode.id + " curr: " + currFinger.id)
                    if (Util.isNodeInBetween(prevBestConn.id, ofNode.id, currFinger.id) && prevBestConn.id < currFinger.id) {
//                        println("yiss")
                        prevBestConn = currFinger
                    }
                    else {
//                        break
                    }
                }
            }

            // Check if we found something closer from this node or not
//            contextPrintln("prev: " + prevBestConn + "\nfromNode: " + fromNode)
            if (prevBestConn.id != fromNode.id) {
                // If so, try find even close
                this.getClosestPredecessor(prevBestConn, ofNode)
            } else {
                // Else return best found so far (since there is no better)
                Option(prevBestConn)
            }
        } else {
            None
        }
    }

    /**
      * Gets predecessor's successor such that successor is after me
      * @param predecessor
      * @param me
      * @return
      */
    def getValidSuccessorFromPredecessor(predecessor: Connection, me: Connection): Connection = {
//        println("STUFF" +
//            "\npredec: " + predecessor +
//            "\nme: " + me +
//            "\n"
//        )

        val predecessorsFingerTableOpt: Option[FingerTable] = Util.getFingerTableFromNode(predecessor)

//        println("STUFF" +
//            "\nisDef: " + predecessorsFingerTableOpt.isDefined +
//            "\n"
//        )

//        var i: Int = 0
        for(finger <- predecessorsFingerTableOpt.get.getTable.values) {
//            println("i: " + i + "\nfinger: " + finger)
//            i += 1
            if(!Util.isNodeInBetween(predecessor.id, me.id, finger.id)) {
                // Means that successor is valid for "me"
                return finger
            }
        }

        // If suddenly Constants.FINGER_TABLE_SIZE get between us and predecessor before
        // we get predecessor's successor (that is valid for us), this might happen
        throw new Exception("This can happen in an extremely rare case, but should be handled")
    }

    def notifyy(): Unit = {
        val successor: Connection = this.getSuccessor

        val successorSocket: Socket = new Socket(successor.ipAddress, successor.port)
        val out = new ObjectOutputStream(successorSocket.getOutputStream)
        val in = new ObjectInputStream(successorSocket.getInputStream)

        out.writeObject("Notify")
        out.writeObject(this.asConnection)
        out.flush()

        successorSocket.close()
    }

    def fixFingers(): Unit = {
        // For every finger, try to find best node

        for (i <- 0 until Constants.FINGER_TABLE_SIZE) {
            breakable {
                // A dummy connection so it fits in with the algorithm (was easier this way...)
                val dummyId = (this.id + Math.pow(2, i).toInt) % Constants.CHORD_SIZE + Constants.PORT_RANGE_START
                val dummyConnection: Connection = new Connection(
                    "dummy",
                    dummyId
                )


                // println("AAAAAAAAAAAAAAAAAAAAA")
                val closestPredOpt = getClosestPredecessor(this.getSuccessor, dummyConnection)

                // If can't get predecessor, move to next finger entry
                if (closestPredOpt.isEmpty) {
                    break
                }

                val validSuccessor = getValidSuccessorFromPredecessor(closestPredOpt.get, dummyConnection)
                // val validSuccessor = closestPredOpt.get.
//                contextPrintln("i: " + i +
//                    "\npred: " + closestPredOpt.get +
//                    "\nsucc: " + validSuccessor
//                )

                // TODO: Move this to setFinger + add lock
                if (Util.getClockwiseDistance(dummyId, validSuccessor.id) < Util.getClockwiseDistance(dummyId, this.fingerTable.getFinger(i).id)) {
                    this.fingerTable.setFinger(i, validSuccessor)
                }
            }
        }
    }

    def checkPredecessor(): Unit = {
        predecessorLock.synchronized {
            if (this.predecessor != null && !Util.pingNode(this.predecessor)) {
                this.predecessor = null
            }
        }
    }

    def setPredecessor(predecessorConn: Connection): Unit = {
        // Use a lock so predecessor doesn't change after isNodeInBetween check
        predecessorLock.synchronized {
            if (predecessor == null || Util.isNodeInBetween(predecessor.id, this.asConnection.id, predecessorConn.id)) {
                contextPrintln("Node: setPredecessor: Set predecessor from " + this.predecessor + " to " + predecessorConn.id)
                this.predecessor = predecessorConn
            }
        }
    }

    def setSuccessorWithPropagate(successorConn: Connection): Unit = {
        // TODO: Do we want to do something extra here?
        val diff: Int = Util.getClockwiseDistance(this.asConnection.id, successorConn.id)
        val goUpto: Int = Util.log2floor(diff)

        for (i <- 0 to goUpto) {
            this.fingerTable.setFinger(i, successorConn)
        }
    }

    def getSuccessor: Connection = this.fingerTable.getSuccessor

    def contextPrintln(string: String): Unit = Node.contextPrintln(this, string)
}
