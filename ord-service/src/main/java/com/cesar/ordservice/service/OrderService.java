package com.cesar.ordservice.service;

import com.cesar.ordservice.dto.OrderDTO;

public interface OrderService {
    OrderDTO getOrderById(String id);
    OrderDTO createOrder(OrderDTO orderDTO);
}
