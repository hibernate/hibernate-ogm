/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.logging.impl;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.IOException;
import java.util.Set;

import org.hibernate.HibernateException;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log messages and exceptions of the Infinispan Remote dialect.
 * The id range reserved for this dialect is 1701-1800.
 */
@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {

	@Message(id = 1701, value = "The Hot Rod client configuration was not defined")
	HibernateException hotrodClientConfigurationMissing();

	@Message(id = 1702, value = "Could not load the Hot Rod client configuration properties")
	HibernateException failedLoadingHotRodConfigurationProperties(@Cause IOException e);

	@Message(id = 1703, value = "Protobuf schema '%s' successfully deployed")
	@LogMessage(level = INFO)
	void successfulSchemaDeploy(String protobufName);

	@Message(id = 1704, value = "Error deploying Protobuf schema '%s' to the server")
	HibernateException errorAtSchemaDeploy(String generatedProtobufName, @Cause Exception schemaDeployError);

	@Message(id = 1705, value = "Generated schema: \n===========\n%s\n===========\n")
	@LogMessage(level = INFO)
	void generatedSchema(String fullSchema);

	@Message(id = 1706, value = "Can not read value '%d' as a char for protobuf field '%s' as it's out of range for a Short type")
	HibernateException truncatingShortOnRead(int readInt, String name);

	@Message(id = 1707, value = "Requested value for an unknown sequence on table '%s', segment '%s'")
	HibernateException valueRequestedForUnknownSequence(String table, String columnValue);

	@Message(id = 1708, value = "Error during parse of Protobuf schema")
	HibernateException errorAtProtobufParsing(@Cause Exception e);

	@Message(id = 1710, value = "The remote caches '%s' were expected to exist but are not defined on the server")
	HibernateException expectedCachesNotDefined(@FormatWith(StringSetFormatter.class) Set<String> cacheNames);

	@Message(id = 1711, value = "This domain model would cause table '%s' to be generated without a primary key." +
			"This is not supported on an Infinispan Remote dialect: check that your embedded collections have a proper ordering definition." )
	HibernateException tableHasNoPrimaryKey(String tableName);

	@Message(id = 1712, value = "Sequence generator '%s' has been retrying optimistic CAS operations 10 times. "
			+ "This is considered high contention and has a significant performance impact;"
			+ " consider using a different id type, like UUID or application assigned.")
	@LogMessage(level = WARN)
	void excessiveCasForSequencer(String segmentName);

	@Message(id = 1713, value = "This thread was interrupted while in a CAS loop to generate a unique sequence number" )
	HibernateException interruptedDuringCASSequenceGeneration();

	@Message(id = 1714, value = "A remote read returned null while this entry was definitely initialized before. Possible data loss on the Infinispan server?" )
	HibernateException criticalDataLossDetected();

	@Message(id = 1715, value = "Property <%s> has to be set to <%s> but it's set to <%s>" )
	HibernateException invalidConfigurationValue(String property, String expectedValue, String actualValue);

	@Message(id = 1716, value = "Error deploying Protobuf schema '%s' to the server: '%s'")
	HibernateException errorAtSchemaDeploy(String generatedProtobufName, String remoteErrorMessage);

	@Message(id = 1717, value = "Invalid Proto file name <%s>. Proto file name should match the pattern: *.proto")
	HibernateException invalidProtoFileName(String protoFileName);

	@Message(id = 1718, value = "The remote cache configurations '%s' were expected to exist but are not defined on the server")
	HibernateException expectedCacheConfiguratiosNotDefined(@FormatWith(StringSetFormatter.class) Set<String> cacheConfigurationNames);

	@Message(id = 1719, value = "Error during caches start phase")
	HibernateException errorAtCachesStart(@Cause Exception cause);
}
