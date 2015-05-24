package org.topbraid.shacl.constraints.sparql;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.topbraid.shacl.constraints.ConstraintExecutable;
import org.topbraid.shacl.constraints.ExecutionLanguage;
import org.topbraid.shacl.constraints.ModelConstraintValidator;
import org.topbraid.shacl.constraints.NativeConstraintExecutable;
import org.topbraid.shacl.constraints.SHACLException;
import org.topbraid.shacl.constraints.TemplateConstraintExecutable;
import org.topbraid.shacl.entailment.SPARQLEntailment;
import org.topbraid.shacl.model.SHACLArgument;
import org.topbraid.shacl.model.SHACLConstraint;
import org.topbraid.shacl.model.SHACLFactory;
import org.topbraid.shacl.model.SHACLShape;
import org.topbraid.shacl.model.SHACLTemplate;
import org.topbraid.shacl.vocabulary.SHACL;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.statistics.SPINStatistics;
import org.topbraid.spin.statistics.SPINStatisticsManager;
import org.topbraid.spin.system.SPINLabels;
import org.topbraid.spin.util.JenaUtil;
import org.topbraid.spin.util.SPINUtil;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * Utilities to operate on SPARQL-based SHACL constraints.
 * 
 * @author Holger Knublauch
 */
public class SPARQLExecutionLanguage implements ExecutionLanguage {
	
	private static SPARQLExecutionLanguage singleton = new SPARQLExecutionLanguage();
	
	public static SPARQLExecutionLanguage get() {
		return singleton;
	}
 
	
	@Override
	public boolean canExecuteNative(NativeConstraintExecutable executable) {
		return executable.getResource().hasProperty(SHACL.sparql);
	}


	@Override
	public boolean canExecuteTemplate(TemplateConstraintExecutable executable) {
		return executable.getTemplate().hasProperty(SHACL.sparql);
	}


	@Override
	public void executeNative(Dataset dataset, Resource shape, Resource shapesGraph,
			Model results, SHACLConstraint constraint,
			Resource focusNode, Property selectorProperty, Resource selectorObject, NativeConstraintExecutable executable) {
		
		Resource entailment = JenaUtil.getResourceProperty(constraint, SHACL.sparqlEntailment);
		dataset = SPARQLEntailment.get().withEntailment(dataset, entailment);
		if(dataset == null) {
			addEntailmentError(entailment, constraint, results);
			return;
		}
		
		Resource resource = executable.getResource();
		String sparql = JenaUtil.getStringProperty(resource, SHACL.sparql);
		if(sparql == null) {
			String message = "Missing " + SHACL.PREFIX + ":" + SHACL.sparql.getLocalName() + " of " + SPINLabels.get().getLabel(resource);
			if(resource.isAnon()) {
				StmtIterator it = resource.getModel().listStatements(null, null, resource);
				if(it.hasNext()) {
					Statement s = it.next();
					it.close();
					message += " at " + SPINLabels.get().getLabel(s.getSubject());
					message += " via " + SPINLabels.get().getLabel(s.getPredicate());
				}
			}
			throw new SHACLException(message);
		}

		String queryString = ARQFactory.get().createPrefixDeclarations(resource.getModel()) + sparql;
		Query query;
		try {
			query = ARQFactory.get().createQuery(queryString);
		}
		catch(QueryParseException ex) {
			throw new SHACLException("Invalid SPARQL constraint (" + ex.getLocalizedMessage() + "):\n" + queryString);
		}

		if(!query.isSelectType()) {
			throw new IllegalArgumentException("SHACL constraints must be SELECT queries");
		}
		
		QuerySolutionMap bindings = new QuerySolutionMap();
		List<SHACLShape> scopes = executable.getScopes();
		for(Resource scope : JenaUtil.getResourceProperties(shape, SHACL.scopeShape)) {
			scopes.add(SHACLFactory.asShape(scope));
		}
		if(selectorProperty != null) {
			query = insertThisAndScopeBindingClause(query, scopes.size(), selectorProperty);
			bindings.add(SPINUtil.TYPE_CLASS_VAR_NAME, selectorObject);
		}
		else if(!scopes.isEmpty()) {
			query = insertScopeClause(query, scopes.size());
		}

		if(focusNode != null) {
			bindings.add(SHACL.thisVar.getVarName(), focusNode);
		}
		bindings.add(SHACL.currentShapeVar.getVarName(), shape);
		bindings.add(SHACL.shapesGraphVar.getVarName(), shapesGraph);
		for(int i = 0; i < scopes.size(); i++) {
			bindings.add(ModelConstraintValidator.SCOPE_VAR_NAME + i, scopes.get(i));
		}
		
		QueryExecution qexec = ARQFactory.get().createQueryExecution(query, dataset, bindings);

		long startTime = System.currentTimeMillis();
		int violationCount = executeSelectQuery(results, constraint, focusNode, executable, qexec);
		if(SPINStatisticsManager.get().isRecording()) {
			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;
			String label = "SHACL SPARQL Constraint (" + violationCount + " violations)";
			SPINStatistics stats = new SPINStatistics(label, sparql, duration, startTime, 
					focusNode != null ? focusNode.asNode() : resource.asNode());
			SPINStatisticsManager.get().add(Collections.singletonList(stats));
		}
	}


