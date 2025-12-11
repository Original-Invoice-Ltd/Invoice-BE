package invoice.services.implementation;

import invoice.data.models.Client;
import invoice.data.models.User;
import invoice.data.repositories.ClientRepository;
import invoice.dtos.request.ClientRequest;
import invoice.dtos.response.ClientResponse;
import invoice.exception.ResourceNotFoundException;
import invoice.services.ClientService;
import invoice.services.UserService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ClientServiceImpl implements ClientService {
    private final ClientRepository clientRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;


    @Override
    public String addClient(String email, ClientRequest clientRequest) {
        User user = userService.getUserByEmail(email);
        Client client  = modelMapper.map(clientRequest, Client.class);
        if(clientRequest.getCountry()!=null)
            client.setCountry(clientRequest.getCountry());
        client.setUser(user);
        clientRepository.save(client);
        return "client added successfully";
    }

    @Override
    public String updateClient(UUID id,ClientRequest clientRequest) {
        Client client = findClient(id);
        User user = client.getUser();
        modelMapper.map(clientRequest, client);
        client.setUser(user);
        clientRepository.save(client);
        return "client updated successfully";
    }

    @Override
    public ClientResponse getClient(UUID id) {
        Client client = findClient(id);
        return new ClientResponse(client);
    }

    @Override
    public List<ClientResponse> getAllClients() {
        List<Client> clients = clientRepository.findAll();
        if (clients.isEmpty())return List.of();
        return clients.stream()
                .map(ClientResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClientResponse> getAllUserClients(String email) {
        List<Client> clients = clientRepository.findAllUser(email);
        if (clients.isEmpty())return List.of();
        return clients.stream()
                .map(ClientResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    public String deleteAllUserClients(String email) {
        List<Client> clients = clientRepository.findAllUser(email);
        if (!clients.isEmpty())clientRepository.deleteAll(clients);
        return "clients deleted successfully";
    }

    @Override
    public String deleteAllClients() {
        List<Client> clients = clientRepository.findAll();
        if (!clients.isEmpty())clientRepository.deleteAll(clients);
        return "clients deleted successfully";
    }

    @Override
    public String deleteClient(UUID id) {
        Client client = findClient(id);
        clientRepository.delete(client);
        return "client deleted successfully";
    }

    private Client findClient(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("client not found"));
    }
}
