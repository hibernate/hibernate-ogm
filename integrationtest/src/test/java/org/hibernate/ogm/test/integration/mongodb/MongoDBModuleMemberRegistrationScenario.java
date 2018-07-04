/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.mongodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;

import org.hibernate.ogm.compensation.ErrorHandler;
import org.hibernate.ogm.compensation.operation.CreateTuple;
import org.hibernate.ogm.compensation.operation.GridDialectOperation;
import org.hibernate.ogm.compensation.operation.OperationType;
import org.hibernate.ogm.test.integration.mongodb.errorhandler.TestErrorHandler;
import org.hibernate.ogm.test.integration.mongodb.model.EmailAddress;
import org.hibernate.ogm.test.integration.mongodb.model.PhoneNumber;
import org.hibernate.ogm.test.integration.mongodb.service.ContactManagementService;
import org.hibernate.ogm.test.integration.mongodb.service.PhoneNumberService;
import org.hibernate.ogm.test.integration.testcase.ModuleMemberRegistrationScenario;
import org.hibernate.ogm.test.integration.testcase.model.Member;
import org.hibernate.ogm.test.integration.testcase.util.ModuleMemberRegistrationDeployment;

import org.junit.Test;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;

public class MongoDBModuleMemberRegistrationScenario extends ModuleMemberRegistrationScenario {

	@Inject
	protected PhoneNumberService phoneNumberService;

	@Inject
	protected ContactManagementService contactManager;

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
			Iterator<ErrorHandler.RollbackContext> onRollbackInvocations = TestErrorHandler.getOnRollbackInvocations().iterator();
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

	protected static Archive<?> createTestArchiveFromPersistenceXml(PersistenceDescriptor persistenceXml) {
		return new ModuleMemberRegistrationDeployment
				.Builder( MongoDBModuleMemberRegistrationIT.class )
				.addClasses( PhoneNumber.class, PhoneNumberService.class, EmailAddress.class, ContactManagementService.class, TestErrorHandler.class, MongoDBModuleMemberRegistrationScenario.class )
				.persistenceXml( persistenceXml )
				.manifestDependencies( "org.hibernate.ogm:${module-slot.org.hibernate.ogm.short-id} services, org.hibernate.ogm.mongodb:${module-slot.org.hibernate.ogm.short-id} services" )
				.createDeployment();
	}

	protected static boolean isNotNull(String mongoHostName) {
		return mongoHostName != null && mongoHostName.length() > 0 && ! "null".equals( mongoHostName );
	}
}
