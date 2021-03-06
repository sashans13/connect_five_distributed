package bootstrap

import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap

import common.{Connection, Constants}

import scala.collection.JavaConverters._
import scala.collection.concurrent.{Map => ConcurrentMap}

object Bootstrap {
    val nodes: ConcurrentMap[Integer, Connection] = new ConcurrentHashMap[Integer, Connection]().asScala

    val reportLock: Object = new Object

    def startServer(): Unit = new Thread(new Bootstrap(Constants.BOOTSTRAP_LISTENER_PORT)).start()

    def addNode(node: Connection): Unit = {
        this.nodes(node.id) = node
    }
}

class Bootstrap(port: Int) extends Runnable {
    val serverSocket = new ServerSocket(port)

    def run(): Unit = {
        while (true) {
            // This blocks until a connection comes in
            val socket = serverSocket.accept()
            new BootstrapHandler(socket).startHandler()
        }
    }
}