package invoice.dtos.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class OriginalInvoiceApiResponse<T>{
    private boolean success;
    private T data;
}
