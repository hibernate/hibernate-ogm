package org.hibernate.ogm.test.integration.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.XADataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@SpringBootApplication
@EnableJpaRepositories("org.hibernate.ogm.test.integration.spring")
@EnableAutoConfiguration(exclude = {HibernateJpaAutoConfiguration.class,  XADataSourceAutoConfiguration.class, DataSourceAutoConfiguration.class})
public class SpringBootTestApp {

	public static void main(String[] args) {
		SpringApplication.run( SpringBootTestApp.class );
	}

	@Bean
	LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();
		bean.setPersistenceUnitName( "mongodb-local" );
		return bean;
	}
}
