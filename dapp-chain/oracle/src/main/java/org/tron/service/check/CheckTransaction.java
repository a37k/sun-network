package org.tron.service.check;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.client.MainChainGatewayApi;
import org.tron.client.SideChainGatewayApi;
import org.tron.common.exception.RpcConnectException;
import org.tron.common.exception.TxExpiredException;
import org.tron.common.exception.TxFailException;
import org.tron.common.exception.TxRollbackException;
import org.tron.common.exception.TxValidateException;
import org.tron.common.utils.AlertUtil;
import org.tron.db.EventStore;
import org.tron.db.TransactionExtensionStore;
import org.tron.service.eventactuator.Actuator;
import org.tron.service.eventactuator.ActuatorRun;
import org.tron.service.task.InitTask;

@Slf4j
public class CheckTransaction {

  private static CheckTransaction instance = new CheckTransaction();

  public static CheckTransaction getInstance() {
    return instance;
  }

  private CheckTransaction() {
  }

  private final ExecutorService syncExecutor = Executors
      .newFixedThreadPool(100);

  public void submitCheck(TransactionExtensionCapsule txExtensionCapsule, int submitCnt) {
    // TODO: from solidity node
    try {
      Thread.sleep(60 * 1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    syncExecutor
        .submit(() -> instance.checkTransaction(txExtensionCapsule, submitCnt));
  }

  private void checkTransaction(TransactionExtensionCapsule txExtensionCapsule, int checkCnt) {

    try {
      if (StringUtils.isEmpty(txExtensionCapsule.getTransactionId())) {
        return;
      }
      switch (txExtensionCapsule.getType()) {
        case MAIN_CHAIN:
          MainChainGatewayApi.checkTxInfo(txExtensionCapsule.getTransactionId());
          break;
        case SIDE_CHAIN:
          SideChainGatewayApi.checkTxInfo(txExtensionCapsule.getTransactionId());
          break;
      }
      // FIXME: fail to delete db, so in main chain contract, it must check dup using nonce.
      byte[] nonceKeyBytes = txExtensionCapsule.getNonceKeyBytes();
      EventStore.getInstance().deleteData(nonceKeyBytes);
      TransactionExtensionStore.getInstance().deleteData(nonceKeyBytes);
    } catch (TxRollbackException e) {
      // NOTE: http://106.39.105.178:8090/pages/viewpage.action?pageId=8992655 4.2
      logger.error(e.getMessage());
      if (checkCnt > 5) {
        AlertUtil.sendAlert("4.2, checkTransaction exceeds 5 times");
        logger.error("checkTransaction exceeds 5 times");
      } else {
        try {
          broadcastTransaction(txExtensionCapsule);
        } catch (RpcConnectException e1) {
          // NOTE: http://106.39.105.178:8090/pages/viewpage.action?pageId=8992655 1.2
          // NOTE: have retried for 5 times in broadcastTransaction
          AlertUtil.sendAlert("1.2");
          logger.error(e1.getMessage(), e1);
          return;
        } catch (TxValidateException e1) {
          // NOTE: http://106.39.105.178:8090/pages/viewpage.action?pageId=8992655 4.1
          AlertUtil.sendAlert("4.1");
          logger.error(e1.getMessage(), e1);
          return;
        } catch (TxExpiredException e1) {
          byte[] nonceKeyBytes = txExtensionCapsule.getNonceKeyBytes();
          byte[] data = EventStore.getInstance().getData(nonceKeyBytes);
          try {
            Actuator eventActuator = InitTask.getActuatorByEventMsg(data);
            ActuatorRun.getInstance().start(eventActuator);
          } catch (InvalidProtocolBufferException e2) {
            e2.printStackTrace();
          }
          return;
        }

        instance.submitCheck(txExtensionCapsule, checkCnt + 1);
      }
    } catch (TxFailException e) {
      // NOTE: http://106.39.105.178:8090/pages/viewpage.action?pageId=8992655 5.1 5.2 5.3
      AlertUtil.sendAlert("5.1 5.2 5.3");
      logger.error(e.getMessage(), e);
    }
  }

  public boolean broadcastTransaction(TransactionExtensionCapsule txExtensionCapsule)
      throws RpcConnectException, TxValidateException, TxExpiredException {
    switch (txExtensionCapsule.getType()) {
      case MAIN_CHAIN:
        return MainChainGatewayApi.broadcast(txExtensionCapsule.getTransaction());
      case SIDE_CHAIN:
        return SideChainGatewayApi.broadcast(txExtensionCapsule.getTransaction());
      default:
        return false;
    }
  }
}
