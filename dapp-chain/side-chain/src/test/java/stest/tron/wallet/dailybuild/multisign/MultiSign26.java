package stest.tron.wallet.dailybuild.multisign;

import static org.hamcrest.core.StringContains.containsString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethedForDailybuild;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class MultiSign26 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethedForDailybuild.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] toAddress = PublicMethedForDailybuild.getFinalAddress(testKey003);

  private final String testWitnesses = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessesKey = PublicMethedForDailybuild.getFinalAddress(testWitnesses);
  private ManagedChannel channelFull = null;
  private ManagedChannel searchChannelFull = null;

  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidityInFullnode = null;

  private WalletGrpc.WalletBlockingStub searchBlockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);


  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] test002Address = ecKey2.getAddress();
  private String sendAccountKey2 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  private ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] test003Address = ecKey3.getAddress();
  String sendAccountKey3 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  private ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] test004Address = ecKey4.getAddress();
  String sendAccountKey4 = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  private ECKey ecKey5 = new ECKey(Utils.getRandom());
  byte[] test005Address = ecKey5.getAddress();
  String sendAccountKey5 = ByteArray.toHexString(ecKey5.getPrivKeyBytes());
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);


  }


  @Test(enabled = true, description = "Sendcoin,use acticve address to sign,"
      + "not meet the requirements.Delete the address,broadcastTransaction.")
  public void testMultiUpdatepermissions_BeforeSign() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    long amount = 2 * updateAccountPermissionFee + 1;

    Assert.assertTrue(PublicMethedForDailybuild
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    final long balance = test001AddressAccount.getBalance();

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethedForDailybuild.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":1,\"operations\""
            + ":\"0100000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey2)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";

    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1, test001Address, 2, dev001Key,
            blockingStubFull);
    Transaction transaction1 = PublicMethedForDailybuild
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("Permission denied"));
    logger.info("transactionSignWeight:" + transactionSignWeight);
    Account test001AddressAccount2 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1, balance2);
    String accountPermissionJson2 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethedForDailybuild.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":1,\"operations\""
            + ":\"0100000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey4)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson2, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));

    Account test001AddressAccount3 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList3 = test001AddressAccount3.getActivePermissionList();
    Permission ownerPermission3 = test001AddressAccount3.getOwnerPermission();
    Permission witnessPermission3 = test001AddressAccount3.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList3);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission3));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission3));

    final Return returnResult = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance2 - balance3, updateAccountPermissionFee);
    logger.info("returnResult:");
    Assert
        .assertThat(returnResult.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult.getMessage().toStringUtf8(),
            containsString("Permission denied"));


  }

  @Test(enabled = true, description = "Sendcoin,use acticve address to sign,"
      + "meet the all requirements.Delete the address,broadcastTransaction.")
  public void testMultiUpdatepermissions_BeforeSign_1() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    long amount = 2 * updateAccountPermissionFee + 1;

    Assert.assertTrue(PublicMethedForDailybuild
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    final long balance = test001AddressAccount.getBalance();

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethedForDailybuild.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":1,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey2)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";

    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1, test001Address, 2, dev001Key,
            blockingStubFull);
    final Transaction transaction1 = PublicMethedForDailybuild
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);

    String accountPermissionJson2 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethedForDailybuild.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":1,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey4)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson2, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("but it is not contained of permission"));
    final Return returnResult = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Account test001AddressAccount2 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList2 = test001AddressAccount2.getActivePermissionList();
    Permission ownerPermission2 = test001AddressAccount2.getOwnerPermission();
    Permission witnessPermission2 = test001AddressAccount2.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList2);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission2));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission2));
    Assert
        .assertThat(returnResult.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult.getMessage().toStringUtf8(),
            containsString("but it is not contained of permission"));

    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1 - balance2, updateAccountPermissionFee);
  }


  @Test(enabled = true, description = "Sendcoin,use owner address to sign,"
      + "Delete the address,broadcastTransaction.")
  public void testMultiUpdatepermissions_BeforeSign_2() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    long amount = 2 * updateAccountPermissionFee + 1;

    Assert.assertTrue(PublicMethedForDailybuild
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    final long balance = test001AddressAccount.getBalance();

    String[] permissionKeyString = new String[2];
    permissionKeyString[0] = dev001Key;
    permissionKeyString[1] = sendAccountKey2;

    String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name"
            + "\":\"owner\",\"threshold\":1,\"keys\":[{"
            + "\"address\":\"" + PublicMethedForDailybuild.getAddressString(dev001Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey2)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
            + "\"threshold\":1,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";

    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1, test001Address, 0, dev001Key,
            blockingStubFull);
    final Transaction transaction1 = PublicMethedForDailybuild
        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);

    String accountPermissionJson2 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address"
            + "\":\"" + PublicMethedForDailybuild.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\""
            + "active0\",\"threshold\":1,\"operations\":"
            + "\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";
    String[] permissionKeyString1 = new String[1];
    permissionKeyString1[0] = dev001Key;
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson2, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString1));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount2 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList2 = test001AddressAccount2.getActivePermissionList();
    Permission ownerPermission2 = test001AddressAccount2.getOwnerPermission();
    final Permission witnessPermission2 = test001AddressAccount2.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList2);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1 - balance2, updateAccountPermissionFee);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission2));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission2));
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transactionSignWeight:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("but it is not contained of permission"));
    Return returnResult = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Assert
        .assertThat(returnResult.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult.getMessage().toStringUtf8(),
            containsString("but it is not contained of permission"));
    Account test001AddressAccount3 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance2 - balance3, 0);
  }

  @Test(enabled = true, description = "AccountPermissionUpdate transaction,"
      + "use owner address to sign,Delete the address,broadcastTransaction.")
  public void testMultiUpdatepermissions_BeforeSign_3() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    long amount = 2 * updateAccountPermissionFee + 1;

    Assert.assertTrue(PublicMethedForDailybuild
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    final long balance = test001AddressAccount.getBalance();

    String[] permissionKeyString = new String[2];
    permissionKeyString[0] = dev001Key;
    permissionKeyString[1] = sendAccountKey2;

    String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name"
            + "\":\"owner\",\"threshold\":1,\"keys\":[{"
            + "\"address\":\"" + PublicMethedForDailybuild.getAddressString(dev001Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey2)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
            + "\"threshold\":1,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";

    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    String accountPermissionJson3 =
        "{\"owner_permission\":{\"type\":0,\"permission_name"
            + "\":\"owner\",\"threshold\":1,\"keys\":[{"
            + "\"address\":\"" + PublicMethedForDailybuild.getAddressString(dev001Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey2)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
            + "\"threshold\":1,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey4)
            + "\",\"weight\":1}]}]} ";

    Transaction transaction = PublicMethedForMutiSign
        .accountPermissionUpdateWithoutSign(accountPermissionJson3, test001Address, dev001Key,
            blockingStubFull,
            permissionKeyString);
    final Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, sendAccountKey2, 0, blockingStubFull);
    String accountPermissionJson2 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address"
            + "\":\"" + PublicMethedForDailybuild.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\""
            + "active0\",\"threshold\":1,\"operations\":"
            + "\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";
    String[] permissionKeyString1 = new String[1];
    permissionKeyString1[0] = dev001Key;
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson2, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString1));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount2 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList2 = test001AddressAccount2.getActivePermissionList();
    Permission ownerPermission2 = test001AddressAccount2.getOwnerPermission();
    Permission witnessPermission2 = test001AddressAccount2.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList2);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission2));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission2));
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1 - balance2, updateAccountPermissionFee);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transactionSignWeight:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("but it is not contained of permission"));
    Return returnResult = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Assert
        .assertThat(returnResult.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult.getMessage().toStringUtf8(),
            containsString("but it is not contained of permission"));

    Account test001AddressAccount3 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance2 - balance3, 0);
  }

  @Test(enabled = true, description =
      "Sendcoin,meet the all requirements.Delete the active address,"
          + "use the address to sign,broadcastTransaction.")
  public void testMultiUpdatepermissions_AfterSign_1() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    long amount = 2 * updateAccountPermissionFee + 1;

    Assert.assertTrue(PublicMethedForDailybuild
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);

    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    final long balance = test001AddressAccount.getBalance();

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;
    String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethedForDailybuild.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":1,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey4)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";

    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);

    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    final Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1, test001Address, 2, dev001Key,
            blockingStubFull);

    String accountPermissionJson2 =
        "{\"owner_permission\":{\"type\":0,\"permission_name"
            + "\":\"owner\",\"threshold\":1,\"keys\":[{"
            + "\"address\":\"" + PublicMethedForDailybuild.getAddressString(dev001Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey2)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
            + "\"threshold\":1,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";
    String[] permissionKeyString1 = new String[1];
    permissionKeyString1[0] = dev001Key;
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson2, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString1));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Account test001AddressAccount2 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList2 = test001AddressAccount2.getActivePermissionList();
    Permission ownerPermission2 = test001AddressAccount2.getOwnerPermission();
    Permission witnessPermission2 = test001AddressAccount2.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList2);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission2));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission2));
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1 - balance2, updateAccountPermissionFee);
    Transaction transaction1 = PublicMethedForDailybuild
        .addTransactionSign(transaction, sendAccountKey4, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transactionSignWeight:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("but it is not contained of permission"));
    Return returnResult = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Assert
        .assertThat(returnResult.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult.getMessage().toStringUtf8(),
            containsString("but it is not contained of permission"));
    Account test001AddressAccount3 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance2 - balance3, 0);
  }

  @Test(enabled = true, description =
      "Sendcoin,not meet the requirements.Delete the active address,"
          + "use the address to sign,broadcastTransaction.")
  public void testMultiUpdatepermissions_AfterSign_2() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    long amount = 2 * updateAccountPermissionFee + 1;

    Assert.assertTrue(PublicMethedForDailybuild
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    final long balance = test001AddressAccount.getBalance();

    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    String[] permissionKeyString = new String[2];
    permissionKeyString[0] = dev001Key;
    permissionKeyString[1] = sendAccountKey2;
    String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethedForDailybuild.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":1,\"operations\""
            + ":\"0100000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey4)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";

    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);

    final Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1, test001Address, 2, dev001Key,
            blockingStubFull);

    String accountPermissionJson2 =
        "{\"owner_permission\":{\"type\":0,\"permission_name"
            + "\":\"owner\",\"threshold\":1,\"keys\":[{"
            + "\"address\":\"" + PublicMethedForDailybuild.getAddressString(dev001Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey2)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
            + "\"threshold\":1,\"operations\""
            + ":\"0100000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";
    String[] permissionKeyString1 = new String[1];
    permissionKeyString1[0] = dev001Key;
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson2, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString1));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount2 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList2 = test001AddressAccount2.getActivePermissionList();
    Permission ownerPermission2 = test001AddressAccount2.getOwnerPermission();
    final Permission witnessPermission2 = test001AddressAccount2.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList2);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1 - balance2, updateAccountPermissionFee);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission2));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission2));
    Transaction transaction1 = PublicMethedForDailybuild
        .addTransactionSign(transaction, sendAccountKey4, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("transactionSignWeight:" + transactionSignWeight);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("Permission denied"));
    Return returnResult = PublicMethedForMutiSign
        .broadcastTransaction1(transaction1, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Assert
        .assertThat(returnResult.getCode().toString(), containsString("SIGERROR"));
    Assert
        .assertThat(returnResult.getMessage().toStringUtf8(),
            containsString("Permission denied"));
    Account test001AddressAccount3 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance2 - balance3, 0);
  }

  @Test(enabled = true, description = "Sendcoin,Delete the owner address,"
      + "use the address to sign,broadcastTransaction.")
  public void testMultiUpdatepermissions_AfterSign() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    long amount = 2 * updateAccountPermissionFee + 1;

    Assert.assertTrue(PublicMethedForDailybuild
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    final long balance = test001AddressAccount.getBalance();

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethedForDailybuild.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":1,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey2)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";

    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));
    logger.info("1-----------------------");

    final Transaction transaction = PublicMethedForMutiSign
        .sendcoinWithPermissionIdNotSign(fromAddress, 1, test001Address, 0, dev001Key,
            blockingStubFull);

    String accountPermissionJson2 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey2) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":1,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey4)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson2, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));

    Account test001AddressAccount2 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList2 = test001AddressAccount2.getActivePermissionList();
    Permission ownerPermission2 = test001AddressAccount2.getOwnerPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList2);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1 - balance2, updateAccountPermissionFee);
    Permission witnessPermission2 = test001AddressAccount2.getWitnessPermission();
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission2));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission2));

    Transaction transaction1 = PublicMethedForDailybuild
        .addTransactionSign(transaction, dev001Key, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("but it is not contained of permission"));
    Assert
        .assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));
    Account test001AddressAccount3 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance2 - balance3, 0);

  }

  @Test(enabled = true, description = "AccountPermissionUpdate transaction,"
      + "Delete the owner address,use the address to sign,broadcastTransaction.")
  public void testMultiUpdatepermissions_AfterSign_4() {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] test001Address = ecKey.getAddress();
    long amount = 3 * updateAccountPermissionFee + 1;

    Assert.assertTrue(PublicMethedForDailybuild
        .sendcoin(test001Address, amount, fromAddress, testKey002,
            blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    final long balance = test001AddressAccount.getBalance();

    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = dev001Key;

    String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethedForDailybuild.getAddressString(dev001Key) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":1,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey2)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";

    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account test001AddressAccount1 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    long balance1 = test001AddressAccount1.getBalance();
    Assert.assertEquals(balance - balance1, updateAccountPermissionFee);
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));

    String accountPermissionJson3 =
        "{\"owner_permission\":{\"type\":0,\"permission_name"
            + "\":\"owner\",\"threshold\":1,\"keys\":[{"
            + "\"address\":\"" + PublicMethedForDailybuild.getAddressString(dev001Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey2)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
            + "\"threshold\":1,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey4)
            + "\",\"weight\":1}]}]} ";

    final Transaction transaction = PublicMethedForMutiSign
        .accountPermissionUpdateWithoutSign(accountPermissionJson3, test001Address, dev001Key,
            blockingStubFull,
            permissionKeyString);

    String accountPermissionJson2 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\""
            + ":\"owner\",\"threshold\":1,\"keys\":[{\"address\""
            + ":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey2) + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"
            + "\"active0\",\"threshold\":1,\"operations\""
            + ":\"0200000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey4)
            + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson2, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString));

    Account test001AddressAccount2 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList2 = test001AddressAccount2.getActivePermissionList();
    Permission ownerPermission2 = test001AddressAccount2.getOwnerPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList2);
    long balance2 = test001AddressAccount2.getBalance();
    Assert.assertEquals(balance1 - balance2, updateAccountPermissionFee);
    Permission witnessPermission2 = test001AddressAccount2.getWitnessPermission();
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission2));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission2));

    Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, dev001Key, 0, blockingStubFull);
    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    Assert
        .assertThat(transactionSignWeight.getResult().getCode().toString(),
            containsString("PERMISSION_ERROR"));
    Assert
        .assertThat(transactionSignWeight.getResult().getMessage(),
            containsString("but it is not contained of permission"));
    Assert
        .assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));

    Account test001AddressAccount3 = PublicMethedForDailybuild.queryAccount(test001Address, blockingStubFull);
    long balance3 = test001AddressAccount3.getBalance();
    Assert.assertEquals(balance2 - balance3, 0);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

  }


}
