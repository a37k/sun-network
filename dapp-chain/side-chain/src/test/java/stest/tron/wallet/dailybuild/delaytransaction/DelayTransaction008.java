package stest.tron.wallet.dailybuild.delaytransaction;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethedForDailybuild;

@Slf4j
public class DelayTransaction008 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethedForDailybuild.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethedForDailybuild.getFinalAddress(testKey003);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final String name = "Asset008_" + Long.toString(now);
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";
  Long delaySecond = 10L;
  private static String updateAccountName;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private Long delayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.delayTransactionFee");
  private Long cancleDelayTransactionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.cancleDelayTransactionFee");
  ByteString assetId;
  private byte[] contractAddress = null;
  SmartContract smartContract;

  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] doUpdateAccountAddress = ecKey.getAddress();
  String doUpdateAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] newAccountAddress = ecKey1.getAddress();
  String newAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = false, description = "Delay account update contract")
  public void test1DelayAccountUpdate() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    doUpdateAccountAddress = ecKey.getAddress();
    doUpdateAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethedForDailybuild.printAddress(doUpdateAccountKey);
    
    Assert.assertTrue(PublicMethedForDailybuild.sendcoin(doUpdateAccountAddress, 1000000L, fromAddress,
        testKey002, blockingStubFull));

    final Long beforeUpdateAccountBalance = PublicMethedForDailybuild.queryAccount(doUpdateAccountKey,
        blockingStubFull).getBalance();
    updateAccountName = "account_" + Long.toString(System.currentTimeMillis());
    byte[] accountNameBytes = ByteArray.fromString(updateAccountName);
    final String txid = PublicMethedForDailybuild.updateAccountDelayGetTxid(doUpdateAccountAddress,
        accountNameBytes,delaySecond,doUpdateAccountKey,blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    String accountName = new String(PublicMethedForDailybuild.queryAccount(doUpdateAccountKey,
        blockingStubFull).getAccountName().toByteArray(), Charset.forName("UTF-8"));
    Assert.assertTrue(accountName.isEmpty());
    Assert.assertTrue(PublicMethedForDailybuild.queryAccount(newAccountAddress,blockingStubFull)
        .getAccountName().isEmpty());
    Long balanceInDelay = PublicMethedForDailybuild.queryAccount(doUpdateAccountKey,blockingStubFull)
        .getBalance();
    Assert.assertTrue(beforeUpdateAccountBalance - balanceInDelay == delayTransactionFee);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    accountName = new String(PublicMethedForDailybuild.queryAccount(doUpdateAccountKey,blockingStubFull)
        .getAccountName().toByteArray(), Charset.forName("UTF-8"));
    logger.info(accountName);
    Assert.assertTrue(accountName.equalsIgnoreCase(updateAccountName));
    Long afterCreateAccountBalance = PublicMethedForDailybuild.queryAccount(doUpdateAccountKey,blockingStubFull)
        .getBalance();
    Long netFee = PublicMethedForDailybuild.getTransactionInfoById(txid,blockingStubFull).get().getReceipt()
        .getNetFee();
    Long fee = PublicMethedForDailybuild.getTransactionInfoById(txid,blockingStubFull).get().getFee();
    Assert.assertTrue(fee - netFee == delayTransactionFee);
    Assert.assertTrue(beforeUpdateAccountBalance - afterCreateAccountBalance
        == delayTransactionFee);

  }

  @Test(enabled = false, description = "Cancel delay account update contract")
  public void test2CancelDelayUpdateAccount() {
    //get account
    ecKey = new ECKey(Utils.getRandom());
    doUpdateAccountAddress = ecKey.getAddress();
    doUpdateAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    PublicMethedForDailybuild.printAddress(doUpdateAccountKey);

    final Long beforeUpdateAccountBalance = PublicMethedForDailybuild.queryAccount(doUpdateAccountKey,
        blockingStubFull).getBalance();
    updateAccountName = "account_" + Long.toString(System.currentTimeMillis());
    byte[] accountNameBytes = ByteArray.fromString(updateAccountName);
    final String txid = PublicMethedForDailybuild.updateAccountDelayGetTxid(doUpdateAccountAddress,
        accountNameBytes,delaySecond,doUpdateAccountKey,blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Assert.assertFalse(PublicMethedForDailybuild.cancelDeferredTransactionById(txid,fromAddress,testKey002,
        blockingStubFull));
    final String cancelTxid = PublicMethedForDailybuild.cancelDeferredTransactionByIdGetTxid(txid,
        doUpdateAccountAddress,doUpdateAccountKey,blockingStubFull);
    Assert.assertFalse(PublicMethedForDailybuild.cancelDeferredTransactionById(txid,doUpdateAccountAddress,
        doUpdateAccountKey,blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    final Long afterUpdateBalance = PublicMethedForDailybuild.queryAccount(doUpdateAccountKey,blockingStubFull)
        .getBalance();
    final Long netFee = PublicMethedForDailybuild.getTransactionInfoById(cancelTxid,blockingStubFull).get()
        .getReceipt().getNetFee();
    final Long fee = PublicMethedForDailybuild.getTransactionInfoById(cancelTxid,blockingStubFull).get()
        .getFee();
    logger.info("net fee : " + PublicMethedForDailybuild.getTransactionInfoById(cancelTxid,blockingStubFull)
        .get().getReceipt().getNetFee());
    logger.info("Fee : " + PublicMethedForDailybuild.getTransactionInfoById(cancelTxid,blockingStubFull)
        .get().getFee());

    Assert.assertTrue(fee - netFee == cancleDelayTransactionFee);
    Assert.assertTrue(beforeUpdateAccountBalance - afterUpdateBalance
        == cancleDelayTransactionFee + delayTransactionFee);

  }


  /**
   * constructor.
   */

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


