package bootstrap

import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap

import common.{Connection, Constants}

import scala.collection.JavaConverters._
import scala.collection.concurrent.{Map => ConcurrentMap}

object Bootstrap {
    var nodes: ConcurrentMap[Integer, Connection] = new ConcurrentHashMap[Integer, Connection]().asScala

    def startServer(): Unit = new Bootstrap(Constants.BOOTSTRAP_LISTENER_PORT).run()
}

class Bootstrap(port: Int) extends Runnable {
    val serverSocket = new ServerSocket(port)

    def run(): Unit = {
        while (true) {
            // This blocks until a connection comes in
            val socket = serverSocket.accept()
            // Accept incoming thread in a new Thread
            new Thread(new BootstrapHandler(socket)).start()
        }
    }
}