package org.hibernate.ogm.teiid;

import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.impl.GridDialectFactory;
import org.teiid.language.Command;
import org.teiid.language.QueryExpression;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.ResultSetExecution;
import org.teiid.translator.Translator;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.UpdateExecution;

@Translator(name="grid-translator")
public class GridExecutionFactory extends ExecutionFactory<GridDialectFactory, GridDialect> {

	@Override
	public GridDialect getConnection(GridDialectFactory factory, ExecutionContext executionContext) throws TranslatorException {
		if (factory != null) {
			return factory.buildGridDialect();
		}
		throw new TranslatorException("No Grid Dialect Found");
	}

	@Override
	public void closeConnection(GridDialect connection, GridDialectFactory factory) {
		//close??
	}

	@Override
	public ResultSetExecution createResultSetExecution(QueryExpression command,
			ExecutionContext executionContext, RuntimeMetadata metadata,
			GridDialect connection) throws TranslatorException {
		return new GridResultSetExecution(command, executionContext, metadata, connection);
	}

	@Override
	public UpdateExecution createUpdateExecution(Command command, ExecutionContext executionContext, RuntimeMetadata metadata, GridDialect connection) throws TranslatorException {
		return new GridUpdateExecution(command, executionContext, metadata, connection);
	}

	@Override
	public boolean isSourceRequired() {
		return true;
	}

	@Override
	public boolean isSourceRequiredForMetadata() {
		return false;
	}

	@Override
	public boolean supportsCompareCriteriaEquals() {
		return true;
	}

	@Override
	public boolean supportsInCriteria() {
		return true;
	}

}
