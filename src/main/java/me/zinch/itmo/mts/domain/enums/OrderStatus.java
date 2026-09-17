package me.zinch.itmo.mts.domain.enums;

public enum OrderStatus {
    NEW, // Пользователь создал заказ
    REJECTED, // Пользователь отказался от заказа
    APPROVED, // Пользователь согласился на заказ
    PLACED, // Ссылка оплаты создана, ожидается оплата
    PAID, // YooKassa подтвердила успешную оплату
    SERVICES_ACTIVATED // Услуги переданы в EIS Kill Bill и активированы
}
