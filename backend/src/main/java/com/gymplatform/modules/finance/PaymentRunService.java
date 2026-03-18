package com.gymplatform.modules.finance;

import com.gymplatform.modules.contract.Contract;
import com.gymplatform.modules.contract.ContractRepository;
import com.gymplatform.modules.contract.ContractStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRunService {

    private final ContractRepository contractRepository;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final PaymentMethodRepository paymentMethodRepository;

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void runDailyPayments() {
        log.info("Starting daily payment run");
        LocalDate today = LocalDate.now();

        List<Contract> dueContracts = contractRepository
                .findByNextBillingDateLessThanEqualAndStatus(today, ContractStatus.ACTIVE);

        log.info("Found {} contracts due for payment", dueContracts.size());

        for (Contract contract : dueContracts) {
            try {
                Invoice invoice = invoiceService.generateInvoice(
                        contract.getMemberId(),
                        contract.getId(),
                        contract.getMonthlyAmount()
                );

                paymentMethodRepository
                        .findByMemberIdAndIsDefaultTrue(contract.getMemberId())
                        .ifPresent(pm -> paymentService.processPayment(invoice, pm));

                contract.setNextBillingDate(calculateNextBillingDate(contract));
                contractRepository.save(contract);

                log.info("Payment processed for contract {}", contract.getId());
            } catch (Exception e) {
                log.error("Payment run failed for contract {}: {}",
                        contract.getId(), e.getMessage());
            }
        }

        log.info("Daily payment run completed");
    }

    private LocalDate calculateNextBillingDate(Contract contract) {
        return contract.getNextBillingDate().plusMonths(1);
    }
}
