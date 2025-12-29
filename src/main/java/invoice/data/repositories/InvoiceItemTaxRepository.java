package invoice.data.repositories;

import invoice.data.models.InvoiceItemTax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemTaxRepository extends JpaRepository<InvoiceItemTax, Long> {
    
    @Query("SELECT iit FROM InvoiceItemTax iit WHERE iit.invoiceItem.id = :itemId")
    List<InvoiceItemTax> findByInvoiceItemId(@Param("itemId") Long itemId);
    
    @Query("SELECT iit FROM InvoiceItemTax iit WHERE iit.invoiceItem.invoice.id = :invoiceId")
    List<InvoiceItemTax> findByInvoiceId(@Param("invoiceId") java.util.UUID invoiceId);
    
    void deleteByInvoiceItemId(Long itemId);
}