---
id: version-0.1.0-rpc-bitcoind
title: bitcoind/Bitcoin Core
original_id: rpc-bitcoind
---

> Note: `bitcoin-s-bitcoind-rpc` requires you to have `bitcoind` (Bitcoin Core daemon) installed. Grab this at [bitcoincore.org](https://bitcoincore.org/en/download/)

The Bitcoin Core RPC client in Bitcoin-S currently supports the Bitcoin Core 0.16 and 0.17
version lines. It can be set up to work with both local and remote Bitcoin Core servers.

## Connecting to a local `bitcoind` instance

### Getting started quickly, with default options:

```scala
import scala.concurrent._
import akka.actor.ActorSystem

import org.bitcoins.{rpc, core}
import core.currency.Bitcoins
import rpc.client.common._

implicit val system = ActorSystem.create()
implicit val ec: ExecutionContext = system.dispatcher

// this reads authentication credentials and
// connection details from the default data
// directory on your platform
val client = BitcoindRpcClient.fromDatadir()

val balance: Future[Bitcoins] = for {
  _ <- client.start()
  balance <- client.getBalance
} yield balance
```

## Connecting to a remote `bitcoind`

First, we create a secure connection to our `bitcoind` instance by setting
up a SSH tunnel:

```bash
$ ssh -L 8332:localhost:8332  \
         my-cool-user@my-cool-website.com
```

> Note: the port number '8332' is the default for mainnet. If you want to
> connect to a testnet `bitcoind`, the default port is '18332'

Now that we have a secure connection between our remote `bitcoind`, we're
ready to create the connection with our RPC client

```scala
import akka.actor.ActorSystem
import java.net.URI
import scala.concurrent._

import org.bitcoins.core.config._
import org.bitcoins.rpc.config._
import org.bitcoins.rpc.client.common._

val username = "FILL_ME_IN" //this username comes from 'rpcuser' in your bitcoin.conf file //this username comes from 'rpcuser' in your bitcoin.conf file
val password = "FILL_ME_IN" //this password comes from your 'rpcpassword' in your bitcoin.conf file //this password comes from your 'rpcpassword' in your bitcoin.conf file

val authCredentials = BitcoindAuthCredentials.PasswordBased(
  username = username,
  password = password
)

val bitcoindInstance = {
  BitcoindInstance (
    network = MainNet,
    uri = new URI(s"http://localhost:${MainNet.port}"),
    rpcUri = new URI(s"http://localhost:${MainNet.rpcPort}"),
    authCredentials = authCredentials
  )
}

implicit val system: ActorSystem = ActorSystem.create()
implicit val ec: ExecutionContext = system.dispatcher

val rpcCli = new BitcoindRpcClient(bitcoindInstance)

rpcCli.getBalance.onComplete { case balance =>
  println(s"Wallet balance=${balance}")
  system.terminate()
}
```

## Testing

To test the Bitcoin-S RPC project you need both version 0.16 and 0.17 of Bitcoin Core. A list of current and previous releases can be found [here](https://bitcoincore.org/en/releases/).

You then need to set environment variables to indicate where Bitcoin-S can find the different versions:

```bash
$ export BITCOIND_V16_PATH=/path/to/v16/bitcoind
$ export BITCOIND_V17_PATH=/path/to/v17/bitcoind
```

If you just run tests testing common functionality it's enough to have either version 0.16 or 0.17 on your `PATH`.

To run all RPC related tests:

```bash
$ bash sbt bitcoindRpcTest/test
```

