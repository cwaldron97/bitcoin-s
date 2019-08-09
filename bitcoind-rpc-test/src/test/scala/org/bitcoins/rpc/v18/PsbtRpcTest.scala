package org.bitcoins.rpc.v18
import org.bitcoins.core.currency.Bitcoins
import org.bitcoins.rpc.client.v18.BitcoindV18RpcClient
import org.bitcoins.testkit.rpc.BitcoindRpcTestUtil
import org.bitcoins.testkit.util.BitcoindRpcTest

import scala.concurrent.Future

class PsbtRpcTest extends BitcoindRpcTest {

  lazy val clientF: Future[BitcoindV18RpcClient] = {
    val client = new BitcoindV18RpcClient(BitcoindRpcTestUtil.v18Instance())
    val clientIsStartedF = BitcoindRpcTestUtil.startServers(Vector(client))
    clientIsStartedF.map(_ => client)
  }

  clientF.map(c => clientAccum.+=(c))

  behavior of "PsbtRpc"

  it should "return something" in {
    clientF.flatMap { client =>
      val resultF = client.analyzePsbt(
        "cHNidP8BAHUCAAAAASaBcTce3/KF6Tet7qSze3gADAVmy7OtZGQXE8pCFxv2AAAAAAD+////AtPf9QUAAAAAGXapFNDFmQPFusKGh2DpD9UhpGZap2UgiKwA4fUFAAAAABepFDVF5uM7gyxHBQ8k0+65PJwDlIvHh7MuEwAAAQD9pQEBAAAAAAECiaPHHqtNIOA3G7ukzGmPopXJRjr6Ljl/hTPMti+VZ+UBAAAAFxYAFL4Y0VKpsBIDna89p95PUzSe7LmF/////4b4qkOnHf8USIk6UwpyN+9rRgi7st0tAXHmOuxqSJC0AQAAABcWABT+Pp7xp0XpdNkCxDVZQ6vLNL1TU/////8CAMLrCwAAAAAZdqkUhc/xCX/Z4Ai7NK9wnGIZeziXikiIrHL++E4sAAAAF6kUM5cluiHv1irHU6m80GfWx6ajnQWHAkcwRAIgJxK+IuAnDzlPVoMR3HyppolwuAJf3TskAinwf4pfOiQCIAGLONfc0xTnNMkna9b7QPZzMlvEuqFEyADS8vAtsnZcASED0uFWdJQbrUqZY3LLh+GFbTZSYG2YVi/jnF6efkE/IQUCSDBFAiEA0SuFLYXc2WHS9fSrZgZU327tzHlMDDPOXMMJ/7X85Y0CIGczio4OFyXBl/saiK9Z9R5E5CVbIBZ8hoQDHAXR8lkqASECI7cr7vCWXRC+B3jv7NYfysb3mk6haTkzgHNEZPhPKrMAAAAAAAAA")
      resultF.map { result =>
        val inputs = result.inputs
        logger.info(inputs.toString)
        assert(inputs.nonEmpty)
      }
    }
  }
  it should "have things in it" in {

    val psbt =
      "cHNidP8BACoCAAAAAAFAQg8AAAAAABepFG6Rty1Vk+fUOR4v9E6R6YXDFkHwhwAAAAAAAA=="
    val analyzedF = clientF.flatMap(client => client.analyzePsbt(psbt))

    analyzedF.map { result =>
      assert(result.inputs.exists(_.next.nonEmpty))
      assert(result.inputs.exists(_.missing.head.pubkeys.head.nonEmpty))
      assert(result.inputs.exists(_.missing.head.signatures.head.nonEmpty))
      assert(result.inputs.exists(_.missing.head.redeemscript.nonEmpty))
      assert(result.inputs.exists(_.missing.head.witnessscript.nonEmpty))
      assert(result.inputs.exists(_.is_final))
      assert(result.inputs.exists(_.has_utxo))
      assert(result.estimated_feerate.nonEmpty)
      assert(result.estimated_vsize.nonEmpty)
      assert(result.fee.nonEmpty)
      assert(result.next.nonEmpty)
    }
  }
  it should "check the results of analyzepsbt " in {
    val psbt =
      "cHNidP8BACoCAAAAAAFAQg8AAAAAABepFG6Rty1Vk+fUOR4v9E6R6YXDFkHwhwAAAAAAAA=="
    val analyzedF = clientF.flatMap(client => client.analyzePsbt(psbt))
    val expectedfee = Bitcoins(0.00001)
    val expectedfeerate = 0.0000005
    val expectedestimatedvsize = 10
    val expectedhasutxo = true
    val expectedisfinal = true
    val expectedrole = "updater"
    analyzedF.map { result =>
      assert(result.fee.get == expectedfee)
      assert(result.next == expectedrole)
      assert(result.inputs.head.has_utxo == expectedhasutxo)

      assert(result.inputs.head.is_final == expectedisfinal)

      assert(result.estimated_vsize.get == expectedestimatedvsize)

      assert(result.estimated_feerate.get == expectedfeerate)

      assert(result.inputs.head.next.get == expectedrole)

    }
  }

