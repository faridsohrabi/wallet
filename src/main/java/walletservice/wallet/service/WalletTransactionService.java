package walletservice.wallet.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import walletservice.wallet.controlleradvice.exception.ServiceException;
import walletservice.wallet.models.entities.*;
import walletservice.wallet.repositories.WalletRepository;
import walletservice.wallet.repositories.WalletTransactionRepository;

import java.time.LocalDateTime;

@Service
public class WalletTransactionService extends AbstractService<WalletTransaction, WalletTransactionRepository> {
    @Autowired
    private WalletRepository walletRepository;

    public void deposit(Wallet receiverWallet, Long amount, TransactionType transactionType) throws ServiceException {
        if (receiverWallet == null) {
            throw new ServiceException("wallet-not-found");
        }
        receiverWallet.setBalance(amount + receiverWallet.getBalance());
        walletRepository.save(receiverWallet);
    }

    public void withdraw(Wallet senderWallet, Long amount, TransactionType transactionType) throws ServiceException {
        if (senderWallet.getBalance() > amount) {
            senderWallet.setBalance(senderWallet.getBalance()-amount);
        } else throw new ServiceException("not-enough-balance");

        walletRepository.save(senderWallet);

    }

    private WalletTransaction setWalletTransaction(Wallet wallet, Long amount, DepositWithdraw depositWithdraw, TransactionType transactionType) {
        WalletTransaction walletTransaction = WalletTransaction
                .builder()
                .walletId(wallet)
                .amount(amount)
                .depositWithdraw(depositWithdraw)
                .transactionType(transactionType)
                .phoneNumber(wallet.getPhoneNumber())
                .dateTime(LocalDateTime.now())
                .status(TransactionStatus.PEND)
                .build();
        insert(walletTransaction);
        return walletTransaction;
    }

    @Transactional(rollbackOn = ServiceException.class)
    public TransactionStatus walletToWallet(String senderWalletId, String receiverWalletId, Long amount, TransactionType transactionType, String phoneNumber) throws ServiceException {
        Wallet senderWallet = walletRepository.findByWalletCode(senderWalletId);
        Wallet recieverWallet = walletRepository.findByWalletCode(receiverWalletId);

        if (senderWallet == null) {
            throw new ServiceException("wallet-not-found");
        }

        if (senderWallet.getPhoneNumber().equals(phoneNumber)) {
            withdraw(senderWallet, amount, transactionType);
            WalletTransaction withdrawTransaction = setWalletTransaction(senderWallet, amount, DepositWithdraw.WITHDRAW, transactionType);

            deposit(recieverWallet, amount, transactionType);
            WalletTransaction depositTransaction = setWalletTransaction(recieverWallet, amount, DepositWithdraw.DEPOSIT, transactionType);

            withdrawTransaction.setStatus(TransactionStatus.SUCCESS);
            depositTransaction.setStatus(TransactionStatus.SUCCESS);

            if (withdrawTransaction.getStatus() == TransactionStatus.SUCCESS && depositTransaction.getStatus()==TransactionStatus.SUCCESS ){
                return TransactionStatus.SUCCESS;
            }
        } else {
            throw new ServiceException("phoneNumber-not-found");
        }
        return TransactionStatus.FAILED;
    }
}

