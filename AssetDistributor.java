package org.tarasca.contracts;

import nxt.addons.*;
import nxt.http.callers.*;
import nxt.http.responses.TransactionResponse;
import static nxt.blockchain.ChildChain.IGNIS;


public class AssetDistributor extends AbstractContract {

    @ValidateChain(accept = 2)
    public JO processTransaction(TransactionContext context) {
        AssetDistributor.ContractParams contractParams = context.getParams(AssetDistributor.ContractParams.class);
        String validSender = contractParams.validSender();
        Integer validType = contractParams.validType();
        Integer validSubtype = contractParams.validSubtype();
        String assetToSend = contractParams.asset();
        String keyString = contractParams.keyString();
        int DEADLINE = 180;

        TransactionResponse txr = context.getTransaction();
        if (txr.getTransactionType().getType() == validType
                && txr.getTransactionType().getSubtype() == validSubtype
                && txr.getSenderRs().equals(validSender)) {
            // valid sender and type
            JO params = context.getRuntimeParams();
            int numAssets = params.getInt(keyString);
            //long share = params.getLong("participations")/params.getLong("totalParticipations");

            if (numAssets == 0)
                return context.generateInfoResponse("requested quantity=0, no asset transferred");

            else {
                context.logInfoMessage("distributing " + numAssets + " assets with id:" + assetToSend + " to: " + txr.getRecipientRs());

                TransferAssetCall transferAsset = TransferAssetCall.create(2)
                        .recipient(txr.getRecipientRs())
                        .asset(assetToSend)
                        .quantityQNT((long)numAssets)
                        .deadline(DEADLINE);
                context.createTransaction(transferAsset);

                return context.getResponse();
            }
        }
        else {
            return context.generateErrorResponse(10001,"invalid sender or tx type");
        }
    }

    private long getAssetsInCirculation(String issuerAccount, String asset, int height){
        JO response = GetAssetAccountsCall.create().asset(asset).height(height).call();
        JA accountAssets = response.getArray("accountAssets");

        long assetsInCirculation = accountAssets.objects().stream()
                .mapToLong(a->{
                    if (a.getString("accountRS").equals(issuerAccount))
                        return 0l;

                    else
                        return a.getLong("quantityQNT");
                }).sum();
        return assetsInCirculation;
    }


    public JO processBlock(BlockContext context){
        AssetDistributor.ContractParams contractParams = context.getParams(AssetDistributor.ContractParams.class);

        int chainId = contractParams.chainId();
        int frequency = contractParams.frequency();
        String assetPayingIgnis = contractParams.assetPayingIgnis();
        String assetPayingGem = contractParams.assetPayingGem();
        String gemAssetId = contractParams.gemAssetId();
        long gemToDistribute = contractParams.gemToDistribute()*IGNIS.ONE_COIN;
        int deadline = contractParams.deadline();

        int height = context.getHeight();
        int modulo = height % frequency;
        int nextJackpotHeight = height - modulo + frequency;

        long STAYINACCOUNT = 20*IGNIS.ONE_COIN;

        int lastJackpotHeight;
        if (modulo == 0 & height != 0) {
            lastJackpotHeight =  (height-1) - ((height-1) % frequency);
        }
        else {
            lastJackpotHeight = height - modulo;
        }

        if(frequency < deadline)
            return context.generateErrorResponse(20001,"frequency is smaller than distance to jackpot block, adjust configuration");

        if (modulo == deadline) {

            int executionHeight = height - 1;

            JO response = GetBalanceCall.create(chainId).account(context.getAccountRs()).height(lastJackpotHeight).call();
            long balance = response.getLong("balanceNQT");

            long assetsInCirculation = getAssetsInCirculation(context.getAccountRs(),assetPayingIgnis,executionHeight);
            long payout = balance - STAYINACCOUNT;
            long ignisPerShare = payout/assetsInCirculation;

            context.logInfoMessage("creating Ignis transaction for parameters: jackpotHeight: %d, balance: %d, assetsInCirculation: %d, payout: %d, ignisPerShare: %d",
                    lastJackpotHeight,balance,assetsInCirculation,payout,ignisPerShare);

            DividendPaymentCall dividendPaymentCallIgnis = DividendPaymentCall.create(chainId)
                    .asset(assetPayingIgnis)
                    .holdingType((byte) 0)
                    .amountNQTPerShare(ignisPerShare)
                    .height(executionHeight)
                    .deadline(deadline);
            context.createTransaction(dividendPaymentCallIgnis);

            long assetsInCirculationGem = getAssetsInCirculation(context.getAccountRs(),assetPayingGem,executionHeight);
            long gemPerShare = gemToDistribute/assetsInCirculationGem;

            context.logInfoMessage("creating GEM transaction for parameters: gemToDistribute: %d, assetsInCirculationGem: %d, gemPerShare: %d",
                    gemToDistribute,assetsInCirculationGem,gemPerShare);

            DividendPaymentCall dividendPaymentCallGem = DividendPaymentCall.create(chainId)
                    .asset(assetPayingGem)
                    .holdingType((byte) 1)
                    .holding(gemAssetId)
                    .amountNQTPerShare(gemPerShare)
                    .height(executionHeight)
                    .deadline(deadline);
            context.createTransaction(dividendPaymentCallGem);

            return context.getResponse();
        }
        else {
            int nextPayoutHeight;

            if (modulo<deadline)
                nextPayoutHeight=lastJackpotHeight+deadline;
            else
                nextPayoutHeight=nextJackpotHeight+deadline;

            return context.generateInfoResponse("no height to pay out dividends (current height: %d, last jackpot : %d, next jackpot: %d, next payout: %d)",
                    height,lastJackpotHeight, nextJackpotHeight, nextPayoutHeight);
        }


    }

    @ContractParametersProvider
    public interface ContractParams {

        @ContractSetupParameter
        default String validSender() { return "123456"; }

        @ContractSetupParameter
        default Integer validType() { return 0; }

        @ContractSetupParameter
        default Integer validSubtype() { return 0; }

        @ContractSetupParameter
        default String asset() { return "123456"; }

        @ContractSetupParameter
        default String keyString() { return "numTarascasWon"; }

        @ContractRunnerParameter
        default int chainId() { return 2;}

        @ContractRunnerParameter
        default int frequency() { return 5040;}

        @ContractRunnerParameter
        default String gemAssetId() { return "10230963490193589789";}

        @ContractRunnerParameter
        default String assetPayingIgnis() { return "13187825386854631652";}

        @ContractRunnerParameter
        default String assetPayingGem() { return "14906207210027210012";}

        @ContractRunnerParameter
        default long gemToDistribute() {return 3000;}

        @ContractRunnerParameter
        default int deadline() {return 60;}
    }
}