	@Override
	public void executeTemplate(Dataset dataset, Resource shape, Resource shapesGraph,
			Model results, SHACLConstraint constraint,
			Resource focusNode, Property selectorProperty, Resource selectorObject, TemplateConstraintExecutable executable) {
		
		Resource entailment = JenaUtil.getResourceProperty(executable.getTemplate(), SHACL.sparqlEntailment);
		dataset = SPARQLEntailment.get().withEntailment(dataset, entailment);
		if(dataset == null) {
			addEntailmentError(entailment, constraint, results);
			return;
		}

		SHACLTemplate template = executable.getTemplate();
		String sparql = JenaUtil.getStringProperty(template, SHACL.sparql);

		String queryString = ARQFactory.get().createPrefixDeclarations(template.getModel()) + sparql;
		Query query;
		try {
			query = ARQFactory.get().createQuery(queryString);
		}
		catch(QueryParseException ex) {
			throw new SHACLException("Invalid SPARQL constraint (" + ex.getLocalizedMessage() + "):\n" + queryString);
		}

		if(!query.isSelectType()) {
			throw new IllegalArgumentException("SHACL constraints must be SELECT queries");
		}
		
		QuerySolutionMap bindings = new QuerySolutionMap();
		List<SHACLShape> scopes = executable.getScopes();
		for(Resource scope : JenaUtil.getResourceProperties(shape, SHACL.scopeShape)) {
			scopes.add(SHACLFactory.asShape(scope));
		}
		if(selectorProperty != null) {
			query = insertThisAndScopeBindingClause(query, scopes.size(), selectorProperty);
			bindings.add(SPINUtil.TYPE_CLASS_VAR_NAME, selectorObject);
		}
		else if(!scopes.isEmpty()) {
			query = insertScopeClause(query, scopes.size());
		}

		for(SHACLArgument arg : template.getArguments(false)) {
			Statement s = constraint.getProperty(arg.getPredicate());
			if(s != null) {
				bindings.add(arg.getVarName(), s.getObject());
			}
			else {
				RDFNode defaultValue = arg.getDefaultValue();
				if(defaultValue != null) {
					bindings.add(arg.getVarName(), defaultValue);
				}
			}
		}
		if(focusNode != null) {
			bindings.add(SHACL.thisVar.getVarName(), focusNode);
		}
		bindings.add(SHACL.currentShapeVar.getVarName(), shape);
		bindings.add(SHACL.shapesGraphVar.getVarName(), shapesGraph);
		for(int i = 0; i < scopes.size(); i++) {
			bindings.add(ModelConstraintValidator.SCOPE_VAR_NAME + i, scopes.get(i));
		}
		
		QueryExecution qexec = ARQFactory.get().createQueryExecution(query, dataset, bindings);

		long startTime = System.currentTimeMillis();
		int violationCount = executeSelectQuery(results, constraint, focusNode, executable, qexec);
		if(SPINStatisticsManager.get().isRecording()) {
			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;
			String label = SPINLabels.get().getLabel(template) + " (" + violationCount + " violations)";
			Iterator<String> varNames = bindings.varNames();
			if(varNames.hasNext()) {
				sparql += "\nBindings:";
				while(varNames.hasNext()) {
					String varName = varNames.next();
					sparql += "\n- ?" + varName + ": " + bindings.get(varName);
				}
			}
			SPINStatistics stats = new SPINStatistics(label, sparql, duration, startTime, 
					focusNode != null ? focusNode.asNode() : template.asNode());
			SPINStatisticsManager.get().add(Collections.singletonList(stats));
		}
	}
	
	
	private void addEntailmentError(Resource entailment, Resource constraint, Model results) {
		Resource error = results.createResource(SHACL.FatalError);
		error.addProperty(SHACL.message, "Unsupported SPARQL entailment " + entailment);
		error.addProperty(SHACL.source, constraint);
	}


