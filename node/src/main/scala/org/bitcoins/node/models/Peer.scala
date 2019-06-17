package org.bitcoins.node.models

import java.net.InetSocketAddress

import org.bitcoins.core.p2p.NetworkIpAddress
import org.bitcoins.db.DbRowAutoInc
import org.bitcoins.rpc.config.BitcoindInstance

case class Peer(networkIpAddress: NetworkIpAddress, id: Option[Long] = None)
    extends DbRowAutoInc[Peer] {

  def socket: InetSocketAddress =
    new InetSocketAddress(networkIpAddress.address, networkIpAddress.port)

  override def copyWithId(id: Long): Peer = {
    this.copy(id = Some(id))
  }

  override def toString(): String =
    s"Peer(${networkIpAddress.address}:${networkIpAddress.port})"

}

object Peer {

  def fromNetworkIpAddress(networkIpAddress: NetworkIpAddress): Peer = {
    Peer(networkIpAddress)
  }

  def fromSocket(socket: InetSocketAddress): Peer = {
    val nip = NetworkIpAddress.fromInetSocketAddress(socket = socket)
    fromNetworkIpAddress(nip)
  }

  /**
    * Constructs a peer from the given `bitcoind` instance
    */
  def fromBitcoind(bitcoind: BitcoindInstance): Peer = {
    val socket = new InetSocketAddress(bitcoind.uri.getHost, bitcoind.p2pPort)
    fromSocket(socket)
  }
}