  //Todo: figure out how to implement a test here
  it should "check to see if the utxoUpdate input has been updated" in {
    val psbt =
      "cHNidP8BACoCAAAAAAFAQg8AAAAAABepFG6Rty1Vk+fUOR4v9E6R6YXDFkHwhwAAAAAAAA=="
    val updatedF = clientF.flatMap(client => client.utxoUpdatePsbt(psbt))

    updatedF.map { result =>
      assert(result.contains(psbt))
    }
  }

  it should "joinpsbts " in {
    val seqofpsbts = Vector(
      "cHNidP8BACoCAAAAAAFAQg8AAAAAABepFG6Rty1Vk+fUOR4v9E6R6YXDFkHwhwAAAAAAAA==",
      "cHNidP8BAHUCAAAAASaBcTce3/KF6Tet7qSze3gADAVmy7OtZGQXE8pCFxv2AAAAAAD+////AtPf9QUAAAAAGXapFNDFmQPFusKGh2DpD9UhpGZap2UgiKwA4fUFAAAAABepFDVF5uM7gyxHBQ8k0+65PJwDlIvHh7MuEwAAAQD9pQEBAAAAAAECiaPHHqtNIOA3G7ukzGmPopXJRjr6Ljl/hTPMti+VZ+UBAAAAFxYAFL4Y0VKpsBIDna89p95PUzSe7LmF/////4b4qkOnHf8USIk6UwpyN+9rRgi7st0tAXHmOuxqSJC0AQAAABcWABT+Pp7xp0XpdNkCxDVZQ6vLNL1TU/////8CAMLrCwAAAAAZdqkUhc/xCX/Z4Ai7NK9wnGIZeziXikiIrHL++E4sAAAAF6kUM5cluiHv1irHU6m80GfWx6ajnQWHAkcwRAIgJxK+IuAnDzlPVoMR3HyppolwuAJf3TskAinwf4pfOiQCIAGLONfc0xTnNMkna9b7QPZzMlvEuqFEyADS8vAtsnZcASED0uFWdJQbrUqZY3LLh+GFbTZSYG2YVi/jnF6efkE/IQUCSDBFAiEA0SuFLYXc2WHS9fSrZgZU327tzHlMDDPOXMMJ/7X85Y0CIGczio4OFyXBl/saiK9Z9R5E5CVbIBZ8hoQDHAXR8lkqASECI7cr7vCWXRC+B3jv7NYfysb3mk6haTkzgHNEZPhPKrMAAAAAAAAA"
    )
    val psbt = seqofpsbts.head
    val joinedF = clientF.flatMap(client => client.joinPsbts(seqofpsbts))

    joinedF.map { result =>
      assert(result.contains(psbt))
    }
  }

}
