import node.Node

object Playground {
    def main(args: Array[String]): Unit = {
        bootstrap.Bootstrap.startServer()

        for (i <- 0 until 10) {
            val node = new Node
            node.startNode()
        }

//        val node1 = new Node
//        val node2 = new Node
//        val node3 = new Node
//        val node4 = new Node
//
//        node1.startNode()
//        node2.startNode()
//        node3.startNode()
//        node4.startNode()
    }
}
