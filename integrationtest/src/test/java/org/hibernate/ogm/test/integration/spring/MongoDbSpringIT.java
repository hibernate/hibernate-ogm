package org.hibernate.ogm.test.integration.spring;

import java.util.List;

import javax.transaction.Transactional;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.XADataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test for the Hibernate OGM module in Spring Framework using MongoDB
 *
 * @author Bato-Bair Tsyrenov &lt;bbtsyrenov@gmail.com&gt;
 */

@RunWith(SpringRunner.class)
@SpringBootTest
@EnableAutoConfiguration(exclude = { HibernateJpaAutoConfiguration.class, XADataSourceAutoConfiguration.class, DataSourceAutoConfiguration.class })
public class MongoDbSpringIT {

	@Autowired
	PersonRepository personRepository;

	@Transactional
	@Test
	public void doTest() {

		List<Person> family = Person.getFamily();
		assertNotNull( family );
		assertNotNull( personRepository );
		personRepository.save( family );
		Person wife = personRepository.findOne( 1l);
		Person maybeTom = null;
		for ( Person p : wife.getChildren() ) {
			if ( p.getName().equals( "Tom" ) ) {
				maybeTom = p;
			}
		}

		assertNotNull( maybeTom );
		assertEquals( "Mary", maybeTom.getSpouse().getName() );
	}
}
