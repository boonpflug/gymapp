package com.gymplatform.modules.finance;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.contract.Contract;
import com.gymplatform.modules.contract.ContractRepository;
import com.gymplatform.modules.contract.ContractStatus;
import com.gymplatform.modules.tenant.Tenant;
import com.gymplatform.modules.tenant.TenantRepository;
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
    private final TenantRepository tenantRepository;

    @Scheduled(cron = "0 0 6 * * *")
    public void runDailyPayments() {
        log.info("Starting daily payment run");
        for (Tenant tenant : tenantRepository.findAll()) {
            try {
                TenantContext.setTenantId(tenant.getSchemaName());
                runDailyPaymentsForTenant();
            } catch (Exception e) {
                log.error("Payment run failed for tenant {}: {}", tenant.getSchemaName(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
        log.info("Daily payment run completed");
    }

    @Transactional
    public void runDailyPaymentsForTenant() {
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
    }

    private LocalDate calculateNextBillingDate(Contract contract) {
        return contract.getNextBillingDate().plusMonths(1);
    }
}
