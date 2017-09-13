package common

class Connection(val ipAddress: String, val port: Int) extends Serializable {
    val id: Int = Util.getIdFromPort(port)

    override def toString = id + "@" + ipAddress + ":" + port
}

