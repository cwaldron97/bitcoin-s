package org.bitcoins.rpc.v18
import org.bitcoins.chain.models.BlockHeaderDbHelper
import org.bitcoins.core.protocol.blockchain.RegTestNetChainParams
import org.bitcoins.rpc.client.common.BitcoindVersion
import org.bitcoins.rpc.client.v18.BitcoindV18RpcClient
import org.bitcoins.testkit.chain.BlockHeaderHelper
import org.bitcoins.testkit.rpc.BitcoindRpcTestUtil
import org.bitcoins.testkit.util.BitcoindRpcTest

import scala.concurrent.Future

class BitcoindV18RpcClientTest extends BitcoindRpcTest {
  lazy val clientF: Future[BitcoindV18RpcClient] = {
    val client = new BitcoindV18RpcClient(BitcoindRpcTestUtil.v18Instance())
    val clientIsStartedF = BitcoindRpcTestUtil.startServers(Vector(client))
    clientIsStartedF.map(_ => client)
  }

  clientF.foreach(c => clientAccum.+=(c))

  behavior of "BitcoindV18RpcClient"

  it should "be able to start a V18 bitcoind instance" in {

    clientF.map { client =>
      assert(client.version == BitcoindVersion.V18)
    }

  }

  it should "return active rpc commands" in {
    val generatedF = clientF.flatMap(client =>
      client.getNewAddress.flatMap(client.generateToAddress(50000, _)))
    val rpcinfoF =
      generatedF.flatMap(_ => clientF.flatMap(client => client.getRpcInfo()))

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

  it should "get node address given a null parameter" in {
    val nodeF = clientF.flatMap(client => client.getNodeAddresses())

    nodeF.map { result =>
      assert(result.head.address.isAbsolute)
      assert(result.head.services >= 0)
      assert(result.nonEmpty)
    }
  }

  it should "get node addresses given a count" in {
    val nodeF = clientF.flatMap(client => client.getNodeAddresses(3))

    nodeF.map({ result =>
      assert(result.head.address.isAbsolute)
      assert(result.head.services >= 0)
      assert(result.nonEmpty)
    })
  }

  it should "successfully submit a header" in {
    val genesisHeader = RegTestNetChainParams.genesisBlock.blockHeader
    val genesisHeaderDb =
      BlockHeaderDbHelper.fromBlockHeader(height = 1, genesisHeader)
    val nextHeader = BlockHeaderHelper.buildNextHeader(genesisHeaderDb)
    clientF.flatMap(client =>
      client.submitHeader(nextHeader.blockHeader).map(_ => succeed))
  }

}
