package org.bitcoins.wallet.fixtures

import org.bitcoins.wallet.config.WalletDbManagement
import org.bitcoins.wallet.models.AccountDAO
import org.bitcoins.wallet.util.BitcoinSWalletTest
import org.scalatest._

import scala.concurrent.Future

trait AccountDAOFixture extends fixture.AsyncFlatSpec with BitcoinSWalletTest {
  override final type FixtureParam = AccountDAO

  override final def withFixture(test: OneArgAsyncTest): FutureOutcome =
    makeDependentFixture(createAccountTable, dropAccountTable)(test)

  private def dropAccountTable(accountDAO: AccountDAO): Future[Unit] = {
    WalletDbManagement.dropTable(accountDAO.table, dbConfig)
  }

  private def createAccountTable(): Future[AccountDAO] = {
    val dao = AccountDAO(dbConfig)
    WalletDbManagement.createTable(dao.table, dbConfig).map(_ => dao)
  }

}