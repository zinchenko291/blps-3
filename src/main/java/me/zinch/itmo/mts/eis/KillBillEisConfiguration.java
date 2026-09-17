package me.zinch.itmo.mts.eis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KillBillEisConfiguration {

    @Bean
    public KillBillResourceAdapter killBillResourceAdapter() {
        return new KillBillResourceAdapter();
    }

    @Bean
    public KillBillManagedConnectionFactory killBillManagedConnectionFactory(KillBillProperties properties,
            KillBillResourceAdapter resourceAdapter) {
        KillBillManagedConnectionFactory factory = new KillBillManagedConnectionFactory();
        factory.setResourceAdapter(resourceAdapter);
        factory.setProperties(properties);
        return factory;
    }

    @Bean
    public BillingEisConnectionFactory billingEisConnectionFactory(KillBillManagedConnectionFactory factory) {
        return factory.createConnectionFactory();
    }
}
