/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.wildfly.mongodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.compensation.ErrorHandler.RollbackContext;
import org.hibernate.ogm.compensation.operation.CreateTuple;
import org.hibernate.ogm.compensation.operation.GridDialectOperation;
import org.hibernate.ogm.compensation.operation.OperationType;
import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.test.integration.wildfly.mongodb.errorhandler.TestErrorHandler;
import org.hibernate.ogm.test.integration.wildfly.mongodb.model.EmailAddress;
import org.hibernate.ogm.test.integration.wildfly.mongodb.model.PhoneNumber;
import org.hibernate.ogm.test.integration.wildfly.mongodb.service.ContactManagementService;
import org.hibernate.ogm.test.integration.wildfly.mongodb.service.PhoneNumberService;
import org.hibernate.ogm.test.integration.wildfly.testcase.ModuleMemberRegistrationScenario;
import org.hibernate.ogm.test.integration.wildfly.testcase.model.Member;
import org.hibernate.ogm.test.integration.wildfly.testcase.util.ModuleMemberRegistrationDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;
import org.junit.Test;
import org.junit.runner.RunWith;
/**
 * Test for the Hibernate OGM module in WildFly using MongoDB
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
@RunWith(Arquillian.class)
public class MongoDBModuleMemberRegistrationIT extends ModuleMemberRegistrationScenario {

	@Inject
	private PhoneNumberService phoneNumberService;

	@Inject
	private ContactManagementService contactManager;

	@Deployment
	public static Archive<?> createTestArchive() {
		return new ModuleMemberRegistrationDeployment
				.Builder( MongoDBModuleMemberRegistrationIT.class )
				.addClasses( PhoneNumber.class, PhoneNumberService.class, EmailAddress.class, ContactManagementService.class, TestErrorHandler.class )
				.persistenceXml( persistenceXml() )
				.manifestDependencies( "org.hibernate.ogm:${hibernate-ogm.module.slot} services, org.hibernate.ogm.mongodb:${hibernate-ogm.module.slot} services" )
				.createDeployment();
	}

	private static PersistenceDescriptor persistenceXml() {
		String host = System.getenv( "MONGODB_HOSTNAME" );
		String port = System.getenv( "MONGODB_PORT" );
		String username = System.getenv( "MONGODB_USERNAME" );
		String password = System.getenv( "MONGODB_PASSWORD" );

		Properties<PersistenceUnit<PersistenceDescriptor>> propertiesContext = Descriptors.create( PersistenceDescriptor.class )
				.version( "2.0" )
				.createPersistenceUnit()
				.name( "primary" )
				.provider( "org.hibernate.ogm.jpa.HibernateOgmPersistence" )
				.getOrCreateProperties();
		if ( isNotNull( host ) ) {
			propertiesContext.createProperty().name( OgmProperties.HOST ).value( host );
		}
		if ( isNotNull( port ) ) {
			propertiesContext.createProperty().name( OgmProperties.PORT ).value( port );
		}
		if ( isNotNull( username ) ) {
			propertiesContext.createProperty().name( OgmProperties.USERNAME ).value( username );
		}
		if ( isNotNull( password ) ) {
			propertiesContext.createProperty().name( OgmProperties.PASSWORD ).value( password );
		}
		return propertiesContext
					.createProperty().name( OgmProperties.DATASTORE_PROVIDER ).value( MongoDB.DATASTORE_PROVIDER_NAME ).up()
					.createProperty().name( OgmProperties.DATABASE ).value( "ogm_test_database" ).up()
					.createProperty().name( OgmProperties.CREATE_DATABASE ).value( "true" ).up()
					.createProperty().name( OgmProperties.ERROR_HANDLER ).value( TestErrorHandler.class.getName() ).up()
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( "ram" ).up()
					.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( "org.hibernate.search.orm:${hibernate-search.module.slot}" ).up()
				.up().up();
	}

	private static boolean isNotNull(String mongoHostName) {
		return mongoHostName != null && mongoHostName.length() > 0 && ! "null".equals( mongoHostName );
	}

	@Test
	public void shouldFindPersistedMemberByIdWithNativeQuery() throws Exception {
		Member newMember = memberRegistration.getNewMember();
		newMember.setName( "Peter O'Tall" );
		memberRegistration.register();

		String nativeQuery = "{ _id: " + newMember.getId() + " }";
		Member found = memberRegistration.findWithNativeQuery( nativeQuery );

		assertNotNull( "Expected at least one result using a native query", found );
		assertEquals( "Native query hasn't found a new member", newMember.getName(), found.getName() );
	}

	@Test
	public void shouldPersistAndFindEntityUsingObjectId() {
		phoneNumberService.createPhoneNumber( "Bob", "123-456" );
		phoneNumberService.createPhoneNumber( "Mortimer", "789-123" );

		assertEquals( "789-123", phoneNumberService.getPhoneNumber( "Mortimer" ).getValue() );
	}

	@Test
	public void shouldInvokeRegisteredErrorHandler() {
		List<PhoneNumber> phoneNumbers = Arrays.asList( new PhoneNumber( "Bob", "123-456" ) );
		List<EmailAddress> emailAddresses = Arrays.asList(
				new EmailAddress( "email-1", "Bob", "bob@example.com" ),
				new EmailAddress( "email-1", "Sarah", "sarah@example.com" )
		);

		try {
			contactManager.persistContacts( phoneNumbers, emailAddresses );
			fail( "Expected exception was not raised" );
		}
		catch (Exception e) {
			Iterator<RollbackContext> onRollbackInvocations = TestErrorHandler.getOnRollbackInvocations().iterator();
			Iterator<GridDialectOperation> appliedOperations = onRollbackInvocations.next().getAppliedGridDialectOperations().iterator();
			assertFalse( onRollbackInvocations.hasNext() );

			// The phone no. insertion should have been applied
			GridDialectOperation operation = appliedOperations.next();
			assertEquals( OperationType.CREATE_TUPLE, operation.getType() );
			assertEquals( "PhoneNumber", operation.as( CreateTuple.class ).getEntityKeyMetadata().getTable() );

			operation = appliedOperations.next();
			assertEquals( OperationType.INSERT_TUPLE, operation.getType() );

			assertFalse( appliedOperations.hasNext() );
		}
	}
}
