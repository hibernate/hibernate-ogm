package org.hibernate.ogm.test.simpleentity;

import java.io.InputStream;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.ogm.dialect.NoopDialect;
import org.hibernate.ogm.jdbc.NoopConnectionProvider;
import org.hibernate.ogm.metadata.GridMetadataManager;
import org.hibernate.testing.junit.functional.annotations.HibernateTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;

/**
 * A base class for all OGM tests.
 *
 * @author Emmnauel Bernand
 * @author Hardy Ferentschik
 */
public abstract class OgmTestCase extends HibernateTestCase {

	protected static SessionFactory sessions;
	private Session session;

	public OgmTestCase() {
		super();
	}

	public OgmTestCase(String name) {
		super( name );
	}

	public Session openSession() throws HibernateException {
		rebuildSessionFactory();
		session = getSessions().openSession();
		return session;
	}

	public Session openSession(Interceptor interceptor) throws HibernateException {
		rebuildSessionFactory();
		session = getSessions().openSession( interceptor );
		return session;
	}

	private void rebuildSessionFactory() {
		if ( sessions == null ) {
			try {
				buildConfiguration();
			}
			catch ( Exception e ) {
				throw new HibernateException( e );
			}
		}
	}

	protected void setSessions(SessionFactory sessions) {
		OgmTestCase.sessions = sessions;
	}

	protected SessionFactory getSessions() {
		return sessions;
	}

	protected SessionFactoryImplementor sfi() {
		return (SessionFactoryImplementor) getSessions();
	}

	//FIXME clear cache when this happens
	protected void runSchemaGeneration() {

	}

	//FIXME clear cache when this happens
	protected void runSchemaDrop() {
		
	}

	@Override
	protected void buildConfiguration() throws Exception {
		if ( getSessions() != null ) {
			getSessions().close();
		}
		try {
			setCfg( new Configuration() );

			//Grid specific configuration
			cfg.setProperty( Environment.DIALECT, NoopDialect.class.getName() );
			cfg.setSessionFactoryObserver( new GridMetadataManager() );
			cfg.setProperty( Environment.CONNECTION_PROVIDER, NoopConnectionProvider.class.getName() );


			//Other configurations
			// by default use the new id generator scheme...
			cfg.setProperty( Configuration.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
			configure( cfg );
			if ( recreateSchema() ) {
				cfg.setProperty( Environment.HBM2DDL_AUTO, "none" );
			}
			for ( String aPackage : getAnnotatedPackages() ) {
				getCfg().addPackage( aPackage );
			}
			for ( Class<?> aClass : getAnnotatedClasses() ) {
				getCfg().addAnnotatedClass( aClass );
			}
			for ( String xmlFile : getXmlFiles() ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				getCfg().addInputStream( is );
			}
			setSessions( getCfg().buildSessionFactory( /* new TestInterceptor() */ ) );
		}
		catch ( Exception e ) {
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	protected void handleUnclosedResources() {
		if ( session != null && session.isOpen() ) {
			if ( session.isConnected() ) {
				session.doWork( new RollbackWork() );
			}
			session.close();
			session = null;
			fail( "unclosed session" );
		}
		else {
			session = null;
		}
	}

	@Override
	protected void closeResources() {
		try {
			if ( session != null && session.isOpen() ) {
				if ( session.isConnected() ) {
					session.doWork( new RollbackWork() );
				}
				session.close();
			}
		}
		catch ( Exception ignore ) {
		}
		try {
			if ( sessions != null ) {
				sessions.close();
				sessions = null;
			}
		}
		catch ( Exception ignore ) {
		}
	}
}
