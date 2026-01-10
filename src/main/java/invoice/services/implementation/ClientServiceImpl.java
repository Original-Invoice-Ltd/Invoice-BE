package invoice.services.implementation;

import invoice.data.models.Client;
import invoice.data.models.User;
import invoice.data.repositories.ClientRepository;
import invoice.dtos.request.ClientRequest;
import invoice.dtos.response.ClientResponse;
import invoice.exception.ResourceNotFoundException;
import invoice.services.ClientService;
import invoice.services.UserService;
import invoice.services.NotificationService;
import invoice.data.constants.NotificationType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ClientServiceImpl implements ClientService {
    private final ClientRepository clientRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final NotificationService notificationService;


    @Override
    public String addClient(String email, ClientRequest clientRequest) {
        User user = userService.getUserByEmail(email);
        Client client  = modelMapper.map(clientRequest, Client.class);
        if(clientRequest.getCountry()!=null)
            client.setCountry(clientRequest.getCountry());
        client.setUser(user);
        Client savedClient = clientRepository.save(client);
        
        // Create notification for client creation
        try {
            notificationService.createNotification(
                user,
                "Client Added",
                "Client " + savedClient.getFullName() + " has been added successfully",
                NotificationType.CLIENT_CREATED,
                savedClient.getId(),
                "CLIENT"
            );
        } catch (Exception e) {
            log.error("Failed to create notification for client creation: {}", e.getMessage());
        }
        
        return "client added successfully";
    }

    @Override
    public String updateClient(UUID id,ClientRequest clientRequest) {
        Client client = findClient(id);
        User user = client.getUser();
        modelMapper.map(clientRequest, client);
        client.setUser(user);
        Client updatedClient = clientRepository.save(client);
        
        // Create notification for client update
        try {
            notificationService.createNotification(
                user,
                "Client Updated",
                "Client " + updatedClient.getFullName() + " has been updated successfully",
                NotificationType.CLIENT_UPDATED,
                updatedClient.getId(),
                "CLIENT"
            );
        } catch (Exception e) {
            log.error("Failed to create notification for client update: {}", e.getMessage());
        }
        
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
        User user = client.getUser();
        String clientName = client.getFullName();
        
        clientRepository.delete(client);
        
        // Create notification for client deletion
        try {
            notificationService.createNotification(
                user,
                "Client Deleted",
                "Client " + clientName + " has been deleted successfully",
                NotificationType.CLIENT_DELETED,
                id,
                "CLIENT"
            );
        } catch (Exception e) {
            log.error("Failed to create notification for client deletion: {}", e.getMessage());
        }
        
        return "client deleted successfully";
    }

    private Client findClient(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("client not found"));
    }
}
