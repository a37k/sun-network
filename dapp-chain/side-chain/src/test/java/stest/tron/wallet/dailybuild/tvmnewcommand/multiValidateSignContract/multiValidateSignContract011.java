package stest.tron.wallet.dailybuild.tvmnewcommand.multiValidateSignContract;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.PublicMethedForDailybuild;

@Slf4j
public class multiValidateSignContract011 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethedForDailybuild
      .getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  String txid = "";

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethedForDailybuild.printAddress(contractExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
    txid = PublicMethedForDailybuild
        .sendcoinGetTransactionId(contractExcAddress, 1000000000L, testNetAccountAddress,
            testNetAccountKey,
            blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/multivalidatesign001.sol";
    String contractName = "Demo";
    HashMap retMap = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethedForDailybuild
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Correct 33 signatures test multivalidatesign")
  public void test01Correct33Signatures() {
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethedForDailybuild
        .getAccountResource(contractExcAddress, blockingStubFull);
    Protocol.Account info = PublicMethedForDailybuild
        .queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(txid.getBytes());
    for (int i = 0; i < 33; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(Wallet.encode58Check(key.getAddress()));
    }
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String input = PublicMethedForDailybuild.parametersString(parameters);
    txid = PublicMethedForDailybuild.triggerContract(contractAddress,
        "testArray(bytes32,bytes[],address[])", input, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);
    Protocol.Account infoafter = PublicMethedForDailybuild
        .queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethedForDailybuild
        .getAccountResource(contractExcAddress,
            blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    if (infoById.get().getResultValue() == 0) {
      Assert.assertEquals("11111111111111111111111111111111", PublicMethedForDailybuild
          .bytes32ToString(infoById.get().getContractResult(0).toByteArray()));
      Assert.assertTrue(afterBalance + fee == beforeBalance);
    } else {
      Assert.assertTrue("CPU timeout for 'PUSH1' operation executing"
          .equals(infoById.get().getResMessage().toStringUtf8())
          || "Already Time Out".equals(infoById.get().getResMessage().toStringUtf8()));
      Assert.assertTrue(afterBalance == 0);
      txid = PublicMethedForDailybuild
          .sendcoinGetTransactionId(contractExcAddress, 1000000000L, testNetAccountAddress,
              testNetAccountKey,
              blockingStubFull);
      PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    }
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

  @Test(enabled = true, description = "33 signatures with 1st incorrect signatures test multivalidatesign")
  public void test02Incorrect1stSignatures() {
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethedForDailybuild
        .getAccountResource(contractExcAddress, blockingStubFull);
    Protocol.Account info = PublicMethedForDailybuild
        .queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(txid.getBytes());
    for (int i = 0; i < 33; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(Wallet.encode58Check(key.getAddress()));
    }
    byte[] sign = new ECKey().sign(Hash.sha3("sdifhsdfihyw888w7".getBytes())).toByteArray();
    signatures.set(0, Hex.toHexString(sign));
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String input = PublicMethedForDailybuild.parametersString(parameters);
    txid = PublicMethedForDailybuild.triggerContract(contractAddress,
        "testArray(bytes32,bytes[],address[])", input, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals("01111111111111111111111111111111", PublicMethedForDailybuild
        .bytes32ToString(infoById.get().getContractResult(0).toByteArray()));
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);
    Protocol.Account infoafter = PublicMethedForDailybuild
        .queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethedForDailybuild
        .getAccountResource(contractExcAddress,
            blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

  @Test(enabled = true, description = "52 signatures with 1st incorrect address test multivalidatesign")
  public void test03Incorrect1stAddress() {
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethedForDailybuild
        .getAccountResource(contractExcAddress, blockingStubFull);
    Protocol.Account info = PublicMethedForDailybuild
        .queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(txid.getBytes());
    for (int i = 0; i < 52; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(Wallet.encode58Check(key.getAddress()));
    }
    addresses.set(0, Wallet.encode58Check(new ECKey().getAddress()));
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String input = PublicMethedForDailybuild.parametersString(parameters);
    txid = PublicMethedForDailybuild.triggerContract(contractAddress,
        "testArray(bytes32,bytes[],address[])", input, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals("01111111111111111111111111111111", PublicMethedForDailybuild
        .bytes32ToString(infoById.get().getContractResult(0).toByteArray()));
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);
    Protocol.Account infoafter = PublicMethedForDailybuild
        .queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethedForDailybuild
        .getAccountResource(contractExcAddress,
            blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

  @Test(enabled = true, description = "91 signatures with 15th incorrect signatures test multivalidatesign")
  public void test04Incorrect15thSignatures() {
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethedForDailybuild
        .getAccountResource(contractExcAddress, blockingStubFull);
    Protocol.Account info = PublicMethedForDailybuild
        .queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(txid.getBytes());
    for (int i = 0; i < 91; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(Wallet.encode58Check(key.getAddress()));
    }
    byte[] sign = new ECKey().sign(Hash.sha3("sdifhsdfihyw888w7".getBytes())).toByteArray();
    signatures.set(14, Hex.toHexString(sign));
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String input = PublicMethedForDailybuild.parametersString(parameters);
    txid = PublicMethedForDailybuild.triggerContract(contractAddress,
        "testArray(bytes32,bytes[],address[])", input, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals("11111111111111011111111111111111", PublicMethedForDailybuild
        .bytes32ToString(infoById.get().getContractResult(0).toByteArray()));
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);
    Protocol.Account infoafter = PublicMethedForDailybuild
        .queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethedForDailybuild
        .getAccountResource(contractExcAddress,
            blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

  @Test(enabled = true, description = "66 signatures with 15th-30th incorrect address test multivalidatesign")
  public void test05Incorrect15thTo30thAddress() {
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethedForDailybuild
        .getAccountResource(contractExcAddress, blockingStubFull);
    Protocol.Account info = PublicMethedForDailybuild
        .queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(txid.getBytes());
    for (int i = 0; i < 66; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(Wallet.encode58Check(key.getAddress()));
    }
    for (int i = 14; i < 30; i++) {
      addresses.set(i, Wallet.encode58Check(new ECKey().getAddress()));
    }
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String input = PublicMethedForDailybuild.parametersString(parameters);
    txid = PublicMethedForDailybuild.triggerContract(contractAddress,
        "testArray(bytes32,bytes[],address[])", input, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals("11111111111111000000000000000011", PublicMethedForDailybuild
        .bytes32ToString(infoById.get().getContractResult(0).toByteArray()));
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);
    Protocol.Account infoafter = PublicMethedForDailybuild
        .queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethedForDailybuild
        .getAccountResource(contractExcAddress,
            blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

  @Test(enabled = true, description = "150 signatures with 2nd、32nd incorrect signatures test multivalidatesign")
  public void test06Incorrect2ndAnd32ndIncorrectSignatures() {
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethedForDailybuild
        .getAccountResource(contractExcAddress, blockingStubFull);
    Protocol.Account info = PublicMethedForDailybuild
        .queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(txid.getBytes());
    for (int i = 0; i < 150; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      if (i == 1 || i == 31) {
        signatures.add(
            Hex.toHexString(key.sign("dgjjsldgjljvjjfdshkh1hgsk0807779".getBytes()).toByteArray()));
      } else {
        signatures.add(Hex.toHexString(sign));
      }
      addresses.add(Wallet.encode58Check(key.getAddress()));
    }
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String input = PublicMethedForDailybuild.parametersString(parameters);
    txid = PublicMethedForDailybuild.triggerContract(contractAddress,
        "testArray(bytes32,bytes[],address[])", input, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);

    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);
    Protocol.Account infoafter = PublicMethedForDailybuild
        .queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethedForDailybuild
        .getAccountResource(contractExcAddress,
            blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    if (infoById.get().getResultValue() == 0) {
      Assert.assertEquals("10111111111111111111111111111110", PublicMethedForDailybuild
          .bytes32ToString(infoById.get().getContractResult(0).toByteArray()));
      Assert.assertTrue(afterBalance + fee == beforeBalance);
    } else {
      Assert.assertTrue("CPU timeout for 'PUSH1' operation executing"
          .equals(infoById.get().getResMessage().toStringUtf8())
          || "Already Time Out".equals(infoById.get().getResMessage().toStringUtf8()));
      Assert.assertEquals(afterBalance.longValue() , 0);
      txid = PublicMethedForDailybuild
          .sendcoinGetTransactionId(contractExcAddress, 1000000000L, testNetAccountAddress,
              testNetAccountKey,
              blockingStubFull);
      PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    }
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

  @Test(enabled = true, description = "88 signatures with 9th、11th、28th、32nd incorrect address test multivalidatesign")
  public void test07IncorrectAddress() {
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethedForDailybuild
        .getAccountResource(contractExcAddress, blockingStubFull);
    Protocol.Account info = PublicMethedForDailybuild
        .queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(txid.getBytes());
    for (int i = 0; i < 88; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(Wallet.encode58Check(key.getAddress()));
    }
    addresses.set(8, Wallet.encode58Check(new ECKey().getAddress()));
    addresses.set(10, Wallet.encode58Check(new ECKey().getAddress()));
    addresses.set(27, Wallet.encode58Check(new ECKey().getAddress()));
    addresses.set(31, Wallet.encode58Check(new ECKey().getAddress()));
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String input = PublicMethedForDailybuild.parametersString(parameters);
    txid = PublicMethedForDailybuild.triggerContract(contractAddress,
        "testArray(bytes32,bytes[],address[])", input, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);
    Protocol.Account infoafter = PublicMethedForDailybuild
        .queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethedForDailybuild
        .getAccountResource(contractExcAddress,
            blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    if (infoById.get().getResultValue() == 0) {
      Assert.assertEquals("11111111010111111111111111101110", PublicMethedForDailybuild
          .bytes32ToString(infoById.get().getContractResult(0).toByteArray()));
      Assert.assertTrue(afterBalance + fee == beforeBalance);
    } else {
      Assert.assertTrue("CPU timeout for 'PUSH1' operation executing"
          .equals(infoById.get().getResMessage().toStringUtf8())
          || "Already Time Out".equals(infoById.get().getResMessage().toStringUtf8()));
      Assert.assertTrue(afterBalance == 0);
      txid = PublicMethedForDailybuild
          .sendcoinGetTransactionId(contractExcAddress, 1000000000L, testNetAccountAddress,
              testNetAccountKey,
              blockingStubFull);
      PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    }
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

  @Test(enabled = true, description = "105 signatures with Incorrect hash test multivalidatesign")
  public void test08IncorrectHash() {
    String incorrecttxid = PublicMethedForDailybuild
        .sendcoinGetTransactionId(contractExcAddress, 1000000000L, testNetAccountAddress,
            testNetAccountKey,
            blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethedForDailybuild
        .getAccountResource(contractExcAddress, blockingStubFull);
    Protocol.Account info = PublicMethedForDailybuild
        .queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(txid.getBytes());
    for (int i = 0; i < 105; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(Wallet.encode58Check(key.getAddress()));
    }
    List<Object> parameters = Arrays
        .asList("0x" + Hex.toHexString(Hash.sha3(incorrecttxid.getBytes())), signatures, addresses);
    String input = PublicMethedForDailybuild.parametersString(parameters);
    txid = PublicMethedForDailybuild.triggerContract(contractAddress,
        "testArray(bytes32,bytes[],address[])", input, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);
    Protocol.Account infoafter = PublicMethedForDailybuild
        .queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethedForDailybuild
        .getAccountResource(contractExcAddress,
            blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    if (infoById.get().getResultValue() == 0) {
      Assert.assertEquals("00000000000000000000000000000000", PublicMethedForDailybuild
          .bytes32ToString(infoById.get().getContractResult(0).toByteArray()));
      Assert.assertTrue(afterBalance + fee == beforeBalance);
    } else {
      Assert.assertTrue("CPU timeout for 'PUSH1' operation executing"
          .equals(infoById.get().getResMessage().toStringUtf8())
          || "Already Time Out".equals(infoById.get().getResMessage().toStringUtf8()));
      Assert.assertTrue(afterBalance == 0);
      txid = PublicMethedForDailybuild
          .sendcoinGetTransactionId(contractExcAddress, 1000000000L, testNetAccountAddress,
              testNetAccountKey,
              blockingStubFull);
      PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    }
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    long balance = PublicMethedForDailybuild.queryAccount(contractExcKey, blockingStubFull)
        .getBalance();
    PublicMethedForDailybuild
        .sendcoin(testNetAccountAddress, balance, contractExcAddress, contractExcKey,
            blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
