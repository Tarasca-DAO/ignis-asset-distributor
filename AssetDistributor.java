package org.tarasca.contracts;

import nxt.addons.*;
import nxt.http.callers.TransferAssetCall;
import nxt.http.responses.TransactionResponse;
import java.util.Map;

public class AssetDistributor extends AbstractContract {

    @ValidateChain(accept = 2)
    public JO processTransaction(TransactionContext context) {
        AssetDistributor.ContractParams contractParams = context.getParams(AssetDistributor.ContractParams.class);
        String validSender = contractParams.validSender();
        Integer validType = contractParams.validType();
        Integer validSubtype = contractParams.validSubtype();
        String asset = contractParams.asset();
        String keyString = contractParams.keyString();
        int DEADLINE = 180;

        TransactionResponse txr = context.getTransaction();
        if (txr.getTransactionType().getType() == validType
                && txr.getTransactionType().getSubtype() == validSubtype
                && txr.getSenderRs().equals(validSender)) {
            // valid sender and type
            JO params = context.getRuntimeParams();
            int numAssets = params.getInt(keyString);

            context.logInfoMessage("distributing " + numAssets + " assets with id:" + asset + " to: " + txr.getRecipientRs());

            TransferAssetCall transferAsset = TransferAssetCall.create(2)
                    .recipient(txr.getRecipientRs())
                    .asset(asset)
                    .quantityQNT((long)numAssets)
                    .deadline(DEADLINE);
            context.createTransaction(transferAsset);
            return context.getResponse();
        }
        else {
            return context.generateErrorResponse(10001,"invalid sender or tx type");
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
    }
}
