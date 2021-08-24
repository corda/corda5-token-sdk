package com.r3.corda.lib.tokens.demo

import net.corda.test.dev.network.Node
import net.corda.test.dev.network.Nodes
import net.corda.test.dev.network.TestNetwork

fun Nodes<Node>.alice() = getNode("alice")
fun Nodes<Node>.bob() = getNode("bob")
fun Nodes<Node>.caroline() = getNode("caroline")
fun Nodes<Node>.denise() = getNode("denise")
fun Nodes<Node>.gic() = getNode("gic")

val diamondDemoNetwork = TestNetwork.forNetwork(System.getenv("TOKEN_DIAMOND_DEMO_NETWORK_NAME")
    ?: System.getProperty("tokenDiamondDemoNetwork", "token-diamond-network"))
