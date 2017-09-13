package node

import java.net.Socket

class NodeServerHandler(node: Node) extends Runnable {

    def startNodeServerHandler(): Unit = {
        val serverHandlerThread: Thread = new Thread(this)
        serverHandlerThread.start()
    }

    override def run() = {
        while (true) {
            val clientSocket: Socket = node.serverSocket.accept()
            // Accept incoming connection in a new Thread
            new Thread(new NodeClientHandler(clientSocket, node)).start()
        }
    }
}
