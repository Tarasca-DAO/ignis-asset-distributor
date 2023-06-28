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
        String assetPayingIgnisSmall = contractParams.assetPayingIgnisSmall();
        String assetPayingGem = contractParams.assetPayingGem();
        String assetSasquatch = contractParams.assetSasquatch();
        String assetMaru = contractParams.assetMaru();
        String gemAssetId = contractParams.gemAssetId();
        long gemToDistribute = contractParams.gemToDistribute()*IGNIS.ONE_COIN;
        int deadline = contractParams.deadline();
        int dividendsDelay = 60;

        int height = context.getHeight();
        int modulo = height % frequency;
        int nextJackpotHeight = height - modulo + frequency;

        long STAYINACCOUNT = 30*IGNIS.ONE_COIN;

        //double ratioSmallCard = contractParams.ratioSmallCard();
        //double ratioLargeCard = 1-(ratioSmallCard*2);
        double ratioTarasca = 0.57;
        double ratioMaru = 0.14;
        double ratioSasquatch = 0.14;
        double ratioMara = 0.14;

        int lastJackpotHeight;
        if (modulo == 0 & height != 0) {
            lastJackpotHeight =  (height-1) - ((height-1) % frequency);
        }
        else {
            lastJackpotHeight = height - modulo;
        }

        if(frequency < deadline)
            return context.generateErrorResponse(20001,"frequency is smaller than distance to jackpot block, adjust configuration");

        if (modulo == dividendsDelay) {

            int executionHeight = height - 1;

            JO response = GetBalanceCall.create(chainId).account(context.getAccountRs()).height(lastJackpotHeight).call();
            long balance = response.getLong("balanceNQT");
            long payout = balance - STAYINACCOUNT;

            // Asset Paying Ignis  = Tarasca Card
            long assetsInCirculation = getAssetsInCirculation(context.getAccountRs(),assetPayingIgnis,executionHeight);
            long payoutLarge =  (long) ((double) payout*ratioTarasca);
            long ignisPerShareLarge = payoutLarge/assetsInCirculation;

            context.logInfoMessage("creating Ignis transaction for parameters: jackpotHeight: %d, balance: %d, assetsInCirculation: %d, payout: %d, ignisPerShare: %d",
                    lastJackpotHeight,balance,assetsInCirculation,payoutLarge,ignisPerShareLarge);

            DividendPaymentCall dividendPaymentCallIgnisLarge = DividendPaymentCall.create(chainId)
                    .asset(assetPayingIgnis)
                    .holdingType((byte) 0)
                    .amountNQTPerShare(ignisPerShareLarge)
                    .height(executionHeight)
                    .deadline(deadline);
            context.createTransaction(dividendPaymentCallIgnisLarge);

            // Asset Paying Ignis (Small) = Mari
            long assetsInCirculationSmall = getAssetsInCirculation(context.getAccountRs(),assetPayingIgnisSmall,executionHeight);
            long payoutSmall =  (long) ((double) payout*ratioMara);
            long ignisPerShareSmall = payoutSmall/assetsInCirculationSmall;
            context.logInfoMessage("creating Ignis transaction for parameters: jackpotHeight: %d, balance: %d, assetsInCirculation: %d, payout: %d, ignisPerShare: %d",
                    lastJackpotHeight,balance,assetsInCirculationSmall,payoutSmall,ignisPerShareSmall);

            DividendPaymentCall dividendPaymentCallIgnisSmall = DividendPaymentCall.create(chainId)
                    .asset(assetPayingIgnisSmall)
                    .holdingType((byte) 0)
                    .amountNQTPerShare(ignisPerShareSmall)
                    .height(executionHeight)
                    .deadline(deadline);
            context.createTransaction(dividendPaymentCallIgnisSmall);

            // Asset Paying Ignis Sasquatch
            long assetsInCirculationSasquatch = getAssetsInCirculation(context.getAccountRs(),assetSasquatch,executionHeight);
            long payoutSasquatch =  (long) ((double) payout*ratioSasquatch);
            long ignisPerShareSasquatch = payoutSasquatch/assetsInCirculationSasquatch;
            context.logInfoMessage("creating Ignis transaction for parameters: jackpotHeight: %d, balance: %d, assetsInCirculation: %d, payout: %d, ignisPerShare: %d",
                    lastJackpotHeight,balance,assetsInCirculationSasquatch,payoutSasquatch,ignisPerShareSasquatch);

            DividendPaymentCall dividendPaymentCallSasquatch = DividendPaymentCall.create(chainId)
                    .asset(assetSasquatch)
                    .holdingType((byte) 0)
                    .amountNQTPerShare(ignisPerShareSasquatch)
                    .height(executionHeight)
                    .deadline(deadline);
            context.createTransaction(dividendPaymentCallSasquatch);

            // Asset Paying Ignis Maru
            long assetsInCirculationMaru = getAssetsInCirculation(context.getAccountRs(),assetMaru,executionHeight);
            long payoutMaru =  (long) ((double) payout*ratioMaru);
            long ignisPerShareMaru = payoutMaru/assetsInCirculationMaru;
            context.logInfoMessage("creating Ignis transaction for parameters: jackpotHeight: %d, balance: %d, assetsInCirculation: %d, payout: %d, ignisPerShare: %d",
                    lastJackpotHeight,balance,assetsInCirculationMaru,payoutMaru,ignisPerShareMaru);

            DividendPaymentCall dividendPaymentCallMaru = DividendPaymentCall.create(chainId)
                    .asset(assetMaru)
                    .holdingType((byte) 0)
                    .amountNQTPerShare(ignisPerShareMaru)
                    .height(executionHeight)
                    .deadline(deadline);
            context.createTransaction(dividendPaymentCallMaru);


            // Asset Paying Gem = Groot
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
        else if (modulo == (dividendsDelay + 120)) {
            context.logInfoMessage("Block for second Mari,Sasquatch dividend payment (Gem)");
            int executionHeight = height - 1;

            // Asset Paying Ignis (Small) = Mari
            long assetsInCirculationSmall = getAssetsInCirculation(context.getAccountRs(),assetPayingIgnisSmall,executionHeight);
            long gemPerShareSmall = 1000*IGNIS.ONE_COIN/assetsInCirculationSmall;

            context.logInfoMessage("creating GEM transaction for parameters: gemToDistribute: %d, assetsInCirculationGem: %d, gemPerShare: %d",
                    1000*IGNIS.ONE_COIN,assetsInCirculationSmall,gemPerShareSmall);

            DividendPaymentCall dividendPaymentCallGemSmall = DividendPaymentCall.create(chainId)
                    .asset(assetPayingIgnisSmall)
                    .holdingType((byte) 1)
                    .holding(gemAssetId)
                    .amountNQTPerShare(gemPerShareSmall)
                    .height(executionHeight)
                    .deadline(deadline);
            context.createTransaction(dividendPaymentCallGemSmall);

            // Asset Sasquatch, GEM payout
            long assetsInCirculationSasquatch = getAssetsInCirculation(context.getAccountRs(),assetSasquatch,executionHeight);
            long gemPerShareSasquatch = 1000*IGNIS.ONE_COIN/assetsInCirculationSasquatch;

            context.logInfoMessage("creating GEM transaction for parameters: gemToDistribute: %d, assetsInCirculationGem: %d, gemPerShare: %d",
                    1000*IGNIS.ONE_COIN,assetsInCirculationSasquatch,gemPerShareSasquatch);

            DividendPaymentCall dividendPaymentCallSasquatch = DividendPaymentCall.create(chainId)
                    .asset(assetSasquatch)
                    .holdingType((byte) 1)
                    .holding(gemAssetId)
                    .amountNQTPerShare(gemPerShareSasquatch)
                    .height(executionHeight)
                    .deadline(deadline);
            context.createTransaction(dividendPaymentCallSasquatch);

            // Asset Maru, GEM payout
            long assetsInCirculationMaru = getAssetsInCirculation(context.getAccountRs(),assetMaru,executionHeight);
            long gemPerShareMaru = 1000*IGNIS.ONE_COIN/assetsInCirculationMaru;

            context.logInfoMessage("creating GEM transaction for parameters: gemToDistribute: %d, assetsInCirculationGem: %d, gemPerShare: %d",
                    1000*IGNIS.ONE_COIN,assetsInCirculationMaru,gemPerShareMaru);

            DividendPaymentCall dividendPaymentCallMaru = DividendPaymentCall.create(chainId)
                    .asset(assetMaru)
                    .holdingType((byte) 1)
                    .holding(gemAssetId)
                    .amountNQTPerShare(gemPerShareMaru)
                    .height(executionHeight)
                    .deadline(deadline);
            context.createTransaction(dividendPaymentCallMaru);


            return context.getResponse();
        }
        else {
            int nextPayoutHeight;

            if (modulo<dividendsDelay)
                nextPayoutHeight=lastJackpotHeight+dividendsDelay;
            else
                nextPayoutHeight=nextJackpotHeight+dividendsDelay;

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
        default String assetPayingIgnisSmall() { return "16453401161130674677";}

        @ContractRunnerParameter
        default String assetPayingGem() { return "14906207210027210012";}

        @ContractRunnerParameter
        default String assetSasquatch() { return "8504616031553931056";}

        @ContractRunnerParameter
        default String assetMaru() { return "3651682276536707874";}

        @ContractRunnerParameter
        default long gemToDistribute() {return 3000;}

        @ContractRunnerParameter
        default int deadline() {return 900;}

        @ContractRunnerParameter
        default double ratioSmallCard() { return 0.166; }
    }
}
