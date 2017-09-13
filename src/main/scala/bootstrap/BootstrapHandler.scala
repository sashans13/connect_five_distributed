package bootstrap

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.Socket

import common.Connection

import scala.util.Random
import scala.util.control.Breaks._

class BootstrapHandler(clientSocket: Socket) extends Runnable {

    def startHandler(): Unit = {
        new Thread(this).start()
    }

    def run(): Unit = {
        // TODO: Implement communication
        val out: ObjectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream)
        val in: ObjectInputStream = new ObjectInputStream(clientSocket.getInputStream)

        while(!clientSocket.isClosed) {
            val message: String = in.readObject().asInstanceOf[String]

            breakable {
                // TODO: change .equals to == everywhere where strings are compared
                if (message == "ReportPresence") Bootstrap.reportLock.synchronized {
                    println("BootstrapHandler:run:Entered ReportPresence")

                    // Get node that's reporting presence
                    val clientNode: Connection = in.readObject().asInstanceOf[Connection]

                    if (Bootstrap.nodes.isEmpty) {
                        // This is initial node, send None
                        val noneOpt: Option[Connection] = None
                        out.writeObject(noneOpt)
                        out.flush()

                    } else {
                        // This is NOT an initial node

                        // Pick a random connection
                        val valuesArray: Seq[Connection] = Bootstrap.nodes.values.toList
                        val r: Random = new Random
                        val connToReturn: Connection = valuesArray(r.nextInt(valuesArray.size))

                        out.writeObject(Option(connToReturn))
                        out.flush()
                    }

                    // Add node to list
                    Bootstrap.addNode(clientNode)

                    clientSocket.close()
                    break
                }

                throw new Exception("BootstrapHandler: Unknown message: " + message)
            }
        }
    }
}
