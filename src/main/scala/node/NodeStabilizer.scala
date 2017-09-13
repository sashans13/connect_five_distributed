package node

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.Socket

import common.{Connection, Constants, Util}

class NodeStabilizer(node: Node) extends Runnable {

    def startStabilizer(): Unit = {
        new Thread(this).start()
    }

    override def run() = {
        while (true) {
            // Sleep some amount of time and then check what's changed
            Thread.sleep(Constants.STABILIZE_SLEEP_TIME_MS)
            node.contextPrintln("NodeStabilizer:run:Run stabilize()")

            val successor: Connection = node.getSuccessor
            val successorSocket: Socket = new Socket(successor.ipAddress, successor.port)

            val outSuccessor = new ObjectOutputStream(successorSocket.getOutputStream)
            val inSuccessor = new ObjectInputStream(successorSocket.getInputStream)

            // Check node is alive
            if (Util.pingNode(successor)) {
                outSuccessor.writeObject("RequestPredecessor")
                outSuccessor.flush()

                val predecessorOfSuccOpt: Option[Connection] = inSuccessor.readObject().asInstanceOf[Option[Connection]]
                successorSocket.close()

                if (predecessorOfSuccOpt.isDefined && Util.isNodeInBetween(node.asConnection.id, successor.id, predecessorOfSuccOpt.get.id)) {
                    // If my successor's predecessor is between me and my successor, update successor
                    node.setSuccessorWithPropagate(predecessorOfSuccOpt.get)
                    // Also notify my new successor that I'm their predecessor
//                    node.notifyy()
                }

                // notifyy() and checkPredecessor() take much less time, so do them first
                node.notifyy()
                node.checkPredecessor()

                node.fixFingers()

                node.contextPrintln("Predecessor: " + node.predecessor +
                    "\nFingerTable:\n" + node.fingerTable.toString) +
                    "\n"
            }

            if (!successorSocket.isClosed) {
                successorSocket.close()
            }
        }
    }
}
