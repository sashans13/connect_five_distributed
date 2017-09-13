package node

import common.{Connection, Constants}

import scala.collection.mutable

class FingerTable(val node: Connection, size: Int = Constants.FINGER_TABLE_SIZE) extends Serializable {
    private val table = new mutable.HashMap[Int, Connection]

    // Initialize all to null
    for (i <- 0 until size) {
        table(i) = node
    }

    def getFinger(index: Int): Connection = {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("index not within range of finger table")
        }

        table(index)
    }

    // TODO: Should deep copy? It happens automatically when sending over TCP so not a worry for now, unless used locally
    def getTable: mutable.HashMap[Int, Connection] = this.table

    def getSuccessor: Connection = table(0)

    def setFinger(index: Int, successorConn: Connection): Unit = table(index) = successorConn

    override def toString: String = {
        val stringBuilder: StringBuilder = new mutable.StringBuilder()

        table.toList.sortBy(_._1).foreach {
            case (i, conn) =>
                stringBuilder
                    .append(i)
                    .append(" ")
                    .append(conn)
                    .append(" ")
                    .append((node.id + Math.pow(2, i).toInt) % Constants.CHORD_SIZE)
                    .append("\n")
        }

        stringBuilder.toString()
    }

}
