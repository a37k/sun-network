package stest.tron.wallet.dailybuild.originenergylimit;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethedForDailybuild;

@Slf4j
public class ContractOriginEnergyLimit004 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethedForDailybuild.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] dev001Address = ecKey1.getAddress();
  String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] user001Address = ecKey2.getAddress();
  String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  private long getAvailableFrozenEnergy(byte[] accountAddress) {
    AccountResourceMessage resourceInfo = PublicMethedForDailybuild.getAccountResource(accountAddress,
        blockingStubFull);
    long energyLimit = resourceInfo.getEnergyLimit();
    long energyUsed = resourceInfo.getEnergyUsed();
    return energyLimit - energyUsed;
  }

  private long getUserAvailableEnergy(byte[] userAddress) {
    AccountResourceMessage resourceInfo = PublicMethedForDailybuild.getAccountResource(userAddress,
        blockingStubFull);
    Account info = PublicMethedForDailybuild.queryAccount(userAddress, blockingStubFull);
    long balance = info.getBalance();
    long energyLimit = resourceInfo.getEnergyLimit();
    long userAvaliableFrozenEnergy = getAvailableFrozenEnergy(userAddress);
    return balance / 100 + userAvaliableFrozenEnergy;
  }

  private long getFeeLimit(String txid) {
    Optional<Transaction> trsById = PublicMethedForDailybuild.getTransactionById(txid, blockingStubFull);
    return trsById.get().getRawData().getFeeLimit();
  }

  private long getUserMax(byte[] userAddress, long feelimit) {
    logger.info("User feeLimit: " + feelimit / 100);
    logger.info("User UserAvaliableEnergy: " + getUserAvailableEnergy(userAddress));
    return Math.min(feelimit / 100, getUserAvailableEnergy(userAddress));
  }

  private long getOriginalEnergyLimit(byte[] contractAddress) {
    SmartContract smartContract = PublicMethedForDailybuild.getContract(contractAddress, blockingStubFull);
    return smartContract.getOriginEnergyLimit();
  }

  private long getConsumeUserResourcePercent(byte[] contractAddress) {
    SmartContract smartContract = PublicMethedForDailybuild.getContract(contractAddress, blockingStubFull);
    return smartContract.getConsumeUserResourcePercent();
  }

  private long getDevMax(byte[] devAddress, byte[] userAddress, long feeLimit,
      byte[] contractAddress) {
    long devMax = Math.min(getAvailableFrozenEnergy(devAddress),
        getOriginalEnergyLimit(contractAddress));
    long p = getConsumeUserResourcePercent(contractAddress);
    if (p != 0) {
      logger.info("p: " + p);
      devMax = Math.min(devMax, getUserMax(userAddress, feeLimit) * (100 - p) / p);
      logger.info("Dev byUserPercent: " + getUserMax(userAddress, feeLimit) * (100 - p) / p);
    }
    logger.info("Dev AvaliableFrozenEnergy: " + getAvailableFrozenEnergy(devAddress));
    logger.info("Dev OriginalEnergyLimit: " + getOriginalEnergyLimit(contractAddress));
    return devMax;
  }

  @Test(enabled = true, description = "Contract use Origin_energy_limit")
  public void testOriginEnergyLimit() {
    Assert.assertTrue(PublicMethedForDailybuild.sendcoin(dev001Address, 1000000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethedForDailybuild.sendcoin(user001Address, 1000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    // A2B1

    //dev balance and Energy
    long devTargetBalance = 10_000_000;
    long devTargetEnergy = 70000;

    // deploy contract parameters
    final long deployFeeLimit = maxFeeLimit;
    final long consumeUserResourcePercent = 0;
    final long originEnergyLimit = 1000;

    //dev balance and Energy
    final long devTriggerTargetBalance = 0;
    final long devTriggerTargetEnergy = 592;

    // user balance and Energy
    final long userTargetBalance = 0;
    final long userTargetEnergy = 2000L;

    // trigger contract parameter, maxFeeLimit 10000000
    final long triggerFeeLimit = maxFeeLimit;
    final boolean expectRet = true;

    // count dev energy, balance
    long devFreezeBalanceSun = PublicMethedForDailybuild.getFreezeBalanceCount(dev001Address, dev001Key,
        devTargetEnergy, blockingStubFull);

    long devNeedBalance = devTargetBalance + devFreezeBalanceSun;

    logger.info("need balance:" + devNeedBalance);

    // get balance
    Assert.assertTrue(PublicMethedForDailybuild.sendcoin(dev001Address, devNeedBalance, fromAddress,
        testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    // get energy
    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceGetEnergy(dev001Address, devFreezeBalanceSun,
        3, 1, dev001Key, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    AccountResourceMessage accountResource = PublicMethedForDailybuild.getAccountResource(dev001Address,
        blockingStubFull);
    long devEnergyLimitBefore = accountResource.getEnergyLimit();
    long devEnergyUsageBefore = accountResource.getEnergyUsed();
    long devBalanceBefore = PublicMethedForDailybuild.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("before deploy, dev energy limit is " + Long.toString(devEnergyLimitBefore));
    logger.info("before deploy, dev energy usage is " + Long.toString(devEnergyUsageBefore));
    logger.info("before deploy, dev balance is " + Long.toString(devBalanceBefore));

    String filePath = "src/test/resources/soliditycode/contractOriginEnergyLimit004.sol";
    String contractName = "findArgsContractTest";
    HashMap retMap = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    final String deployTxid = PublicMethedForDailybuild
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            deployFeeLimit, 0L, consumeUserResourcePercent, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethedForDailybuild.getAccountResource(dev001Address, blockingStubFull);
    long devEnergyLimitAfter = accountResource.getEnergyLimit();
    long devEnergyUsageAfter = accountResource.getEnergyUsed();
    long devBalanceAfter = PublicMethedForDailybuild.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("after deploy, dev energy limit is " + Long.toString(devEnergyLimitAfter));
    logger.info("after deploy, dev energy usage is " + Long.toString(devEnergyUsageAfter));
    logger.info("after deploy, dev balance is " + Long.toString(devBalanceAfter));

    Optional<TransactionInfo> infoById = PublicMethedForDailybuild
        .getTransactionInfoById(deployTxid, blockingStubFull);

    ByteString contractAddressString = infoById.get().getContractAddress();
    contractAddress = contractAddressString.toByteArray();
    SmartContract smartContract = PublicMethedForDailybuild.getContract(contractAddress, blockingStubFull);

    Assert.assertTrue(smartContract.getAbi() != null);

    Assert.assertTrue(devEnergyLimitAfter > 0);
    Assert.assertTrue(devEnergyUsageAfter > 0);
    Assert.assertEquals(devBalanceBefore, devBalanceAfter);

    // count dev energy, balance
    devFreezeBalanceSun = PublicMethedForDailybuild.getFreezeBalanceCount(dev001Address, dev001Key,
        devTriggerTargetEnergy, blockingStubFull);

    devNeedBalance = devTriggerTargetBalance + devFreezeBalanceSun;
    logger.info("dev need  balance:" + devNeedBalance);

    // count user energy, balance
    long userFreezeBalanceSun = PublicMethedForDailybuild.getFreezeBalanceCount(user001Address, user001Key,
        userTargetEnergy, blockingStubFull);

    long userNeedBalance = userTargetBalance + userFreezeBalanceSun;

    logger.info("User need  balance:" + userNeedBalance);

    // get balance
    Assert.assertTrue(PublicMethedForDailybuild.sendcoin(dev001Address, devNeedBalance, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethedForDailybuild.sendcoin(user001Address, userNeedBalance, fromAddress,
        testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    // get energy
    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceGetEnergy(dev001Address, devFreezeBalanceSun,
        3, 1, dev001Key, blockingStubFull));
    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceGetEnergy(user001Address, userFreezeBalanceSun,
        3, 1, user001Key, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethedForDailybuild.getAccountResource(dev001Address, blockingStubFull);
    devEnergyLimitBefore = accountResource.getEnergyLimit();
    devEnergyUsageBefore = accountResource.getEnergyUsed();
    devBalanceBefore = PublicMethedForDailybuild.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("before trigger, dev devEnergyLimitBefore is "
        + Long.toString(devEnergyLimitBefore));
    logger.info("before trigger, dev devEnergyUsageBefore is "
        + Long.toString(devEnergyUsageBefore));
    logger.info("before trigger, dev devBalanceBefore is " + Long.toString(devBalanceBefore));

    accountResource = PublicMethedForDailybuild.getAccountResource(user001Address, blockingStubFull);
    long userEnergyLimitBefore = accountResource.getEnergyLimit();
    long userEnergyUsageBefore = accountResource.getEnergyUsed();
    long userBalanceBefore = PublicMethedForDailybuild.queryAccount(
        user001Address, blockingStubFull).getBalance();

    logger.info("before trigger, user userEnergyLimitBefore is "
        + Long.toString(userEnergyLimitBefore));
    logger.info("before trigger, user userEnergyUsageBefore is "
        + Long.toString(userEnergyUsageBefore));
    logger.info("before trigger, user userBalanceBefore is " + Long.toString(userBalanceBefore));

    logger.info("==================================");
    long userMax = getUserMax(user001Address, triggerFeeLimit);
    long devMax = getDevMax(dev001Address, user001Address, triggerFeeLimit, contractAddress);

    logger.info("userMax: " + userMax);
    logger.info("devMax: " + devMax);
    logger.info("==================================");

    String param = "\"" + 0 + "\"";
    final String triggerTxid = PublicMethedForDailybuild
        .triggerContract(contractAddress, "findArgsByIndexTest(uint256)",
            param, false, 0, triggerFeeLimit,
            user001Address, user001Key, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethedForDailybuild.getAccountResource(dev001Address, blockingStubFull);
    devEnergyLimitAfter = accountResource.getEnergyLimit();
    devEnergyUsageAfter = accountResource.getEnergyUsed();
    devBalanceAfter = PublicMethedForDailybuild.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("after trigger, dev devEnergyLimitAfter is " + Long.toString(devEnergyLimitAfter));
    logger.info("after trigger, dev devEnergyUsageAfter is " + Long.toString(devEnergyUsageAfter));
    logger.info("after trigger, dev devBalanceAfter is " + Long.toString(devBalanceAfter));

    accountResource = PublicMethedForDailybuild.getAccountResource(user001Address, blockingStubFull);
    long userEnergyLimitAfter = accountResource.getEnergyLimit();
    long userEnergyUsageAfter = accountResource.getEnergyUsed();
    long userBalanceAfter = PublicMethedForDailybuild.queryAccount(user001Address,
        blockingStubFull).getBalance();

    logger.info("after trigger, user userEnergyLimitAfter is "
        + Long.toString(userEnergyLimitAfter));
    logger.info("after trigger, user userEnergyUsageAfter is "
        + Long.toString(userEnergyUsageAfter));
    logger.info("after trigger, user userBalanceAfter is " + Long.toString(userBalanceAfter));

    infoById = PublicMethedForDailybuild.getTransactionInfoById(triggerTxid, blockingStubFull);
    boolean isSuccess = true;
    if (triggerTxid == null || infoById.get().getResultValue() != 0) {
      logger.info("transaction failed with message: " + infoById.get().getResMessage());
      isSuccess = false;
    }

    long fee = infoById.get().getFee();
    long energyFee = infoById.get().getReceipt().getEnergyFee();
    long energyUsage = infoById.get().getReceipt().getEnergyUsage();
    long originEnergyUsage = infoById.get().getReceipt().getOriginEnergyUsage();
    long energyTotalUsage = infoById.get().getReceipt().getEnergyUsageTotal();
    long netUsage = infoById.get().getReceipt().getNetUsage();
    long netFee = infoById.get().getReceipt().getNetFee();

    logger.info("fee: " + fee);
    logger.info("energyFee: " + energyFee);
    logger.info("energyUsage: " + energyUsage);
    logger.info("originEnergyUsage: " + originEnergyUsage);
    logger.info("energyTotalUsage: " + energyTotalUsage);
    logger.info("netUsage: " + netUsage);
    logger.info("netFee: " + netFee);

    smartContract = PublicMethedForDailybuild.getContract(contractAddress, blockingStubFull);
    long consumeUserPercent = smartContract.getConsumeUserResourcePercent();
    logger.info("ConsumeURPercent: " + consumeUserPercent);

    long devExpectCost = energyTotalUsage * (100 - consumeUserPercent) / 100;
    long userExpectCost = energyTotalUsage - devExpectCost;
    final long totalCost = devExpectCost + userExpectCost;

    logger.info("devExpectCost: " + devExpectCost);
    logger.info("userExpectCost: " + userExpectCost);

    Assert.assertTrue(devEnergyLimitAfter > 0);
    Assert.assertEquals(devBalanceBefore, devBalanceAfter);

    // dev original is the dev max expense A2B1
    Assert.assertEquals(getOriginalEnergyLimit(contractAddress), devMax);

    // DEV is enough to pay
    Assert.assertEquals(originEnergyUsage, devExpectCost);
    //    Assert.assertEquals(devEnergyUsageAfter,devExpectCost + devEnergyUsageBefore);
    // User Energy is enough to pay");
    Assert.assertEquals(energyUsage, userExpectCost);
    Assert.assertEquals(userBalanceBefore, userBalanceAfter);
    Assert.assertEquals(userEnergyUsageAfter, userEnergyUsageBefore);
    Assert.assertEquals(userBalanceBefore, userBalanceAfter);
    Assert.assertEquals(totalCost, energyTotalUsage);

    if (expectRet) {
      Assert.assertTrue(isSuccess);
    } else {
      Assert.assertFalse(isSuccess);
    }
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


