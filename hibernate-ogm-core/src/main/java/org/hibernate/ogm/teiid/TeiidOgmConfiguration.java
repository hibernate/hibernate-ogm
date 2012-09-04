package org.hibernate.ogm.teiid;

import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.ogm.cfg.impl.OgmNamingStrategy;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactory;
import org.teiid.deployers.VirtualDatabaseException;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository.ConnectorManagerException;
import org.teiid.translator.TranslatorException;

public class TeiidOgmConfiguration extends Configuration {
	public static final String OGM_ON = "hibernate.ogm._activate";
	
	public TeiidOgmConfiguration() {
		super();
		resetOgm();
	}

	private void resetOgm() {
		super.setNamingStrategy( OgmNamingStrategy.INSTANCE );
		setProperty( OGM_ON, "true" );
	}

	@Override
	public Configuration setProperties(Properties properties) {
		super.setProperties( properties );
		//Unless the new configuration properties explicitly disable OGM,
		//assume there was no intention to disable it:
		if ( ! properties.containsKey( OGM_ON ) ) {
			setProperty( OGM_ON, "true" );
		}
		return this;
	}
	
	@Override
	public SessionFactory buildSessionFactory() throws HibernateException {
		SessionFactoryImpl sf = (SessionFactoryImpl)super.buildSessionFactory() ;
		try {
			((TeiidConnectionProvider)sf.getConnectionProvider()).addVDB(getTableMappings(), sf);
		} catch (VirtualDatabaseException e) {
			throw new HibernateException(e);
		} catch (TranslatorException e) {
			throw new HibernateException(e);
		} catch (ConnectorManagerException e) {
			throw new HibernateException(e);
		}
		return sf;
	}
}
