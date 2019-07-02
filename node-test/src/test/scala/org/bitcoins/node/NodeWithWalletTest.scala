package org.bitcoins.node

import akka.actor.Cancellable
import org.bitcoins.chain.blockchain.ChainHandler
import org.bitcoins.chain.config.ChainAppConfig
import org.bitcoins.chain.models.BlockHeaderDAO
import org.bitcoins.core.crypto.{DoubleSha256Digest, DoubleSha256DigestBE}
import org.bitcoins.core.currency._
import org.bitcoins.node.config.NodeAppConfig
import org.bitcoins.node.models.Peer
import org.bitcoins.node.networking.peer.DataMessageHandler
import org.bitcoins.testkit.node.NodeTestUtil
import org.bitcoins.testkit.wallet.BitcoinSWalletTest
import org.scalatest.FutureOutcome
import org.scalatest.compatible.Assertion
import org.scalatest.exceptions.TestFailedException

import scala.concurrent.Promise
import scala.concurrent.duration._

class NodeWithWalletTest extends BitcoinSWalletTest {

  override type FixtureParam = WalletWithBitcoind

  def withFixture(test: OneArgAsyncTest): FutureOutcome =
    withNewWalletAndBitcoind(test)

  it must "load a bloom filter and receive information about received payments" in {
    param =>
      val WalletWithBitcoind(wallet, rpc) = param

      /**
        * This is not ideal, how do we get one implicit value (`config`)
        * to resolve to multiple implicit parameters?
        */
      implicit val nodeConfig: NodeAppConfig = config
      implicit val chainConfig: ChainAppConfig = config

      var expectedTxId: Option[DoubleSha256Digest] = None
      var unexpectedTxId: Option[DoubleSha256Digest] = None
      var cancellable: Option[Cancellable] = None

      val completionP = Promise[Assertion]

      val callbacks = {
        val onBlock: DataMessageHandler.OnBlockReceived = { block =>
          completionP.failure(
            new TestFailedException(
              s"Received a block! We are only expecting merkle blocks",
              failedCodeStackDepth = 0))
        }

        val onTx: DataMessageHandler.OnTxReceived = { tx =>
          if (expectedTxId.contains(tx.txId)) {
            cancellable.map(_.cancel())
            completionP.success(succeed)
          } else if (unexpectedTxId.contains(tx.txId)) {
            completionP.failure(
              new TestFailedException(
                s"Got ${tx.txId}, which is a TX we expect to NOT get notified about!",
                failedCodeStackDepth = 0))
          } else {
            logger.info(s"Didn't match expected TX or unexpected TX")
          }
        }

        SpvNodeCallbacks(
          onBlockReceived = Seq(onBlock),
          onTxReceived = Seq(onTx)
        )
      }

      def processWalletTx(tx: DoubleSha256DigestBE) = {
        expectedTxId = Some(tx.flip)
        // how long we're waiting for a tx notify before failing the test
        val delay = 15.seconds
        val runnable: Runnable = new Runnable {
          override def run = {
            val msg =
              s"Did not receive sent transaction within $delay"
            logger.error(msg)
            completionP.failure(new TestFailedException(msg, 0))
          }
        }

        cancellable = Some(actorSystem.scheduler.scheduleOnce(delay, runnable))
        tx
      }

      for {
        _ <- config.initialize()

        address <- wallet.getNewAddress()
        bloom <- wallet.getBloomFilter()

        spv <- {
          val peer = Peer.fromBitcoind(rpc.instance)
          val chainHandler = {
            val bhDao = BlockHeaderDAO()
            ChainHandler(bhDao, config)
          }

          val spv =
            SpvNode(peer,
                    chainHandler,
                    bloomFilter = bloom,
                    callbacks = callbacks)
          spv.start()
        }
        _ <- spv.sync()
        _ <- NodeTestUtil.awaitSync(spv, rpc)

        ourTxid <- rpc.sendToAddress(address, 1.bitcoin).map(processWalletTx)

        notOurTxid <- rpc.getNewAddress
          .flatMap(rpc.sendToAddress(_, 1.bitcoin))
          .map { tx =>
            // we're generating a TX from bitcoind that should _not_ trigger
            // our bloom filter, and not get sent to us. if it gets sent to
            // us we fail the test
            unexpectedTxId = Some(tx.flip)
            tx
          }

        ourTx <- rpc.getTransaction(ourTxid)
        notOurTx <- rpc.getTransaction(notOurTxid)

        assertion <- {
          assert(bloom.isRelevant(ourTx.hex))
          assert(!bloom.isRelevant(notOurTx.hex))

          completionP.future
        }
      } yield assertion

  }
}
