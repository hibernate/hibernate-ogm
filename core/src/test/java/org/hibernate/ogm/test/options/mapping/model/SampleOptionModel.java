/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options.mapping.model;

import org.hibernate.ogm.options.navigation.EntityContext;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.options.navigation.PropertyContext;
import org.hibernate.ogm.options.navigation.spi.BaseEntityContext;
import org.hibernate.ogm.options.navigation.spi.BaseGlobalContext;
import org.hibernate.ogm.options.navigation.spi.BasePropertyContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;
import org.hibernate.ogm.test.options.examples.EmbedExampleOption;
import org.hibernate.ogm.test.options.examples.ForceExampleOption;
import org.hibernate.ogm.test.options.examples.NameExampleOption;
import org.hibernate.ogm.test.options.examples.NamedQueryOption;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class SampleOptionModel {

	public static SampleGlobalContext createGlobalContext(ConfigurationContext context) {
		return context.createGlobalContext( SampleGlobalContextImpl.class, SampleEntityContextImpl.class, SamplePropertyContextImpl.class );
	}

	public interface SampleGlobalContext extends GlobalContext<SampleGlobalContext, SampleEntityContext> {

		SampleGlobalContext force(boolean force);

		SampleGlobalContext namedQuery(String name, String hql);

	}

	public interface SampleEntityContext extends EntityContext<SampleEntityContext, SamplePropertyContext> {

		// inherited
		SampleEntityContext force(boolean force);

		SampleEntityContext name(String name);
	}

	public interface SamplePropertyContext extends PropertyContext<SampleEntityContext, SamplePropertyContext> {

		SamplePropertyContext embed(Object object);
	}

	public abstract static class SampleGlobalContextImpl extends BaseGlobalContext<SampleGlobalContext, SampleEntityContext> implements SampleGlobalContext {

		public SampleGlobalContextImpl(ConfigurationContext context) {
			super( context );
		}

		@Override
		public SampleGlobalContext force(boolean force) {
			addGlobalOption( new ForceExampleOption(), force );
			return this;
		}

		@Override
		public SampleGlobalContext namedQuery(String name, String hql) {
			addGlobalOption( new NamedQueryOption( name ), hql );
			return this;
		}
	}

	public abstract static class SampleEntityContextImpl extends BaseEntityContext<SampleEntityContext, SamplePropertyContext> implements SampleEntityContext {

		public SampleEntityContextImpl(ConfigurationContext context) {
			super( context );
		}

		@Override
		public SampleEntityContext force(boolean force) {
			addEntityOption( new ForceExampleOption(), force );
			return this;
		}

		@Override
		public SampleEntityContext name(String name) {
			addEntityOption( new NameExampleOption(), name );
			return this;
		}
	}

	public abstract static class SamplePropertyContextImpl extends BasePropertyContext<SampleEntityContext, SamplePropertyContext> implements
			SamplePropertyContext {

		public SamplePropertyContextImpl(ConfigurationContext context) {
			super( context );
		}

		@Override
		public SamplePropertyContext embed(Object object) {
			addPropertyOption( new EmbedExampleOption(), object );
			return this;
		}
	}
}
