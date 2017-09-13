package node

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.Socket

import common.Connection

import util.control.Breaks._

class NodeClientHandler(clientSocket: Socket, node: Node) extends Runnable {

    def startNodeClientHandler(): Unit = {
        val clientHandlerThread: Thread = new Thread(this)
        clientHandlerThread.start()
    }

    override def run(): Unit = {
        val in = new ObjectInputStream(clientSocket.getInputStream)
        val out = new ObjectOutputStream(clientSocket.getOutputStream)

        while (!clientSocket.isClosed) {
            // Communication protocol
            val message: String = in.readObject().asInstanceOf[String]

            breakable {
                // break inside breakable evaluates to continue if there's nothing in the loop after breakable{}
                // =============== Ping - Pong ===============
                if (message == "Ping") {
                    out.writeObject("Pong")
                    out.flush()

                    clientSocket.close()
                    break // Is actually continue when in breakable{}
                }

                if (message == "AreYouConnected") {
                    out.writeObject(node.isConnected)
                    out.flush()

                    clientSocket.close()
                    break
                }

                if (message == "FingerTableRequest") {
                    out.writeObject(node.fingerTable)
                    out.flush()

                    clientSocket.close()
                    break
                }

                if (message == "RequestPredecessor") {
                    out.writeObject(Option(node.predecessor))
                    out.flush()

                    clientSocket.close()
                    break
                }

                if (message == "Notify") {
                    val predecessorConn: Connection = in.readObject().asInstanceOf[Connection]
                    node.setPredecessor(predecessorConn)

                    clientSocket.close()
                    break
                }



                throw new Exception("NodeClientHandler: Unknown message: " + message)
            }
        }
    }
}
