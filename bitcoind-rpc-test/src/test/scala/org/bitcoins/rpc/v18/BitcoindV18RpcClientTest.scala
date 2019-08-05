package org.bitcoins.rpc.v18
import org.bitcoins.rpc.client.common.BitcoindVersion
import org.bitcoins.rpc.client.v18.BitcoindV18RpcClient
import org.bitcoins.testkit.rpc.BitcoindRpcTestUtil
import org.bitcoins.testkit.util.BitcoindRpcTest

import scala.concurrent.Future

class BitcoindV18RpcClientTest extends BitcoindRpcTest {
  lazy val clientF: Future[BitcoindV18RpcClient] = {
    val client = new BitcoindV18RpcClient(BitcoindRpcTestUtil.v18Instance())
    val clientIsStartedF = BitcoindRpcTestUtil.startServers(Vector(client))
  }

  clientAccum.+=(clientF)

  behavior of "BitcoindV18RpcClient"

  it should "be able to start a V18 bitcoind instance" in {

    clientF.map { client =>
      assert(client.version == BitcoindVersion.V18)
    }

  }

  it should "return active rpc commands" in {
    clientF.map(client =>
      client.getNewAddress.flatMap(client.generateToAddress(50000, _)))
    val rpcinfoF = clientF.flatMap(client => client.getRpcInfo())

    rpcinfoF.map { result =>
      assert(result.active_commands.length == 2)
    }
  }

  it should "return a list of wallets" in {
    val listF = clientF.flatMap(client => client.listWalletDir())

    listF.map { result =>
      assert(result.wallets.nonEmpty)
      //asserting that the wallet name is correct based on the minimum and maximum characters allowed for a wallet
      assert(
        result.wallets.head.name.length >= 25 && result.wallets.head.name.length <= 34)
    }
  }

  it should "analyze a descriptor" in {

    val descriptor =
      "pk(0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798)"

    val descriptorF =
      clientF.flatMap(client => client.getDescriptorInfo(descriptor))

    descriptorF.map { result =>
      assert(result.hasprivatekey.equals(false))
      assert(result.isrange.equals(true))
      assert(result.issolvable.equals(true))
    }
  }

  it should "get node address" in {
    val nodeF = clientF.flatMap(client => client.getNodeAddresses())

    nodeF.map { result =>
      assert(result.head.address.isAbsolute)
      assert(result.head.services >= 0)
      assert(result.nonEmpty)
    }
  }

}
