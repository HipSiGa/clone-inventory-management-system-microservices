package com.cesar.usservice.service.implementation;

import com.cesar.usservice.dto.ClientDTO;
import com.cesar.usservice.dto.mapper.ClientMapper;
import com.cesar.usservice.entity.ClientEntity;
import com.cesar.usservice.dto.OrderDetails;
import com.cesar.usservice.repository.ClientRepository;
import com.cesar.usservice.service.ClientService;
import com.cesar.usservice.exception.ClientException;
import com.cesar.usservice.utils.ClientField;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

@Service
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final Map<ClientField, BiConsumer<ClientEntity, ClientDTO>> updateFieldMap = new EnumMap<>(ClientField.class);

    public ClientServiceImpl(ClientRepository clientRepository, ClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;

        initializeUpdateFieldMap();
    }

    private void initializeUpdateFieldMap() {
        updateFieldMap.put(ClientField.ID, (entity, request) -> {
            if (request.getId() != null) {
                entity.setId(request.getId());
            }
        });
        updateFieldMap.put(ClientField.FIRST_NAME, (entity, request) -> {
            if (request.getFirstName() != null) {
                entity.setFirstName(request.getFirstName());
            }
        });
        updateFieldMap.put(ClientField.LAST_NAME, (entity, request) -> {
            if (request.getLastName() != null) {
                entity.setLastName(request.getLastName());
            }
        });
        updateFieldMap.put(ClientField.EMAIL, (entity, request) -> {
            if (request.getEmail() != null) {
                entity.setEmail(request.getEmail());
            }
        });
        updateFieldMap.put(ClientField.ADDRESS, (entity, request) -> {
            if (request.getAddress() != null) {
                entity.setAddress(request.getAddress());
            }
        });
        updateFieldMap.put(ClientField.ORDER_DETAILS, (entity, request) -> {
            if (request.getPendingOrders() != null) {
                if (entity.getPendingOrders().isEmpty()) {
                    entity.setPendingOrders(request.getPendingOrders());
                }
                entity.getPendingOrders().addAll(request.getPendingOrders());
            }
        });
    }

    @Override
    public ClientDTO getClientById(String id) {
        return clientMapper.toDTO(clientRepository.findById(id)
                .orElseThrow(() -> new ClientException("User not found")));
    }

    @Override
    public ClientDTO getClientByOrderId(String id) {
        return null;
    }

    @Override
    public ClientDTO saveClient(ClientDTO clientDTO) {
        if (clientDTO.getId() != null && clientDTO.getFirstName() != null && clientDTO.getEmail() != null) {
            if (clientRepository.findByIdAndEmailAndFirstName(clientDTO.getId(), clientDTO.getFirstName(), clientDTO.getEmail()) != null) {
                throw new ClientException("User already exists");
            }
            return clientMapper.toDTO(clientRepository.save(clientMapper.toEntity(clientDTO)));
        }
        throw new ClientException("User id, email and first name cannot be empty");
    }

    @Override
    public ClientDTO updateClient(ClientDTO clientDTO, String id) {
        if (clientDTO == null) {
            throw new ClientException("User request cannot be null");
        }

        ClientEntity clientEntity = clientMapper.toEntity(getClientById(id));

        for (Map.Entry<ClientField, BiConsumer<ClientEntity, ClientDTO>> entry : updateFieldMap.entrySet()) {
            entry.getValue().accept(clientEntity, clientDTO);
        }

        return clientMapper.toDTO(clientRepository.save(clientMapper.toEntity(clientDTO)));
    }

    @Override
    public ClientDTO updateClientOrdersByClientId(OrderDetails orderDetails, String id) {
        if (orderDetails == null) {
            throw new ClientException("The order details cannot be empty");
        }
        ClientEntity clientEntity = clientMapper.toEntity(getClientById(id));
        ClientDTO clientDTO = ClientDTO.builder()
                .pendingOrders(List.of(orderDetails))
                .build();

        BiConsumer<ClientEntity, ClientDTO> updateOrderDetails = updateFieldMap.get(ClientField.ORDER_DETAILS);
        updateOrderDetails.accept(clientEntity, clientDTO);

        return clientMapper.toDTO(clientRepository.save(clientEntity));
    }

    @Override
    public ClientDTO updateClientOrderStatusByClientId(String orderId, String newOrderStatus, String clientId) {
        ClientEntity clientEntity = clientMapper.toEntity(getClientById(clientId));

        List<OrderDetails> orderDetailsList = clientEntity.getPendingOrders();
        for (OrderDetails orderDetails : orderDetailsList) {
            if (Objects.equals(orderDetails.getId(), orderId)) {
                orderDetails.setOrderStatus(newOrderStatus);
                break;
            }
        }
        return clientMapper.toDTO(clientEntity);
    }

    @Override
    public String deleteClientById(String id) {
        ClientEntity clientEntity = clientMapper.toEntity(getClientById(id));
        if (clientEntity != null) {
            clientRepository.delete(clientEntity);
            return "Client with id: " + id + " was successfully deleted";
        }
        throw new ClientException("Client with id: " + id + " not found");
    }
}
