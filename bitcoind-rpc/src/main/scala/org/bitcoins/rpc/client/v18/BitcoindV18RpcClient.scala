package org.bitcoins.rpc.client.v18
import akka.actor.ActorSystem
import org.bitcoins.rpc.client.common.{BitcoindRpcClient, BitcoindVersion}
import org.bitcoins.rpc.config.BitcoindInstance

import scala.util.Try

/**
  * Class for creating a BitcoindV18 instance that can access RPCs
  * @param instance
  * @param actorSystem
  */
class BitcoindV18RpcClient(override val instance: BitcoindInstance)(
    implicit
    actorSystem: ActorSystem)
    extends BitcoindRpcClient(instance)
    with V18PsbtRpc
    with V18DescriptorRpc
    with V18AssortedRpc {

  override lazy val version: BitcoindVersion = BitcoindVersion.V18

}

object BitcoindV18RpcClient {

  def fromUnknownVersion(rpcClient: BitcoindRpcClient)(
      implicit actorSystem: ActorSystem): Try[BitcoindV18RpcClient] =
    Try {
      new BitcoindV18RpcClient(rpcClient.instance)
    }

}
