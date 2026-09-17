package me.zinch.itmo.mts.service.reporting;

import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WeeklyReportEmailService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.analyst-email}")
    private String analystEmail;

    public void send(WeeklyOrderReport report) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(analystEmail);
        message.setSubject("Недельный отчёт по заказам");
        message.setText("""
                Период: %s — %s
                Всего создано заказов: %d
                Успешно оплачено (PAID): %d
                Сумма оплаченных заказов: %s
                """.formatted(
                DATE_TIME_FORMAT.format(report.periodFrom()),
                DATE_TIME_FORMAT.format(report.periodTo()),
                report.totalOrders(),
                report.successfulOrders(),
                report.successfulOrdersAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()));
        mailSender.send(message);
    }
}
