/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.id.impl;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.GetGeneratedKeysDelegate;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.ogm.dialect.identity.spi.IdentityColumnAwareGridDialect;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.spi.GridDialect;

/**
 * Define what kind of identity generation the {@link GridDialect} support.
 *
 * @see IdentityColumnAwareGridDialect
 * @see IdentityColumnSupportImpl
 * @author Davide D'Alto
 */
public class OgmIdentityColumnSupport implements IdentityColumnSupport {

	public final boolean supportIdGenerationDuringInsert;

	public OgmIdentityColumnSupport(GridDialect gridDialect) {
		this.supportIdGenerationDuringInsert =  GridDialects.hasFacet( gridDialect, IdentityColumnAwareGridDialect.class );
	}

	@Override
	public boolean supportsIdentityColumns() {
		return supportIdGenerationDuringInsert;
	}

	@Override
	public boolean supportsInsertSelectIdentity() {
		return false;
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return true;
	}

	@Override
	public String appendIdentitySelectToInsert(String insertString) {
		return insertString;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) throws MappingException {
		throw new MappingException( getClass().getName() + " does not support identity key generation" );
	}

	@Override
	public String getIdentityColumnString(int type) throws MappingException {
		throw new MappingException( getClass().getName() + " does not support identity key generation" );
	}

	@Override
	public String getIdentityInsertString() {
		return null;
	}

	@Override
	public GetGeneratedKeysDelegate buildGetGeneratedKeysDelegate(
			PostInsertIdentityPersister persister,
			Dialect dialect) {
		return new GetGeneratedKeysDelegate( persister, dialect );
	}
}
