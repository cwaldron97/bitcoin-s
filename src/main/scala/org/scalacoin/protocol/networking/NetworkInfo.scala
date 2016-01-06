package org.scalacoin.protocol.networking

/**
 * Created by Tom on 1/5/2016.
 */
trait NetworkInfo {
  def version : Int
  def subVersion : String
  def protocolVersion: Int
  def localServices : String
  def timeOffSet : Int
  def connections : Int
  def networks : Seq[String]
  def relayFee : Double
  def localAddresses : Seq[String]
}

//TODO: Change localaddresses from array[string] to array of connections. see https://bitcoin.org/en/developer-reference#getnetworkinfo

case class NetworkInfoImpl(version : Int, subVersion : String, protocolVersion : Int, localServices : String,
  timeOffSet : Int, connections : Int, networks : Seq[String], relayFee : Double, localAddresses: Seq[String]) extends NetworkInfo