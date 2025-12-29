package invoice.services;

import invoice.dtos.request.ClientRequest;
import invoice.dtos.response.ClientResponse;

import java.util.List;
import java.util.UUID;

public interface ClientService {
    String addClient(String email, ClientRequest clientRequest);

    String updateClient(UUID id, ClientRequest clientRequest);

    ClientResponse getClient(UUID id);

    List<ClientResponse> getAllClients();

    List<ClientResponse> getAllUserClients(String email);

    String deleteAllUserClients(String email);

    String deleteAllClients();

    String deleteClient(UUID id);
}