	private static int executeSelectQuery(Model results, SHACLConstraint constraint,
			Resource focusNode, ConstraintExecutable executable,
			QueryExecution qexec) {
		
	
		ResultSet rs = qexec.execSelect();
		int violationCount = 0;
		Resource severity = executable.getSeverity();
		List<Literal> defaultMessages = executable.getMessages();
		while(rs.hasNext()) {
			QuerySolution sol = rs.next();
			
			Resource vio = results.createResource(severity);
			vio.addProperty(SHACL.source, constraint);
			
			RDFNode selectMessage = sol.get(SHACL.messageVar.getVarName());
			if(selectMessage != null) {
				vio.addProperty(SHACL.message, selectMessage);
			}
			else {
				for(Literal defaultMessage : defaultMessages) {
					vio.addProperty(SHACL.message, withSubstitutions(defaultMessage, sol));
				}
			}
			
			RDFNode selectPath = sol.get(SHACL.predicateVar.getVarName());
			if(selectPath instanceof Resource) {
				vio.addProperty(SHACL.predicate, selectPath);
			}
			else {
				Resource path = executable.getPredicate();
				if(path != null) {
					vio.addProperty(SHACL.predicate, path);
				}
			}
			
			RDFNode selectObject = sol.get(SHACL.objectVar.getVarName());
			if(selectObject != null) {
				vio.addProperty(SHACL.object, selectObject);
			}
			
			RDFNode selectSubject = sol.get(SHACL.subjectVar.getVarName());
			if(selectSubject instanceof Resource) {
				vio.addProperty(SHACL.subject, selectSubject);
			}
			
			RDFNode root = sol.get(SHACL.rootVar.getVarName());
			if(root != null) {
				vio.addProperty(SHACL.root, root);
			}
			else {
				root = sol.get(SHACL.thisVar.getVarName());
				if(root != null) {
					vio.addProperty(SHACL.root, root);
				}
			}
	
			violationCount++;
		}
		qexec.close();
		
		return violationCount;
	}
	
	
	// TODO: Algorithm incorrect, e.g. if { is included as a comment
	private Query insertScopeClause(Query query, int scopeCount) {
		String str = query.toString();
		Pattern pattern = Pattern.compile("(?i)WHERE\\s*\\{");
		Matcher matcher = pattern.matcher(str);
		if(matcher.find()) {
			int index = matcher.end();
			StringBuilder sb = new StringBuilder(str);
			
			StringBuffer s = new StringBuffer();
			for(int i = 0; i < scopeCount; i++) {
				s.append("{ FILTER <");
				s.append(SHACL.hasShape.getURI());
				s.append(">(?this, ?");
				s.append(ModelConstraintValidator.SCOPE_VAR_NAME + i);
				s.append(", ?" + SHACL.shapesGraphVar.getVarName() + ") }");
			}
			
			sb.insert(index, s.toString());
			return ARQFactory.get().createQuery(sb.toString());
		}
		else {
			throw new IllegalArgumentException("Cannot find first '{' in query string: " + str);
		}
	}
	
	
	// TODO: Algorithm incorrect, e.g. if { is included as a comment
	private Query insertThisAndScopeBindingClause(Query query, int scopeCount, Property selectorProperty) {
		String str = query.toString();
		Pattern pattern = Pattern.compile("(?i)WHERE\\s*\\{");
		Matcher matcher = pattern.matcher(str);
		if(matcher.find()) {
			int index = matcher.end();
			StringBuilder sb = new StringBuilder(str);
			
			// TODO: Maybe we can do a rdfs:subClassOf* traversal here
			
			StringBuffer s = new StringBuffer();
			s.append(" {?this <" + selectorProperty + "> ?");
			s.append(SPINUtil.TYPE_CLASS_VAR_NAME);
			for(int i = 0; i < scopeCount; i++) {
				s.append(" . FILTER <");
				s.append(SHACL.hasShape.getURI());
				s.append(">(?this, ?");
				s.append(ModelConstraintValidator.SCOPE_VAR_NAME + i);
				s.append(", ?" + SHACL.shapesGraphVar.getVarName() + ")");
			}
			s.append(" }. ");
			
			sb.insert(index, s.toString());
			return ARQFactory.get().createQuery(sb.toString());
		}
		else {
			throw new IllegalArgumentException("Cannot find first '{' in query string: " + str);
		}
	}


	private static Literal withSubstitutions(Literal template, QuerySolution bindings) {
		StringBuffer buffer = new StringBuffer();
		String labelTemplate = template.getLexicalForm();
		for(int i = 0; i < labelTemplate.length(); i++) {
			if(i < labelTemplate.length() - 3 && labelTemplate.charAt(i) == '{' && labelTemplate.charAt(i + 1) == '?') {
				int varEnd = i + 2;
				while(varEnd < labelTemplate.length()) {
					if(labelTemplate.charAt(varEnd) == '}') {
						String varName = labelTemplate.substring(i + 2, varEnd);
						RDFNode varValue = bindings.get(varName);
						if(varValue instanceof Resource) {
							buffer.append(SPINLabels.get().getLabel((Resource)varValue));
						}
						else if(varValue instanceof Literal) {
							buffer.append(varValue.asNode().getLiteralLexicalForm());
						}
						break;
					}
					else {
						varEnd++;
					}
				}
				i = varEnd;
			}
			else {
				buffer.append(labelTemplate.charAt(i));
			}
		}
		if(template.getLanguage().isEmpty()) {
			return ResourceFactory.createTypedLiteral(buffer.toString());
		}
		else {
			return ResourceFactory.createLangLiteral(buffer.toString(), template.getLanguage());
		}
	}
}