package org.tarasca.contracts;

import com.jelurida.ardor.contracts.DistributedRandomNumberGenerator;
import nxt.addons.JA;
import nxt.addons.JO;
import nxt.http.callers.*;
import com.jelurida.ardor.contracts.AbstractContractTest;
import nxt.util.Logger;
import org.junit.Assert;
import org.junit.Test;



import static org.tarasca.contracts.TarascaTester.*;
import static nxt.blockchain.ChildChain.IGNIS;
import com.jelurida.ardor.contracts.ContractTestHelper;

public class AssetDistributorTest extends AbstractContractTest {

    @Test
    public void distributeAsset(){
        Logger.logDebugMessage("TEST: testTarascaCardDistribution(): Start");

        JO assetJo = initSpecialCardAsset(ALICE);

        JO distributorParams = new JO();
        distributorParams.put("asset",assetJo.getString("asset"));
        distributorParams.put("validSender",BOB.getRsAccount());
        String distributor = ContractTestHelper.deployContract(AssetDistributor.class,distributorParams,true);

        Logger.logDebugMessage("TEST: testTarascaCardDistribution(): Start test");
        Logger.logDebugMessage("TEST: Accounts");
        Logger.logDebugMessage("TEST: Contract (Alice): "+ALICE.getRsAccount()+", numeric: "+ALICE.getAccount());
        Logger.logDebugMessage("TEST: Player1  (Bob  ): "+BOB.getRsAccount()+", numeric: "+BOB.getAccount());
        Logger.logDebugMessage("TEST: Player2  (Dave ): "+DAVE.getRsAccount()+", numeric: "+DAVE.getAccount());
        Logger.logDebugMessage("TEST: Asset " + assetJo.getString("asset"));


        JO params = new JO();
        int numTarascasWon = 1;
        params.put("numTarascasWon",numTarascasWon);

        JO triggerMsg = new JO();
        triggerMsg.put("contract","AssetDistributor");
        triggerMsg.put("params",params);

        SendMoneyCall.create(2)
                .secretPhrase(BOB.getSecretPhrase())
                .recipient(CHUCK.getRsAccount())
                .amountNQT((long)100*IGNIS.ONE_COIN)
                .messageIsPrunable(true)
                .messageIsText(true)
                .message(triggerMsg.toJSONString())
                .feeNQT(IGNIS.ONE_COIN)
                .call();

        generateBlock(); // ignis is send

        generateBlock(); // contract responds

        Logger.logDebugMessage("TEST: testTarascaCardDistribution(): Evaluate results");

        JO response = GetAccountAssetsCall.create().account(CHUCK.getRsAccount()).asset(assetJo.getString("asset")).call();

        Assert.assertTrue("assetId matches", response.getString("asset").equals(assetJo.getString("asset")));
        Assert.assertEquals("quantity as expected",response.getLong("quantityQNT"),(long)numTarascasWon);

        Logger.logDebugMessage("TEST: testTarascaCardDistribution(): Done");
    }

    @Test
    public void distributeMultipleAssets(){
        Logger.logDebugMessage("TEST: testTarascaCardDistribution(): Start");

        JO assetJo = initSpecialCardAsset(ALICE);

        JO distributorParams = new JO();
        distributorParams.put("asset",assetJo.getString("asset"));
        distributorParams.put("validSender",BOB.getRsAccount());
        String distributor = ContractTestHelper.deployContract(AssetDistributor.class,distributorParams,true);

        Logger.logDebugMessage("TEST: testTarascaCardDistribution(): Start test");
        Logger.logDebugMessage("TEST: Accounts");
        Logger.logDebugMessage("TEST: Contract (Alice): "+ALICE.getRsAccount()+", numeric: "+ALICE.getAccount());
        Logger.logDebugMessage("TEST: Player1  (Bob  ): "+BOB.getRsAccount()+", numeric: "+BOB.getAccount());
        Logger.logDebugMessage("TEST: Player2  (Dave ): "+DAVE.getRsAccount()+", numeric: "+DAVE.getAccount());
        Logger.logDebugMessage("TEST: Asset " + assetJo.getString("asset"));


        JO params = new JO();
        int numTarascasWon = 1;
        params.put("numTarascasWon",numTarascasWon);

        JO triggerMsg = new JO();
        triggerMsg.put("reason","sendPrize");
        triggerMsg.put("contract","AssetDistributor");
        triggerMsg.put("params",params);

        SendMoneyCall.create(2)
                .secretPhrase(BOB.getSecretPhrase())
                .recipient(CHUCK.getRsAccount())
                .amountNQT((long)100*IGNIS.ONE_COIN)
                .messageIsPrunable(true)
                .messageIsText(true)
                .message(triggerMsg.toJSONString())
                .feeNQT(IGNIS.ONE_COIN)
                .call();

        int numTarascasWonDave = 2;
        params.put("numTarascasWon",numTarascasWonDave);
        triggerMsg.put("params",params);

        SendMoneyCall.create(2)
                .secretPhrase(BOB.getSecretPhrase())
                .recipient(DAVE.getRsAccount())
                .amountNQT((long)200*IGNIS.ONE_COIN)
                .messageIsPrunable(true)
                .messageIsText(true)
                .message(triggerMsg.toJSONString())
                .feeNQT(IGNIS.ONE_COIN)
                .call();

        generateBlock(); // ignis is send

        generateBlock(); // contract responds

        Logger.logDebugMessage("TEST: testTarascaCardDistribution(): Evaluate results");

        JO response = GetAccountAssetsCall.create().account(CHUCK.getRsAccount()).asset(assetJo.getString("asset")).call();
        Assert.assertTrue("assetId matches", response.getString("asset").equals(assetJo.getString("asset")));
        Assert.assertEquals("quantity as expected",response.getLong("quantityQNT"),(long)numTarascasWon);

        // Assert Dave has one card
        response = GetAccountAssetsCall.create().account(DAVE.getRsAccount()).asset(assetJo.getString("asset")).call();
        Assert.assertTrue("assetId matches", response.getString("asset").equals(assetJo.getString("asset")));
        Assert.assertEquals("quantity as expected",response.getLong("quantityQNT"),(long)numTarascasWonDave);


        Logger.logDebugMessage("TEST: testTarascaCardDistribution(): Done");
    }
}

