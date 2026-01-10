package invoice.data.repositories;

import invoice.data.models.InvoiceTax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceTaxRepository extends JpaRepository<InvoiceTax, Long> {
    
    @Query("SELECT it FROM InvoiceTax it WHERE it.invoice.id = :invoiceId")
    List<InvoiceTax> findByInvoiceId(UUID invoiceId);
    
    @Query("SELECT it FROM InvoiceTax it WHERE it.tax.id = :taxId")
    List<InvoiceTax> findByTaxId(UUID taxId);
    
    void deleteByInvoiceId(UUID invoiceId);
}