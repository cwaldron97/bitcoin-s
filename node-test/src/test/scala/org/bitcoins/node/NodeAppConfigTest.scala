package org.bitcoins.node

import com.typesafe.config.ConfigFactory
import org.bitcoins.core.config.{MainNet, RegTest, TestNet3}
import org.bitcoins.node.config.NodeAppConfig
import org.bitcoins.testkit.util.BitcoinSUnitTest

class NodeAppConfigTest extends BitcoinSUnitTest {
  val config = NodeAppConfig()

  it must "be overridable" in {
    assert(config.network == RegTest)

    val otherConf = ConfigFactory.parseString("bitcoin-s.network = testnet3")
    val withOther: NodeAppConfig = config.withOverrides(otherConf)
    assert(withOther.network == TestNet3)

    val mainnetConf = ConfigFactory.parseString("bitcoin-s.network = mainnet")
    val mainnet: NodeAppConfig = withOther.withOverrides(mainnetConf)
    assert(mainnet.network == MainNet)
  }

  it must "be overridable with multiple levels" in {
    val testnet = ConfigFactory.parseString("bitcoin-s.network = testnet3")
    val mainnet = ConfigFactory.parseString("bitcoin-s.network = mainnet")
    val overriden: NodeAppConfig = config.withOverrides(testnet, mainnet)
    assert(overriden.network == MainNet)

  }
}
