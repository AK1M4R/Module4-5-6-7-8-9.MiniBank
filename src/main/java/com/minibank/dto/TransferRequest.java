package com.minibank.dto;

import java.math.BigDecimal;

public class TransferRequest {
    public Long fromAccountId;
    public Long toAccountId;
    public BigDecimal amount;
}
