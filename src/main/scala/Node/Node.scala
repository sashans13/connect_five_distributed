package Node

import java.net.{InetAddress, ServerSocket}

import common.{Connection, Constants, Util}

object Node {
    def createNode(): Node = {
        // Find an available port
        socket = Util.tryBindSocket(Constants.PORT_RANGE_START, Constants.PORT_RANGE_WIDTH)

        new Node(
            serverSocket = socket,
            ipAddress = localhostIpAddress, //InetAddress.getLocalHost.getHostAddress,
            port = ???,
            id = Util.getIdFromPort(port),
            connection = new Connection(
                ipAddress = localhostIpAddress,
                port = ???
            )
        )
    }

}

class Node extends Runnable {
    val serverSocket: ServerSocket = Util.tryBindSocket(Constants.PORT_RANGE_START, Constants.PORT_RANGE_WIDTH).get

    val ipAddress: String = InetAddress.getLocalHost.getHostAddress
    val port: Int = serverSocket.getLocalPort

    val id: Int = Util.getIdFromPort(port)
    val connection: Connection = new Connection(ipAddress, port)


    override def run(): Unit = {

    }
}
