package com.cesar.usservice.service.implementation;

import com.cesar.usservice.dto.ClientDTO;
import com.cesar.usservice.dto.mapper.ClientMapper;
import com.cesar.usservice.entity.ClientEntity;
import com.cesar.usservice.dto.OrderDetailsDTO;
import com.cesar.usservice.repository.ClientRepository;
import com.cesar.usservice.service.ClientService;
import com.cesar.usservice.exception.ClientException;
import com.cesar.usservice.utils.ClientField;
import com.cesar.usservice.utils.OrderStatus;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

@Service
public class ClientServiceImpl implements ClientService {

    private static final Logger logger = LoggerFactory.getLogger(ClientServiceImpl.class);
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
                List<OrderDetailsDTO> orderList = entity.getPendingOrders();
                OrderDetailsDTO newOrderDetails = request.getPendingOrders().get(0);

                updateItemList(orderList, newOrderDetails, entity);
            }
        });
    }

    private void updateItemList(List<OrderDetailsDTO> orderList, OrderDetailsDTO newOrderDetails, ClientEntity entity) {
        if (orderList != null) {
            if (orderList.isEmpty()) {
                orderList.add(newOrderDetails);
            }
            else {
                for (OrderDetailsDTO order : orderList) {
                    if (Objects.equals(order.getId(), newOrderDetails.getId())) {
                        order.setOrderStatus(newOrderDetails.getOrderStatus());
                        order.setOrderTotal(newOrderDetails.getOrderTotal());
                        entity.setPendingOrders(orderList);
                        break;
                    }
                }
                orderList.add(newOrderDetails);
            }
            entity.setPendingOrders(orderList);
        }
    }

    @Override
    public ClientDTO getClientById(String id) {
        return clientMapper.toDTO(clientRepository.findClientEntityById(id)
                .orElseThrow(() -> new ClientException("User not found with id: " + id)));
    }

    @Override
    public List<ClientDTO> getListOfClientsByKeyword(String keyword) {
        if (!keyword.isEmpty()) {
            return clientRepository.findClientsByKeyword(keyword)
                    .stream().map(clientMapper::toDTO)
                    .toList();
        }
        logger.error("Keyword cannot be empty!");
        throw new ClientException("keyword cannot be empty!");
    }

    @Override
    public ClientDTO getClientByOrderId(String orderId) {
        if (orderId.isEmpty()) {
            logger.error("Order id cannot be empty!");
            throw new ClientException("Order id cannot be Empty");
        }

        return clientMapper.toDTO(clientRepository.findByOrderDetailsId(orderId)
                .orElseThrow(() -> new ClientException("No client found with that order id, ORDER ID: " + orderId)));
    }

    @Override
    public ClientDTO saveClient(ClientDTO clientDTO) {
        if (clientDTO == null) {
            throw new ClientException("ClientDTO cannot be null");
        }
        if (!clientDTO.getFirstName().isEmpty() && !clientDTO.getEmail().isEmpty() && !clientDTO.getId().isEmpty()) {
            if (clientRepository.findById(clientDTO.getId()).isPresent()) {
                logger.info("User with id : '{}' already exists", clientDTO.getId());
                throw new ClientException("User already exists");
            }
            ClientEntity newClient = clientMapper.toEntity(clientDTO);
            ClientEntity createdClient = clientRepository.save(newClient);
            return clientMapper.toDTO(createdClient);
        }
        logger.error("Basic fields of new client are empty");
        throw new ClientException("User id, email and first name cannot be empty");
    }

    @Override
    public ClientDTO updateClientById(ClientDTO clientDTO, String id) {
        if (clientDTO == null) {
            logger.error("At least one attribute has to not be null");
            throw new ClientException("User request cannot be null");
        }

        ClientEntity clientEntity = clientMapper.toEntity(getClientById(id));

        for (Map.Entry<ClientField, BiConsumer<ClientEntity, ClientDTO>> entry : updateFieldMap.entrySet()) {
            entry.getValue().accept(clientEntity, clientDTO);
        }

        return clientMapper.toDTO(clientRepository.save(clientEntity));
    }

    @Override
    public ClientDTO updateClientOrdersByClientId(OrderDetailsDTO orderDetailsDTO, String id) {
        if (orderDetailsDTO == null) {
            logger.error("User details cannot be null, every field must be full");
            throw new ClientException("The order details cannot be empty");
        }

        List<OrderDetailsDTO> newOrder = new ArrayList<>();
        newOrder.add(orderDetailsDTO);

        ClientEntity clientEntity = clientMapper.toEntity(getClientById(id));

        ClientDTO clientDTO = ClientDTO.builder()
                .pendingOrders(newOrder)
                .build();

        BiConsumer<ClientEntity, ClientDTO> updateOrderDetails = updateFieldMap.get(ClientField.ORDER_DETAILS);
        updateOrderDetails.accept(clientEntity, clientDTO);

        return clientMapper.toDTO(clientRepository.save(clientEntity));
    }

    @Override
    public ClientDTO updateClientOrderStatusByClientId(String orderId, OrderStatus newOrderStatus, String clientId) {
        ClientEntity clientEntity = clientMapper.toEntity(getClientById(clientId));

        List<OrderDetailsDTO> orderDetailsDTOList = clientEntity.getPendingOrders();
        for (OrderDetailsDTO orderDetailsDTO : orderDetailsDTOList) {
            if (Objects.equals(orderDetailsDTO.getId(), orderId)) {
                orderDetailsDTO.setOrderStatus(newOrderStatus);
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
            return "Client with id: '" + id + "' was successfully deleted";
        }
        logger.info("Client with id: {} not found", id);
        return "Client with id: '" + id + " not found";
    }

    @Override
    public String deleteOrderDetailsById(String clientId, String orderId) {
        ClientEntity clientEntity = clientRepository.findClientEntityById(clientId).orElse(null);

        if (clientEntity == null) {
            return "User not found";
        }

        List<OrderDetailsDTO> orderDetailsList = clientEntity.getPendingOrders();
        if (orderDetailsList.removeIf(orderDetailsDTO -> Objects.equals(orderDetailsDTO.getId(), orderId))) {
            clientEntity.setPendingOrders(orderDetailsList);
            clientRepository.save(clientEntity);
            return "Order with id: '" + orderId + "' was successfully deleted";
        }

        return "Order with id: '" + orderId + "' not found";
    }
}
