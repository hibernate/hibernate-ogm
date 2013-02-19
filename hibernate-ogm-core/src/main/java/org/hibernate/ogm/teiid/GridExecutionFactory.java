package org.hibernate.ogm.teiid;

import org.hibernate.ogm.dialect.GridTranslator;
import org.hibernate.ogm.hibernatecore.impl.OgmSession;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactory;
import org.teiid.language.QueryExpression;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.ResultSetExecution;
import org.teiid.translator.Translator;
import org.teiid.translator.TranslatorException;

@Translator(name="grid-translator")
public class GridExecutionFactory extends GridTranslator<OgmSessionFactory, OgmSession> {

	public GridExecutionFactory() {
		HibernateExecutionFactory delegate = new HibernateExecutionFactory();
		setDelegate(delegate);
	}
	
	static class HibernateExecutionFactory extends ExecutionFactory<OgmSessionFactory, OgmSession> {

		public HibernateExecutionFactory() {
			setSupportedJoinCriteria(SupportedJoinCriteria.THETA);	
			setSourceRequired(true);
			setSourceRequiredForMetadata(false);
		}
		
		@Override
		public OgmSession getConnection(OgmSessionFactory factory, ExecutionContext executionContext) throws TranslatorException {
			return (OgmSession)factory.openSession();
		}

		@Override
		public void closeConnection(OgmSession connection, OgmSessionFactory factory) {
			connection.close();
		}

		@Override
		public ResultSetExecution createResultSetExecution(QueryExpression command, ExecutionContext executionContext, RuntimeMetadata metadata, OgmSession connection)
				throws TranslatorException {
			return new GridResultSetExecution(command, executionContext, metadata, connection);
		}
		
		@Override
		public boolean supportsCompareCriteriaEquals() {
			return true;
		}		
		
		@Override
		public boolean supportsOnlyLiteralComparison() {
			return true;
		}	
		
		@Override
		public boolean supportsInCriteria() {
			return true;
		}
		
		@Override
		public boolean isForkable() {
			return false;
		}		
	}
}
